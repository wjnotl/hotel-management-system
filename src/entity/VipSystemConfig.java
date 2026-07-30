package entity;

import java.io.Serializable;

public class VipSystemConfig implements Serializable {
  private static final long serialVersionUID = 1L;

  private String activeStrategyName;
  private String activeFormulaInfix;

  // Tier-Specific Patience Limits (in minutes)
  private int diamondPatienceLimitMins;
  private int goldPatienceLimitMins;
  private int silverPatienceLimitMins;

  // Tier-Specific No-Show Grace Windows (in minutes)
  private int diamondGraceWindowMins;
  private int goldGraceWindowMins;
  private int silverGraceWindowMins;

  // Tier-Specific Max Strikes Limits
  private int diamondMaxStrikes;
  private int goldMaxStrikes;
  private int silverMaxStrikes;

  // Tier Base Importance Values (TIER)
  private int diamondBaseValue;
  private int goldBaseValue;
  private int silverBaseValue;

  // Tier Patience Accumulation Rates (W_time)
  private double diamondTimeWeight;
  private double goldTimeWeight;
  private double silverTimeWeight;

  // Tier Boiling Boosts (W_boiling)
  private double diamondBoilingBoost;
  private double goldBoilingBoost;
  private double silverBoilingBoost;

  // Tier Strike Penalties (W_strike)
  private double diamondStrikePenalty;
  private double goldStrikePenalty;
  private double silverStrikePenalty;

  public VipSystemConfig() {
    // Default baseline initializations based on blueprint
    this.activeStrategyName = "Balanced Lobby Flow";
    this.activeFormulaInfix = "( TIER * W_TIER ) + ( WAIT * W_TIME ) - ( STRIKES * W_STRIKE )";

    this.diamondPatienceLimitMins = 30;
    this.goldPatienceLimitMins = 45;
    this.silverPatienceLimitMins = 60;

    this.diamondGraceWindowMins = 10;
    this.goldGraceWindowMins = 5;
    this.silverGraceWindowMins = 3;

    this.diamondMaxStrikes = 5;
    this.goldMaxStrikes = 3;
    this.silverMaxStrikes = 2;

    this.diamondBaseValue = 9000;
    this.goldBaseValue = 7000;
    this.silverBaseValue = 5000;

    this.diamondTimeWeight = 25.0;
    this.goldTimeWeight = 15.0;
    this.silverTimeWeight = 10.0;

    this.diamondBoilingBoost = 5000.0;
    this.goldBoilingBoost = 3000.0;
    this.silverBoilingBoost = 1500.0;

    this.diamondStrikePenalty = 1000.0;
    this.goldStrikePenalty = 500.0;
    this.silverStrikePenalty = 200.0;
  }

  // Getters & Setters
  public String getActiveStrategyName() {
    return activeStrategyName;
  }

  public void setActiveStrategyName(String activeStrategyName) {
    this.activeStrategyName = activeStrategyName;
  }

  public String getActiveFormulaInfix() {
    return activeFormulaInfix;
  }

  public void setActiveFormulaInfix(String activeFormulaInfix) {
    this.activeFormulaInfix = activeFormulaInfix;
  }

  public int getDiamondPatienceLimitMins() {
    return diamondPatienceLimitMins;
  }

  public void setDiamondPatienceLimitMins(int val) {
    this.diamondPatienceLimitMins = val;
  }

  public int getGoldPatienceLimitMins() {
    return goldPatienceLimitMins;
  }

  public void setGoldPatienceLimitMins(int val) {
    this.goldPatienceLimitMins = val;
  }

  public int getSilverPatienceLimitMins() {
    return silverPatienceLimitMins;
  }

  public void setSilverPatienceLimitMins(int val) {
    this.silverPatienceLimitMins = val;
  }

  public int getDiamondGraceWindowMins() {
    return diamondGraceWindowMins;
  }

  public void setDiamondGraceWindowMins(int val) {
    this.diamondGraceWindowMins = val;
  }

  public int getGoldGraceWindowMins() {
    return goldGraceWindowMins;
  }

  public void setGoldGraceWindowMins(int val) {
    this.goldGraceWindowMins = val;
  }

  public int getSilverGraceWindowMins() {
    return silverGraceWindowMins;
  }

  public void setSilverGraceWindowMins(int val) {
    this.silverGraceWindowMins = val;
  }

  public int getDiamondMaxStrikes() {
    return diamondMaxStrikes;
  }

  public void setDiamondMaxStrikes(int val) {
    this.diamondMaxStrikes = val;
  }

  public int getGoldMaxStrikes() {
    return goldMaxStrikes;
  }

  public void setGoldMaxStrikes(int val) {
    this.goldMaxStrikes = val;
  }

  public int getSilverMaxStrikes() {
    return silverMaxStrikes;
  }

  public void setSilverMaxStrikes(int val) {
    this.silverMaxStrikes = val;
  }

  public int getDiamondBaseValue() {
    return diamondBaseValue;
  }

  public void setDiamondBaseValue(int val) {
    this.diamondBaseValue = val;
  }

  public int getGoldBaseValue() {
    return goldBaseValue;
  }

  public void setGoldBaseValue(int val) {
    this.goldBaseValue = val;
  }

  public int getSilverBaseValue() {
    return silverBaseValue;
  }

  public void setSilverBaseValue(int val) {
    this.silverBaseValue = val;
  }

  public double getDiamondTimeWeight() {
    return diamondTimeWeight;
  }

  public void setDiamondTimeWeight(double val) {
    this.diamondTimeWeight = val;
  }

  public double getGoldTimeWeight() {
    return goldTimeWeight;
  }

  public void setGoldTimeWeight(double val) {
    this.goldTimeWeight = val;
  }

  public double getSilverTimeWeight() {
    return silverTimeWeight;
  }

  public void setSilverTimeWeight(double val) {
    this.silverTimeWeight = val;
  }

  public double getDiamondBoilingBoost() {
    return diamondBoilingBoost;
  }

  public void setDiamondBoilingBoost(double val) {
    this.diamondBoilingBoost = val;
  }

  public double getGoldBoilingBoost() {
    return goldBoilingBoost;
  }

  public void setGoldBoilingBoost(double val) {
    this.goldBoilingBoost = val;
  }

  public double getSilverBoilingBoost() {
    return silverBoilingBoost;
  }

  public void setSilverBoilingBoost(double val) {
    this.silverBoilingBoost = val;
  }

  public double getDiamondStrikePenalty() {
    return diamondStrikePenalty;
  }

  public void setDiamondStrikePenalty(double val) {
    this.diamondStrikePenalty = val;
  }

  public double getGoldStrikePenalty() {
    return goldStrikePenalty;
  }

  public void setGoldStrikePenalty(double val) {
    this.goldStrikePenalty = val;
  }

  public double getSilverStrikePenalty() {
    return silverStrikePenalty;
  }

  public void setSilverStrikePenalty(double val) {
    this.silverStrikePenalty = val;
  }
}
