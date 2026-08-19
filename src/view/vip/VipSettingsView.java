package view.vip;

import entity.VipSystemConfig;
import util.ConsoleUtil;

public class VipSettingsView {

  public int displayMasterSettingsMenu(VipSystemConfig config) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("VIP SYSTEM SETTINGS & CONFIGURATION");

    System.out.println("Current Strategy : " + config.getActiveStrategyName());
    System.out.println("Active Equation  : " + config.getActiveFormulaInfix() + "\n");
    System.out.println("1. Manage Scoring Strategies");
    System.out.println("2. Tweak Operational Rules");
    System.out.println("3. Adjust Component Weights");
    System.out.println("4. Edit Report Alert Target Limits");
    System.out.println("5. Apply Settings to Active Queue");
    System.out.println("6. Reset to Factory Default Settings");
    System.out.println("7. Back\n");

    return ConsoleUtil.getMenuInput("Choose an option: ", 1, 7).getAsInt();
  }

  public static String formatNumber(double val) {
    if (val == (long) val) {
      return String.format("%d", (long) val);
    }
    return String.format("%.4f", val).replaceAll("0+$", "").replaceAll("\\.$", "");
  }

  public int promptWizardStep(
      int stepNum, int totalSteps, String title, String description, boolean hasPrevious) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox(
        "QUEUE RECONCILIATION WIZARD (STEP " + stepNum + " OF " + totalSteps + ")");
    System.out.println(" [ " + title + " ]");
    System.out.println(" " + description + "\n");
    System.out.println(
        "----------------------------------------------------------------------------------------");
    System.out.println("1. Yes (Enable / Apply)");
    System.out.println("2. No  (Skip / Disable)");
    int optionCount = 2;
    if (hasPrevious) {
      System.out.println("3. Previous Step (Go back)");
      System.out.println("4. Cancel Wizard (Exit without changes)\n");
      optionCount = 4;
    } else {
      System.out.println("3. Cancel Wizard (Exit without changes)\n");
      optionCount = 3;
    }

    return ConsoleUtil.getMenuInput("Choose an option: ", 1, optionCount).getAsInt();
  }

  public boolean promptReconciliationConfirmation(
      boolean evictOverStrikes, boolean forceBoilingCheck, boolean updateActiveGraceTimers) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("QUEUE RECONCILIATION SUMMARY REVIEW");
    System.out.println(" Review the selected reconciliation parameters below before execution:\n");
    System.out.println(
        " 1. Strike Threshold Eviction  : " + (evictOverStrikes ? "[ ENABLED ]" : "[ DISABLED ]"));
    System.out.println(
        " 2. Boiling Status Re-eval     : " + (forceBoilingCheck ? "[ ENABLED ]" : "[ DISABLED ]"));
    System.out.println(
        " 3. Active Grace Timer Reset   : "
            + (updateActiveGraceTimers ? "[ ENABLED ]" : "[ DISABLED ]"));
    System.out.println(
        "\n"
            + "----------------------------------------------------------------------------------------");

    return ConsoleUtil.getMenuInput(
            "Confirm execution of selected queue reconciliation rules? (Y/N): ",
            new char[] {'Y', 'N'})
        .input
        .equalsIgnoreCase("Y");
  }

  public void displayApplySuccessScreen(int processedCount) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("QUEUE RECONCILIATION COMPLETE");
    System.out.println(" >> SUCCESS: Active queues recalculated and re-sorted successfully!");
    System.out.println(" >> Total Waiting Reservations Processed: " + processedCount + "\n");
    ConsoleUtil.printContinueMessage();
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
    System.out.println("   Equation: TIER - ( STRIKES * W_STRIKE )\n");
    System.out.println("2. Balanced Lobby Flow");
    System.out.println("   Equation: TIER + ( BOILING * W_BOILING ) - ( STRIKES * W_STRIKE )\n");
    System.out.println("3. Emergency Customer Care");
    System.out.println("   Equation: TIER + ( BOILING * W_BOILING )\n");
    System.out.println("4. Back\n");

    return ConsoleUtil.getMenuInput("Choose an option: ", 1, 4).getAsInt();
  }

  public int displayWizardComponentTypeMenu(
      String currentFormula,
      boolean allowOperand,
      boolean allowOperator,
      boolean allowOpenBracket,
      boolean allowCloseBracket,
      boolean allowUndo,
      boolean allowRedo,
      boolean allowSave) {

    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("STEP-BY-STEP FORMULA WIZARD");
    System.out.println(" [ CURRENT FORMULA ]");
    System.out.println(" >> " + (currentFormula.isEmpty() ? "( Empty )" : currentFormula) + "\n");
    System.out.println("------------------------------------------------------");
    System.out.println("Select component type to append:");
    System.out.println("------------------------------------------------------");

    int optionNum = 1;

    if (allowOperand) {
      System.out.println(optionNum++ + ". [System Variable]         -> TIER, STRIKES, BOILING");
      System.out.println(optionNum++ + ". [Weight Variable]         -> W_BOILING, W_STRIKE");
      System.out.println(optionNum++ + ". [Numeric Value]           -> Static constant");
    }

    if (allowOperator) {
      System.out.println(optionNum++ + ". [Math Operator]           -> +, -, *, /");
    }

    if (allowOpenBracket) {
      System.out.println(optionNum++ + ". [Grouping Bracket '(']    -> Open parenthesis");
    }

    if (allowCloseBracket) {
      System.out.println(optionNum++ + ". [Grouping Bracket ')']    -> Close parenthesis");
    }

    if (allowUndo) {
      System.out.println(optionNum++ + ". Undo Last Action");
    }

    if (allowRedo) {
      System.out.println(optionNum++ + ". Redo Action");
    }

    if (allowSave) {
      System.out.println(optionNum++ + ". Save & Apply Formula");
    }

    System.out.println(optionNum + ". Back\n");

    int maxOptions = optionNum;
    return ConsoleUtil.getMenuInput("Choose an option: ", 1, maxOptions).getAsInt();
  }

  public int displaySystemVariableSubmenu() {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("SELECT SYSTEM VARIABLE");
    System.out.println("1. TIER    -> Numerical base priority value mapped to member tier");
    System.out.println("2. STRIKES -> Accumulated no-show penalty count");
    System.out.println("3. BOILING -> Starvation flag (1 if wait >= boiling limit, else 0)");
    System.out.println("4. Back\n");

    return ConsoleUtil.getMenuInput("Choose an option: ", 1, 4).getAsInt();
  }

  public int displayWeightVariableSubmenu() {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("SELECT DYNAMIC WEIGHT VARIABLE");
    System.out.println("1. W_BOILING -> Point boost when boiling limit is exceeded");
    System.out.println("2. W_STRIKE  -> Point deduction applied per logged strike");
    System.out.println("3. Back\n");

    return ConsoleUtil.getMenuInput("Choose an option: ", 1, 3).getAsInt();
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
    System.out.println("Enter static integer or decimal constant to inject:");
    System.out.println(
        " (Supports up to 4 decimal places. Trailing zeros will be formatted cleanly)");
    System.out.println(
        "----------------------------------------------------------------------------------------");
    System.out.println(" Press ENTER / 'C' to Cancel\n");
    return ConsoleUtil.getStringInput("[ Numeric Value ]: ");
  }

  public int displayOperationalRulesMenu() {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("OPERATIONAL RULES MANAGEMENT");
    System.out.println("1. Tier Patience Limits (SLA Targets in Mins)");
    System.out.println("2. Tier Boiling Point Limits (Starvation Boost Triggers in Mins)");
    System.out.println("3. Max No-Show Strike Limits");
    System.out.println("4. No-Show Grace Period Windows (Holding Bay Mins)");
    System.out.println("5. Back\n");

    return ConsoleUtil.getMenuInput("Choose an option: ", 1, 5).getAsInt();
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

  public Integer promptRuleIntInput(
      String targetVar, int currentVal, int min, int max, String activeFormula) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("MODIFY " + targetVar.toUpperCase());
    System.out.println(" Active Formula  : " + activeFormula);
    System.out.println(" Target Variable : " + targetVar);
    System.out.println(" Current Value   : " + currentVal);
    System.out.println(" Allowed Range   : [" + min + " - " + max + "]\n");
    System.out.println("------------------------------------------------------");
    System.out.println(" Press ENTER / 'C' to Keep Current Value\n");

    return ConsoleUtil.getIntegerInput("[ New Value ]: ", min, max);
  }

  public Double promptRuleDoubleInput(
      String targetVar, double currentVal, double min, double max, String activeFormula) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("MODIFY " + targetVar.toUpperCase());
    System.out.println(" Active Formula  : " + activeFormula);
    System.out.println(" Target Variable : " + targetVar);
    System.out.println(" Current Value   : " + currentVal);
    System.out.println(" Allowed Range   : [" + min + " - " + max + "]\n");
    System.out.println("------------------------------------------------------");
    System.out.println(" Press ENTER / 'C' to Keep Current Value\n");

    return ConsoleUtil.getDoubleInput("[ New Value ]: ", min, max);
  }

  public int displayComponentWeightsMenu() {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("BUSINESS COMPONENT WEIGHT CONFIG");
    System.out.println("1. Tier Base Values (TIER)");
    System.out.println("2. Boiling Point Boosts (W_BOILING)");
    System.out.println("3. Strike Penalties (W_STRIKE)");
    System.out.println("4. Back\n");

    return ConsoleUtil.getMenuInput("Choose an option: ", 1, 4).getAsInt();
  }

  public int displayReportAlertTargetsMenu() {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("REPORT ALERT TARGET THRESHOLDS (%)");
    System.out.println("1. Tier SLA Attainment Targets (%)");
    System.out.println("2. Tier Max Eviction Rate Limits (%)");
    System.out.println("3. Tier Max Grace Utilization Limits (%)");
    System.out.println("4. Back\n");

    return ConsoleUtil.getMenuInput("Choose an option: ", 1, 4).getAsInt();
  }

  public void displayResetSuccessScreen() {
    ConsoleUtil.clearScreen();
    System.out.println(">> SUCCESS: System settings reset to baseline factory defaults.\n");
    ConsoleUtil.printContinueMessage();
  }
}
