package control.vip;

import adt.ArrayList;
import adt.LinkedStack;
import adt.ListInterface;
import adt.StackInterface;
import entity.VipSystemConfig;
import repo.AllocationRepo;
import repo.GuestRepo;
import repo.MemberRepo;
import repo.VipReservationRepo;
import repo.VipSystemConfigRepo;
import util.ConsoleUtil;
import view.vip.VipSettingsView;

public class VipSettingsController {

  private final VipSettingsView settingsView = new VipSettingsView();
  private final VipSystemConfigRepo configRepo;
  private final VipReservationRepo vipReservationRepo;
  private final GuestRepo guestRepo;
  private final MemberRepo memberRepo;
  private final AllocationRepo allocationRepo;

  public VipSettingsController(
      VipSystemConfigRepo configRepo,
      VipReservationRepo vipReservationRepo,
      GuestRepo guestRepo,
      MemberRepo memberRepo,
      AllocationRepo allocationRepo) {
    this.configRepo = configRepo;
    this.vipReservationRepo = vipReservationRepo;
    this.guestRepo = guestRepo;
    this.memberRepo = memberRepo;
    this.allocationRepo = allocationRepo;
  }

  public void startSettingsManagement() {
    while (true) {
      try {
        VipSystemConfig config = configRepo.getConfig();
        int choice = settingsView.displayMasterSettingsMenu(config);

        if (choice == 1) {
          handleStrategyEngine();
        } else if (choice == 2) {
          handleOperationalRules();
        } else if (choice == 3) {
          handleComponentWeights();
        } else if (choice == 4) {
          handleReportAlertTargets();
        } else if (choice == 5) {
          handleApplyToQueue();
        } else if (choice == 6) {
          handleResetToDefaults();
        } else if (choice == 7) {
          break;
        }
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  private void handleStrategyEngine() {
    while (true) {
      try {
        int choice = settingsView.displayStrategyEngineMenu();
        if (choice == 1) {
          handleApplyPresetStrategy();
        } else if (choice == 2) {
          handleWizardBuilder();
        } else if (choice == 3) {
          break;
        }
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  private void handleApplyPresetStrategy() {
    VipSystemConfig config = configRepo.getConfig();
    while (true) {
      try {
        int preset = settingsView.displayPresetStrategyMenu();

        String targetFormula = null;
        String strategyName = null;

        if (preset == 1) {
          strategyName = "Strict Loyalty Focus";
          targetFormula = "TIER - ( STRIKES * W_STRIKE )";
        } else if (preset == 2) {
          strategyName = "Balanced Lobby Flow";
          targetFormula = "TIER + ( BOILING * W_BOILING ) - ( STRIKES * W_STRIKE )";
        } else if (preset == 3) {
          strategyName = "Emergency Customer Care";
          targetFormula = "TIER + ( BOILING * W_BOILING )";
        } else if (preset == 4) {
          break;
        }

        if (targetFormula != null) {
          validateInfixFormulaWithConfig(targetFormula);
          config.setActiveStrategyName(strategyName);
          config.setActiveFormulaInfix(targetFormula);
          configRepo.updateConfig(config);
        }
        break;
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  private void handleWizardBuilder() {
    VipSystemConfig config = configRepo.getConfig();
    ListInterface<String> tokens = new ArrayList<>();

    StackInterface<String> undoStack = new LinkedStack<>();
    StackInterface<String> redoStack = new LinkedStack<>();

    while (true) {
      try {
        StringBuilder currentInfix = new StringBuilder();
        for (int i = 1; i <= tokens.getNumberOfEntries(); i++) {
          currentInfix.append(tokens.getEntry(i)).append(" ");
        }

        int size = tokens.getNumberOfEntries();
        String lastToken = (size > 0) ? tokens.getEntry(size) : null;

        int openCount = 0;
        int closeCount = 0;
        for (int i = 1; i <= size; i++) {
          if ("(".equals(tokens.getEntry(i))) openCount++;
          if (")".equals(tokens.getEntry(i))) closeCount++;
        }

        boolean isLastOperatorOrBracket =
            (lastToken == null || isOperator(lastToken) || "(".equals(lastToken));
        boolean isLastOperandOrBracket =
            (lastToken != null && (isOperand(lastToken) || ")".equals(lastToken)));

        boolean allowOperand = isLastOperatorOrBracket;
        boolean allowOperator = isLastOperandOrBracket;
        boolean allowOpenBracket = isLastOperatorOrBracket;
        boolean allowCloseBracket = isLastOperandOrBracket && (openCount > closeCount);
        boolean allowUndo = !undoStack.isEmpty();
        boolean allowRedo = !redoStack.isEmpty();
        boolean allowSave = isLastOperandOrBracket && (openCount == closeCount);

        int choice =
            settingsView.displayWizardComponentTypeMenu(
                currentInfix.toString().trim(),
                allowOperand,
                allowOperator,
                allowOpenBracket,
                allowCloseBracket,
                allowUndo,
                allowRedo,
                allowSave);

        int optionIndex = 1;

        if (allowOperand) {
          if (choice == optionIndex++) {
            String token = selectSystemVariable();
            if (token != null) {
              tokens.add(token);
              undoStack.push(token);
              redoStack.clear();
            }
            continue;
          }

          if (choice == optionIndex++) {
            String token = selectWeightVariable();
            if (token != null) {
              tokens.add(token);
              undoStack.push(token);
              redoStack.clear();
            }
            continue;
          }

          if (choice == optionIndex++) {
            String token = promptNumericValue();
            if (token != null) {
              tokens.add(token);
              undoStack.push(token);
              redoStack.clear();
            }
            continue;
          }
        }

        if (allowOperator) {
          if (choice == optionIndex++) {
            String token = selectOperator();
            if (token != null) {
              tokens.add(token);
              undoStack.push(token);
              redoStack.clear();
            }
            continue;
          }
        }

        if (allowOpenBracket) {
          if (choice == optionIndex++) {
            tokens.add("(");
            undoStack.push("(");
            redoStack.clear();
            continue;
          }
        }

        if (allowCloseBracket) {
          if (choice == optionIndex++) {
            tokens.add(")");
            undoStack.push(")");
            redoStack.clear();
            continue;
          }
        }

        if (allowUndo) {
          if (choice == optionIndex++) {
            String popped = undoStack.pop();
            redoStack.push(popped);
            tokens.removeAt(tokens.getNumberOfEntries());
            continue;
          }
        }

        if (allowRedo) {
          if (choice == optionIndex++) {
            String restored = redoStack.pop();
            undoStack.push(restored);
            tokens.add(restored);
            continue;
          }
        }

        if (allowSave) {
          if (choice == optionIndex++) {
            validateFormulaTokens(tokens);

            config.setActiveStrategyName("Custom Wizard Formula");
            config.setActiveFormulaInfix(currentInfix.toString().trim());
            configRepo.updateConfig(config);
            break;
          }
        }

        if (choice == optionIndex) {
          if (!tokens.isEmpty()) {
            boolean confirmExit =
                ConsoleUtil.showConfirmMessage(
                    "You have unsaved formula changes! Are you sure you want to discard and exit?");
            if (!confirmExit) {
              continue;
            }
          }
          break;
        }

      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  private String selectSystemVariable() {
    while (true) {
      try {
        int v = settingsView.displaySystemVariableSubmenu();
        if (v == 1) return "TIER";
        if (v == 2) return "STRIKES";
        if (v == 3) return "BOILING";
        if (v == 4) return null;
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  private String selectWeightVariable() {
    while (true) {
      try {
        int w = settingsView.displayWeightVariableSubmenu();
        if (w == 1) return "W_BOILING";
        if (w == 2) return "W_STRIKE";
        if (w == 3) return null;
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  private String selectOperator() {
    while (true) {
      try {
        int op = settingsView.displayOperatorSubmenu();
        if (op == 1) return "+";
        if (op == 2) return "-";
        if (op == 3) return "*";
        if (op == 4) return "/";
        if (op == 5) return null;
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  private String promptNumericValue() {
    while (true) {
      try {
        String num = settingsView.promptNumericInput();
        if (num == null || num.trim().isEmpty() || "C".equalsIgnoreCase(num.trim())) {
          return null;
        }

        Double.parseDouble(num.trim());
        return num.trim();
      } catch (NumberFormatException e) {
        ConsoleUtil.printError("Invalid numeric format! Enter a valid number or 'C' to cancel.");
      }
    }
  }

  private void validateFormulaTokens(ListInterface<String> tokens) {
    if (tokens == null || tokens.isEmpty()) {
      throw new IllegalArgumentException("Cannot save an empty formula!");
    }

    StringBuilder infixBuilder = new StringBuilder();
    boolean hasDivision = false;

    for (int i = 1; i <= tokens.getNumberOfEntries(); i++) {
      String t = tokens.getEntry(i);
      if ("/".equals(t)) {
        hasDivision = true;
      }
      infixBuilder.append(t).append(" ");
    }

    if (!hasDivision) {
      return;
    }

    String formulaInfix = infixBuilder.toString().trim();
    validateInfixFormulaWithConfig(formulaInfix);
  }

  private void validateInfixFormulaWithConfig(String formulaInfix) {
    if (formulaInfix == null || !formulaInfix.contains("/")) {
      return;
    }

    VipSystemConfig config = configRepo.getConfig();

    double[][] tierProfiles = {
      {
        config.getSilverBaseValue(),
        config.getSilverBoilingBoost(),
        config.getSilverStrikePenalty(),
        config.getSilverMaxStrikes()
      },
      {
        config.getGoldBaseValue(),
        config.getGoldBoilingBoost(),
        config.getGoldStrikePenalty(),
        config.getGoldMaxStrikes()
      },
      {
        config.getDiamondBaseValue(),
        config.getDiamondBoilingBoost(),
        config.getDiamondStrikePenalty(),
        config.getDiamondMaxStrikes()
      }
    };
    String[] tierNames = {"Silver", "Gold", "Diamond"};

    for (int t = 0; t < tierProfiles.length; t++) {
      double tVal = tierProfiles[t][0];
      double wBVal = tierProfiles[t][1];
      double wSVal = tierProfiles[t][2];
      int tierMaxStrikes = (int) tierProfiles[t][3];
      String tName = tierNames[t];

      for (int boilingState = 0; boilingState <= 1; boilingState++) {
        double bVal = boilingState;

        for (int strikeCount = 0; strikeCount <= tierMaxStrikes; strikeCount++) {
          double sVal = strikeCount;

          java.util.function.Function<String, Double> resolver =
              (var) -> {
                if (var == null || var.trim().isEmpty()) return 0.0;
                switch (var.trim().toUpperCase()) {
                  case "TIER":
                    return tVal;
                  case "W_BOILING":
                    return wBVal;
                  case "W_STRIKE":
                    return wSVal;
                  case "BOILING":
                    return bVal;
                  case "STRIKES":
                    return sVal;
                  default:
                    try {
                      return Double.parseDouble(var.trim());
                    } catch (NumberFormatException e) {
                      return 0.0;
                    }
                }
              };

          try {
            double result = util.ExpressionEvaluator.evaluateInfix(formulaInfix, resolver);

            if (Double.isInfinite(result) || Double.isNaN(result)) {
              throw new ArithmeticException("Division by zero");
            }
          } catch (ArithmeticException e) {
            throw new IllegalArgumentException(
                "Mathematical Error: Formula results in division by zero for "
                    + tName
                    + " Tier! (Triggered when STRIKES="
                    + strikeCount
                    + ", BOILING="
                    + boilingState
                    + ")");
          } catch (IllegalArgumentException e) {
            throw e;
          } catch (Exception e) {
            throw new IllegalArgumentException("Invalid Formula Expression: " + e.getMessage());
          }
        }
      }
    }
  }

  private boolean isOperator(String token) {
    return "+".equals(token) || "-".equals(token) || "*".equals(token) || "/".equals(token);
  }

  private boolean isOperand(String token) {
    return !isOperator(token) && !"(".equals(token) && !")".equals(token);
  }

  private void handleOperationalRules() {
    while (true) {
      try {
        VipSystemConfig config = configRepo.getConfig();
        int choice = settingsView.displayOperationalRulesMenu();

        if (choice == 1) {
          managePatienceLimits(config);
        } else if (choice == 2) {
          manageBoilingPointLimits(config);
        } else if (choice == 3) {
          manageStrikeLimits(config);
        } else if (choice == 4) {
          manageGraceWindows(config);
        } else if (choice == 5) {
          break;
        }
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  private void managePatienceLimits(VipSystemConfig config) {
    while (true) {
      int tier;
      try {
        tier =
            settingsView.displayTierSelectionMenu(
                "TIER PATIENCE LIMITS (SLA TARGET MINS)",
                config.getDiamondPatienceLimitMins() + " Mins",
                config.getGoldPatienceLimitMins() + " Mins",
                config.getSilverPatienceLimitMins() + " Mins");
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
        continue;
      }

      if (tier == 4) break;

      while (true) {
        try {
          Integer newVal = null;
          String formula = config.getActiveFormulaInfix();
          if (tier == 1)
            newVal =
                settingsView.promptRuleIntInput(
                    "Diamond Patience Limit Mins",
                    config.getDiamondPatienceLimitMins(),
                    1,
                    180,
                    formula);
          else if (tier == 2)
            newVal =
                settingsView.promptRuleIntInput(
                    "Gold Patience Limit Mins", config.getGoldPatienceLimitMins(), 1, 180, formula);
          else if (tier == 3)
            newVal =
                settingsView.promptRuleIntInput(
                    "Silver Patience Limit Mins",
                    config.getSilverPatienceLimitMins(),
                    1,
                    180,
                    formula);

          if (newVal != null) {
            if (tier == 1) config.setDiamondPatienceLimitMins(newVal);
            else if (tier == 2) config.setGoldPatienceLimitMins(newVal);
            else if (tier == 3) config.setSilverPatienceLimitMins(newVal);
            configRepo.updateConfig(config);
          }
          break;
        } catch (Exception e) {
          ConsoleUtil.printError(e.getMessage());
        }
      }
    }
  }

  private void manageBoilingPointLimits(VipSystemConfig config) {
    while (true) {
      int tier;
      try {
        tier =
            settingsView.displayTierSelectionMenu(
                "TIER BOILING POINT LIMITS (STARVATION BOOST MINS)",
                config.getDiamondBoilingLimitMins() + " Mins",
                config.getGoldBoilingLimitMins() + " Mins",
                config.getSilverBoilingLimitMins() + " Mins");
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
        continue;
      }

      if (tier == 4) break;

      while (true) {
        try {
          Integer newVal = null;
          String formula = config.getActiveFormulaInfix();
          if (tier == 1)
            newVal =
                settingsView.promptRuleIntInput(
                    "Diamond Boiling Limit Mins",
                    config.getDiamondBoilingLimitMins(),
                    1,
                    180,
                    formula);
          else if (tier == 2)
            newVal =
                settingsView.promptRuleIntInput(
                    "Gold Boiling Limit Mins", config.getGoldBoilingLimitMins(), 1, 180, formula);
          else if (tier == 3)
            newVal =
                settingsView.promptRuleIntInput(
                    "Silver Boiling Limit Mins",
                    config.getSilverBoilingLimitMins(),
                    1,
                    180,
                    formula);

          if (newVal != null) {
            int oldVal =
                (tier == 1)
                    ? config.getDiamondBoilingLimitMins()
                    : (tier == 2)
                        ? config.getGoldBoilingLimitMins()
                        : config.getSilverBoilingLimitMins();

            if (tier == 1) config.setDiamondBoilingLimitMins(newVal);
            else if (tier == 2) config.setGoldBoilingLimitMins(newVal);
            else if (tier == 3) config.setSilverBoilingLimitMins(newVal);

            try {
              validateInfixFormulaWithConfig(config.getActiveFormulaInfix());
              configRepo.updateConfig(config);
            } catch (Exception e) {
              if (tier == 1) config.setDiamondBoilingLimitMins(oldVal);
              else if (tier == 2) config.setGoldBoilingLimitMins(oldVal);
              else if (tier == 3) config.setSilverBoilingLimitMins(oldVal);
              throw e;
            }
          }
          break;
        } catch (Exception e) {
          ConsoleUtil.printError(e.getMessage());
        }
      }
    }
  }

  private void manageStrikeLimits(VipSystemConfig config) {
    while (true) {
      int tier;
      try {
        tier =
            settingsView.displayTierSelectionMenu(
                "TIER MAX STRIKE LIMITS",
                config.getDiamondMaxStrikes() + " Calls",
                config.getGoldMaxStrikes() + " Calls",
                config.getSilverMaxStrikes() + " Calls");
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
        continue;
      }

      if (tier == 4) break;

      while (true) {
        try {
          Integer newVal = null;
          String formula = config.getActiveFormulaInfix();
          if (tier == 1)
            newVal =
                settingsView.promptRuleIntInput(
                    "Diamond Max Strikes", config.getDiamondMaxStrikes(), 0, 10, formula);
          else if (tier == 2)
            newVal =
                settingsView.promptRuleIntInput(
                    "Gold Max Strikes", config.getGoldMaxStrikes(), 0, 10, formula);
          else if (tier == 3)
            newVal =
                settingsView.promptRuleIntInput(
                    "Silver Max Strikes", config.getSilverMaxStrikes(), 0, 10, formula);

          if (newVal != null) {
            int oldVal =
                (tier == 1)
                    ? config.getDiamondMaxStrikes()
                    : (tier == 2) ? config.getGoldMaxStrikes() : config.getSilverMaxStrikes();

            if (tier == 1) config.setDiamondMaxStrikes(newVal);
            else if (tier == 2) config.setGoldMaxStrikes(newVal);
            else if (tier == 3) config.setSilverMaxStrikes(newVal);

            try {
              validateInfixFormulaWithConfig(config.getActiveFormulaInfix());
              configRepo.updateConfig(config);
            } catch (Exception e) {
              if (tier == 1) config.setDiamondMaxStrikes(oldVal);
              else if (tier == 2) config.setGoldMaxStrikes(oldVal);
              else if (tier == 3) config.setSilverMaxStrikes(oldVal);
              throw e;
            }
          }
          break;
        } catch (Exception e) {
          ConsoleUtil.printError(e.getMessage());
        }
      }
    }
  }

  private void manageGraceWindows(VipSystemConfig config) {
    while (true) {
      int tier;
      try {
        tier =
            settingsView.displayTierSelectionMenu(
                "TIER NO-SHOW GRACE WINDOWS (MINS)",
                config.getDiamondGraceWindowMins() + " Mins",
                config.getGoldGraceWindowMins() + " Mins",
                config.getSilverGraceWindowMins() + " Mins");
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
        continue;
      }

      if (tier == 4) break;

      while (true) {
        try {
          Integer newVal = null;
          String formula = config.getActiveFormulaInfix();
          if (tier == 1)
            newVal =
                settingsView.promptRuleIntInput(
                    "Diamond Grace Mins", config.getDiamondGraceWindowMins(), 1, 60, formula);
          else if (tier == 2)
            newVal =
                settingsView.promptRuleIntInput(
                    "Gold Grace Mins", config.getGoldGraceWindowMins(), 1, 60, formula);
          else if (tier == 3)
            newVal =
                settingsView.promptRuleIntInput(
                    "Silver Grace Mins", config.getSilverGraceWindowMins(), 1, 60, formula);

          if (newVal != null) {
            if (tier == 1) config.setDiamondGraceWindowMins(newVal);
            else if (tier == 2) config.setGoldGraceWindowMins(newVal);
            else if (tier == 3) config.setSilverGraceWindowMins(newVal);
            configRepo.updateConfig(config);
          }
          break;
        } catch (Exception e) {
          ConsoleUtil.printError(e.getMessage());
        }
      }
    }
  }

  private void handleComponentWeights() {
    while (true) {
      try {
        VipSystemConfig config = configRepo.getConfig();
        int choice = settingsView.displayComponentWeightsMenu();

        if (choice == 1) {
          manageBaseValues(config);
        } else if (choice == 2) {
          manageBoilingBoosts(config);
        } else if (choice == 3) {
          manageStrikePenalties(config);
        } else if (choice == 4) {
          break;
        }
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  private void manageBaseValues(VipSystemConfig config) {
    while (true) {
      int tier;
      try {
        tier =
            settingsView.displayTierSelectionMenu(
                "TIER BASE VALUES (TIER)",
                String.valueOf(config.getDiamondBaseValue()),
                String.valueOf(config.getGoldBaseValue()),
                String.valueOf(config.getSilverBaseValue()));
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
        continue;
      }

      if (tier == 4) break;

      while (true) {
        try {
          Integer newVal = null;
          String formula = config.getActiveFormulaInfix();
          if (tier == 1)
            newVal =
                settingsView.promptRuleIntInput(
                    "Diamond Base Score", config.getDiamondBaseValue(), 1, 100000, formula);
          else if (tier == 2)
            newVal =
                settingsView.promptRuleIntInput(
                    "Gold Base Score", config.getGoldBaseValue(), 1, 100000, formula);
          else if (tier == 3)
            newVal =
                settingsView.promptRuleIntInput(
                    "Silver Base Score", config.getSilverBaseValue(), 1, 100000, formula);

          if (newVal != null) {
            int oldVal =
                (tier == 1)
                    ? config.getDiamondBaseValue()
                    : (tier == 2) ? config.getGoldBaseValue() : config.getSilverBaseValue();

            if (tier == 1) config.setDiamondBaseValue(newVal);
            else if (tier == 2) config.setGoldBaseValue(newVal);
            else if (tier == 3) config.setSilverBaseValue(newVal);

            try {
              validateInfixFormulaWithConfig(config.getActiveFormulaInfix());
              configRepo.updateConfig(config);
            } catch (Exception e) {
              if (tier == 1) config.setDiamondBaseValue(oldVal);
              else if (tier == 2) config.setGoldBaseValue(oldVal);
              else if (tier == 3) config.setSilverBaseValue(oldVal);
              throw e;
            }
          }
          break;
        } catch (Exception e) {
          ConsoleUtil.printError(e.getMessage());
        }
      }
    }
  }

  private void manageBoilingBoosts(VipSystemConfig config) {
    while (true) {
      int tier;
      try {
        tier =
            settingsView.displayTierSelectionMenu(
                "BOILING BOOSTS (W_BOILING)",
                config.getDiamondBoilingBoost() + " pts",
                config.getGoldBoilingBoost() + " pts",
                config.getSilverBoilingBoost() + " pts");
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
        continue;
      }

      if (tier == 4) break;

      while (true) {
        try {
          Double newVal = null;
          String formula = config.getActiveFormulaInfix();
          if (tier == 1)
            newVal =
                settingsView.promptRuleDoubleInput(
                    "Diamond Boiling Boost",
                    config.getDiamondBoilingBoost(),
                    0.01,
                    50000.0,
                    formula);
          else if (tier == 2)
            newVal =
                settingsView.promptRuleDoubleInput(
                    "Gold Boiling Boost", config.getGoldBoilingBoost(), 0.01, 50000.0, formula);
          else if (tier == 3)
            newVal =
                settingsView.promptRuleDoubleInput(
                    "Silver Boiling Boost", config.getSilverBoilingBoost(), 0.01, 50000.0, formula);

          if (newVal != null) {
            double oldVal =
                (tier == 1)
                    ? config.getDiamondBoilingBoost()
                    : (tier == 2) ? config.getGoldBoilingBoost() : config.getSilverBoilingBoost();

            if (tier == 1) config.setDiamondBoilingBoost(newVal);
            else if (tier == 2) config.setGoldBoilingBoost(newVal);
            else if (tier == 3) config.setSilverBoilingBoost(newVal);

            try {
              validateInfixFormulaWithConfig(config.getActiveFormulaInfix());
              configRepo.updateConfig(config);
            } catch (Exception e) {
              if (tier == 1) config.setDiamondBoilingBoost(oldVal);
              else if (tier == 2) config.setGoldBoilingBoost(oldVal);
              else if (tier == 3) config.setSilverBoilingBoost(oldVal);
              throw e;
            }
          }
          break;
        } catch (Exception e) {
          ConsoleUtil.printError(e.getMessage());
        }
      }
    }
  }

  private void manageStrikePenalties(VipSystemConfig config) {
    while (true) {
      int tier;
      try {
        tier =
            settingsView.displayTierSelectionMenu(
                "STRIKE PENALTIES (W_STRIKE)",
                config.getDiamondStrikePenalty() + " pts",
                config.getGoldStrikePenalty() + " pts",
                config.getSilverStrikePenalty() + " pts");
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
        continue;
      }

      if (tier == 4) break;

      while (true) {
        try {
          Double newVal = null;
          String formula = config.getActiveFormulaInfix();
          if (tier == 1)
            newVal =
                settingsView.promptRuleDoubleInput(
                    "Diamond Strike Penalty",
                    config.getDiamondStrikePenalty(),
                    0.01,
                    50000.0,
                    formula);
          else if (tier == 2)
            newVal =
                settingsView.promptRuleDoubleInput(
                    "Gold Strike Penalty", config.getGoldStrikePenalty(), 0.01, 50000.0, formula);
          else if (tier == 3)
            newVal =
                settingsView.promptRuleDoubleInput(
                    "Silver Strike Penalty",
                    config.getSilverStrikePenalty(),
                    0.01,
                    50000.0,
                    formula);

          if (newVal != null) {
            double oldVal =
                (tier == 1)
                    ? config.getDiamondStrikePenalty()
                    : (tier == 2) ? config.getGoldStrikePenalty() : config.getSilverStrikePenalty();

            if (tier == 1) config.setDiamondStrikePenalty(newVal);
            else if (tier == 2) config.setGoldStrikePenalty(newVal);
            else if (tier == 3) config.setSilverStrikePenalty(newVal);

            try {
              validateInfixFormulaWithConfig(config.getActiveFormulaInfix());
              configRepo.updateConfig(config);
            } catch (Exception e) {
              if (tier == 1) config.setDiamondStrikePenalty(oldVal);
              else if (tier == 2) config.setGoldStrikePenalty(oldVal);
              else if (tier == 3) config.setSilverStrikePenalty(oldVal);
              throw e;
            }
          }
          break;
        } catch (Exception e) {
          ConsoleUtil.printError(e.getMessage());
        }
      }
    }
  }

  private void handleReportAlertTargets() {
    while (true) {
      try {
        VipSystemConfig config = configRepo.getConfig();
        ConsoleUtil.clearScreen();
        ConsoleUtil.printTitleBox("REPORT ALERT TARGET THRESHOLDS (%)");
        System.out.println("1. Tier SLA Attainment Targets (%)");
        System.out.println("2. Tier Max Eviction Rate Limits (%)");
        System.out.println("3. Tier Max Grace Utilization Limits (%)");
        System.out.println("4. Back\n");

        int choice = ConsoleUtil.getMenuInput("Choose an option: ", 1, 4).getAsInt();
        if (choice == 4) break;

        if (choice == 1) {
          manageSlaAttainmentTargets(config);
        } else if (choice == 2) {
          manageEvictionRateTargets(config);
        } else if (choice == 3) {
          manageGraceUtilTargets(config);
        }
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  private void manageSlaAttainmentTargets(VipSystemConfig config) {
    while (true) {
      int tier;
      try {
        tier =
            settingsView.displayTierSelectionMenu(
                "TIER SLA ATTAINMENT TARGETS (%)",
                String.format("%.1f%%", config.getDiamondSlaTargetPct()),
                String.format("%.1f%%", config.getGoldSlaTargetPct()),
                String.format("%.1f%%", config.getSilverSlaTargetPct()));
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
        continue;
      }

      if (tier == 4) break;

      while (true) {
        try {
          Double newVal = null;
          String formula = config.getActiveFormulaInfix();
          if (tier == 1)
            newVal =
                settingsView.promptRuleDoubleInput(
                    "Diamond SLA Target %", config.getDiamondSlaTargetPct(), 1.0, 100.0, formula);
          else if (tier == 2)
            newVal =
                settingsView.promptRuleDoubleInput(
                    "Gold SLA Target %", config.getGoldSlaTargetPct(), 1.0, 100.0, formula);
          else if (tier == 3)
            newVal =
                settingsView.promptRuleDoubleInput(
                    "Silver SLA Target %", config.getSilverSlaTargetPct(), 1.0, 100.0, formula);

          if (newVal != null) {
            if (tier == 1) config.setDiamondSlaTargetPct(newVal);
            else if (tier == 2) config.setGoldSlaTargetPct(newVal);
            else if (tier == 3) config.setSilverSlaTargetPct(newVal);
            configRepo.updateConfig(config);
          }
          break;
        } catch (Exception e) {
          ConsoleUtil.printError(e.getMessage());
        }
      }
    }
  }

  private void manageEvictionRateTargets(VipSystemConfig config) {
    while (true) {
      int tier;
      try {
        tier =
            settingsView.displayTierSelectionMenu(
                "TIER MAX EVICTION RATE LIMITS (%)",
                String.format("%.1f%%", config.getDiamondEvictionRateTargetPct()),
                String.format("%.1f%%", config.getGoldEvictionRateTargetPct()),
                String.format("%.1f%%", config.getSilverEvictionRateTargetPct()));
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
        continue;
      }

      if (tier == 4) break;

      while (true) {
        try {
          Double newVal = null;
          String formula = config.getActiveFormulaInfix();
          if (tier == 1)
            newVal =
                settingsView.promptRuleDoubleInput(
                    "Diamond Eviction Limit %",
                    config.getDiamondEvictionRateTargetPct(), 0.1, 100.0, formula);
          else if (tier == 2)
            newVal =
                settingsView.promptRuleDoubleInput(
                    "Gold Eviction Limit %",
                    config.getGoldEvictionRateTargetPct(), 0.1, 100.0, formula);
          else if (tier == 3)
            newVal =
                settingsView.promptRuleDoubleInput(
                    "Silver Eviction Limit %",
                    config.getSilverEvictionRateTargetPct(), 0.1, 100.0, formula);

          if (newVal != null) {
            if (tier == 1) config.setDiamondEvictionRateTargetPct(newVal);
            else if (tier == 2) config.setGoldEvictionRateTargetPct(newVal);
            else if (tier == 3) config.setSilverEvictionRateTargetPct(newVal);
            configRepo.updateConfig(config);
          }
          break;
        } catch (Exception e) {
          ConsoleUtil.printError(e.getMessage());
        }
      }
    }
  }

  private void manageGraceUtilTargets(VipSystemConfig config) {
    while (true) {
      int tier;
      try {
        tier =
            settingsView.displayTierSelectionMenu(
                "TIER MAX GRACE UTILIZATION (%)",
                String.format("%.1f%%", config.getDiamondGraceUtilTargetPct()),
                String.format("%.1f%%", config.getGoldGraceUtilTargetPct()),
                String.format("%.1f%%", config.getSilverGraceUtilTargetPct()));
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
        continue;
      }

      if (tier == 4) break;

      while (true) {
        try {
          Double newVal = null;
          String formula = config.getActiveFormulaInfix();
          if (tier == 1)
            newVal =
                settingsView.promptRuleDoubleInput(
                    "Diamond Grace Util %",
                    config.getDiamondGraceUtilTargetPct(), 1.0, 100.0, formula);
          else if (tier == 2)
            newVal =
                settingsView.promptRuleDoubleInput(
                    "Gold Grace Util %", config.getGoldGraceUtilTargetPct(), 1.0, 100.0, formula);
          else if (tier == 3)
            newVal =
                settingsView.promptRuleDoubleInput(
                    "Silver Grace Util %",
                    config.getSilverGraceUtilTargetPct(), 1.0, 100.0, formula);

          if (newVal != null) {
            if (tier == 1) config.setDiamondGraceUtilTargetPct(newVal);
            else if (tier == 2) config.setGoldGraceUtilTargetPct(newVal);
            else if (tier == 3) config.setSilverGraceUtilTargetPct(newVal);
            configRepo.updateConfig(config);
          }
          break;
        } catch (Exception e) {
          ConsoleUtil.printError(e.getMessage());
        }
      }
    }
  }

  private void handleApplyToQueue() {
    VipSystemConfig config = configRepo.getConfig();

    while (true) {
      try {
        int choice1 =
            settingsView.promptApplyOptionWithBack(
                "STRIKE THRESHOLD EVICTION",
                "Automatically cancel and evict waiting guests who exceed the active Tier Strike"
                    + " Limits?");
        if (choice1 == 3) return;
        boolean evictOverStrikes = (choice1 == 1);

        int choice2 =
            settingsView.promptApplyOptionWithBack(
                "BOILING STATUS RE-EVALUATION",
                "Re-evaluate live wait times against active Patience Thresholds and update BOILING"
                    + " flags?");
        if (choice2 == 3) return;
        boolean forceBoilingCheck = (choice2 == 1);

        int choice3 =
            settingsView.promptApplyOptionWithBack(
                "ACTIVE ALLOCATION GRACE TIMERS",
                "Reset active countdown timers in the Holding Bay using the newly configured Grace"
                    + " Periods?");
        if (choice3 == 3) return;
        boolean updateActiveGraceTimers = (choice3 == 1);

        boolean confirmExecution =
            ConsoleUtil.showConfirmMessage(
                "Confirm execution of selected queue reconciliation rules?");
        if (!confirmExecution) return;

        int processedWaitlist =
            vipReservationRepo.applySettingsToQueue(
                config, guestRepo, memberRepo, evictOverStrikes, forceBoilingCheck);

        int processedAllocations = 0;
        if (updateActiveGraceTimers) {
          processedAllocations =
              allocationRepo.recalculateActiveGraceTimers(
                  vipReservationRepo, guestRepo, memberRepo, configRepo);
        }

        settingsView.displayApplySuccessScreen(processedWaitlist + processedAllocations);
        break;

      } catch (Exception e) {
        ConsoleUtil.printError("Failed to apply settings: " + e.getMessage());
      }
    }
  }

  private void handleResetToDefaults() {
    boolean confirmed =
        ConsoleUtil.showConfirmMessage(
            "Are you sure you want to reset all VIP rules, weights, and strategies to factory"
                + " defaults?");

    if (confirmed) {
      VipSystemConfig config = configRepo.getConfig();
      config.resetToDefaults();
      configRepo.updateConfig(config);

      ConsoleUtil.clearScreen();
      System.out.println(">> SUCCESS: System settings reset to baseline factory defaults.\n");
      ConsoleUtil.printContinueMessage();
    }
  }
}
