package risk.agent.mc.log;

import risk.agent.mc.mcts.UCBLogic;
import risk.agent.mc.mcts.UCBNode;

public class EventLogUtils {

  /**
   * Calculates all USB values of the tree and sets the value inside the tree node.
   * This method is used to send the uct values to the frontend
   * @param node the node of which to start the calculation and from
   */
  public static void calculateUCBRecursive(UCBNode node) {
    node.getChildren().forEach((c -> {
      c.setUcbValue(UCBLogic.calculateUCB(c));
      calculateUCBRecursive(c);
    }));
  }
}
