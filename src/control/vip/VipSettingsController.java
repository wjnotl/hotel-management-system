package control.vip;

import adt.ArrayList;
import adt.ListInterface;
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
          break; // Back to Master Settings
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
          config.setActiveFormulaInfix("TIER * W_TIER + WAIT");
        } else if (preset == 2) {
          config.setActiveStrategyName("Balanced Lobby Flow");
          config.setActiveFormulaInfix(
              "( TIER * W_TIER ) + ( WAIT * W_TIME ) - ( STRIKES * W_STRIKE )");
        } else if (preset == 3) {
          config.setActiveStrategyName("Emergency Customer Care");
          config.setActiveFormulaInfix("( TIER * W_TIER ) + ( BOILING * W_BOILING )");
        } else if (preset == 4) {
          break; // Back to Strategy Engine Menu
        }

        configRepo.updateConfig(config);
        break; // Save and return after valid selection
      } catch (Exception e) {
        ConsoleUtil.printError(
            e.getMessage()); // Re-prompts the SAME Preset Strategy Menu on error!
      }
    }
  }

  private void handleWizardBuilder() {
    VipSystemConfig config = configRepo.getConfig();
    ListInterface<String> tokens = new ArrayList<>();

    while (true) {
      try {
        StringBuilder currentInfix = new StringBuilder();
        for (int i = 1; i <= tokens.getNumberOfEntries(); i++) {
          currentInfix.append(tokens.getEntry(i)).append(" ");
        }

        int choice = settingsView.displayWizardComponentTypeMenu(currentInfix.toString().trim());

        if (choice == 1) {
          int v = settingsView.displaySystemVariableSubmenu();
          if (v == 1) tokens.add("TIER");
          else if (v == 2) tokens.add("WAIT");
          else if (v == 3) tokens.add("STRIKES");
          else if (v == 4) tokens.add("BOILING");
        } else if (choice == 2) {
          int w = settingsView.displayWeightVariableSubmenu();
          if (w == 1) tokens.add("W_TIER");
          else if (w == 2) tokens.add("W_TIME");
          else if (w == 3) tokens.add("W_BOILING");
          else if (w == 4) tokens.add("W_STRIKE");
        } else if (choice == 3) {
          int op = settingsView.displayOperatorSubmenu();
          if (op == 1) tokens.add("+");
          else if (op == 2) tokens.add("-");
          else if (op == 3) tokens.add("*");
          else if (op == 4) tokens.add("/");
        } else if (choice == 4) {
          String num = settingsView.promptNumericInput();
          if (num != null && !num.trim().isEmpty()) {
            tokens.add(num.trim());
          }
        } else if (choice == 5) {
          int b = settingsView.displayBracketSubmenu();
          if (b == 1) tokens.add("(");
          else if (b == 2) tokens.add(")");
        } else if (choice == 6) {
          if (tokens.isEmpty()) {
            ConsoleUtil.printError("Cannot save an empty formula!");
            continue;
          }
          config.setActiveStrategyName("Custom Wizard Formula");
          config.setActiveFormulaInfix(currentInfix.toString().trim());
          configRepo.updateConfig(config);
          break;
        } else if (choice == 7) {
          break;
        }
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
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
          break; // Success or cancelled -> return to Tier Menu
        } catch (Exception e) {
          ConsoleUtil.printError(e.getMessage()); // Re-prompts the SAME modify screen!
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
          manageTimeWeights(config);
        } else if (choice == 3) {
          manageBoilingBoosts(config);
        } else if (choice == 4) {
          manageStrikePenalties(config);
        } else if (choice == 5) {
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

  private void manageTimeWeights(VipSystemConfig config) {
    while (true) {
      int tier;
      try {
        tier =
            settingsView.displayTierSelectionMenu(
                "PATIENCE ACCUMULATION (W_TIME)",
                config.getDiamondTimeWeight() + " pts/min",
                config.getGoldTimeWeight() + " pts/min",
                config.getSilverTimeWeight() + " pts/min");
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
                    "Diamond Time Weight", config.getDiamondTimeWeight());
          else if (tier == 2)
            newVal =
                settingsView.promptRuleDoubleInput("Gold Time Weight", config.getGoldTimeWeight());
          else if (tier == 3)
            newVal =
                settingsView.promptRuleDoubleInput(
                    "Silver Time Weight", config.getSilverTimeWeight());

          if (newVal != null) {
            if (tier == 1) config.setDiamondTimeWeight(newVal);
            else if (tier == 2) config.setGoldTimeWeight(newVal);
            else if (tier == 3) config.setSilverTimeWeight(newVal);
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
