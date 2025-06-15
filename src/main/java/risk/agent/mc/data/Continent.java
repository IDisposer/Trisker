package risk.agent.mc.data;

import at.ac.tuwien.ifs.sge.game.risk.board.RiskContinent;
import at.ac.tuwien.ifs.sge.game.risk.board.RiskTerritory;

import java.util.List;

/**
 * This extension of {@link RiskContinent} has a list of all territories, the associated id of the continent
 * and a base reward for reward calculations as additional fields.
 */
public class Continent extends RiskContinent {
  private Integer id = 0;
  private List<RiskTerritory> territories = null;
  private Double baseReward = 0.d;

  /**
   * Create a new continent
   * @param territories the {@link RiskTerritory} objects of this continent
   * @param riskContinent the associated {@link RiskContinent}
   * @param id the id of this continent given by {@link at.ac.tuwien.ifs.sge.game.risk.board.RiskBoard}
   */
  public Continent(List<RiskTerritory> territories, RiskContinent riskContinent, Integer id) {
    super(riskContinent);
    this.territories = territories;
    this.id = id;
  }

  /**
   * The id of this continent
   * @return the id of this continent as an integer
   */
  public Integer getId() {
    return id;
  }

  /**
   * All territories that are associated with this continent
   * @return a List of this continent's territories
   */
  public List<RiskTerritory> getTerritories() {
    return territories;
  }

  /**
   * The base reward given for this continent. Used during reward calculations.
   * @return the base reward as a double
   */
  public Double getBaseReward() {
    return baseReward;
  }

  /**
   * Set the base reward for this continent that should be used in reward calculations
   * @param baseReward the base reward to be set
   */
  public void setBaseReward(Double baseReward) {
    this.baseReward = baseReward;
  }
}
