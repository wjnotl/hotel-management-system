package view.vip;

import entity.VipSystemConfig;
import util.ConsoleUtil;

public class VipSettingsView {

  public int displayMasterSettingsMenu(VipSystemConfig config) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("VIP SYSTEM SETTINGS & CONFIGURATION");

    System.out.println("Current Strategy : " + config.getActiveStrategyName());
    System.out.println("Active Equation  : " + config.getActiveFormulaInfix() + "\n");
    System.out.println("------------------------------------------------------");
    System.out.println("1. Manage Scoring Strategies");
    System.out.println("2. Tweak Operational Rules");
    System.out.println("3. Adjust Component Weights");
    System.out.println("4. Back\n");

    return ConsoleUtil.getMenuInput("Choose an option: ", 1, 4).getAsInt();
  }

  public int displayStrategyEngineMenu() {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("STRATEGY CONFIGURATION ENGINE");
    System.out.println("1. Apply Strategy Preset");
    System.out.println("2. Build Custom Formula Wizard");
    System.out.println("3. Back\n");

    return ConsoleUtil.getMenuInput("Choose an option: ", 1, 3).getAsInt();
  }

  public int displayPresetStrategyMenu() {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("SELECT PRESET STRATEGY");
    System.out.println("1. Strict Loyalty Focus");
    System.out.println("   Equation: TIER * W_TIER + WAIT\n");
    System.out.println("2. Balanced Lobby Flow");
    System.out.println(
        "   Equation: ( TIER * W_TIER ) + ( WAIT * W_TIME ) - ( STRIKES * W_STRIKE )\n");
    System.out.println("3. Emergency Customer Care");
    System.out.println("   Equation: ( TIER * W_TIER ) + ( BOILING * W_BOILING )\n");
    System.out.println("4. Back\n");

    return ConsoleUtil.getMenuInput("Choose an option: ", 1, 4).getAsInt();
  }

  public int displayWizardComponentTypeMenu(String currentFormula) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("STEP-BY-STEP FORMULA WIZARD");
    System.out.println(" [ CURRENT FORMULA ]");
    System.out.println(" >> " + (currentFormula.isEmpty() ? "( Empty )" : currentFormula) + "\n");
    System.out.println("------------------------------------------------------");
    System.out.println("Select component type to append:");
    System.out.println("------------------------------------------------------");
    System.out.println("1. [System Variable]         -> TIER, WAIT, STRIKES, BOILING");
    System.out.println("2. [Weight Variable]         -> W_TIER, W_TIME, W_BOILING, W_STRIKE");
    System.out.println("3. [Math Operator]           -> +, -, *, /");
    System.out.println("4. [Numeric Value]           -> Static constant");
    System.out.println("5. [Grouping Bracket]        -> ( / )");
    System.out.println("6. Save & Apply Formula");
    System.out.println("7. Back\n");

    return ConsoleUtil.getMenuInput("Choose an option: ", 1, 7).getAsInt();
  }

  public int displaySystemVariableSubmenu() {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("SELECT SYSTEM VARIABLE");
    System.out.println("1. TIER    -> Numerical priority mapped to member tier");
    System.out.println("2. WAIT    -> Physical minutes waiting in lobby queue");
    System.out.println("3. STRIKES -> Accumulated no-show penalty count");
    System.out.println("4. BOILING -> Starvation flag (1 if wait > limit, else 0)");
    System.out.println("5. Back\n");

    return ConsoleUtil.getMenuInput("Choose an option: ", 1, 5).getAsInt();
  }

  public int displayWeightVariableSubmenu() {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("SELECT DYNAMIC WEIGHT VARIABLE");
    System.out.println("1. W_TIER    -> Baseline tier weight modifier");
    System.out.println("2. W_TIME    -> Points accumulated per minute waiting");
    System.out.println("3. W_BOILING -> Point boost when patience limit exceeds");
    System.out.println("4. W_STRIKE  -> Point deduction applied per logged strike");
    System.out.println("5. Back\n");

    return ConsoleUtil.getMenuInput("Choose an option: ", 1, 5).getAsInt();
  }

  public int displayOperatorSubmenu() {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("SELECT MATH OPERATOR");
    System.out.println("1. Addition (+)");
    System.out.println("2. Subtraction (-)");
    System.out.println("3. Multiplication (*)");
    System.out.println("4. Division (/)");
    System.out.println("5. Back\n");

    return ConsoleUtil.getMenuInput("Choose an option: ", 1, 5).getAsInt();
  }

  public String promptNumericInput() {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("ENTER NUMERIC VALUE");
    System.out.println("Enter static integer or decimal constant to inject:\n");
    return ConsoleUtil.getStringInput("Choose an option: ");
  }

  public int displayBracketSubmenu() {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("SELECT GROUPING BRACKET");
    System.out.println("1. Open Parenthesis ( ");
    System.out.println("2. Close Parenthesis ) ");
    System.out.println("3. Back\n");

    return ConsoleUtil.getMenuInput("Choose an option: ", 1, 3).getAsInt();
  }

  public int displayOperationalRulesMenu() {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("OPERATIONAL RULES MANAGEMENT");
    System.out.println("1. Max No-Show Strike Limits (By Tier)");
    System.out.println("2. Starvation Patience Limits (By Tier)");
    System.out.println("3. No-Show Grace Periods (By Tier)");
    System.out.println("4. Back\n");

    return ConsoleUtil.getMenuInput("Choose an option: ", 1, 4).getAsInt();
  }

  public int displayTierSelectionMenu(String title, String dVal, String gVal, String sVal) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox(title);
    System.out.println("1. Diamond Tier   [Current: " + dVal + "]");
    System.out.println("2. Gold Tier      [Current: " + gVal + "]");
    System.out.println("3. Silver Tier    [Current: " + sVal + "]");
    System.out.println("4. Back\n");

    return ConsoleUtil.getMenuInput("Choose an option: ", 1, 4).getAsInt();
  }

  public Integer promptRuleIntInput(String targetVar, int currentVal) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("MODIFY " + targetVar.toUpperCase());
    System.out.println(" Target Variable : " + targetVar);
    System.out.println(" Current Value   : " + currentVal + "\n");
    System.out.println("------------------------------------------------------");
    System.out.println(" [Press ENTER / 'C' to Keep Current Value]\n");

    return ConsoleUtil.getIntegerInput(" [ New Value ]: ", 0);
  }

  public Double promptRuleDoubleInput(String targetVar, double currentVal) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("MODIFY " + targetVar.toUpperCase());
    System.out.println(" Target Variable : " + targetVar);
    System.out.println(" Current Value   : " + currentVal + "\n");
    System.out.println("------------------------------------------------------");
    System.out.println(" [Press ENTER / 'C' to Keep Current Value]\n");

    return ConsoleUtil.getDoubleInput(" [ New Value ]: ", 0.0);
  }

  public int displayComponentWeightsMenu() {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("BUSINESS COMPONENT WEIGHT CONFIG");
    System.out.println("1. Tier Base Values (TIER)");
    System.out.println("2. Patience Accumulation Rates (W_TIME)");
    System.out.println("3. Boiling Point Boosts (W_BOILING)");
    System.out.println("4. Strike Penalties (W_STRIKE)");
    System.out.println("5. Back\n");

    return ConsoleUtil.getMenuInput("Choose an option: ", 1, 5).getAsInt();
  }
}
