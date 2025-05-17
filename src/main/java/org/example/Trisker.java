package org.example;

import at.ac.tuwien.ifs.sge.agent.*;
import at.ac.tuwien.ifs.sge.engine.Logger;
import at.ac.tuwien.ifs.sge.game.risk.board.Risk;
import at.ac.tuwien.ifs.sge.game.risk.board.RiskAction;
import at.ac.tuwien.ifs.sge.game.risk.board.RiskBoard;
import at.ac.tuwien.ifs.sge.game.risk.board.RiskTerritory;
import org.example.data.Continent;
import org.example.data.RewardFactors;
import org.example.general.RiskState;
import org.example.log.EventLogService;
import org.example.mcts.UCBLogic;
import org.example.mcts.UCBNode;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.List;
import java.util.regex.Pattern;

//Run command for the game engine:
//java -jar sge-1.0.7-exe.jar match --file=sge-risk-1.0.7-exe.jar --directory=agentstest

public class Trisker extends AbstractGameAgent<Risk, RiskAction>
        implements GameAgent<Risk, RiskAction> {

  private HashMap<Integer, Continent> continents = null;
  private boolean isFirstRound = true;
  private HashSet<Integer> opponentIds = new HashSet<>();
  private static int counter = 0;



  public Trisker(Logger log){
    super(log);
  }

  @Override
  public void setUp(int numberOfPlayers, int playerId) {
    super.setUp(numberOfPlayers, playerId);
    EventLogService.reset();
  }

  private void ownSetup(Risk game)  {
    continents = createContinentsWithAllTerritories(game.getBoard());
    createRewardsByContinent();
  }

  @Override
  public RiskAction computeNextAction(Risk game,
                             long computationTime,
                             TimeUnit timeUnit){
    EventLogService.logBoard("ENEMY", game);
    if(isFirstRound){
      isFirstRound = false;
      ownSetup(game);
    }
    counter++;
    //optionally set AbstractGameAgent timers
    super.setTimers(computationTime, timeUnit);
    if(RiskState.isInitialPlacingPhase(game.getBoard())) {
      UCBNode root = startMCSTree(game);
      UCBLogic.expandAll(root, game.getPossibleActions());

      UCBNode node = root;
      while(!shouldStopComputation()) {
        if(node.getVisits() == 0 && node.getChildren().isEmpty()) {
          double value = startSimulation(node);
          //log.warn(value);
          UCBLogic.backpropagate(node, value);
          node = root;

          EventLogService.logTree(root);
        } else if (node.getChildren().isEmpty()) {
          UCBLogic.expandAll(node, node.getState().getPossibleActions());
          node = UCBLogic.selectBest(node);
          double value = startSimulation(node);
          //log.warn(value);
          UCBLogic.backpropagate(node, value);
          node = root;

          EventLogService.logTree(root);
        } else {
          node = UCBLogic.selectBest(node);
        }
      }
      log.warn("Best one Taken: ");
      log.warn(UCBLogic.calculateUCB(UCBLogic.selectBest(root)));
      RiskAction bestAction = UCBLogic.selectBest(root).getRiskAction();
      EventLogService.logBoard("OWN", (Risk) game.doAction(bestAction).getGame());
      return bestAction;
    } else {
      System.exit(1);
      return List.copyOf(game.getPossibleActions()).get(0);
    }
  }

  private UCBNode startMCSTree(Risk game) {
    return new UCBNode(null, null, game);
  }

  private double startSimulation(UCBNode node) {
    return startGreedySimulation(node);
  }

  private double startRandomSimulation(UCBNode node) {
    Risk game = new Risk(node.getState());
    int cnt = 0;
    while(RiskState.isInitialPlacingPhase(game.getBoard())) {
      RiskAction action = game.getPossibleActions().stream()
              .skip(random.nextInt(game.getPossibleActions().size())).findFirst().get();
      //log.warn(action.toString());
      game = (Risk) game.doAction(action);
      if(game.getCurrentPlayer() != playerId)
        opponentIds.add(game.getCurrentPlayer());
      cnt++;
    }
    return calculateTotalRewardsOfPlayerMinusOpponentsRewards(playerId, game);
  }

  private double startGreedySimulation(UCBNode node) {
    Risk game = new Risk(node.getState());
    int cnt = 0;
    while(RiskState.isInitialPlacingPhase(game.getBoard()) && cnt < 4) {
      HashMap<Integer, Double> territoryRewards = distributeTerritoryRewards(game);
      RiskAction action = findHighestRewardAction(territoryRewards, game.getPossibleActions());
      game = (Risk) game.doAction(action);
      if(game.getCurrentPlayer() != playerId)
        opponentIds.add(game.getCurrentPlayer());
      cnt++;
    }
    return calculateTotalRewardsOfPlayerMinusOpponentsRewards(playerId, game);
  }

  private RiskAction findHighestRewardAction(HashMap<Integer, Double> territoryRewards, Set<RiskAction> possibleActions) {
    RiskAction best = null;
    double bestValue = Double.NEGATIVE_INFINITY;
    int territoryId = 0;
    double territoryReward = 0;
    for(RiskAction action : possibleActions) {
      //The only way to get the target of an action is to extract it from action.toString()
      territoryId = Integer.parseInt(action.toString().split(Pattern.quote(")->"))[1]);
      territoryReward = territoryRewards.get(territoryId);
      if(territoryReward > bestValue || best == null) {
        bestValue = territoryReward;
        best = action;
      }
    }
    return best;
  }

  private double calculateTotalRewardsOfPlayerMinusOpponentsRewards(int playerId, Risk game) {
    double rewards = calculateTotalRewardsOfPlayer(playerId, (Risk) game.getGame(playerId), distributeTerritoryRewards((Risk) game.getGame(playerId)));
    for(Integer opponentId : opponentIds) {
      rewards -= calculateTotalRewardsOfPlayer(opponentId, (Risk) game.getGame(opponentId), distributeTerritoryRewards((Risk) game.getGame(opponentId)));
    }
    return rewards;
  }

  private double calculateTotalRewardsOfPlayer(int playerId, Risk game, HashMap<Integer, Double> territoryRewards) {
    RiskBoard board = game.getBoard();
    double total = 0;

    for(Map.Entry<Integer, RiskTerritory> entry : board.getTerritories().entrySet()){
      if(entry.getValue().getOccupantPlayerId() == playerId){
        total += territoryRewards.get(entry.getKey());
      }
    }
    return total;
  }

  @Override
  public void tearDown() {
    super.tearDown();
  }

  @Override
  public void ponderStart() {
    super.ponderStart();
  }

  @Override
  public void ponderStop() {
    super.ponderStop();
  }

  @Override
  public void destroy() {
    super.destroy();
  }

  private HashMap<Integer, Double> distributeTerritoryRewards(Risk game) {
    RiskBoard board = game.getBoard();

    HashMap<Integer, Double> territoryRewards = new HashMap<>();
    HashMap<Integer, Integer> occupiedContinents = new HashMap();

    for(Map.Entry<Integer, RiskTerritory> entry : board.getTerritories().entrySet()){
      if(entry.getValue().getOccupantPlayerId() == game.getCurrentPlayer()) {
        if(occupiedContinents.containsKey(entry.getKey())) {
          occupiedContinents.put(entry.getKey(), occupiedContinents.get(entry.getKey()) + 1);
        } else {
          occupiedContinents.put(entry.getKey(), 1);
        }
      }
    }

    updateRewardsByContinent(occupiedContinents);

    board.getTerritoryIds().forEach(id -> {
      double rewardToGive = 0.d;
      Set<Integer> neighbors = board.neighboringTerritories(id);
      List<Integer> nList = new ArrayList<>(neighbors);

      if(territoriesBelongToDifferentContinents(board, nList)) {
        //territory is a transition
        rewardToGive += RewardFactors.TRANSITION_REWARD_FACTOR;
        //all neighbouring territories get reward since they are one territory away from a transition
        neighbors.forEach(neighborId -> {
          double toAdd = territoryRewards.get(neighborId) != null ? territoryRewards.get(neighborId) : 0.d;
          territoryRewards.put(neighborId,
                toAdd + RewardFactors.TRANSITION_NEIGHBOR_REWARD_FACTOR);
        });
      }
      int continentId = board.getTerritories().get(id).getContinentId();
      if(occupiedContinents.containsKey(continentId)) {
        rewardToGive += RewardFactors.CONTINENT_REWARD_FACTOR * continents.get(continentId).getReward();
        int neighboringEnemyTerritories = board.neighboringEnemyTerritories(id).size();
        if(neighboringEnemyTerritories > 0 && neighboringEnemyTerritories < 3) {
          rewardToGive += RewardFactors.NEAR_ENEMY_REWARD_FACTOR;
        }
      }
      territoryRewards.put(id, rewardToGive);
    });
    return territoryRewards;
  }

  private boolean territoriesBelongToDifferentContinents(RiskBoard board, List<Integer> territoryIds) {
    Map<Integer, RiskTerritory> territories = board.getTerritories();
    int previous = territories.get(territoryIds.get(0)).getContinentId();
    Integer current = null;
    for(int i = 0; i < territoryIds.size(); i++) {
      current = territories.get(territoryIds.get(i)).getContinentId();
      if(previous != current)
        return true;
      previous = current;
    }
    return false;
  }

  private void createRewardsByContinent() {
    continents.forEach((id, continent) -> {
      double reward = 0.d;
      if(continent.getTerritories().size() < 6) {
        //small continents are easier to fill => higher reward
        reward += RewardFactors.SMALL_CONTINENT_REWARD_FACTOR;
      } else if(continent.getTerritories().size() >= 6) {
        reward += RewardFactors.BIG_CONTINENT_REWARD_FACTOR;
      }
      continent.setReward(reward);
    });
  }

  private void updateRewardsByContinent(HashMap<Integer, Integer> occupiedContinents) {
    continents.forEach((id, continent) -> {
      Double reward = continent.getReward();
      if(occupiedContinents.containsKey(id)) {
        //prioritize continents we already have some territories in
        reward += RewardFactors.OCCUPIED_CONTINENT_REWARD_FACTOR +
                occupiedContinents.get(id) * RewardFactors.OCCUPIED_CONTINENT_ADDITIONAL_FOR_EACH_TERRITORY_ALREADY_OCCUPIED_REWARD_FACTOR;
      }
      continent.setReward(reward);
    });
  }

  private HashMap<Integer, Continent> createContinentsWithAllTerritories(RiskBoard board) {
    HashMap<Integer, Continent> continents = new HashMap<>();
    board.getTerritories().forEach((id, territory) -> {
      if(!continents.containsKey(territory.getContinentId())) {
        Continent continent = new Continent(new ArrayList<>(),
                board.getContinents().get(territory.getContinentId()),
                territory.getContinentId());
        continent.getTerritories().add(territory);
        continents.put(territory.getContinentId(), continent);
      } else {
        continents.get(territory.getContinentId()).getTerritories().add(territory);
      }
    });
    return continents;
  }
}