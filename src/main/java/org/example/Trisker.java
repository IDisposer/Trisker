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
  private int proportion = 0;



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
    try {
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
        proportion = game.getPossibleActions().size();
        UCBNode node = root;
        while(!shouldStopComputation()) {
          if(node.getVisits() == 0 && node.getChildren().isEmpty()) {
            double value = startSimulation(node);
            //log.warn(value);
            UCBLogic.backpropagate(node, value);
            node = root;

            UCBLogic.calculateUCBRecursive(root);
            EventLogService.logTree(root);
          } else if (node.getChildren().isEmpty()) {
            UCBLogic.expandAll(node, node.getState().getPossibleActions());
            proportion = node.getState().getPossibleActions().size();
            node = UCBLogic.selectBest(node);
            double value = startSimulation(node);
            //log.warn(value);
            UCBLogic.backpropagate(node, value);
            node = root;

            UCBLogic.calculateUCBRecursive(root);
            EventLogService.logTree(root);
          } else {
            node = UCBLogic.selectBest(node);
          }
        }
        log.warn("Best one Taken: ");
        UCBNode bestNode = UCBLogic.selectBest(root);
        RiskAction bestAction = bestNode.getRiskAction();

        UCBNode frontend = root.getChildren().stream().max((o1, o2) -> o1.getUcbValue() < o2.getUcbValue() ? -1 : 1).get();

        log.warn(String.format("Backend: %s (%f), Frontend: %s (%f)",
            bestAction, UCBLogic.calculateUCB(bestNode),
            frontend.getRiskAction(), frontend.getUcbValue()
        ));
        log.warn("Best one Taken: ");
        log.warn(bestNode.getRiskAction() + " with ucbscore: "
                + UCBLogic.calculateUCB(bestNode) + " t: " + bestNode.getTotal() + " v: " + bestNode.getVisits());
        log.warn("Out of: ");
        for(UCBNode child : root.getChildren()) {
          log.warn(child.getRiskAction() + " with ucbscore: "
                  + UCBLogic.calculateUCB(child)+ " t: " + child.getTotal() + " v: " + child.getVisits());
        }
        EventLogService.logBoard("OWN", (Risk) game.doAction(bestAction).getGame());
        return bestAction;
      } else {
        System.exit(1);
        return List.copyOf(game.getPossibleActions()).get(0);
      }
    } catch (Exception ex) {
      ex.printStackTrace();
      throw ex;
    }
  }

  private UCBNode startMCSTree(Risk game) {
    return new UCBNode(null, null, game);
  }

  private double startSimulation(UCBNode node) {
    return startRandomSimulation(node);
  }

  private double startRandomSimulation(UCBNode node) {
    Risk game = new Risk(node.getState());
    int cnt = 0;
    while(!game.isGameOver() && !shouldStopComputation(2) && cnt < 31) { //RiskState.isInitialPlacingPhase(game.getBoard())
      RiskAction action = game.getPossibleActions().stream()
              .skip(random.nextInt(game.getPossibleActions().size())).findFirst().get();
      //log.warn(action.toString());
      game = (Risk) game.doAction(action);
      if(game.getCurrentPlayer() != playerId)
        opponentIds.add(game.getCurrentPlayer());
      cnt++;
    }
    System.out.println(cnt);
    return game.isGameOver() && game.getBoard().isPlayerStillAlive(playerId) ?
            5000 : !game.isGameOver() ? calculateTotalRewardsOfPlayerMinusOpponentsRewards(playerId, game) : -1000;//calculateTotalRewardsOfPlayerMinusOpponentsRewards(playerId, game);
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
    //log.warn("Total rewards: " + rewards);
    for(Integer opponentId : opponentIds) {
      //log.warn("Total rewards Opponent: " + calculateTotalRewardsOfPlayer(opponentId, (Risk) game.getGame(opponentId), distributeTerritoryRewards((Risk) game.getGame(opponentId))));
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
    //log.warn("Hello darkness my old friend: " + total
    //        + "(" + board.getTerritories().values().stream().filter(territory -> territory.getOccupantPlayerId() == playerId).count() + ")");
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
    HashMap<Integer, Integer> occupiedContinents = new HashMap<>();

    for(Map.Entry<Integer, RiskTerritory> entry : board.getTerritories().entrySet()){
      if(entry.getValue().getOccupantPlayerId() == game.getCurrentPlayer()) {
        if(occupiedContinents.containsKey(entry.getValue().getContinentId())) {
          occupiedContinents.put(entry.getValue().getContinentId(),
                  occupiedContinents.get(entry.getValue().getContinentId()) + 1);
        } else {
          occupiedContinents.put(entry.getValue().getContinentId(), 1);
        }
      }
    }

    HashMap<Integer, Double> contRewards = updateRewardsByContinent(occupiedContinents);

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
        rewardToGive += RewardFactors.CONTINENT_REWARD_FACTOR * contRewards.get(continentId);
        int neighboringEnemyTerritories = board.neighboringEnemyTerritories(id).size();
        if(neighboringEnemyTerritories > 0 && neighboringEnemyTerritories < 3) {
          rewardToGive += RewardFactors.NEAR_ENEMY_REWARD_FACTOR;
        }
      }

      if(game.getCurrentPlayer() == playerId && isLastAvailableOfEnemyContinent(game, id, continentId)) {
        rewardToGive += RewardFactors.LAST_ON_ENEMY_CONTINENT_REWARD_FACTOR;
      }
      double toAdd = territoryRewards.get(id) != null ? territoryRewards.get(id) : 0.d;
      territoryRewards.put(id, toAdd + rewardToGive);
    });
    return territoryRewards;
  }

  private boolean isLastAvailableOfEnemyContinent(Risk game, int territoryId, int continentId) {
    if(opponentIds.contains(game.getBoard().getTerritories().get(territoryId).getOccupantPlayerId()))
      return false;
    Continent c = continents.get(continentId);
    int seizedTerritories = 0;
    for(RiskTerritory t : c.getTerritories()) {
      if(opponentIds.contains(t.getOccupantPlayerId())) {
        seizedTerritories++;
      }
    }
    return seizedTerritories + 1 == c.getTerritories().size();
  }

  private boolean territoriesBelongToDifferentContinents(RiskBoard board, List<Integer> territoryIds) {
    Map<Integer, RiskTerritory> territories = board.getTerritories();
    int baseline = territories.get(territoryIds.get(0)).getContinentId();
    Integer current = null;
    for (Integer territoryId : territoryIds) {
      current = territories.get(territoryId).getContinentId();
      if (baseline != current)
        return true;
    }
    return false;
  }

  private void createRewardsByContinent() {
    continents.forEach((id, continent) -> {
      double reward = 0.d;
      if(continent.getTerritories().size() < 5) {
        //small continents are easier to fill => higher reward
        reward += RewardFactors.SMALL_CONTINENT_REWARD_FACTOR;
      } else if(continent.getTerritories().size() >= 5) {
        reward += RewardFactors.BIG_CONTINENT_REWARD_FACTOR;
      }
      continent.setBaseReward(reward);
    });
  }

  private HashMap<Integer, Double> updateRewardsByContinent(HashMap<Integer, Integer> occupiedContinents) {
    HashMap<Integer, Double> contRewards = new HashMap<>();
    continents.forEach((id, continent) -> {
      if(occupiedContinents.containsKey(id)) {
        Double reward = continent.getBaseReward();
        //prioritize continents we already have some territories in
        reward += RewardFactors.OCCUPIED_CONTINENT_REWARD_FACTOR +
                occupiedContinents.get(id) * RewardFactors.OCCUPIED_CONTINENT_ADDITIONAL_FOR_EACH_TERRITORY_ALREADY_OCCUPIED_REWARD_FACTOR;
        contRewards.put(id, reward);
      }
    });
    return contRewards;
  }

  private HashMap<Integer, Continent> createContinentsWithAllTerritories(RiskBoard board) {
    HashMap<Integer, Continent> continents = new HashMap<>();
    board.getTerritories().forEach((id, territory) -> {
      if(!continents.containsKey(territory.getContinentId())) {
        Continent continent = new Continent(new ArrayList<>(),
                board.getContinents().get(territory.getContinentId()),
                territory.getContinentId());
        continents.put(territory.getContinentId(), continent);
      }
      continents.get(territory.getContinentId()).getTerritories().add(territory);
    });
    return continents;
  }
}