package risk.agent.mc.util;

import at.ac.tuwien.ifs.sge.game.risk.board.Risk;
import at.ac.tuwien.ifs.sge.game.risk.board.RiskAction;

import java.util.HashSet;
import java.util.Set;

/**
 * This class contains several pruning methods for {@link RiskAction} objects
 */
public class RiskActionPruner {

  /**
   * Prunes all unfavorable actions from the possible actions of the game state.
   * @param game the game state of which to get the possible actions from
   * @return the pruned actions
   */
  public static Set<RiskAction> getPrunedActions(Risk game) {
    if(!RiskUtils.isInitialPlacingPhase(game.getBoard())) {
      Set<RiskAction> prunedActions = RiskUtils.groupActions(game.getPossibleActions());
      prunedActions = pruneBadAttacks(game, prunedActions);
      prunedActions = pruneBadReinforcements(game, prunedActions);
      prunedActions = pruneBadEndphase(game, prunedActions);
      prunedActions = pruneBadFortifies(game, prunedActions);
      return prunedActions;
    }
    return game.getPossibleActions();
  }

  /**
   * Remove actions from the given actions that are attacks and
   * have troops that are less than or equal to the defender's troops.
   * Does not permute the original actions.
   * @param game the gamestate before the actions are taken as an instance of {@link Risk}
   * @param actions the actions to remove bad attacks from
   * @return the given actions minus the removed actions
   */
  public static Set<RiskAction> pruneBadAttacks(Risk game, Set<RiskAction> actions) {
    if(!game.getBoard().isAttackPhase()) {
      return actions;
    }
    int targetId;
    Set<RiskAction> goodActions = new HashSet<>();
    for(RiskAction action : actions) {
      targetId = RiskUtils.getTargetOfAction(action);
      if(!RiskUtils.isTerritoryOfEnemy(game, targetId)
              || game.getBoard().getTerritories().get(targetId).getTroops() < action.troops()) {
        //we are NOT attacking with less than or equal troops to the defender
        goodActions.add(action);
      }
    }
    return goodActions;
  }

  /**
   * Returns all actions minus reinforcements that don't reinforce a territory that is next to an enemy.
   * If no reinforcements have nearby enemies all actions are returned.
   * @param game the current game state the given actions would apply to
   * @param actions the actions to check for bad reinforcements in
   * @return the filtered actions
   */
  public static Set<RiskAction> pruneBadReinforcements(Risk game, Set<RiskAction> actions) {
    if(!game.getBoard().isReinforcementPhase()) {
      return actions;
    }
    int targetId;
    Set<RiskAction> goodActions = new HashSet<>();
    for(RiskAction action : actions) {
      targetId = RiskUtils.getTargetOfAction(action);
      if (!(targetId >= 0 && action.attackingId() == -1) || !game.getBoard().neighboringEnemyTerritories(targetId).isEmpty()) {
        goodActions.add(action);
      }
    }
    return goodActions.isEmpty() ? actions : goodActions;
  }

  /**
   * If there are good attacks left the endphase action will be removed.
   * Prerequisite: the actions parameter has to be run through {@link RiskActionPruner#pruneBadAttacks(Risk, Set)} first.
   * @param game the current game state the given actions would apply to
   * @param actions the actions to check for a bad endphase in
   * @return the filtered actions
   */
  public static Set<RiskAction> pruneBadEndphase(Risk game, Set<RiskAction> actions) {
    if(game.getBoard().isReinforcementPhase()) {
      //there is no endphase in the reinforcement phase
      return actions;
    }
    Set<RiskAction> goodActions = new HashSet<>(actions);
    RiskAction endPhase = null;
    boolean hasGoodActionLeft = false;
    for(RiskAction action : actions) {
      if (action.attackingId() == -2 && action.selected() == -4 && action.troops() == -8) {
        endPhase = action;
      }
      if(RiskUtils.isTerritoryOfEnemy(game, action.selected())) {
        hasGoodActionLeft = true;
      }
      if(hasGoodActionLeft && endPhase != null) {
        goodActions.remove(endPhase);
        return goodActions;
      }
    }
    return goodActions;
  }

  /**
   * Remove actions from the given actions that are fortifies and
   * where the fortifying distance to enemies is more remote than the fortified distance.
   * Does not permute the original actions.
   * @param game the gamestate before the actions are taken as an instance of {@link Risk}
   * @param actions the actions to remove bad fortifies from
   * @return the given actions minus the removed actions
   */
  public static Set<RiskAction> pruneBadFortifies(Risk game, Set<RiskAction> actions) {
    if (!game.getBoard().isFortifyPhase()) {
      return actions;
    }

    Set<RiskAction> prunedActions = new HashSet<>(actions);
    int[] distances = RiskUtils.calculateDistanceMapToClosestEnemyTerritories(game);
    for (RiskAction action : actions) {
      if (action.equals(RiskAction.endPhase())) continue;

      if (distances[action.fortifyingId()] <= distances[action.fortifiedId()]
        || (game.getBoard().getTerritoryTroops(action.fortifyingId()) - 1 != action.troops())) {
        prunedActions.remove(action);
      }
    }

    if (prunedActions.size() > 1 && prunedActions.size() < actions.size()) {
      prunedActions.remove(RiskAction.endPhase());
    }

    return prunedActions;
  }
}
