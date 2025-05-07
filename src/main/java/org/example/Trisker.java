package org.example;

import at.ac.tuwien.ifs.sge.agent.*;
import at.ac.tuwien.ifs.sge.engine.Logger;
import at.ac.tuwien.ifs.sge.game.risk.board.Risk;
import at.ac.tuwien.ifs.sge.game.risk.board.RiskAction;
import at.ac.tuwien.ifs.sge.game.risk.board.RiskBoard;
import at.ac.tuwien.ifs.sge.game.risk.board.RiskContinent;
import at.ac.tuwien.ifs.sge.game.risk.board.RiskTerritory;
import org.example.data.Continent;
import org.example.data.RewardFactors;
import org.example.general.RiskState;
import org.example.mcts.UCBLogic;
import org.example.mcts.UCBNode;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.List;
import java.util.stream.Collectors;

//Run command for the game engine:
//java -jar sge-1.0.7-exe.jar match --file=sge-risk-1.0.7-exe.jar --directory=agentstest

public class Trisker extends AbstractGameAgent<Risk, RiskAction>
        implements GameAgent<Risk, RiskAction> {

  private HashMap<Integer, Double> territoryRewards = null;
  private HashMap<Integer, RiskContinent> occupiedContinents = new HashMap<>();
  private HashMap<Integer, Continent> continents = null;
  private boolean isFirstRound = true;
  private static int counter = 0;



  public Trisker(Logger log){
    super(log);
  }

  @Override
  public void setUp(int numberOfPlayers, int playerId) {
    super.setUp(numberOfPlayers, playerId);
  }

  private void ownSetup(Risk game)  {
    continents = createContinentsWithAllTerritories(game.getBoard());
    createRewardsByContinent();
  }

  @Override
  public RiskAction computeNextAction(Risk game,
                             long computationTime,
                             TimeUnit timeUnit){
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
          double value = startRandomSimulation(node);
          //log.warn(value);
          UCBLogic.backpropagate(node, value);
          node = root;
        } else if (node.getChildren().isEmpty()) {
          UCBLogic.expandAll(node, node.getState().getPossibleActions());
          node = UCBLogic.selectBest(node);
          double value = startRandomSimulation(node);
          //log.warn(value);
          UCBLogic.backpropagate(node, value);
          node = root;
        } else {
          node = UCBLogic.selectBest(node);
        }
      }
      log.warn("Best one Taken: ");
      log.warn(UCBLogic.calculateUCB(UCBLogic.selectBest(root)));
      return UCBLogic.selectBest(root).getRiskAction();
    } else {
      System.exit(1);
      return List.copyOf(game.getPossibleActions()).get(0);
    }
  }

  private UCBNode startMCSTree(Risk game) {
    return new UCBNode(null, null, game);
  }

  private double startRandomSimulation(UCBNode node) {
    Risk game = new Risk(node.getState());
    int cnt = 0;
    while(RiskState.isInitialPlacingPhase(game.getBoard()) && cnt < 4) {
      RiskAction action = game.getPossibleActions().stream()
              .skip(random.nextInt(game.getPossibleActions().size())).findFirst().get();
      //log.warn(action.toString());
      game = (Risk) game.doAction(action);
      cnt++;
    }

    return calculateTotalRewardsOfPlayer(playerId, game, distributeTerritoryRewards(game));
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
    updateRewardsByContinent();
    HashMap<Integer, Double> territoryRewards = new HashMap<>();
    Set<Integer> occupiedContinentIds = new HashSet();

    for(Map.Entry<Integer, RiskTerritory> entry : board.getTerritories().entrySet()){
      if(entry.getValue().getOccupantPlayerId() == playerId) {
        occupiedContinentIds.add(entry.getValue().getContinentId());
      }
    }

    board.getTerritoryIds().forEach(id -> {
      double rewardToGive = 0.d;
      Set<Integer> neighbors = board.neighboringTerritories(id);
      List<Integer> nList = new ArrayList<>(neighbors);

      if(neighbors.size() == 2 && territoriesBelongToDifferentContinents(board, nList.getFirst(), nList.get(1))) {
        //territory is a transition
        rewardToGive += RewardFactors.TRANSITION_REWARD_FACTOR;
        //both neighbouring territories get +1 reward since they are one territory away from a transition
        neighbors.forEach(neighborId -> {
          territoryRewards.put(neighborId,
                territoryRewards.get(neighborId) + RewardFactors.TRANSITION_NEIGHBOR_REWARD_FACTOR);
        });
      }
      int continentId = board.getTerritories().get(id).getContinentId();
      if(occupiedContinentIds.contains(continentId)) {
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

  private boolean territoriesBelongToDifferentContinents(RiskBoard board, Integer t1, Integer t2) {
    Map<Integer, RiskTerritory> territories = board.getTerritories();
    return territories.get(t1).getContinentId() != territories.get(t2).getContinentId();
  }

  private boolean isPartOfOccupiedContinent(RiskBoard board, Integer t) {
    Integer cId = board.getTerritories().get(t).getContinentId();
    return occupiedContinents.containsKey(cId);
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

  private void updateRewardsByContinent() {
    continents.forEach((id, continent) -> {
      Double reward = continent.getReward();
      if(occupiedContinents.containsKey(id)) {
        //prioritize continents we already have some territories in
        reward += RewardFactors.OCCUPIED_CONTINENT_REWARD_FACTOR;
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