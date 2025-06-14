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

public class RiskUtils {

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

  public static int calculateDistanceToClosestEnemyTerritory(Risk game, int territoryId) {
    Queue<Integer> q = new LinkedList<>();
    int nrOfTerritories = game.getBoard().getTerritories().size();
    boolean[] visited = new boolean[nrOfTerritories];
    int[] distance = new int[nrOfTerritories];

    visited[territoryId] = true;
    distance[territoryId] = 0;
    q.add(territoryId);

    while (!q.isEmpty()) {
      int curr = q.poll();

      for (int neighbour : game.getBoard().neighboringTerritories(curr)) {
        if (!visited[neighbour]) {
          // Check if enemy territory
          if (game.getBoard().getTerritories().get(territoryId).getOccupantPlayerId() != game.getCurrentPlayer()) {
            return distance[neighbour] + 1;
          }

          visited[neighbour] = true;
          q.add(neighbour);
          distance[neighbour] = distance[curr] + 1;
        }
      }
    }
    return -1;
  }

  public static int[] calculateDistanceMapToClosestEnemyTerritories(Risk game) {
    Queue<Integer> q = new LinkedList<>();
    int nrOfTerritories = game.getBoard().getTerritories().size();
    boolean[] visited = new boolean[nrOfTerritories];
    int[] distance = new int[nrOfTerritories];

    // Add all enemy territories to the queue
    for (Map.Entry<Integer, RiskTerritory> entry : game.getBoard().getTerritories().entrySet()) {
      if (entry.getValue().getOccupantPlayerId() == game.getCurrentPlayer()) continue;

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

  public static int getTotalTroopsOfNeighbouringEnemies(Risk game, Collection<Integer> neighbouringEnemyList) {
    int total = 0;
    Map<Integer, RiskTerritory> territories = game.getBoard().getTerritories();
    for(Integer id : neighbouringEnemyList) {
      total += territories.get(id).getTroops();
    }
    return total;
  }

  public static int getTotalTroopsOfNeighbouringEnemies(Risk game, int territoryId) {
    return getTotalTroopsOfNeighbouringEnemies(game, game.getBoard().neighboringEnemyTerritories(territoryId));
  }

  public static boolean isTerritoryOfEnemy(Risk game, int territoryId) {
    if(territoryId < 0) //territory id is a special id
      return false;
    int id = game.getBoard().getTerritories().get(territoryId).getOccupantPlayerId();
    return id != game.getCurrentPlayer() && id != -1;
  }

  public static boolean isNewTerritoryCloserToEnemy(Risk game, int initialT, int newT) {
    return calculateDistanceToClosestEnemyTerritory(game, newT) - calculateDistanceToClosestEnemyTerritory(game, initialT) < 0;
  }

  public static int getTargetOfAction(RiskAction action) {
    return action.selected();
  }

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

  public static boolean isInitialPlacingPhase(RiskBoard board) {
    return board.getTerritories().values().parallelStream()
            .anyMatch(territory -> territory.getOccupantPlayerId() == -1);
  }
}
