package org.example.general;

import at.ac.tuwien.ifs.sge.game.risk.board.RiskBoard;

public class RiskState {

  public static boolean isInitialPlacingPhase(RiskBoard board) {
    return board.getTerritories().values().parallelStream()
            .anyMatch(territory -> territory.getOccupantPlayerId() == -1);
  }
}
