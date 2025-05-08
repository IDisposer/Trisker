package org.example.mcts;

import at.ac.tuwien.ifs.sge.game.risk.board.Risk;
import at.ac.tuwien.ifs.sge.game.risk.board.RiskAction;

import java.util.Set;

public class UCBLogic {

  private static double EXPLORATION_FACTOR = 2;

  public static double calculateUCB(UCBNode node) {
    if(node.getVisits() == 0)
      return Double.MAX_VALUE;

    double ucb = node.getTotal() + EXPLORATION_FACTOR
            * Math.sqrt(Math.log(node.getParent().getVisits()) / node.getVisits());
    return ucb;
  }

  public static void backpropagate(UCBNode node, double value) {
    node.setVisits(node.getVisits() + 1);
    node.setTotal(node.getTotal() + value);
    if(node.getParent() != null)
      backpropagate(node.getParent(), value);
  }

  public static void expandAll(UCBNode node, Set<RiskAction> possibleActions) {
    for(RiskAction action : possibleActions) {
      Risk newState = new Risk(node.getState());
      newState.doAction(action);
      UCBNode child = new UCBNode(node, action, newState);
      node.addChild(child);
    }
  }

  public static UCBNode selectBest(UCBNode node) {
    UCBNode best = null;
    double bestValue = Double.MIN_VALUE;
    for(UCBNode child : node.getChildren()) {
      double ucb = calculateUCB(child);
      if(ucb == Double.MAX_VALUE)
        return child;
      if(ucb > bestValue || best == null) {
        best = child;
        bestValue = ucb;
      }
    }
    return best;
  }
}
