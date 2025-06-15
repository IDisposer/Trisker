package risk.agent.mc.mcts;

import at.ac.tuwien.ifs.sge.game.risk.board.Risk;
import at.ac.tuwien.ifs.sge.game.risk.board.RiskAction;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.ArrayList;
import java.util.List;

/**
 * Serves as a template for different types of nodes and lets them act as tree nodes for an MCTS-Tree
 * @param <T> the class to act as a template for
 */
public class TreeNode<T extends TreeNode<T>> {

  @JsonIgnore
  protected final T parent;
  protected List<T> children = new ArrayList<>();
  protected final RiskAction riskAction;
  @JsonIgnore
  protected final Risk state;
  protected boolean ignore = false;


  /**
   * Creates a new TreeNode
   * @param parent The parent of the tree node
   * @param riskAction the action this node represents
   * @param state the state of the game before the action of this node is executed
   */
  public TreeNode(T parent, RiskAction riskAction, Risk state) {
    this.parent = parent;
    this.riskAction = riskAction;
    this.state = state;
  }

  /**
   * Add a child node to this node. There may be duplicates
   * @param child the child to be added
   */
  public void addChild(T child) {
    children.add(child);
  }

  /**
   * All children of this node
   * @return an Arraylist of this node's children
   */
  public List<T> getChildren() {
    return children;
  }

  /**
   * Sets if this node should be ignored during serialization. Only used for event-logging.
   * @param ignore true if this node should be ignored, false if not
   */
  public void setIgnore(boolean ignore) {
    this.ignore = ignore;
  }

  /**
   * The parent of this node
   * @return the parent of this node. Null if no parent exists.
   */
  @JsonIgnore
  public T getParent() {
    return parent;
  }

  /**
   * The {@link RiskAction} this node is associated with.
   * @return this node's {@link RiskAction}, null if there is none associated
   */
  public RiskAction getRiskAction() {
    return riskAction;
  }

  /**
   * The state of the game before this node's action is executed
   * @return this node's state or null if there is none associated
   */
  public Risk getState() {
    return state;
  }

  /**
   * If this node is ignored during serialization. Only used in event-logging.
   * @return true if this node is ignored, false if not
   */
  public boolean isIgnore() {
    return ignore;
  }
}
