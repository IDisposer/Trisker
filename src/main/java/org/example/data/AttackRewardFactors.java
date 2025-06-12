package org.example.data;

public class AttackRewardFactors {
  public static double CONTINENT_REWARD_FACTOR = 1;
  public static double TRANSITION_REWARD_FACTOR = 0;
  public static double NEAR_ENEMY_REWARD_FACTOR = 0.00;
  public static double TRANSITION_NEIGHBOR_REWARD_FACTOR = 0;
  public static double SMALL_CONTINENT_REWARD_FACTOR = 15;
  public static double BIG_CONTINENT_REWARD_FACTOR = 1;
  public static double OCCUPIED_CONTINENT_REWARD_FACTOR = 2;
  public static double OCCUPIED_CONTINENT_ADDITIONAL_FOR_EACH_TERRITORY_ALREADY_OCCUPIED_REWARD_FACTOR = 5;
  public static double LAST_ON_ENEMY_CONTINENT_REWARD_FACTOR = 0;
  public static double CARD_PLAYED_REWARD_FACTOR = 0.00;
  public static double LESS_CASUALTIES_REWARD_FACTOR = 5;
  public static double MORE_CASUALTIES_REWARD_FACTOR = -5;
  public static double MORE_TROOPS_NEAR_LAST_ENEMY_TERRITORY = 10;
  public static double LESS_TROOPS_FOR_ATTACK = -1000;
  public static double ONE_MORE_UNIT_FOR_ATTACK = 10;
  public static double TWO_MORE_UNITS_FOR_ATTACK = 40;
  public static double THREE_OR_MORE_UNITS_FOR_ATTACK = 100;
  public static double REINFORCED_TERRITORY_WITHOUT_ENEMY_NEARBY = -60;
  public static double REINFORCED_TERRITORY_WITH_MORE_ENEMY_TROOPS_NEARBY = 90;
  public static double REINFORCED_TERRITORY_AND_HAS_MORE_TROOPS_THAN_ENEMY_AFTER = 200;
  public static double FORTIFIED_TERRITORY_CLOSER_TO_ENEMY = 100;
  public static double FORTIFIED_TERRITORY_NOT_CLOSER_TO_ENEMY = -20;
}
