package org.example.mcts;

import at.ac.tuwien.ifs.sge.game.risk.board.Risk;
import at.ac.tuwien.ifs.sge.game.risk.board.RiskAction;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.ArrayList;
import java.util.List;

public class TreeNode<T extends TreeNode<T>> {

  @JsonIgnore
  protected final T parent;
  protected List<T> children = new ArrayList<>();
  protected final RiskAction riskAction;
  @JsonIgnore
  protected final Risk state;
  protected boolean ignore = false;


  public TreeNode(T parent, RiskAction riskAction, Risk state) {
    this.parent = parent;
    this.riskAction = riskAction;
    this.state = state;
  }

  public void addChild(T child) {
    children.add(child);
  }

  public List<T> getChildren() {
    return children;
  }

  public void setIgnore(boolean ignore) {
    this.ignore = ignore;
  }

  @JsonIgnore
  public T getParent() {
    return parent;
  }

  public RiskAction getRiskAction() {
    return riskAction;
  }

  public Risk getState() {
    return state;
  }

  public boolean isIgnore() {
    return ignore;
  }
}
