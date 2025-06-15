package risk.agent.mc.data;

import java.util.Objects;

/**
 * Used to group multiple actions with the same source and target IDs.
 */
public class RiskActionIdentifier {
  private final int sourceId;
  private final int targetId;

  public RiskActionIdentifier(final int sourceId, final int targetId) {
    this.sourceId = sourceId;
    this.targetId = targetId;
  }

  public int getSourceId() {
    return sourceId;
  }

  public int getTargetId() {
    return targetId;
  }

  @Override
  public int hashCode() {
    return Objects.hash(sourceId, targetId);
  }

  @Override
  public boolean equals(Object obj) {
    return obj.getClass() == this.getClass() && this.hashCode() == obj.hashCode();
  }
}
