package control.booking;

import entity.Guest;
import repo.GuestRepo;
import util.ConsoleUtil;
import view.booking.GuestRegistrationView;

public class GuestRegistrationController {
  private static final int STEP_NAME = 1;
  private static final int STEP_IC = 2;
  private static final int STEP_PASSPORT = 3;
  private static final int STEP_PHONE = 4;
  private static final int STEP_EMAIL = 5;
  private static final int STEP_CONFIRM = 6;

  private static final int MIN_NAME_LENGTH = 2;
  private static final int MAX_NAME_LENGTH = 60;
  private static final int MIN_PASSPORT_LENGTH = 5;
  private static final int MAX_PASSPORT_LENGTH = 15;
  private static final int MIN_PHONE_DIGITS = 7;

  private static final int IC_LENGTH = 14;
  private static final int IC_DIGIT_COUNT = 12;
  private static final int IC_FIRST_HYPHEN = 6;
  private static final int IC_SECOND_HYPHEN = 9;

  private static final String CLEAR = "-";

  private final GuestRegistrationView registrationView = new GuestRegistrationView();
  private final GuestRepo guestRepo;

  public GuestRegistrationController(GuestRepo guestRepo) {
    this.guestRepo = guestRepo;
  }

  private static class StepResult {
    private static final int NEXT = 1;
    private static final int BACK = 2;
    private static final int CANCEL = 3;

    private final int outcome;
    private final String value;

    private StepResult(int outcome, String value) {
      this.outcome = outcome;
      this.value = value;
    }

    private static StepResult next(String value) {
      return new StepResult(NEXT, value);
    }

    private static StepResult back() {
      return new StepResult(BACK, null);
    }

    private static StepResult cancel() {
      return new StepResult(CANCEL, null);
    }
  }

  // Returns the saved guest, or null if the clerk cancelled or walked back out of the form.
  // The suggestion is whatever was typed at the booking screen, so a clerk who searched by
  // name does not have to type it a second time.
  public Guest registerNewGuest(String suggestedName) {
    String name = (suggestedName == null) ? "" : suggestedName.trim();
    String icNumber = "";
    String passportNumber = "";
    String phoneNumber = "";
    String email = "";

    int step = STEP_NAME;

    while (true) {
      if (step == STEP_CONFIRM) {
        // The id is minted only once the clerk is looking at the summary, so a cancelled or
        // reopened form never burns a number that the next guest would then skip over.
        String guestId = guestRepo.generateGuestId();

        int choice =
            registrationView.displayConfirmationScreen(
                guestId, name, icNumber, passportNumber, phoneNumber, email);

        if (choice == 2) {
          step = STEP_EMAIL;
          continue;
        } else if (choice != 1) {
          return null;
        }

        Guest guest =
            new Guest(
                guestId,
                name,
                icNumber.isEmpty() ? null : icNumber,
                passportNumber.isEmpty() ? null : passportNumber,
                email.isEmpty() ? null : email,
                phoneNumber,
                null,
                0);

        guestRepo.addGuest(guest);
        registrationView.displaySuccessScreen(guest);
        return guest;
      }

      StepResult result;

      if (step == STEP_IC) {
        result = collectIcNumber(name, icNumber);
        if (result.outcome == StepResult.NEXT) icNumber = result.value;
      } else if (step == STEP_PASSPORT) {
        result = collectPassportNumber(name, passportNumber);
        if (result.outcome == StepResult.NEXT) passportNumber = result.value;
      } else if (step == STEP_PHONE) {
        result = collectPhoneNumber(name, phoneNumber);
        if (result.outcome == StepResult.NEXT) phoneNumber = result.value;
      } else if (step == STEP_EMAIL) {
        result = collectEmail(name, email);
        if (result.outcome == StepResult.NEXT) email = result.value;
      } else {
        result = collectName(name);
        if (result.outcome == StepResult.NEXT) name = result.value;
      }

      if (result.outcome == StepResult.CANCEL) {
        return null;
      }

      if (result.outcome == StepResult.BACK) {
        step--;
        // Stepping back off the first field leaves the form, which returns the clerk to the
        // guest search they came from rather than trapping them in the registration screens.
        if (step < STEP_NAME) return null;
        continue;
      }

      step++;

      // Checked on the way out of the passport step rather than at the summary, so the clerk
      // is not asked for three more fields before being told the file has no document on it.
      if (step == STEP_PHONE && icNumber.isEmpty() && passportNumber.isEmpty()) {
        registrationView.displayFieldErrorScreen(
            "Identity Document",
            "",
            "A guest file needs at least one identity document. Capture either the IC number or"
                + " the passport number.");
        step = STEP_IC;
      }
    }
  }

  private StepResult collectName(String current) {
    while (true) {
      try {
        String input = registrationView.promptName(STEP_NAME, current);
        if (isCancel(input)) return StepResult.cancel();
        if (isBack(input)) return StepResult.back();

        String name = input.trim();
        if (name.isEmpty() && !current.isEmpty()) {
          return StepResult.next(current);
        }

        if (name.length() < MIN_NAME_LENGTH || name.length() > MAX_NAME_LENGTH) {
          registrationView.displayFieldErrorScreen(
              "Full Name",
              name,
              "A name must be between "
                  + MIN_NAME_LENGTH
                  + " and "
                  + MAX_NAME_LENGTH
                  + " characters.");
          continue;
        }

        if (!isValidName(name)) {
          registrationView.displayFieldErrorScreen(
              "Full Name",
              name,
              "A name may only contain letters, spaces, apostrophes, hyphens, full stops and"
                  + " slashes.");
          continue;
        }

        // findByName returns the first match, so a second guest under the same name would be
        // unreachable by name at every booking screen. The clerk is told before that happens.
        Guest sameName = guestRepo.findByName(name);
        if (sameName != null && !registrationView.displaySameNameWarningScreen(name, sameName)) {
          continue;
        }

        return StepResult.next(name);
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  private StepResult collectIcNumber(String name, String current) {
    while (true) {
      try {
        String input = registrationView.promptIcNumber(STEP_IC, name, current);
        if (isCancel(input)) return StepResult.cancel();
        if (isBack(input)) return StepResult.back();

        String typed = input.trim();
        if (typed.isEmpty()) return StepResult.next(current);
        if (CLEAR.equals(typed)) return StepResult.next("");

        // Storing the hyphens is what lets findByIdentityDocument match at all, so a clerk
        // who reads the 12 digits straight off the card still ends up with the same value as
        // one who typed the mask.
        String icNumber = applyIcMask(typed);

        if (!isValidMalaysianIc(icNumber)) {
          registrationView.displayFieldErrorScreen(
              "IC Number",
              icNumber,
              "A Malaysian IC must read XXXXXX-XX-XXXX, where the first six digits are the date"
                  + " of birth as YYMMDD. Example: 920101-14-5543. The 12 digits may be typed"
                  + " without hyphens and the mask is applied automatically.");
          continue;
        }

        Guest owner = guestRepo.findByIdentityDocument(icNumber);
        if (owner != null) {
          registrationView.displayDuplicateGuestScreen("IC Number", icNumber, owner);
          continue;
        }

        return StepResult.next(icNumber);
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  private StepResult collectPassportNumber(String name, String current) {
    while (true) {
      try {
        String input = registrationView.promptPassportNumber(STEP_PASSPORT, name, current);
        if (isCancel(input)) return StepResult.cancel();
        if (isBack(input)) return StepResult.back();

        String passportNumber = input.trim();
        if (passportNumber.isEmpty()) return StepResult.next(current);
        if (CLEAR.equals(passportNumber)) return StepResult.next("");

        if (passportNumber.length() < MIN_PASSPORT_LENGTH
            || passportNumber.length() > MAX_PASSPORT_LENGTH
            || !isAlphanumeric(passportNumber)) {
          registrationView.displayFieldErrorScreen(
              "Passport Number",
              passportNumber,
              "A passport number must be "
                  + MIN_PASSPORT_LENGTH
                  + " to "
                  + MAX_PASSPORT_LENGTH
                  + " letters and digits with no spaces.");
          continue;
        }

        Guest owner = guestRepo.findByIdentityDocument(passportNumber);
        if (owner != null) {
          registrationView.displayDuplicateGuestScreen("Passport Number", passportNumber, owner);
          continue;
        }

        return StepResult.next(passportNumber);
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  private StepResult collectPhoneNumber(String name, String current) {
    while (true) {
      try {
        String input = registrationView.promptPhoneNumber(STEP_PHONE, name, current);
        if (isCancel(input)) return StepResult.cancel();
        if (isBack(input)) return StepResult.back();

        String phoneNumber = input.trim();
        if (phoneNumber.isEmpty() && !current.isEmpty()) {
          return StepResult.next(current);
        }

        if (countDigits(phoneNumber) < MIN_PHONE_DIGITS || !isPhoneShaped(phoneNumber)) {
          registrationView.displayFieldErrorScreen(
              "Phone Number",
              phoneNumber,
              "A phone number must hold at least "
                  + MIN_PHONE_DIGITS
                  + " digits and may only contain digits, spaces, hyphens and a leading plus.");
          continue;
        }

        Guest owner = guestRepo.findByPhoneNumber(phoneNumber);
        if (owner != null) {
          registrationView.displayDuplicateGuestScreen("Phone Number", phoneNumber, owner);
          continue;
        }

        return StepResult.next(phoneNumber);
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  private StepResult collectEmail(String name, String current) {
    while (true) {
      try {
        String input = registrationView.promptEmail(STEP_EMAIL, name, current);
        if (isCancel(input)) return StepResult.cancel();
        if (isBack(input)) return StepResult.back();

        String email = input.trim();
        if (email.isEmpty()) return StepResult.next(current);
        if (CLEAR.equals(email)) return StepResult.next("");

        if (!isValidEmail(email)) {
          registrationView.displayFieldErrorScreen(
              "Email Address",
              email,
              "An email address needs one '@' with text on both sides and a dot in the domain,"
                  + " for example name@example.com.");
          continue;
        }

        return StepResult.next(email);
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  private boolean isCancel(String input) {
    return input == null || "C".equalsIgnoreCase(input.trim());
  }

  private boolean isBack(String input) {
    return input != null && "B".equalsIgnoreCase(input.trim());
  }

  // 12 bare digits are the same identity as the masked form, so the hyphens are inserted
  // rather than the entry being rejected. Anything else is handed back untouched so the
  // format check reports what the clerk actually typed.
  private String applyIcMask(String value) {
    if (value.length() != IC_DIGIT_COUNT) return value;

    for (int i = 0; i < IC_DIGIT_COUNT; i++) {
      if (!Character.isDigit(value.charAt(i))) return value;
    }

    return value.substring(0, IC_FIRST_HYPHEN)
        + "-"
        + value.substring(IC_FIRST_HYPHEN, 8)
        + "-"
        + value.substring(8);
  }

  // A Malaysian IC is a fixed mask, not a free-form string: YYMMDD-PB-###G. Checking the
  // shape alone would still admit 999999-99-9999, so the birth-date block is range checked
  // too. The place-of-birth block is deliberately left open because the assigned code list
  // has gaps, and rejecting an unlisted code would turn away a real guest at the counter.
  private boolean isValidMalaysianIc(String value) {
    if (value.length() != IC_LENGTH) return false;

    for (int i = 0; i < IC_LENGTH; i++) {
      char c = value.charAt(i);
      if (i == IC_FIRST_HYPHEN || i == IC_SECOND_HYPHEN) {
        if (c != '-') return false;
      } else if (!Character.isDigit(c)) {
        return false;
      }
    }

    return isValidBirthBlock(value.substring(0, IC_FIRST_HYPHEN));
  }

  private boolean isValidBirthBlock(String yymmdd) {
    int month = Integer.parseInt(yymmdd.substring(2, 4));
    int day = Integer.parseInt(yymmdd.substring(4, 6));

    if (month < 1 || month > 12 || day < 1) return false;
    return day <= daysInMonth(month);
  }

  // The year is only two digits so the century is unknown, which means 29 February can never
  // be ruled out from the IC alone and is allowed.
  private int daysInMonth(int month) {
    if (month == 2) return 29;
    if (month == 4 || month == 6 || month == 9 || month == 11) return 30;
    return 31;
  }

  private boolean isValidName(String value) {
    for (int i = 0; i < value.length(); i++) {
      char c = value.charAt(i);
      if (!Character.isLetter(c) && c != ' ' && c != '\'' && c != '-' && c != '.' && c != '/') {
        return false;
      }
    }
    return true;
  }

  private boolean isAlphanumeric(String value) {
    for (int i = 0; i < value.length(); i++) {
      if (!Character.isLetterOrDigit(value.charAt(i))) {
        return false;
      }
    }
    return true;
  }

  private boolean isPhoneShaped(String value) {
    for (int i = 0; i < value.length(); i++) {
      char c = value.charAt(i);
      if (c == '+' && i == 0) continue;
      if (!Character.isDigit(c) && c != '-' && c != ' ') {
        return false;
      }
    }
    return true;
  }

  private int countDigits(String value) {
    int digits = 0;
    for (int i = 0; i < value.length(); i++) {
      if (Character.isDigit(value.charAt(i))) {
        digits++;
      }
    }
    return digits;
  }

  private boolean isValidEmail(String value) {
    int at = value.indexOf('@');
    if (at <= 0 || at != value.lastIndexOf('@') || at == value.length() - 1) {
      return false;
    }

    String domain = value.substring(at + 1);
    int dot = domain.indexOf('.');
    if (dot <= 0 || dot == domain.length() - 1) {
      return false;
    }

    for (int i = 0; i < value.length(); i++) {
      if (Character.isWhitespace(value.charAt(i))) {
        return false;
      }
    }
    return true;
  }
}
