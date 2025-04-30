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


import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.List;

//Run command for the game engine:
//java -jar sge-1.0.7-exe.jar match --file=sge-risk-1.0.7-exe.jar --directory=agentstest

public class Trisker extends AbstractGameAgent<Risk, RiskAction>
        implements GameAgent<Risk, RiskAction> {

  private HashMap<Integer, Double> territoryRewards = null;
  private HashMap<Integer, RiskContinent> occupiedContinents = new HashMap<>();
  private HashMap<Integer, Continent> continents = null;
  private boolean isFirstRound = true;



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
    distributeTerritoryRewards(game);
    //optionally set AbstractGameAgent timers
    super.setTimers(computationTime, timeUnit);
    //choose the first option
    return List.copyOf(game.getPossibleActions()).get(0);
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

  private void distributeTerritoryRewards(Risk game) {
    RiskBoard board = game.getBoard();
    updateRewardsByContinent();
    territoryRewards = new HashMap<>(); //reset the rewards every round
    board.getTerritoryIds().forEach(id -> {
      Double rewardToGive = 0.d;
      Set<Integer> neighbors = board.neighboringTerritories(id);
      Object[] nArray = neighbors.toArray();


      if(neighbors.size() == 2
              && territoriesBelongToDifferentContinents(board, (Integer) nArray[0], (Integer) nArray[1])) {
        //territory is a transition
        rewardToGive += 1 * RewardFactors.TRANSITION_REWARD_FACTOR;
        //both neighbouring territories get +1 reward since they are one territory away from a transition
        neighbors.forEach(neighborId -> {
          territoryRewards.put(neighborId,
                territoryRewards.get(neighborId) + 1.5 * RewardFactors.TRANSITION_NEIGHBOR_REWARD_FACTOR);
        });
      }

      if(isPartOfOccupiedContinent(board, id)) {
        rewardToGive += 1 * RewardFactors.CONTINENT_REWARD_FACTOR;
        int neighboringEnemyTerritories = board.neighboringEnemyTerritories(id).size();
        if(neighboringEnemyTerritories > 0 && neighboringEnemyTerritories < 3) {
          rewardToGive += 1 * RewardFactors.NEAR_ENEMY_REWARD_FACTOR;
        }
      }





      territoryRewards.put(id, rewardToGive);
    });

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
        reward += 2 * RewardFactors.SMALL_CONTINENT_REWARD_FACTOR;
      } else if(continent.getTerritories().size() >= 6) {
        reward += 1 * RewardFactors.BIG_CONTINENT_REWARD_FACTOR;
      }
      continent.setReward(reward);
    });
  }

  private void updateRewardsByContinent() {
    continents.forEach((id, continent) -> {
      Double reward = continent.getReward();
      if(occupiedContinents.containsKey(id)) {
        //prioritize continents we already have some territories in
        reward += 1 * RewardFactors.OCCUPIED_CONTINENT_REWARD_FACTOR;
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