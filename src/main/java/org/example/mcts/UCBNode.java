package org.example.mcts;

import at.ac.tuwien.ifs.sge.game.risk.board.Risk;
import at.ac.tuwien.ifs.sge.game.risk.board.RiskAction;

public class UCBNode extends TreeNode<UCBNode> {
  private double total = 0;
  private int visits = 0;


  public UCBNode(UCBNode parent, RiskAction riskAction, Risk state) {
    super(parent, riskAction, state);
  }

  public double getTotal() {
    return total;
  }

  public void setTotal(double total) {
    this.total = total;
  }

  public int getVisits() {
    return visits;
  }

  public void setVisits(int visits) {
    this.visits = visits;
  }

}
