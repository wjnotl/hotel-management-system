package control.vip;

import adt.ArrayList;
import adt.LinkedStack;
import adt.ListInterface;
import adt.StackInterface;
import entity.VipSystemConfig;
import repo.VipSystemConfigRepo;
import util.ConsoleUtil;
import view.vip.VipSettingsView;

public class VipSettingsController {

  private final VipSettingsView settingsView = new VipSettingsView();
  private final VipSystemConfigRepo configRepo;

  public VipSettingsController(VipSystemConfigRepo configRepo) {
    this.configRepo = configRepo;
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

        if (preset == 1) {
          config.setActiveStrategyName("Strict Loyalty Focus");
          config.setActiveFormulaInfix("TIER - ( STRIKES * W_STRIKE )");
        } else if (preset == 2) {
          config.setActiveStrategyName("Balanced Lobby Flow");
          config.setActiveFormulaInfix("TIER + ( BOILING * W_BOILING ) - ( STRIKES * W_STRIKE )");
        } else if (preset == 3) {
          config.setActiveStrategyName("Emergency Customer Care");
          config.setActiveFormulaInfix("TIER + ( BOILING * W_BOILING )");
        } else if (preset == 4) {
          break;
        }

        configRepo.updateConfig(config);
        break;
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  private void handleWizardBuilder() {
    VipSystemConfig config = configRepo.getConfig();
    ListInterface<String> tokens = new ArrayList<>();

    // Stack-based Undo/Redo Engine
    StackInterface<String> undoStack = new LinkedStack<>();
    StackInterface<String> redoStack = new LinkedStack<>();

    while (true) {
      try {
        StringBuilder currentInfix = new StringBuilder();
        for (int i = 1; i <= tokens.getNumberOfEntries(); i++) {
          currentInfix.append(tokens.getEntry(i)).append(" ");
        }

        // --- Contextual Rule State Engine ---
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
          // 1. System Variable Submenu
          if (choice == optionIndex++) {
            String token = selectSystemVariable();
            if (token != null) {
              tokens.add(token);
              undoStack.push(token);
              redoStack.clear();
            }
            continue;
          }

          // 2. Weight Variable Submenu
          if (choice == optionIndex++) {
            String token = selectWeightVariable();
            if (token != null) {
              tokens.add(token);
              undoStack.push(token);
              redoStack.clear();
            }
            continue;
          }

          // 3. Numeric Value Prompt
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
          // 4. Operator Submenu
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

        // UNDO ACTION
        if (allowUndo) {
          if (choice == optionIndex++) {
            String popped = undoStack.pop();
            redoStack.push(popped);
            tokens.removeAt(tokens.getNumberOfEntries());
            continue;
          }
        }

        // REDO ACTION
        if (allowRedo) {
          if (choice == optionIndex++) {
            String restored = redoStack.pop();
            undoStack.push(restored);
            tokens.add(restored);
            continue;
          }
        }

        // SAVE ACTION
        if (allowSave) {
          if (choice == optionIndex++) {
            validateFormulaTokens(tokens);

            config.setActiveStrategyName("Custom Wizard Formula");
            config.setActiveFormulaInfix(currentInfix.toString().trim());
            configRepo.updateConfig(config);
            break;
          }
        }

        // Final option: Back
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
        if (v == 4) return null; // Back
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
        if (w == 3) return null; // Back
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
        if (op == 5) return null; // Back
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
          return null; // Cancel
        }
        Double.parseDouble(num.trim()); // Validate number format
        return num.trim();
      } catch (NumberFormatException e) {
        ConsoleUtil.printError("Invalid numeric format! Enter a valid number or 'C' to cancel.");
      }
    }
  }

  private void validateFormulaTokens(ListInterface<String> tokens) {
    if (tokens.isEmpty()) {
      throw new IllegalArgumentException("Cannot save an empty formula!");
    }

    for (int i = 1; i <= tokens.getNumberOfEntries(); i++) {
      String current = tokens.getEntry(i);
      String next = (i < tokens.getNumberOfEntries()) ? tokens.getEntry(i + 1) : null;

      if ("/".equals(current) && next != null) {
        try {
          double divisor = Double.parseDouble(next);
          if (divisor == 0.0) {
            throw new IllegalArgumentException(
                "Mathematical Error: Division by zero is not allowed!");
          }
        } catch (NumberFormatException ignored) {
          // Non-static variables are evaluated safely at runtime
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

  // --- SUBMODULE 2: OPERATIONAL RULES ENGINE ---

  private void handleOperationalRules() {
    while (true) {
      try {
        VipSystemConfig config = configRepo.getConfig();
        int choice = settingsView.displayOperationalRulesMenu();

        if (choice == 1) {
          manageStrikeLimits(config);
        } else if (choice == 2) {
          managePatienceLimits(config);
        } else if (choice == 3) {
          manageGraceWindows(config);
        } else if (choice == 4) {
          break;
        }
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
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
          if (tier == 1)
            newVal =
                settingsView.promptRuleIntInput(
                    "Diamond Max Strikes", config.getDiamondMaxStrikes());
          else if (tier == 2)
            newVal =
                settingsView.promptRuleIntInput("Gold Max Strikes", config.getGoldMaxStrikes());
          else if (tier == 3)
            newVal =
                settingsView.promptRuleIntInput("Silver Max Strikes", config.getSilverMaxStrikes());

          if (newVal != null) {
            if (tier == 1) config.setDiamondMaxStrikes(newVal);
            else if (tier == 2) config.setGoldMaxStrikes(newVal);
            else if (tier == 3) config.setSilverMaxStrikes(newVal);
            configRepo.updateConfig(config);
          }
          break;
        } catch (Exception e) {
          ConsoleUtil.printError(e.getMessage());
        }
      }
    }
  }

  private void managePatienceLimits(VipSystemConfig config) {
    while (true) {
      int tier;
      try {
        tier =
            settingsView.displayTierSelectionMenu(
                "TIER PATIENCE LIMITS (MINS)",
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
          if (tier == 1)
            newVal =
                settingsView.promptRuleIntInput(
                    "Diamond Patience Mins", config.getDiamondPatienceLimitMins());
          else if (tier == 2)
            newVal =
                settingsView.promptRuleIntInput(
                    "Gold Patience Mins", config.getGoldPatienceLimitMins());
          else if (tier == 3)
            newVal =
                settingsView.promptRuleIntInput(
                    "Silver Patience Mins", config.getSilverPatienceLimitMins());

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
          if (tier == 1)
            newVal =
                settingsView.promptRuleIntInput(
                    "Diamond Grace Mins", config.getDiamondGraceWindowMins());
          else if (tier == 2)
            newVal =
                settingsView.promptRuleIntInput("Gold Grace Mins", config.getGoldGraceWindowMins());
          else if (tier == 3)
            newVal =
                settingsView.promptRuleIntInput(
                    "Silver Grace Mins", config.getSilverGraceWindowMins());

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

  // --- SUBMODULE 3: COMPONENT WEIGHTS ENGINE ---

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
          if (tier == 1)
            newVal =
                settingsView.promptRuleIntInput("Diamond Base Score", config.getDiamondBaseValue());
          else if (tier == 2)
            newVal = settingsView.promptRuleIntInput("Gold Base Score", config.getGoldBaseValue());
          else if (tier == 3)
            newVal =
                settingsView.promptRuleIntInput("Silver Base Score", config.getSilverBaseValue());

          if (newVal != null) {
            if (tier == 1) config.setDiamondBaseValue(newVal);
            else if (tier == 2) config.setGoldBaseValue(newVal);
            else if (tier == 3) config.setSilverBaseValue(newVal);
            configRepo.updateConfig(config);
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
          if (tier == 1)
            newVal =
                settingsView.promptRuleDoubleInput(
                    "Diamond Boiling Boost", config.getDiamondBoilingBoost());
          else if (tier == 2)
            newVal =
                settingsView.promptRuleDoubleInput(
                    "Gold Boiling Boost", config.getGoldBoilingBoost());
          else if (tier == 3)
            newVal =
                settingsView.promptRuleDoubleInput(
                    "Silver Boiling Boost", config.getSilverBoilingBoost());

          if (newVal != null) {
            if (tier == 1) config.setDiamondBoilingBoost(newVal);
            else if (tier == 2) config.setGoldBoilingBoost(newVal);
            else if (tier == 3) config.setSilverBoilingBoost(newVal);
            configRepo.updateConfig(config);
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
          if (tier == 1)
            newVal =
                settingsView.promptRuleDoubleInput(
                    "Diamond Strike Penalty", config.getDiamondStrikePenalty());
          else if (tier == 2)
            newVal =
                settingsView.promptRuleDoubleInput(
                    "Gold Strike Penalty", config.getGoldStrikePenalty());
          else if (tier == 3)
            newVal =
                settingsView.promptRuleDoubleInput(
                    "Silver Strike Penalty", config.getSilverStrikePenalty());

          if (newVal != null) {
            if (tier == 1) config.setDiamondStrikePenalty(newVal);
            else if (tier == 2) config.setGoldStrikePenalty(newVal);
            else if (tier == 3) config.setSilverStrikePenalty(newVal);
            configRepo.updateConfig(config);
          }
          break;
        } catch (Exception e) {
          ConsoleUtil.printError(e.getMessage());
        }
      }
    }
  }
}
