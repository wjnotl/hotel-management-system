package entity;

import java.io.Serializable;

public class VipSystemConfig implements Serializable {
  private static final long serialVersionUID = 1L;

  private String activeStrategyName;
  private String activeFormulaInfix;

  // Tier-Specific Patience Limits / SLA Targets (in minutes)
  private int diamondPatienceLimitMins;
  private int goldPatienceLimitMins;
  private int silverPatienceLimitMins;

  // Tier-Specific Boiling Point Limits / Boost Triggers (in minutes)
  private int diamondBoilingLimitMins;
  private int goldBoilingLimitMins;
  private int silverBoilingLimitMins;

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

  // Tier Boiling Boosts (W_boiling)
  private double diamondBoilingBoost;
  private double goldBoilingBoost;
  private double silverBoilingBoost;

  // Tier Strike Penalties (W_strike)
  private double diamondStrikePenalty;
  private double goldStrikePenalty;
  private double silverStrikePenalty;

  // Executive Report Alert Targets (%)
  private double diamondSlaTargetPct;
  private double goldSlaTargetPct;
  private double silverSlaTargetPct;

  private double diamondEvictionRateTargetPct;
  private double goldEvictionRateTargetPct;
  private double silverEvictionRateTargetPct;

  private double diamondGraceUtilTargetPct;
  private double goldGraceUtilTargetPct;
  private double silverGraceUtilTargetPct;

  public VipSystemConfig() {
    resetToDefaults();
  }

  public void resetToDefaults() {
    this.activeStrategyName = "Balanced Lobby Flow";
    this.activeFormulaInfix = "TIER + ( BOILING * W_BOILING ) - ( STRIKES * W_STRIKE )";

    // Patience Limits (SLA Targets)
    this.diamondPatienceLimitMins = 15;
    this.goldPatienceLimitMins = 30;
    this.silverPatienceLimitMins = 45;

    // Boiling Limits (Boost Triggers)
    this.diamondBoilingLimitMins = 10;
    this.goldBoilingLimitMins = 20;
    this.silverBoilingLimitMins = 30;

    this.diamondGraceWindowMins = 10;
    this.goldGraceWindowMins = 15;
    this.silverGraceWindowMins = 20;

    this.diamondMaxStrikes = 3;
    this.goldMaxStrikes = 2;
    this.silverMaxStrikes = 2;

    this.diamondBaseValue = 9000;
    this.goldBaseValue = 7000;
    this.silverBaseValue = 5000;

    this.diamondBoilingBoost = 5000.0;
    this.goldBoilingBoost = 3000.0;
    this.silverBoilingBoost = 1500.0;

    this.diamondStrikePenalty = 1000.0;
    this.goldStrikePenalty = 500.0;
    this.silverStrikePenalty = 200.0;

    this.diamondSlaTargetPct = 90.0;
    this.goldSlaTargetPct = 80.0;
    this.silverSlaTargetPct = 70.0;

    this.diamondEvictionRateTargetPct = 5.0;
    this.goldEvictionRateTargetPct = 10.0;
    this.silverEvictionRateTargetPct = 15.0;

    this.diamondGraceUtilTargetPct = 70.0;
    this.goldGraceUtilTargetPct = 80.0;
    this.silverGraceUtilTargetPct = 85.0;
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

  public int getDiamondBoilingLimitMins() {
    return diamondBoilingLimitMins;
  }

  public void setDiamondBoilingLimitMins(int val) {
    this.diamondBoilingLimitMins = val;
  }

  public int getGoldBoilingLimitMins() {
    return goldBoilingLimitMins;
  }

  public void setGoldBoilingLimitMins(int val) {
    this.goldBoilingLimitMins = val;
  }

  public int getSilverBoilingLimitMins() {
    return silverBoilingLimitMins;
  }

  public void setSilverBoilingLimitMins(int val) {
    this.silverBoilingLimitMins = val;
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

  public double getDiamondSlaTargetPct() {
    return diamondSlaTargetPct;
  }

  public void setDiamondSlaTargetPct(double val) {
    this.diamondSlaTargetPct = val;
  }

  public double getGoldSlaTargetPct() {
    return goldSlaTargetPct;
  }

  public void setGoldSlaTargetPct(double val) {
    this.goldSlaTargetPct = val;
  }

  public double getSilverSlaTargetPct() {
    return silverSlaTargetPct;
  }

  public void setSilverSlaTargetPct(double val) {
    this.silverSlaTargetPct = val;
  }

  public double getDiamondEvictionRateTargetPct() {
    return diamondEvictionRateTargetPct;
  }

  public void setDiamondEvictionRateTargetPct(double val) {
    this.diamondEvictionRateTargetPct = val;
  }

  public double getGoldEvictionRateTargetPct() {
    return goldEvictionRateTargetPct;
  }

  public void setGoldEvictionRateTargetPct(double val) {
    this.goldEvictionRateTargetPct = val;
  }

  public double getSilverEvictionRateTargetPct() {
    return silverEvictionRateTargetPct;
  }

  public void setSilverEvictionRateTargetPct(double val) {
    this.silverEvictionRateTargetPct = val;
  }

  public double getDiamondGraceUtilTargetPct() {
    return diamondGraceUtilTargetPct;
  }

  public void setDiamondGraceUtilTargetPct(double val) {
    this.diamondGraceUtilTargetPct = val;
  }

  public double getGoldGraceUtilTargetPct() {
    return goldGraceUtilTargetPct;
  }

  public void setGoldGraceUtilTargetPct(double val) {
    this.goldGraceUtilTargetPct = val;
  }

  public double getSilverGraceUtilTargetPct() {
    return silverGraceUtilTargetPct;
  }

  public void setSilverGraceUtilTargetPct(double val) {
    this.silverGraceUtilTargetPct = val;
  }
}
