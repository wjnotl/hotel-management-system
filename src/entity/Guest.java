package entity;

public class Guest {
  private String guestId; // Unique customer ID (e.g., "G-10023")
  private String name; // Full Name
  private String icNumber; // IC Number
  private String passportNumber; // Passport Number
  private String email; // Email Address
  private String phoneNumber; // Phone Number
  private String memberId; // Links to Member card (null if non-member)

  public Guest(
      String guestId,
      String name,
      String icNumber,
      String passportNumber,
      String email,
      String phoneNumber,
      String memberId) {
    this.guestId = guestId;
    this.name = name;
    this.icNumber = icNumber;
    this.passportNumber = passportNumber;
    this.email = email;
    this.phoneNumber = phoneNumber;
    this.memberId = memberId;
  }

  public String getGuestId() {
    return guestId;
  }

  public String getName() {
    return name;
  }

  public String getIcNumber() {
    return icNumber;
  }

  public String getPassportNumber() {
    return passportNumber;
  }

  public String getEmail() {
    return email;
  }

  public String getPhoneNumber() {
    return phoneNumber;
  }

  public String getMemberId() {
    return memberId;
  }

  public void setGuestId(String guestId) {
    this.guestId = guestId;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setIcNumber(String icNumber) {
    this.icNumber = icNumber;
  }

  public void setPassportNumber(String passportNumber) {
    this.passportNumber = passportNumber;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public void setPhoneNumber(String phoneNumber) {
    this.phoneNumber = phoneNumber;
  }

  public void setMemberId(String memberId) {
    this.memberId = memberId;
  }
}
