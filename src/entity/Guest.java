package entity;

import java.io.Serializable;

public class Guest implements Serializable {
  private static final long serialVersionUID = 1L;

  private String guestId; // Unique customer ID (e.g., "G-10023")
  private String name; // Full Name
  private String icNumber; // IC Number
  private String passportNumber; // Passport Number
  private String email; // Email Address
  private String phoneNumber; // Phone Number
  private String memberId; // Links to Member (null if non-member)
  private int strikeCount; // Daily no-show counter

  public Guest(
      String guestId,
      String name,
      String icNumber,
      String passportNumber,
      String email,
      String phoneNumber,
      String memberId,
      int strikeCount) {
    this.guestId = guestId;
    this.name = name;
    this.icNumber = icNumber;
    this.passportNumber = passportNumber;
    this.email = email;
    this.phoneNumber = phoneNumber;
    this.memberId = memberId;
    this.strikeCount = strikeCount;
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

  public int getStrikeCount() {
    return strikeCount;
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

  public void setStrikeCount(int strikeCount) {
    this.strikeCount = strikeCount;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    Guest other = (Guest) obj;
    return guestId != null && guestId.equals(other.guestId);
  }

  @Override
  public int hashCode() {
    return guestId != null ? guestId.hashCode() : 0;
  }

  @Override
  public String toString() {
    return "Guest{"
        + "guestId='"
        + guestId
        + "'"
        + ", name='"
        + name
        + "'"
        + ", icNumber='"
        + icNumber
        + "'"
        + ", passportNumber='"
        + passportNumber
        + "'"
        + ", email='"
        + email
        + "'"
        + ", phoneNumber='"
        + phoneNumber
        + "'"
        + ", memberId='"
        + memberId
        + "'"
        + ", strikeCount="
        + strikeCount
        + "}";
  }
}
