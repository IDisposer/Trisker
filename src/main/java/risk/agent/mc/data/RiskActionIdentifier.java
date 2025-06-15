package risk.agent.mc.data;

import java.util.Objects;

/**
 * Used to group multiple actions with the same source and target IDs.
 */
public class RiskActionIdentifier {
  private final int sourceId;
  private final int targetId;

  /**
   * Create a new RiskActionIdentifier
   * @param sourceId the sourceId of the original {@link at.ac.tuwien.ifs.sge.game.risk.board.RiskAction}
   * @param targetId the targetId of the original {@link at.ac.tuwien.ifs.sge.game.risk.board.RiskAction}
   */
  public RiskActionIdentifier(final int sourceId, final int targetId) {
    this.sourceId = sourceId;
    this.targetId = targetId;
  }

  /**
   * The sourceId of the original {@link at.ac.tuwien.ifs.sge.game.risk.board.RiskAction}
   * @return the sourceId as an integer
   */
  public int getSourceId() {
    return sourceId;
  }

  /**
   * The targetId of the original {@link at.ac.tuwien.ifs.sge.game.risk.board.RiskAction}
   * @return the targetId as an integer
   */
  public int getTargetId() {
    return targetId;
  }

  /**
   * The hashcode is created as a combination of sourceId and targetId.
   * As a result all RiskActionIdentifiers with the same sourceId and targetId have the same hashcode.
   * @return the hashcode
   */
  @Override
  public int hashCode() {
    return Objects.hash(sourceId, targetId);
  }

  /**
   * Compares this and the given object for equality
   * @param obj the object to compare this to
   * @return true if both objects are equal, false if not
   */
  @Override
  public boolean equals(Object obj) {
    return obj.getClass() == this.getClass() && this.hashCode() == obj.hashCode();
  }
}
