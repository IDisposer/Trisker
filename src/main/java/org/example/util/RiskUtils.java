package org.example.util;

import at.ac.tuwien.ifs.sge.game.risk.board.Risk;
import at.ac.tuwien.ifs.sge.game.risk.board.RiskAction;
import at.ac.tuwien.ifs.sge.game.risk.board.RiskBoard;
import at.ac.tuwien.ifs.sge.game.risk.board.RiskTerritory;
import org.example.data.RiskActionIdentifier;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

/**
 * Provides several utility functions for writing a game agent for the game risk.
 * {@link RiskUtils#initialize(int)} has to be called before any other method in this class to ensure functionality.
 */
public class RiskUtils {

  private static int playerId = -1;

  /**
   * Sets the player id of our agent to be used in future calculations.
   * Should be called before any other method in {@link RiskUtils} is called
   * @param pId the id of our agent in the current game
   */
  public static void initialize(int pId) {
    playerId = pId;
  }

  /**
   * Groups actions with similar sources and targets into two actions.
   * One with the highest value and one with half of the highest value.
   * @param actions the actions to reduce
   * @return the reduced actions
   */
  public static Set<RiskAction> groupActions(Set<RiskAction> actions) {
    HashMap<RiskActionIdentifier, RiskAction> actionsMap = new HashMap<>();
    RiskActionIdentifier rai;
    for(RiskAction action : actions) {  //add the actions with the biggest values
      rai = new RiskActionIdentifier(action.attackingId(), action.defendingId());
      if(actionsMap.containsKey(rai)) {
        actionsMap.put(rai, actionsMap.get(rai).troops() > action.troops() ? actionsMap.get(rai) : action);
      } else {
        actionsMap.put(rai, action);
      }
    }
    Set<RiskAction> result = new HashSet<>();
    for(RiskAction action : actions) {   //add the actions with half of the biggest value (rounded down because int)
      rai = new RiskActionIdentifier(action.attackingId(), action.defendingId());
      if(action.troops() == actionsMap.get(rai).troops() / 2) {
        result.add(action);
      }
    }
    result.addAll(actionsMap.values());
    return result;
  }

  /**
   * Calculates the distance from the territory with territoryId
   * to the closest enemy territory
   * @param game the game state on which to make the calculation
   * @param territoryId the origin territory for the calculation
   * @return the closest distance to an enemy territory
   */
  public static int calculateDistanceToClosestEnemyTerritory(Risk game, int territoryId) {
    Queue<Integer> q = new LinkedList<>();
    int nrOfTerritories = game.getBoard().getTerritories().size();
    boolean[] visited = new boolean[nrOfTerritories];
    int[] distance = new int[nrOfTerritories];

    distance[territoryId] = 0;
    q.add(territoryId);

    while (!q.isEmpty()) {
      int curr = q.poll();

      for (int neighbour : game.getBoard().neighboringTerritories(curr)) {
        if (!visited[neighbour]) {
          // Check if enemy territory
          if (game.getBoard().getTerritories().get(neighbour).getOccupantPlayerId() != playerId) {
            return distance[curr] + 1;
          }
          visited[neighbour] = true;
          q.add(neighbour);
          distance[neighbour] = distance[curr] + 1;
        }
      }
    }
    return -1;
  }

  /**
   * Calculates a distance map from each friendly territory to each nearest neighbor
   * Distance 0 means that the territory with that id is an enemy territory
   * @param game the game state on which to make the calculation
   * @return a map from all territories with their distances to the closest enemy
   */
  public static int[] calculateDistanceMapToClosestEnemyTerritories(Risk game) {
    Queue<Integer> q = new LinkedList<>();
    int nrOfTerritories = game.getBoard().getTerritories().size();
    boolean[] visited = new boolean[nrOfTerritories];
    int[] distance = new int[nrOfTerritories];

    // Add all enemy territories to the queue
    for (Map.Entry<Integer, RiskTerritory> entry : game.getBoard().getTerritories().entrySet()) {
      if (entry.getValue().getOccupantPlayerId() == playerId) continue;

      visited[entry.getKey()] = true;
      distance[entry.getKey()] = 0;
      q.add(entry.getKey());
    }

    while (!q.isEmpty()) {
      int curr = q.poll();

      for (int neighbour : game.getBoard().neighboringTerritories(curr)) {
        if (!visited[neighbour]) {
          visited[neighbour] = true;
          q.add(neighbour);
          distance[neighbour] = distance[curr] + 1;
        }
      }
    }
    return distance;
  }

  /**
   * Sums up all troops of the territories given in the given game state
   * @param game the game state
   * @param neighbouringEnemyList the list of territories
   * @return the sum of the number of troops in the given territories
   */
  public static int getTotalTroopsOfNeighbouringEnemies(Risk game, Collection<Integer> neighbouringEnemyList) {
    int total = 0;
    Map<Integer, RiskTerritory> territories = game.getBoard().getTerritories();
    for(Integer id : neighbouringEnemyList) {
      total += territories.get(id).getTroops();
    }
    return total;
  }

  /**
   * Calculates the sum of troops of all enemy territories surrounding the territory with the given id
   * @param game the game state
   * @param territoryId the territory from where to get the enemies
   * @return the sum of enemy troops around the given territory
   */
  public static int getTotalTroopsOfNeighbouringEnemies(Risk game, int territoryId) {
    return getTotalTroopsOfNeighbouringEnemies(game, game.getBoard().neighboringEnemyTerritories(territoryId));
  }

  /**
   * Return if the enemy territory is friendly or not
   * @param game the game state
   * @param territoryId the territory to check the friendliness for
   * @return true if enemy, else false
   */
  public static boolean isTerritoryOfEnemy(Risk game, int territoryId) {
    if(territoryId < 0) //territory id is a special id
      return false;
    int id = game.getBoard().getTerritories().get(territoryId).getOccupantPlayerId();
    return id != playerId && id != -1;
  }

  /**
   * Checks if territory B is closer to an enemy as territory A
   * @param game the game state
   * @param initialT the first territory id (A)
   * @param newT the second territory id (B)
   * @return if B is closer as A true, else false
   */
  public static boolean isNewTerritoryCloserToEnemy(Risk game, int initialT, int newT) {
    return calculateDistanceToClosestEnemyTerritory(game, newT) - calculateDistanceToClosestEnemyTerritory(game, initialT) < 0;
  }

  /**
   * Gets the target territory id of an action
   * @param action the action to extract the target from
   * @return the territory id of the action
   */
  public static int getTargetOfAction(RiskAction action) {
    return action.selected();
  }

  /**
   * Checks if the given territories belong to different continents
   * @param board the game state
   * @param territoryIds the territories to check for
   * @return true if they belong to different continents, else false
   */
  public static boolean territoriesBelongToDifferentContinents(RiskBoard board, List<Integer> territoryIds) {
    Map<Integer, RiskTerritory> territories = board.getTerritories();
    int baseline = territories.get(territoryIds.get(0)).getContinentId();
    int current;
    for (Integer territoryId : territoryIds) {
      current = territories.get(territoryId).getContinentId();
      if (baseline != current)
        return true;
    }
    return false;
  }

  /**
   * Checks if the board is in the initial placing phase
   * @param board the board to check on
   * @return true if in the initial placing phase, else false
   */
  public static boolean isInitialPlacingPhase(RiskBoard board) {
    return board.getTerritories().values().parallelStream()
            .anyMatch(territory -> territory.getOccupantPlayerId() == -1);
  }
}
