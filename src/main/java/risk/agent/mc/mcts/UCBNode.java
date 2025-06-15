package risk.agent.mc.mcts;

import at.ac.tuwien.ifs.sge.game.risk.board.Risk;
import at.ac.tuwien.ifs.sge.game.risk.board.RiskAction;

/**
 * A tree node that can be used in an MCTS-Tree that uses UCB.
 */
public class UCBNode extends TreeNode<UCBNode> {
  private double total = 0;
  private int visits = 0;
  private double ucbValue = -1;


  /**
   * Creates a new {@link UCBNode} object
   * @param parent the parent of the node to create, null if no parent should be set
   * @param riskAction the {@link RiskAction} this node should represent, null if no action should be associated
   * @param state the game state before the action of this node is executed
   */
  public UCBNode(UCBNode parent, RiskAction riskAction, Risk state) {
    super(parent, riskAction, state);
  }

  /**
   * Returns the total value of this node
   * @return the total value of this node as a double
   */
  public double getTotal() {
    return total;
  }

  /**
   * Set the total value of this node
   * @param total the total to be set
   */
  public void setTotal(double total) {
    this.total = total;
  }

  /**
   * The visits this node has received during MCTS-Operations
   * @return the visits of this node as an integer
   */
  public int getVisits() {
    return visits;
  }

  /**
   * Set the visits this node has received during MCTS-Operations
   * @param visits the visits to be set
   */
  public void setVisits(int visits) {
    this.visits = visits;
  }

  /**
   * The UCB value set to this node. Only used for event-logging.
   * May not be accurate for other purposes. Use {@link UCBLogic#calculateUCB(UCBNode)} instead.
   * @return the UCB value of this node
   */
  public double getUcbValue() {
    return ucbValue;
  }

  /**
   * Set the UCB value for this node. Only used for event-logging.
   * May not be accurate for other purposes. Use {@link UCBLogic#calculateUCB(UCBNode)} instead.
   * @param ucbValue the UCB value to set
   * @return this
   */
  public UCBNode setUcbValue(double ucbValue) {
    this.ucbValue = ucbValue;
    return this;
  }
}
