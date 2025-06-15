package org.example.log;

import org.example.mcts.UCBLogic;
import org.example.mcts.UCBNode;

public class EventLogUtils {

  public static void calculateUCBRecursive(UCBNode node) {
    node.getChildren().forEach((c -> {
      c.setUcbValue(UCBLogic.calculateUCB(c));
      calculateUCBRecursive(c);
    }));
  }
}
