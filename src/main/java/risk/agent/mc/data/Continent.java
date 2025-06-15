package risk.agent.mc.data;

import at.ac.tuwien.ifs.sge.game.risk.board.RiskContinent;
import at.ac.tuwien.ifs.sge.game.risk.board.RiskTerritory;

import java.util.List;

public class Continent extends RiskContinent {
  private Integer id = 0;
  private List<RiskTerritory> territories = null;
  private Double baseReward = 0.d;

  public Continent(List<RiskTerritory> territories, int troopBonus, Integer id) {
    super(troopBonus);
    this.territories = territories;
    this.id = id;
  }

  public Continent(List<RiskTerritory> territories, RiskContinent riskContinent, Integer id) {
    super(riskContinent);
    this.territories = territories;
    this.id = id;
  }

  public Integer getId() {
    return id;
  }

  public List<RiskTerritory> getTerritories() {
    return territories;
  }

  public Double getBaseReward() {
    return baseReward;
  }

  public void setBaseReward(Double baseReward) {
    this.baseReward = baseReward;
  }
}
