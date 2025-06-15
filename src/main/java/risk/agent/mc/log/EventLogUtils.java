package risk.agent.mc.log;

import risk.agent.mc.mcts.UCBLogic;
import risk.agent.mc.mcts.UCBNode;

public class EventLogUtils {

  public static void calculateUCBRecursive(UCBNode node) {
    node.getChildren().forEach((c -> {
      c.setUcbValue(UCBLogic.calculateUCB(c));
      calculateUCBRecursive(c);
    }));
  }
}
