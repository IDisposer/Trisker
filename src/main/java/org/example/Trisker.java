package org.example;

import at.ac.tuwien.ifs.sge.agent.*;
import at.ac.tuwien.ifs.sge.engine.Logger;
import at.ac.tuwien.ifs.sge.game.Game;
import at.ac.tuwien.ifs.sge.game.risk.board.Risk;
import at.ac.tuwien.ifs.sge.game.risk.board.RiskAction;
import at.ac.tuwien.ifs.sge.game.risk.board.RiskBoard;
import at.ac.tuwien.ifs.sge.game.risk.board.RiskTerritory;
import at.ac.tuwien.ifs.sge.util.Util;
import org.example.data.AttackRewardFactors;
import org.example.data.Continent;
import org.example.data.PlacingRewardFactors;
import org.example.data.RiskActionIdentifier;
import org.example.general.RiskState;
import org.example.log.EventLogService;
import org.example.mcts.UCBLogic;
import org.example.mcts.UCBNode;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.List;
import java.util.stream.Collectors;

//Run command for the game engine:
//java -jar sge-1.0.7-exe.jar match --file=sge-risk-1.0.7-exe.jar --directory=agentstest
//https://gitlab.com/StrategyGameEngine/sge-risk

public class Trisker extends AbstractGameAgent<Risk, RiskAction>
        implements GameAgent<Risk, RiskAction> {

  private HashMap<Integer, Continent> continents = null;
  private boolean isFirstRound = true;
  private HashSet<Integer> opponentIds = new HashSet<>();
  private static int counter = 0;
  private int proportion = 0;
  private final int TOTAL_RUNS_PER_ROUND = 1400;


  public Trisker(Logger log){
    super(log);
  }

  @Override
  public void setUp(int numberOfPlayers, int playerId) {
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
      counter++;
      //optionally set AbstractGameAgent timers
      super.setTimers(computationTime, timeUnit);
      if(!game.isGameOver()) {  // RiskState.isInitialPlacingPhase(game.getBoard())
        UCBNode root = startMCSTree(game);
        UCBLogic.expandAll(root, getActions(game));
        proportion = root.getChildren().size();
        UCBNode node = root;
        while(!shouldStopComputation()) {
          if(node.getVisits() == 0 && node.getChildren().isEmpty()) {
            double value = startSimulation(node);

            UCBLogic.backpropagate(node, value);
            node = root;
          } else if (node.getChildren().isEmpty()) {
            UCBLogic.expandAll(node, node.getState().getPossibleActions());
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

        EventLogService.logBoard("OWN", (Risk) game.doAction(bestAction).getGame());
        log.warn("BoardCounter: " + EventLogService.getBoardCounter());

        if(bestNode.getRiskAction().selected() >= 0 && isTerritoryOfEnemy(bestNode.getState(), bestNode.getRiskAction().selected())) {
          log.warn("Attacked: " + bestNode.getRiskAction() + "| against = " + bestNode.getState().getBoard().getTerritories().get(bestNode.getRiskAction().selected()).getTroops());
        }
        log.warn("Best one Taken: ");
        log.warn(bestNode.getRiskAction() + " with ucbscore: "
                + UCBLogic.calculateUCB(bestNode) + " t: " + bestNode.getTotal() + " v: " + bestNode.getVisits());
        log.warn("Out of: ");
        for(UCBNode child : root.getChildren()) {
          log.warn(child.getRiskAction() + " with ucbscore: "
                  + UCBLogic.calculateUCB(child)+ " t: " + child.getTotal() + " v: " + child.getVisits());
        }
        return bestAction;
      } else {
        System.exit(1);
        return List.copyOf(getActions(game)).get(0);
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
    return startRandomSimulation2(node);
  }

  private Set<RiskAction> getActions(Risk game) {
    if(!RiskState.isInitialPlacingPhase(game.getBoard()))
      return pruneBadEndphase(game,
              pruneBadReinforcements(game,
                      pruneBadAttacks(game,
                              groupActions(game.getPossibleActions()))));
    return game.getPossibleActions();
  }

  /**
   * Remove actions from the given actions that are attacks and
   * have troops that are less than or equal to the defender's troops.
   * Does not permute the original actions.
   * @param game the gamestate before the actions are taken as an instance of {@link Risk}
   * @param actions the actions to remove bad attacks from
   * @return the given actions minus the removed actions
   */
  private Set<RiskAction> pruneBadAttacks(Risk game, Set<RiskAction> actions) {
    int targetId;
    Set<RiskAction> goodActions = new HashSet<>();
    for(RiskAction action : actions) {
      targetId = getTargetOfAction(action);
      if(!isTerritoryOfEnemy(game, targetId)
              || game.getBoard().getTerritories().get(targetId).getTroops() < action.troops()) {
        //we are NOT attacking with less than or equal troops to the defender
        goodActions.add(action);
      }
    }
    return goodActions;
  }

  /**
   * Returns all actions minus reinforcements that don't reinforce a territory that is next to an enemy.
   * If no reinforcements have nearby enemies all actions are returned.
   * @param game
   * @param actions
   * @return
   */
  private Set<RiskAction> pruneBadReinforcements(Risk game, Set<RiskAction> actions) {
    int targetId;
    Set<RiskAction> goodActions = new HashSet<>();
    for(RiskAction action : actions) {
      targetId = getTargetOfAction(action);
      if (!(targetId >= 0 && action.attackingId() == -1) || !game.getBoard().neighboringEnemyTerritories(targetId).isEmpty()) {
        goodActions.add(action);
      }
    }
    return goodActions.isEmpty() ? actions : goodActions;
  }

  private Set<RiskAction> pruneBadEndphase(Risk game, Set<RiskAction> actions) {
    Set<RiskAction> goodActions = new HashSet<>(actions);
    RiskAction endPhase = null;
    boolean hasGoodActionLeft = false;
    for(RiskAction action : actions) {
      if (action.attackingId() == -2 && action.selected() == -4 && action.troops() == -8) {
        endPhase = action;
      }
      if(isTerritoryOfEnemy(game, action.selected())) {
        hasGoodActionLeft = true;
      }
      if(hasGoodActionLeft && endPhase != null) {
        goodActions.remove(endPhase);
        return goodActions;
      }
    }
    return goodActions;
  }

  private double startRandomSimulation(UCBNode node) {
    Risk game = new Risk(node.getState());
    game = (Risk) game.doAction(node.getRiskAction());
    int cnt = 0;
    while(!game.isGameOver() && !shouldStopComputation() && cnt < TOTAL_RUNS_PER_ROUND / proportion) { //RiskState.isInitialPlacingPhase(game.getBoard())
      RiskAction action = getActions(game).stream()
              .skip(random.nextInt(getActions(game).size())).findFirst().get();
      //log.warn(action.toString());
      game = (Risk) game.doAction(action);
      if(game.getCurrentPlayer() != playerId)
        opponentIds.add(game.getCurrentPlayer());
      cnt++;
    }
    //System.out.println(cnt);
    return game.isGameOver() && game.getBoard().isPlayerStillAlive(playerId) ?
            5000 : !game.isGameOver() ? calculateTotalRewardsOfPlayerMinusOpponentsRewards(playerId, game) : -1000;//calculateTotalRewardsOfPlayerMinusOpponentsRewards(playerId, game);
  }

  private double startRandomSimulation2(UCBNode node) {
    double points = 0.d;
    Risk game = new Risk(node.getState());
    Risk gameBefore = game;
    game = (Risk) game.doAction(node.getRiskAction());
    int cnt = 0;
    while(!game.isGameOver() && !shouldStopComputation() && cnt < 10) { //RiskState.isInitialPlacingPhase(game.getBoard())
      cnt++;
      if(game.getPreviousActionRecord().getPlayer() == playerId) {
        opponentIds.add(game.getCurrentPlayer());
        points += calculateRewardForPreviousAction(game, gameBefore) / cnt * 10;
      }
      Set<RiskAction> actions = game.getCurrentPlayer() != playerId ? game.getPossibleActions() : getActions(game);
      RiskAction action = actions.stream()
              .skip(random.nextInt(actions.size())).findFirst().get();
      gameBefore = (Risk) game.getGame(playerId);
      game = (Risk) game.doAction(action);
    }
    return game.isGameOver() && game.getBoard().isPlayerStillAlive(playerId) ?
            5000 : !game.isGameOver() ? points : -1000;//calculateTotalRewardsOfPlayerMinusOpponentsRewards(playerId, game);
  }

  private double calculateRewardForPreviousAction(Risk gameAfter, Risk gameBefore) {
    double reward = 0.d;
    int targetId = getTargetOfAction(gameAfter.getPreviousAction());
    int initId = gameAfter.getPreviousAction().attackingId();
    if(targetId >= 0) {
      HashMap<Integer, Double> territoryRewards = RiskState.isInitialPlacingPhase(gameAfter.getBoard()) ? distributeTerritoryRewards((Risk) gameAfter.getGame(playerId)) : distributeTerritoryRewardsAttack((Risk) gameAfter.getGame(playerId));
      if(RiskState.isInitialPlacingPhase(gameAfter.getBoard())) {
        reward += territoryRewards.get(targetId);
      } else if(isTerritoryOfEnemy(gameBefore, targetId)) {
        //we are attacking
        reward += getRewardForAttack(gameBefore, initId, targetId);
        if(isTerritoryOfEnemy(gameAfter, targetId)) {
          //give reward for winning a territory based on what that territory gives
          reward += territoryRewards.get(targetId);
        }
      } else if(initId == -1) {
        //we are reinforcing
        reward += getRewardForReinforcing(gameBefore.getBoard().getTerritoryTroops(targetId),
                gameAfter.getBoard().getTerritoryTroops(targetId),
                getTotalTroopsOfNeighbouringEnemies(gameAfter,
                        new ArrayList<>(gameAfter.getBoard().neighboringEnemyTerritories(targetId))));
      } else {
        //we are fortifying
        reward += getRewardForFortifying(gameAfter, initId, targetId);
      }
      /*
      Set<Integer> targetNeighbors = gameAfter.getBoard().neighboringTerritories(targetId);
      List<Integer> tnList = new ArrayList<>(targetNeighbors);
      */
    } else if (targetId == -1) {
      reward += getRewardForCasualties(gameAfter.getPreviousAction());
    } else if (targetId == -2) {
      reward += getRewardForOccupy(gameBefore, gameAfter, gameAfter.getPreviousAction());
    } else if (targetId == -3) {
      reward += getRewardForCards(gameBefore, gameAfter, gameAfter.getPreviousAction());
    }
    return reward;
  }

  /**
   * Groups actions with similar sources and targets into one action with the highest value between them.
   * @param actions the actions to reduce
   * @return the reduced actions
   */
  private Set<RiskAction> groupActions(Set<RiskAction> actions) {
    HashMap<RiskActionIdentifier, RiskAction> actionsMap = new HashMap<>();
    RiskActionIdentifier rai;
    for(RiskAction action : actions) {  //add the actions with the biggest values
      rai = new RiskActionIdentifier(action.attackingId(), action.defendingId());
      if(actionsMap.containsKey(rai)) {
        actionsMap.put(rai, actionsMap.get(rai).troops() > action.troops() ? actionsMap.get(rai) : action);
      } else {
        actionsMap.put(rai, action);
      }
    }
    Set<RiskAction> result = new HashSet<>();
    for(RiskAction action : actions) {   //add the actions with half of the biggest value (rounded down because int)
      rai = new RiskActionIdentifier(action.attackingId(), action.defendingId());
      if(action.troops() == actionsMap.get(rai).troops() / 2) {
        result.add(action);
      }
    }
    result.addAll(actionsMap.values());
    return result;
  }

  private int calculateDistanceToClosestEnemyTerritory(Risk game, int territoryId) {
    Queue<Integer> q = new LinkedList<>();
    int nrOfTerritories = game.getBoard().getTerritories().size();
    boolean[] visited = new boolean[nrOfTerritories];
    int[] distance = new int[nrOfTerritories];

    visited[territoryId] = true;
    distance[territoryId] = 0;
    q.add(territoryId);

    while (!q.isEmpty()) {
      int curr = q.poll();

      for (int neighbour : game.getBoard().neighboringTerritories(curr)) {
        if (!visited[neighbour]) {
          // Check if enemy territory
          if (game.getBoard().getTerritories().get(territoryId).getOccupantPlayerId() != playerId) {
            return distance[neighbour] + 1;
          }

          visited[neighbour] = true;
          q.add(neighbour);
          distance[neighbour] = distance[curr] + 1;
        }
      }
    }
    return -1;
  }

  private int getTotalTroopsOfNeighbouringEnemies(Risk game, Collection<Integer> neighbouringEnemyList) {
    int total = 0;
    Map<Integer, RiskTerritory> territories = game.getBoard().getTerritories();
    for(Integer id : neighbouringEnemyList) {
      total += territories.get(id).getTroops();
    }
    return total;
  }

  private int getTotalTroopsOfNeighbouringEnemies(Risk game, int territoryId) {
    return getTotalTroopsOfNeighbouringEnemies(game, game.getBoard().neighboringEnemyTerritories(territoryId));
  }

  private boolean isTerritoryOfEnemy(Risk game, int territoryId) {
    if(territoryId < 0) //territory id is a special id
      return false;
    int id = game.getBoard().getTerritories().get(territoryId).getOccupantPlayerId();
    return id != playerId && id != -1;
  }

  private double getRewardForFortifying(Risk game, int initId, int targetId) {
    if(isNewTerritoryCloserToEnemy(game, initId, targetId)) {
      return AttackRewardFactors.FORTIFIED_TERRITORY_CLOSER_TO_ENEMY;
    } else {
      return AttackRewardFactors.FORTIFIED_TERRITORY_NOT_CLOSER_TO_ENEMY;
    }
  }

  private double getRewardForReinforcing(int troopsBefore, int troopsAfter, int totalEnemyTroops) {
    if(totalEnemyTroops == 0)
      return AttackRewardFactors.REINFORCED_TERRITORY_WITHOUT_ENEMY_NEARBY;
    int differenceAfter = totalEnemyTroops - troopsAfter;
    int differenceBefore = totalEnemyTroops - troopsBefore;
    double reward = 0.d;
    if(differenceBefore <= 0) {
      //same or less troops than enemy
      reward += AttackRewardFactors.REINFORCED_TERRITORY_WITH_MORE_ENEMY_TROOPS_NEARBY;
      if(differenceAfter > 0) {
        //after reinforcing we have more troops than the nearby enemy territories combined
        reward += AttackRewardFactors.REINFORCED_TERRITORY_AND_HAS_MORE_TROOPS_THAN_ENEMY_AFTER;
      }
    }
    return reward;
  }

  private double getRewardForAttack(Risk game, int attackingId, int defendingId) {
    Map<Integer, RiskTerritory> territories = game.getBoard().getTerritories();
    int troopDifference = territories.get(attackingId).getTroops() - territories.get(defendingId).getTroops();
    //log.warn("Troops difference: " + troopDifference);
    if (troopDifference <= 0) {
      //log.warn(">>>>>>>>>>>>>>>>>>ATTACKED WITH LESS TROOPS!<<<<<<<<<<<<<<<<<<");
      return AttackRewardFactors.LESS_TROOPS_FOR_ATTACK;
    }
    if (troopDifference == 1) {
      return AttackRewardFactors.ONE_MORE_UNIT_FOR_ATTACK;
    }
    if (troopDifference == 2){
      return AttackRewardFactors.TWO_MORE_UNITS_FOR_ATTACK;
    }
    return AttackRewardFactors.THREE_OR_MORE_UNITS_FOR_ATTACK;
  }

  private double getRewardForCasualties(RiskAction action) {
    return action.attackerCasualties() - action.defenderCasualties() < 0 ?
            AttackRewardFactors.LESS_CASUALTIES_REWARD_FACTOR : AttackRewardFactors.MORE_CASUALTIES_REWARD_FACTOR;
  }

  private double getRewardForOccupy(Risk gameBefore, Risk gameAfter, RiskAction action) {
    double rewards = 0;
    int targetId = getTargetOfAction(gameBefore.getActionRecords().get(gameBefore.getActionRecords().size()-2).getAction());
    int attacking = getTargetOfAction(gameBefore.getActionRecords().get(gameBefore.getActionRecords().size()-2).getAction());
    int placedTroops = action.troops();

    // If territory is closer to enemy
    if (isNewTerritoryCloserToEnemy(gameAfter, attacking, targetId) && placedTroops > 1) {
      rewards += AttackRewardFactors.OCCUPY_INITIAL_TERRITORY_CLOSER_TO_ENEMY;
    }

    int availableTroops = gameAfter.getGame().getBoard().getTerritoryTroops(attacking) + placedTroops - 1;
    int enemyTroopsSurroundingTarget = getTotalTroopsOfNeighbouringEnemies(gameAfter, targetId);
    int enemyTroopsSurroundingSource = getTotalTroopsOfNeighbouringEnemies(gameAfter, attacking);

    // To nothing, because any decision could be wrong
    if (availableTroops < enemyTroopsSurroundingSource && availableTroops < enemyTroopsSurroundingTarget) {
      return rewards;
    }

    // Occupy has two bad pairings
    if (availableTroops - placedTroops < enemyTroopsSurroundingSource && placedTroops < enemyTroopsSurroundingTarget) {
      rewards += AttackRewardFactors.OCCUPY_BOTH_TERRITORIES_HAVE_TOO_LESS_TROOPS;
    }

    return rewards;
  }

  private double getRewardForCards(Risk gameBefore, Risk gameAfter, RiskAction previousAction) {
    return AttackRewardFactors.CARD_PLAYED_REWARD_FACTOR;
  }

  private boolean isNewTerritoryCloserToEnemy(Risk game, int initialT, int newT) {
    return calculateDistanceToClosestEnemyTerritory(game, newT) - calculateDistanceToClosestEnemyTerritory(game, initialT) < 0;
  }

  private int getTargetOfAction(RiskAction action) {
    return action.selected();
  }

  private double calculateTotalRewardsOfPlayerMinusOpponentsRewards(int playerId, Risk game) {
    double rewards = calculateTotalRewardsOfPlayer(playerId, (Risk) game.getGame(playerId), distributeTerritoryRewards((Risk) game.getGame(playerId)));
    //log.warn("Total rewards: " + rewards);
    for(Integer opponentId : opponentIds) {
      //log.warn("Total rewards Opponent: " + calculateTotalRewardsOfPlayer(opponentId, (Risk) game.getGame(opponentId), distributeTerritoryRewards((Risk) game.getGame(opponentId))));
      rewards -= calculateTotalRewardsOfPlayer(opponentId, (Risk) game.getGame(opponentId), distributeTerritoryRewards((Risk) game.getGame(opponentId)));
    }
    //log.warn("");
    return rewards;
  }

  private double calculateTotalRewardsOfPlayer(int playerId, Risk game, HashMap<Integer, Double> territoryRewards) {
    RiskBoard board = game.getBoard();
    double total = 0;

    for(Map.Entry<Integer, RiskTerritory> entry : board.getTerritories().entrySet()){
      if(entry.getValue().getOccupantPlayerId() == playerId){
        total += territoryRewards.get(entry.getKey());
      }
    }
    //log.warn(playerId + " Total: " + total
    //        + "(" + board.getTerritories().values().stream().filter(territory -> territory.getOccupantPlayerId() == playerId).count() + ")");
    return total;
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

    board.getTerritoryIds().forEach(id -> {
      double rewardToGive = 0.d;
      Set<Integer> neighbors = board.neighboringTerritories(id);
      List<Integer> nList = new ArrayList<>(neighbors);

      if(territoriesBelongToDifferentContinents(board, nList)) {
        //territory is a transition
        rewardToGive += PlacingRewardFactors.TRANSITION_REWARD_FACTOR;
        //all neighbouring territories get reward since they are one territory away from a transition
        neighbors.forEach(neighborId -> {
          double toAdd = territoryRewards.get(neighborId) != null ? territoryRewards.get(neighborId) : 0.d;
          territoryRewards.put(neighborId,
                toAdd + PlacingRewardFactors.TRANSITION_NEIGHBOR_REWARD_FACTOR);
        });
      }
      int continentId = board.getTerritories().get(id).getContinentId();
      if(occupiedContinents.containsKey(continentId)) {
        rewardToGive += PlacingRewardFactors.CONTINENT_REWARD_FACTOR * contRewards.get(continentId);
        int neighboringEnemyTerritories = board.neighboringEnemyTerritories(id).size();
        if(neighboringEnemyTerritories > 0 && neighboringEnemyTerritories < 3) {
          rewardToGive += PlacingRewardFactors.NEAR_ENEMY_REWARD_FACTOR;
        }
      }

      if(game.getCurrentPlayer() == playerId && isLastAvailableOfEnemyContinent(game, id)) {
        rewardToGive += PlacingRewardFactors.LAST_ON_ENEMY_CONTINENT_REWARD_FACTOR;
      }
      double toAdd = territoryRewards.get(id) != null ? territoryRewards.get(id) : 0.d;
      territoryRewards.put(id, toAdd + rewardToGive);
    });
    return territoryRewards;
  }

  private HashMap<Integer, Double> distributeTerritoryRewardsAttack(Risk game) {
    RiskBoard board = game.getBoard();
    HashMap<Integer, Double> territoryRewards = new HashMap<>();
    HashMap<Integer, Integer> occupiedContinents = createOccupiedContinentsMap(game);
    HashMap<Integer, Double> contRewards = updateRewardsByContinent(occupiedContinents);

    board.getTerritoryIds().forEach(id -> {
      double rewardToGive = 0.d;
      Set<Integer> neighbors = board.neighboringTerritories(id);
      List<Integer> nList = new ArrayList<>(neighbors);

      if(isLastEnemyOnContinent(game, id)) {
        List<Integer> neighborsOnContinent = nList.stream()
                .filter(x -> board.getTerritories().get(id).getContinentId() == board.getTerritories().get(x).getContinentId())
                .collect(Collectors.toList());
        for(Integer neighborId : neighborsOnContinent) {
          double toAdd = territoryRewards.get(neighborId) != null ? territoryRewards.get(neighborId) : 0.d;
          toAdd += board.getTerritories().get(id).getTroops() < board.getTerritories().get(neighborId).getTroops() ?
                  AttackRewardFactors.MORE_TROOPS_NEAR_LAST_ENEMY_TERRITORY : 0.d;
          territoryRewards.put(neighborId, toAdd);
        }
      }

      if(territoriesBelongToDifferentContinents(board, nList)) {
        //territory is a transition
        rewardToGive += AttackRewardFactors.TRANSITION_REWARD_FACTOR;
        //all neighbouring territories get reward since they are one territory away from a transition
        neighbors.forEach(neighborId -> {
          double toAdd = territoryRewards.get(neighborId) != null ? territoryRewards.get(neighborId) : 0.d;
          territoryRewards.put(neighborId,
                  toAdd + AttackRewardFactors.TRANSITION_NEIGHBOR_REWARD_FACTOR);
        });
      }
      int continentId = board.getTerritories().get(id).getContinentId();
      if(occupiedContinents.containsKey(continentId)) {
        rewardToGive += AttackRewardFactors.CONTINENT_REWARD_FACTOR * contRewards.get(continentId);
        int neighboringEnemyTerritories = board.neighboringEnemyTerritories(id).size();
        if(neighboringEnemyTerritories > 0 && neighboringEnemyTerritories < 3) {
          rewardToGive += AttackRewardFactors.NEAR_ENEMY_REWARD_FACTOR;
        }
      }

      if(game.getCurrentPlayer() == playerId && isLastAvailableOfEnemyContinent(game, id)) {
        rewardToGive += AttackRewardFactors.LAST_ON_ENEMY_CONTINENT_REWARD_FACTOR;
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
      if(entry.getValue().getOccupantPlayerId() == game.getCurrentPlayer()) {
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

  private boolean territoriesBelongToDifferentContinents(RiskBoard board, List<Integer> territoryIds) {
    Map<Integer, RiskTerritory> territories = board.getTerritories();
    int baseline = territories.get(territoryIds.get(0)).getContinentId();
    Integer current = null;
    for (Integer territoryId : territoryIds) {
      current = territories.get(territoryId).getContinentId();
      if (baseline != current)
        return true;
    }
    return false;
  }

  private void createRewardsByContinent() {
    continents.forEach((id, continent) -> {
      double reward = 0.d;
      if(continent.getTerritories().size() < 5) {
        //small continents are easier to fill => higher reward
        reward += PlacingRewardFactors.SMALL_CONTINENT_REWARD_FACTOR;
      } else if(continent.getTerritories().size() >= 5) {
        reward += PlacingRewardFactors.BIG_CONTINENT_REWARD_FACTOR;
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
        reward += PlacingRewardFactors.OCCUPIED_CONTINENT_REWARD_FACTOR +
                occupiedContinents.get(id) * PlacingRewardFactors.OCCUPIED_CONTINENT_ADDITIONAL_FOR_EACH_TERRITORY_ALREADY_OCCUPIED_REWARD_FACTOR
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