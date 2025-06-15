package risk.agent.mc.data;

/**
 * These are the values that are supposed to be given/subtracted during the simulation process for different actions.
 */
public class RewardFactors {
  public static final double CONTINENT_REWARD_FACTOR = 1;
  public static final double TRANSITION_REWARD_FACTOR = 0;
  public static final double NEAR_ENEMY_REWARD_FACTOR = 0;
  public static final double TRANSITION_NEIGHBOR_REWARD_FACTOR = 0;
  public static final double SMALL_CONTINENT_REWARD_FACTOR = 2;
  public static final double BIG_CONTINENT_REWARD_FACTOR = 1;
  public static final double OCCUPIED_CONTINENT_REWARD_FACTOR = 2;
  public static final double OCCUPIED_CONTINENT_ADDITIONAL_FOR_EACH_TERRITORY_ALREADY_OCCUPIED_REWARD_FACTOR = 1.2;
  public static final double LAST_ON_ENEMY_CONTINENT_REWARD_FACTOR = 0;
  public static final double CARD_PLAYED_REWARD_FACTOR = 0.00;
  public static final double LESS_CASUALTIES_REWARD_FACTOR = 5;
  public static final double MORE_CASUALTIES_REWARD_FACTOR = -5;
  public static final double MORE_TROOPS_NEAR_LAST_ENEMY_TERRITORY = 1;
  public static final double LESS_TROOPS_FOR_ATTACK = -10;
  public static final double ONE_MORE_UNIT_FOR_ATTACK = 1;
  public static final double TWO_MORE_UNITS_FOR_ATTACK = 4;
  public static final double THREE_OR_MORE_UNITS_FOR_ATTACK = 10;
  public static final double REINFORCED_TERRITORY_WITHOUT_ENEMY_NEARBY = -6;
  public static final double REINFORCED_TERRITORY_WITH_MORE_ENEMY_TROOPS_NEARBY = 9;
  public static final double REINFORCED_TERRITORY_AND_HAS_MORE_TROOPS_THAN_ENEMY_AFTER = 2;
  public static final double FORTIFIED_TERRITORY_CLOSER_TO_ENEMY = 20;
  public static final double FORTIFIED_TERRITORY_NOT_CLOSER_TO_ENEMY = -20;
  public static final double OCCUPY_INITIAL_TERRITORY_CLOSER_TO_ENEMY = -3;
  public static final double OCCUPY_BOTH_TERRITORIES_HAVE_TOO_LESS_TROOPS = -5;
}
