package org.example;

import at.ac.tuwien.ifs.sge.agent.AbstractGameAgent;
import at.ac.tuwien.ifs.sge.agent.GameAgent;
import at.ac.tuwien.ifs.sge.engine.Logger;
import at.ac.tuwien.ifs.sge.game.risk.board.Risk;
import at.ac.tuwien.ifs.sge.game.risk.board.RiskAction;
import at.ac.tuwien.ifs.sge.game.risk.board.RiskBoard;
import at.ac.tuwien.ifs.sge.game.risk.board.RiskTerritory;
import org.example.data.Continent;
import org.example.data.RewardFactors;
import org.example.log.EventLogService;
import org.example.mcts.UCBLogic;
import org.example.mcts.UCBNode;
import org.example.util.RiskActionPruner;
import org.example.util.RiskUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

//Run command for the game engine:
//java -jar sge-1.0.7-exe.jar match --file=sge-risk-1.0.7-exe.jar --directory=agentstest
//https://gitlab.com/StrategyGameEngine/sge-risk

public class Trisker extends AbstractGameAgent<Risk, RiskAction>
        implements GameAgent<Risk, RiskAction> {

  private HashMap<Integer, Continent> continents = null;
  private boolean isFirstRound = true;
  private final HashSet<Integer> opponentIds = new HashSet<>();
  private int proportion = 0;
  private final int TOTAL_RUNS_PER_ROUND = 1400;
  private int counter = 0;


  public Trisker(Logger log){
    super(log);
  }

  @Override
  public void setUp(int numberOfPlayers, int playerId) {
    RiskUtils.initialize(playerId);
    super.setUp(numberOfPlayers, playerId);
    EventLogService.reset();
  }

  private void ownSetup(Risk game)  {
    continents = createContinentsWithAllTerritories(game.getBoard());
    createRewardsByContinent();
  }

  @Override
  public RiskAction computeNextAction(Risk game,
                             long computationTime,
                             TimeUnit timeUnit){
    try {
      EventLogService.logBoard("ENEMY", game);

      if(isFirstRound){
        isFirstRound = false;
        ownSetup(game);
      }
      super.setTimers(computationTime, timeUnit);
      if(!game.isGameOver()) {
        UCBNode root = startMCSTree(game);
        UCBLogic.expandAll(root, RiskActionPruner.getPrunedActions(game));
        proportion = root.getChildren().size();
        UCBNode node = root;
        while(!shouldStopComputation()) {
          if(node.getVisits() == 0 && node.getChildren().isEmpty()) {
            double value = startSimulation(node);

            UCBLogic.backpropagate(node, value);
            node = root;
          } else if (node.getChildren().isEmpty()) {
            UCBLogic.expandAll(node, RiskActionPruner.getPrunedActions(node.getState()));
            proportion = node.getChildren().size();
            node = UCBLogic.selectBest(node);
            double value = startSimulation(node);

            UCBLogic.backpropagate(node, value);
            node = root;
          } else {
            node = UCBLogic.selectBest(node);
          }
        }
        UCBNode bestNode = UCBLogic.selectBest(root);
        RiskAction bestAction = bestNode.getRiskAction();
        /*
        EventLogService.logBoard("OWN", (Risk) game.doAction(bestAction).getGame());
        log.warn("BoardCounter: " + EventLogService.getBoardCounter());

        if(bestNode.getRiskAction().selected() >= 0 && RiskUtils.isTerritoryOfEnemy(bestNode.getState(), bestNode.getRiskAction().selected())) {
          log.warn("Attacked: " + bestNode.getRiskAction() + "| against = " + bestNode.getState().getBoard().getTerritories().get(bestNode.getRiskAction().selected()).getTroops());
        } else if (!RiskUtils.isInitialPlacingPhase(bestNode.getState().getBoard()) && bestNode.getRiskAction().defendingId() >= 0 && bestNode.getRiskAction().attackingId() != -1){
          int distanceFrom = RiskUtils.calculateDistanceToClosestEnemyTerritory(bestNode.getState(), bestAction.attackingId());
          int distanceTo = RiskUtils.calculateDistanceToClosestEnemyTerritory(bestNode.getState(), bestAction.defendingId());
          log.warn("Fortified: " + bestNode.getRiskAction() +
                  "| from distance: " + distanceFrom +
                  "| to distance: " + distanceTo);
          if(distanceFrom - distanceTo < 0) {
            counter++;
          }
        }
        log.warn("Best one Taken: ");
        log.warn(bestNode.getRiskAction() + " with ucbscore: "
                + UCBLogic.calculateUCB(bestNode) + " t: " + bestNode.getTotal() + " v: " + bestNode.getVisits());
        log.warn("Out of: ");
        for(UCBNode child : root.getChildren()) {
          log.warn(child.getRiskAction() + " with ucbscore: "
                  + UCBLogic.calculateUCB(child)+ " t: " + child.getTotal() + " v: " + child.getVisits());
        }
        log.warn("times moved away from the enemy: " + counter);
        */
        return bestAction;
      } else {
        System.exit(1);
        return List.copyOf(RiskActionPruner.getPrunedActions(game)).get(0);
      }
    } catch (Exception ex) {
      ex.printStackTrace();
      throw ex;
    }
  }

  private UCBNode startMCSTree(Risk game) {
    return new UCBNode(null, null, game);
  }

  private double startSimulation(UCBNode node) {
    return startRandomSimulation(node);
  }

  /**
   * Starts a simulation according to MCTS-logic and calculates the value given to the node from which this simulation starts.
   * This simulation is random.
   * @param node the {@link UCBNode} to start the simulation from
   * @return the value reached as a sum of all rewards obtained during the simulation weighted by when they happened
   */
  private double startRandomSimulation(UCBNode node) {
    double points = 0.d;
    Risk game = new Risk(node.getState());
    Risk gameBefore = game;
    game = (Risk) game.doAction(node.getRiskAction());
    int cnt = 0;
    while(!game.isGameOver() && !shouldStopComputation() && cnt < TOTAL_RUNS_PER_ROUND / proportion) { //RiskState.isInitialPlacingPhase(game.getBoard())
      cnt++;
      if(game.getPreviousActionRecord().getPlayer() == playerId) {
        opponentIds.add(game.getCurrentPlayer());
        points += calculateRewardForPreviousAction(game, gameBefore) / cnt * 10;
      }
      Set<RiskAction> actions = game.getCurrentPlayer() != playerId ?
              game.getPossibleActions() : RiskActionPruner.getPrunedActions(game);
      RiskAction action = actions.stream()
              .skip(random.nextInt(actions.size())).findFirst().get();
      gameBefore = (Risk) game.getGame(playerId);
      game = (Risk) game.doAction(action);
    }
    return game.isGameOver() && game.getBoard().isPlayerStillAlive(playerId) ?
            5000 : !game.isGameOver() ? points : -1000;
  }

  /**
   * Calculates the reward that should be given during simulation based on the previous action.
   * @param gameAfter the game state after the action in question has been applied. The action is obtained via calling
   *                  {@link Risk#getPreviousAction()} on this instance of {@link Risk}.
   * @param gameBefore the game state exactly before the action in question was executed.
   * @return a number that represents the reward that should be given for choosing the action in question
   */
  private double calculateRewardForPreviousAction(Risk gameAfter, Risk gameBefore) {
    double reward = 0.d;
    int targetId = RiskUtils.getTargetOfAction(gameAfter.getPreviousAction());
    int initId = gameAfter.getPreviousAction().attackingId();
    if(targetId >= 0) {
      HashMap<Integer, Double> territoryRewards = distributeTerritoryRewards((Risk) gameAfter.getGame(playerId));
      if(RiskUtils.isInitialPlacingPhase(gameAfter.getBoard())) {
        reward += territoryRewards.get(targetId);
      } else if(RiskUtils.isTerritoryOfEnemy((Risk) gameBefore.getGame(playerId), targetId)) {
        //we are attacking
        reward += getRewardForAttack((Risk) gameBefore.getGame(playerId), initId, targetId);
        if(RiskUtils.isTerritoryOfEnemy((Risk) gameAfter.getGame(playerId), targetId)) {
          //give reward for winning a territory based on what that territory gives
          reward += territoryRewards.get(targetId);
        }
      } else if(initId == -1) {
        //we are reinforcing
        reward += getRewardForReinforcing(gameBefore.getBoard().getTerritoryTroops(targetId),
                gameAfter.getBoard().getTerritoryTroops(targetId),
                RiskUtils.getTotalTroopsOfNeighbouringEnemies((Risk) gameAfter.getGame(playerId),
                        new ArrayList<>(gameAfter.getGame(playerId).getBoard().neighboringEnemyTerritories(targetId))));
      } else {
        //we are fortifying
        reward += getRewardForFortifying(gameAfter, initId, targetId);
      }
    } else if (targetId == -1) {
      reward += getRewardForCasualties(gameAfter.getPreviousAction());
    } else if (targetId == -2) {
      reward += getRewardForOccupy(gameBefore, gameAfter, gameAfter.getPreviousAction());
    } else if (targetId == -3) {
      reward += getRewardForCards();
    }
    return reward;
  }

  /**
   * Gives a reward based on how good a fortification was
   * @param game the game state after the fortifying action has been executed
   * @param initId the id of the fortifying {@link RiskTerritory} (where troops come from)
   * @param targetId the id of the fortified {@link RiskTerritory} (where troops go to)
   * @return a number representing the reward given for this fortification
   */
  private double getRewardForFortifying(Risk game, int initId, int targetId) {
    if(RiskUtils.isNewTerritoryCloserToEnemy(game, initId, targetId)) {
      return RewardFactors.FORTIFIED_TERRITORY_CLOSER_TO_ENEMY;
    } else {
      return RewardFactors.FORTIFIED_TERRITORY_NOT_CLOSER_TO_ENEMY;
    }
  }

  /**
   * Gives a reward based on how good a reinforcement was. An extra reward is issued if before the reinforcements
   * the total amount of enemy troops in neighbouring territories was bigger than the ones in the reinforced territory
   * and afterwards this is no longer the case.
   * @param troopsBefore the amount of troops that are in the territory to be reinforced before that reinforcement happens
   * @param troopsAfter the amount of troops that are in the territory to be reinforced after that reinforcement happens
   * @param totalEnemyTroops the total amount of troops stationed in all surrounding enemy territories
   * @return a number representing the reward given for this reinforcement
   */
  private double getRewardForReinforcing(int troopsBefore, int troopsAfter, int totalEnemyTroops) {
    if(totalEnemyTroops == 0)
      return RewardFactors.REINFORCED_TERRITORY_WITHOUT_ENEMY_NEARBY;
    int differenceAfter = totalEnemyTroops - troopsAfter;
    int differenceBefore = totalEnemyTroops - troopsBefore;
    double reward = 0.d;
    if(differenceBefore <= 0) {
      //same or less troops than enemy
      reward += RewardFactors.REINFORCED_TERRITORY_WITH_MORE_ENEMY_TROOPS_NEARBY;
      if(differenceAfter > 0) {
        //after reinforcing we have more troops than the nearby enemy territories combined
        reward += RewardFactors.REINFORCED_TERRITORY_AND_HAS_MORE_TROOPS_THAN_ENEMY_AFTER;
      }
    }
    return reward;
  }

  /**
   * Gives a reward based on how good a reinforcement was. Gives more rewards if more than one more unit than the enemy
   * has to defend was used.
   * @param game the game state after the attack
   * @param attackingId the id of the territory where the attack is happening from
   * @param defendingId the id of the territory that is being attacked
   * @return
   */
  private double getRewardForAttack(Risk game, int attackingId, int defendingId) {
    Map<Integer, RiskTerritory> territories = game.getBoard().getTerritories();
    int troopDifference = territories.get(attackingId).getTroops() - territories.get(defendingId).getTroops();
    if (troopDifference <= 0) {
      return RewardFactors.LESS_TROOPS_FOR_ATTACK;
    }
    if (troopDifference == 1) {
      return RewardFactors.ONE_MORE_UNIT_FOR_ATTACK;
    }
    if (troopDifference == 2){
      return RewardFactors.TWO_MORE_UNITS_FOR_ATTACK;
    }
    return RewardFactors.THREE_OR_MORE_UNITS_FOR_ATTACK;
  }

  private double getRewardForCasualties(RiskAction action) {
    return action.attackerCasualties() - action.defenderCasualties() < 0 ?
            RewardFactors.LESS_CASUALTIES_REWARD_FACTOR : RewardFactors.MORE_CASUALTIES_REWARD_FACTOR;
  }

  private double getRewardForOccupy(Risk gameBefore, Risk gameAfter, RiskAction action) {
    double rewards = 0;
    int targetId = RiskUtils.getTargetOfAction(gameBefore.getActionRecords().get(gameBefore.getActionRecords().size()-2).getAction());
    int attacking = RiskUtils.getTargetOfAction(gameBefore.getActionRecords().get(gameBefore.getActionRecords().size()-2).getAction());
    int placedTroops = action.troops();

    // If territory is closer to enemy
    if (RiskUtils.isNewTerritoryCloserToEnemy(gameAfter, attacking, targetId) && placedTroops > 1) {
      rewards += RewardFactors.OCCUPY_INITIAL_TERRITORY_CLOSER_TO_ENEMY;
    }

    int availableTroops = gameAfter.getGame().getBoard().getTerritoryTroops(attacking) + placedTroops - 1;
    int enemyTroopsSurroundingTarget = RiskUtils.getTotalTroopsOfNeighbouringEnemies(gameAfter, targetId);
    int enemyTroopsSurroundingSource = RiskUtils.getTotalTroopsOfNeighbouringEnemies(gameAfter, attacking);

    // To nothing, because any decision could be wrong
    if (availableTroops < enemyTroopsSurroundingSource && availableTroops < enemyTroopsSurroundingTarget) {
      return rewards;
    }

    // Occupy has two bad pairings
    if (availableTroops - placedTroops < enemyTroopsSurroundingSource && placedTroops < enemyTroopsSurroundingTarget) {
      rewards += RewardFactors.OCCUPY_BOTH_TERRITORIES_HAVE_TOO_LESS_TROOPS;
    }

    return rewards;
  }

  private double getRewardForCards() {
    return RewardFactors.CARD_PLAYED_REWARD_FACTOR;
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

  private HashMap<Integer, Double> distributeTerritoryRewards(Risk game) {
    RiskBoard board = game.getBoard();
    HashMap<Integer, Double> territoryRewards = new HashMap<>();
    HashMap<Integer, Integer> occupiedContinents = createOccupiedContinentsMap(game);
    HashMap<Integer, Double> contRewards = updateRewardsByContinent(occupiedContinents);
    boolean isPlacingPhase = RiskUtils.isInitialPlacingPhase(game.getBoard());

    board.getTerritoryIds().forEach(id -> {
      double rewardToGive = 0.d;
      Set<Integer> neighbors = board.neighboringTerritories(id);
      List<Integer> nList = new ArrayList<>(neighbors);

      if(!isPlacingPhase && isLastEnemyOnContinent(game, id)) {
        List<Integer> neighborsOnContinent = nList.stream()
                .filter(x -> board.getTerritories().get(id).getContinentId() == board.getTerritories().get(x).getContinentId())
                .collect(Collectors.toList());
        for(Integer neighborId : neighborsOnContinent) {
          double toAdd = territoryRewards.get(neighborId) != null ? territoryRewards.get(neighborId) : 0.d;
          toAdd += board.getTerritories().get(id).getTroops() < board.getTerritories().get(neighborId).getTroops() ?
                  RewardFactors.MORE_TROOPS_NEAR_LAST_ENEMY_TERRITORY : 0.d;
          territoryRewards.put(neighborId, toAdd);
        }
      }

      if(RiskUtils.territoriesBelongToDifferentContinents(board, nList)) {
        //territory is a transition
        rewardToGive += RewardFactors.TRANSITION_REWARD_FACTOR;
        //all neighbouring territories get reward since they are one territory away from a transition
        neighbors.forEach(neighborId -> {
          double toAdd = territoryRewards.get(neighborId) != null ? territoryRewards.get(neighborId) : 0.d;
          territoryRewards.put(neighborId,
                  toAdd + RewardFactors.TRANSITION_NEIGHBOR_REWARD_FACTOR);
        });
      }
      int continentId = board.getTerritories().get(id).getContinentId();
      if(occupiedContinents.containsKey(continentId)) {
        rewardToGive += RewardFactors.CONTINENT_REWARD_FACTOR * contRewards.get(continentId);
        int neighboringEnemyTerritories = board.neighboringEnemyTerritories(id).size();
        if(neighboringEnemyTerritories > 0 && neighboringEnemyTerritories < 3) {
          rewardToGive += RewardFactors.NEAR_ENEMY_REWARD_FACTOR;
        }
      }

      if(game.getCurrentPlayer() == playerId && isLastAvailableOfEnemyContinent(game, id)) {
        rewardToGive += RewardFactors.LAST_ON_ENEMY_CONTINENT_REWARD_FACTOR;
      }

      double toAdd = territoryRewards.get(id) != null ? territoryRewards.get(id) : 0.d;
      territoryRewards.put(id, toAdd + rewardToGive);
    });
    return territoryRewards;
  }

  private HashMap<Integer, Integer> createOccupiedContinentsMap(Risk game) {
    RiskBoard board = game.getBoard();
    HashMap<Integer, Integer> occupiedContinents = new HashMap<>();

    for(Map.Entry<Integer, RiskTerritory> entry : board.getTerritories().entrySet()){
      if(entry.getValue().getOccupantPlayerId() == playerId) {
        if(occupiedContinents.containsKey(entry.getValue().getContinentId())) {
          occupiedContinents.put(entry.getValue().getContinentId(),
                  occupiedContinents.get(entry.getValue().getContinentId()) + 1);
        } else {
          occupiedContinents.put(entry.getValue().getContinentId(), 1);
        }
      }
    }
    return occupiedContinents;
  }

  private boolean isLastEnemyOnContinent(Risk game, int territoryId) {
    if(game.getBoard().getTerritories().get(territoryId).getOccupantPlayerId() == playerId)
      return false;
    Continent c = continents.get(game.getBoard().getTerritories().get(territoryId).getContinentId());
    int seizedTerritories = 0;
    for(RiskTerritory t : c.getTerritories()) {
      if(t.getOccupantPlayerId() == playerId) {
        seizedTerritories++;
      }
    }
    return seizedTerritories + 1 == c.getTerritories().size();
  }

  private boolean isLastAvailableOfEnemyContinent(Risk game, int territoryId) {
    if(opponentIds.contains(game.getBoard().getTerritories().get(territoryId).getOccupantPlayerId()))
      return false;
    Continent c = continents.get(game.getBoard().getTerritories().get(territoryId).getContinentId());
    int seizedTerritories = 0;
    for(RiskTerritory t : c.getTerritories()) {
      if(opponentIds.contains(t.getOccupantPlayerId())) {
        seizedTerritories++;
      }
    }
    return seizedTerritories + 1 == c.getTerritories().size();
  }

  private void createRewardsByContinent() {
    continents.forEach((id, continent) -> {
      double reward = 0.d;
      if(continent.getTerritories().size() < 5) {
        //small continents are easier to fill => higher reward
        reward += RewardFactors.SMALL_CONTINENT_REWARD_FACTOR;
      } else if(continent.getTerritories().size() >= 5) {
        reward += RewardFactors.BIG_CONTINENT_REWARD_FACTOR;
      }
      continent.setBaseReward(reward);
    });
  }

  private HashMap<Integer, Double> updateRewardsByContinent(HashMap<Integer, Integer> occupiedContinents) {
    HashMap<Integer, Double> contRewards = new HashMap<>();
    continents.forEach((id, continent) -> {
      if(occupiedContinents.containsKey(id)) {
        Double reward = continent.getBaseReward();
        //prioritize continents we already have some territories in
        reward += RewardFactors.OCCUPIED_CONTINENT_REWARD_FACTOR +
                occupiedContinents.get(id) * RewardFactors.OCCUPIED_CONTINENT_ADDITIONAL_FOR_EACH_TERRITORY_ALREADY_OCCUPIED_REWARD_FACTOR
                * continent.getBaseReward();
        contRewards.put(id, reward);
      }
    });
    return contRewards;
  }

  private HashMap<Integer, Continent> createContinentsWithAllTerritories(RiskBoard board) {
    HashMap<Integer, Continent> continents = new HashMap<>();
    board.getTerritories().forEach((id, territory) -> {
      if(!continents.containsKey(territory.getContinentId())) {
        Continent continent = new Continent(new ArrayList<>(),
                board.getContinents().get(territory.getContinentId()),
                territory.getContinentId());
        continents.put(territory.getContinentId(), continent);
      }
      continents.get(territory.getContinentId()).getTerritories().add(territory);
    });
    return continents;
  }
}