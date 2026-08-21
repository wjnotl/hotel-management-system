package entity;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public class Billing implements Serializable {
  private static final long serialVersionUID = 1L;

  // Malaysian Sales & Service Tax rate applied to room charges.
  public static final double SST_RATE = 0.08;

  public static enum Status {
    UNPAID,
    PAID
  }

  private String billingId; // Unique billing/folio ID (e.g. "BILL-1001")
  private String guestId; // References Guest.guestId
  private String reservationId; // References Reservation.reservationId
  private String roomNumber; // References Room.roomNumber
  private Room.RoomType roomType;
  private LocalDate checkInDate;
  private LocalDate checkOutDate;
  private double ratePerNight;
  private long paidNights;
  private Status status;
  private LocalDateTime createdAt; // Used to order billing/stay history newest -> oldest

  public Billing(
      String billingId,
      String guestId,
      String reservationId,
      String roomNumber,
      Room.RoomType roomType,
      LocalDate checkInDate,
      LocalDate checkOutDate,
      double ratePerNight,
      Status status,
      LocalDateTime createdAt) {
    this.billingId = billingId;
    this.guestId = guestId;
    this.reservationId = reservationId;
    this.roomNumber = roomNumber;
    this.roomType = roomType;
    this.checkInDate = checkInDate;
    this.checkOutDate = checkOutDate;
    this.ratePerNight = ratePerNight;
    this.status = status;
    this.createdAt = createdAt;
  }

  // --- GETTERS ---

  public String getBillingId() {
    return billingId;
  }

  public String getGuestId() {
    return guestId;
  }

  public String getReservationId() {
    return reservationId;
  }

  public String getRoomNumber() {
    return roomNumber;
  }

  public Room.RoomType getRoomType() {
    return roomType;
  }

  public LocalDate getCheckInDate() {
    return checkInDate;
  }

  public LocalDate getCheckOutDate() {
    return checkOutDate;
  }

  public double getRatePerNight() {
    return ratePerNight;
  }

  public Status getStatus() {
    return status;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  // --- SETTERS ---

  public void setBillingId(String billingId) {
    this.billingId = billingId;
  }

  public void setGuestId(String guestId) {
    this.guestId = guestId;
  }

  public void setReservationId(String reservationId) {
    this.reservationId = reservationId;
  }

  public void setRoomNumber(String roomNumber) {
    this.roomNumber = roomNumber;
  }

  public void setRoomType(Room.RoomType roomType) {
    this.roomType = roomType;
  }

  public void setCheckInDate(LocalDate checkInDate) {
    this.checkInDate = checkInDate;
  }

  public void setCheckOutDate(LocalDate checkOutDate) {
    this.checkOutDate = checkOutDate;
  }

  public void setRatePerNight(double ratePerNight) {
    this.ratePerNight = ratePerNight;
  }

  public long getPaidNights() {
    return paidNights;
  }

  public void setPaidNights(long paidNights) {
    this.paidNights = paidNights;
  }

  public void setStatus(Status status) {
    this.status = status;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }

  // --- BILLING / RECEIPT CALCULATIONS ---

  public long getNumberOfNights() {
    if (checkInDate == null || checkOutDate == null) return 0;
    long nights = ChronoUnit.DAYS.between(checkInDate, checkOutDate);
    return Math.max(nights, 1);
  }

  public double getSubtotal() {
    return getNumberOfNights() * ratePerNight;
  }

  public double getSstAmount() {
    return getSubtotal() * SST_RATE;
  }

  public double getTotalAmount() {
    return getSubtotal() + getSstAmount();
  }

  public long getOutstandingNights() {
    return Math.max(0, getNumberOfNights() - paidNights);
  }

  public double getOutstandingSubtotal() {
    return getOutstandingNights() * ratePerNight;
  }

  public double getOutstandingSST() {
    return getOutstandingSubtotal() * SST_RATE;
  }

  public double getOutstandingTotal() {
    return getOutstandingSubtotal() + getOutstandingSST();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    Billing other = (Billing) obj;
    return billingId != null && billingId.equalsIgnoreCase(other.billingId);
  }

  @Override
  public int hashCode() {
    return billingId != null ? billingId.toLowerCase().hashCode() : 0;
  }

  @Override
  public String toString() {
    return "Billing{"
        + "billingId='"
        + billingId
        + "'"
        + ", guestId='"
        + guestId
        + "'"
        + ", reservationId='"
        + reservationId
        + "'"
        + ", roomNumber='"
        + roomNumber
        + "'"
        + ", roomType="
        + roomType
        + ", checkInDate="
        + checkInDate
        + ", checkOutDate="
        + checkOutDate
        + ", ratePerNight="
        + ratePerNight
        + ", status="
        + status
        + "}";
  }
}
