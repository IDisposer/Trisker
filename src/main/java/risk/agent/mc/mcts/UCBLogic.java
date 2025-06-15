package risk.agent.mc.mcts;

import at.ac.tuwien.ifs.sge.game.risk.board.Risk;
import at.ac.tuwien.ifs.sge.game.risk.board.RiskAction;

import java.util.Set;

/**
 * Houses the logic for UCB calculation and the backpropagation, expansion and selection phases of MCTS
 */
public class UCBLogic {

  /**
   * The Exploration factor used in the UCB-Value calculation
   */
  private static double EXPLORATION_FACTOR = 8000;

  /**
   * Calculates the UCB value of the given node
   * @param node the {@link UCBNode} to calculate the UCB value for
   * @return the UCB value of the given node
   */
  public static double calculateUCB(UCBNode node) {
    if(node.getVisits() == 0)
      return Double.MAX_VALUE;

    return node.getTotal() + EXPLORATION_FACTOR
            * Math.sqrt(Math.log(node.getParent().getVisits()) / node.getVisits());
  }

  /**
   * Corresponds to the backpropagation phase of MCTS. Takes a value and adds it to the total of the given node.
   * Works recursively up the tree until a node without parent (root) is reached.
   * @param node the {@link UCBNode} to add the given value to. This node's parent will be the next node the value will be added to.
   *             If this node does not have a parent, the recursion is terminated.
   * @param value the value to add to node and future nodes up the tree
   */
  public static void backpropagate(UCBNode node, double value) {
    node.setVisits(node.getVisits() + 1);
    node.setTotal(node.getTotal() + value);
    if(node.getParent() != null) {
      backpropagate(node.getParent(), value);
    }
  }

  /**
   * Corresponds to the expansion phase of MCTS. Takes the given actions and attaches them as children to the given node.
   * New nodes will be created with these actions.
   * @param node the {@link UCBNode} to attach children to
   * @param possibleActions a Set of {@link RiskAction} that is supposed to be attached to the given node as new nodes
   */
  public static void expandAll(UCBNode node, Set<RiskAction> possibleActions) {
    for(RiskAction action : possibleActions) {
      Risk newState = new Risk(node.getState());
      newState.doAction(action);
      UCBNode child = new UCBNode(node, action, newState);
      node.addChild(child);
    }
  }

  /**
   * Selects the child node of the given node that has the highest UCB value
   * @param node the {@link UCBNode} whose children should be searched in
   * @return the child node of the given node with the highest UCB value
   */
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
