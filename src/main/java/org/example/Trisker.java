package org.example;

import at.ac.tuwien.ifs.sge.agent.*;
import at.ac.tuwien.ifs.sge.engine.Logger;
import at.ac.tuwien.ifs.sge.game.risk.board.Risk;
import at.ac.tuwien.ifs.sge.game.risk.board.RiskAction;


import java.util.concurrent.TimeUnit;
import java.util.List;

//Run command for the game engine:
//java -jar sge-1.0.7-exe.jar match --file=sge-risk-1.0.7-exe.jar --directory=agentstest

public class Trisker extends AbstractGameAgent<Risk, RiskAction>
        implements GameAgent<Risk, RiskAction> {
  public Trisker(Logger log){
    super(log);
  }
  @Override
  public RiskAction computeNextAction(Risk game,
                             long computationTime,
                             TimeUnit timeUnit){
    //optionally set AbstractGameAgent timers
    super.setTimers(computationTime, timeUnit);
    //choose the first option
    return List.copyOf(game.getPossibleActions()).get(0);
  }

  @Override
  public void tearDown() {
    super.tearDown();
  }

  @Override
  public void ponderStart() {
    super.ponderStart();
  }

  @Override
  public void ponderStop() {
    super.ponderStop();
  }

  @Override
  public void destroy() {
    super.destroy();
  }
}