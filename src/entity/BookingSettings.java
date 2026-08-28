package entity;

import java.io.Serializable;

// One record for the whole module. Every field is read at runtime, so a value changes behaviour.
public class BookingSettings implements Serializable {
  private static final long serialVersionUID = 1L;

  public static final int UNLIMITED_QUEUE = 0;
  public static final int UNLIMITED_LEAD_DAYS = 0;

  // This value defers to the house wide figure. No overridable setting can be negative.
  public static final int USE_HOUSE_VALUE = -1;

  private static final int ROOM_TYPE_COUNT = 3;

  // Serialisation cannot tell an absent field from a false one, so the stamp dates the file.
  private static final int CURRENT_SCHEMA = 6;
  private int schemaVersion;

  // Hold & No-Show Rules
  private int holdGraceMinutes;
  private int maxStrikes;
  private boolean requeueOnLapse;

  // Queue Rules
  private int initialQueueCapacity;
  private boolean allowQueueExpansion;
  private int maxQueueLength;
  private boolean autoAssignWhenRoomFree;
  private boolean enforceVipBypass;
  private boolean allowBypassOverride;
  private boolean blockDuplicateAcrossLines;
  private boolean allowNonFrontAllocation;

  // Advance Booking Rules
  private int advanceBookingLeadDays;
  private boolean allowSameDayAdvanceBooking;
  private boolean checkoutDayReusable;
  private boolean blockOverbooking;

  // Desk Defaults
  private int pageSize;
  private int maxStayNights;
  private String defaultQueueSort;
  private String defaultAdvanceSort;
  private String defaultReportPeriod;
  private int defaultRecordLimit;

  // Per-Room-Type Overrides, indexed by Room.RoomType.ordinal()
  private int[] typeHoldGraceMinutes;
  private int[] typeInitialQueueCapacity;
  private int[] typeMaxQueueLength;

  public BookingSettings() {
    resetToDefaults();
  }

  public final void resetToDefaults() {
    this.schemaVersion = CURRENT_SCHEMA;
    this.holdGraceMinutes = 15;
    this.maxStrikes = 3;
    this.requeueOnLapse = true;

    this.initialQueueCapacity = 8;
    this.allowQueueExpansion = false;
    this.maxQueueLength = UNLIMITED_QUEUE;
    this.autoAssignWhenRoomFree = true;
    this.enforceVipBypass = true;
    this.allowBypassOverride = true;
    this.blockDuplicateAcrossLines = false;
    this.allowNonFrontAllocation = true;

    this.advanceBookingLeadDays = 365;
    this.allowSameDayAdvanceBooking = false;
    this.checkoutDayReusable = true;
    this.blockOverbooking = true;

    this.pageSize = 10;
    this.maxStayNights = 30;
    this.defaultQueueSort = "QUEUE POSITION (ARRIVAL ORDER)";
    this.defaultAdvanceSort = "ARRIVAL DATE (SOONEST -> LATEST)";
    this.defaultReportPeriod = "TODAY";
    this.defaultRecordLimit = 10;

    this.typeHoldGraceMinutes = blankOverrides();
    this.typeInitialQueueCapacity = blankOverrides();
    this.typeMaxQueueLength = blankOverrides();
  }

  private int[] blankOverrides() {
    int[] overrides = new int[ROOM_TYPE_COUNT];
    for (int i = 0; i < ROOM_TYPE_COUNT; i++) {
      overrides[i] = USE_HOUSE_VALUE;
    }
    return overrides;
  }

  // Older files carry null override arrays and renamed sort options, so they are repaired on load.
  public void normalize() {
    if (schemaVersion < 2) {
      this.allowNonFrontAllocation = true;
      this.allowSameDayAdvanceBooking = true;
      this.checkoutDayReusable = true;
      this.blockOverbooking = true;
      this.advanceBookingLeadDays = 365;
      this.defaultReportPeriod = "TODAY";
      this.defaultRecordLimit = 10;
    }

    // An array that doubled on demand made the capacity figure decorative, so growth is opt-in.
    if (schemaVersion < 3) {
      this.allowQueueExpansion = false;
    }

    // Wanting LUXURY and settling for SUITE is two enquiries, not cheating, so the wide rule is
    // off.
    if (schemaVersion < 4) {
      this.blockDuplicateAcrossLines = false;
    }

    // Someone asking for a room tonight is a walk-in, so the window opens tomorrow.
    if (schemaVersion < 5) {
      this.allowSameDayAdvanceBooking = false;
    }

    // The sort label lost its jargon, so a file still holding the old wording is renamed.
    if (schemaVersion < 6 && "QUEUE POSITION (FIFO)".equals(defaultQueueSort)) {
      this.defaultQueueSort = "QUEUE POSITION (ARRIVAL ORDER)";
    }

    if (schemaVersion < CURRENT_SCHEMA) {
      this.schemaVersion = CURRENT_SCHEMA;
    }

    if (typeHoldGraceMinutes == null || typeHoldGraceMinutes.length != ROOM_TYPE_COUNT) {
      typeHoldGraceMinutes = blankOverrides();
    }
    if (typeInitialQueueCapacity == null || typeInitialQueueCapacity.length != ROOM_TYPE_COUNT) {
      typeInitialQueueCapacity = blankOverrides();
    }
    if (typeMaxQueueLength == null || typeMaxQueueLength.length != ROOM_TYPE_COUNT) {
      typeMaxQueueLength = blankOverrides();
    }
    if (defaultQueueSort == null || defaultQueueSort.isEmpty()) {
      defaultQueueSort = "QUEUE POSITION (ARRIVAL ORDER)";
    }
    if (defaultAdvanceSort == null || defaultAdvanceSort.isEmpty()) {
      defaultAdvanceSort = "ARRIVAL DATE (SOONEST -> LATEST)";
    }
    if (defaultReportPeriod == null || defaultReportPeriod.isEmpty()) {
      defaultReportPeriod = "TODAY";
    }
    if (pageSize <= 0) pageSize = 10;
    if (maxStayNights <= 0) maxStayNights = 30;
    if (initialQueueCapacity <= 0) initialQueueCapacity = 8;
    if (holdGraceMinutes <= 0) holdGraceMinutes = 15;
    if (maxStrikes <= 0) maxStrikes = 3;
    if (advanceBookingLeadDays < 0) advanceBookingLeadDays = 365;
    if (defaultRecordLimit < 0) defaultRecordLimit = 10;
  }

  private int overrideOr(int[] overrides, int index, int houseValue) {
    if (overrides == null || index < 0 || index >= overrides.length) return houseValue;
    return (overrides[index] == USE_HOUSE_VALUE) ? houseValue : overrides[index];
  }

  private int typeIndex(Room.RoomType roomType) {
    return (roomType == null) ? -1 : roomType.ordinal();
  }

  public int getHoldGraceMinutes() {
    return holdGraceMinutes;
  }

  public int getHoldGraceMinutes(Room.RoomType roomType) {
    return overrideOr(typeHoldGraceMinutes, typeIndex(roomType), holdGraceMinutes);
  }

  public int getMaxStrikes() {
    return maxStrikes;
  }

  public boolean isRequeueOnLapse() {
    return requeueOnLapse;
  }

  public int getInitialQueueCapacity() {
    return initialQueueCapacity;
  }

  public int getInitialQueueCapacity(Room.RoomType roomType) {
    return overrideOr(typeInitialQueueCapacity, typeIndex(roomType), initialQueueCapacity);
  }

  public boolean isAllowQueueExpansion() {
    return allowQueueExpansion;
  }

  public int getMaxQueueLength() {
    return maxQueueLength;
  }

  public int getMaxQueueLength(Room.RoomType roomType) {
    return overrideOr(typeMaxQueueLength, typeIndex(roomType), maxQueueLength);
  }

  public boolean isAutoAssignWhenRoomFree() {
    return autoAssignWhenRoomFree;
  }

  public boolean isEnforceVipBypass() {
    return enforceVipBypass;
  }

  public boolean isAllowBypassOverride() {
    return allowBypassOverride;
  }

  public boolean isBlockDuplicateAcrossLines() {
    return blockDuplicateAcrossLines;
  }

  public boolean isAllowNonFrontAllocation() {
    return allowNonFrontAllocation;
  }

  public int getAdvanceBookingLeadDays() {
    return advanceBookingLeadDays;
  }

  public boolean isAllowSameDayAdvanceBooking() {
    return allowSameDayAdvanceBooking;
  }

  public boolean isCheckoutDayReusable() {
    return checkoutDayReusable;
  }

  public boolean isBlockOverbooking() {
    return blockOverbooking;
  }

  public int getPageSize() {
    return pageSize;
  }

  public int getMaxStayNights() {
    return maxStayNights;
  }

  public String getDefaultQueueSort() {
    return defaultQueueSort;
  }

  public String getDefaultAdvanceSort() {
    return defaultAdvanceSort;
  }

  public String getDefaultReportPeriod() {
    return defaultReportPeriod;
  }

  public int getDefaultRecordLimit() {
    return defaultRecordLimit;
  }

  public int getHoldGraceMinutesOverride(Room.RoomType roomType) {
    return rawOverride(typeHoldGraceMinutes, typeIndex(roomType));
  }

  public int getInitialQueueCapacityOverride(Room.RoomType roomType) {
    return rawOverride(typeInitialQueueCapacity, typeIndex(roomType));
  }

  public int getMaxQueueLengthOverride(Room.RoomType roomType) {
    return rawOverride(typeMaxQueueLength, typeIndex(roomType));
  }

  private int rawOverride(int[] overrides, int index) {
    if (overrides == null || index < 0 || index >= overrides.length) return USE_HOUSE_VALUE;
    return overrides[index];
  }

  public void setHoldGraceMinutes(int holdGraceMinutes) {
    this.holdGraceMinutes = holdGraceMinutes;
  }

  public void setMaxStrikes(int maxStrikes) {
    this.maxStrikes = maxStrikes;
  }

  public void setRequeueOnLapse(boolean requeueOnLapse) {
    this.requeueOnLapse = requeueOnLapse;
  }

  public void setInitialQueueCapacity(int initialQueueCapacity) {
    this.initialQueueCapacity = initialQueueCapacity;
  }

  public void setAllowQueueExpansion(boolean allowQueueExpansion) {
    this.allowQueueExpansion = allowQueueExpansion;
  }

  public void setMaxQueueLength(int maxQueueLength) {
    this.maxQueueLength = maxQueueLength;
  }

  public void setAutoAssignWhenRoomFree(boolean autoAssignWhenRoomFree) {
    this.autoAssignWhenRoomFree = autoAssignWhenRoomFree;
  }

  public void setEnforceVipBypass(boolean enforceVipBypass) {
    this.enforceVipBypass = enforceVipBypass;
  }

  public void setAllowBypassOverride(boolean allowBypassOverride) {
    this.allowBypassOverride = allowBypassOverride;
  }

  public void setBlockDuplicateAcrossLines(boolean blockDuplicateAcrossLines) {
    this.blockDuplicateAcrossLines = blockDuplicateAcrossLines;
  }

  public void setAllowNonFrontAllocation(boolean allowNonFrontAllocation) {
    this.allowNonFrontAllocation = allowNonFrontAllocation;
  }

  public void setAdvanceBookingLeadDays(int advanceBookingLeadDays) {
    this.advanceBookingLeadDays = advanceBookingLeadDays;
  }

  public void setAllowSameDayAdvanceBooking(boolean allowSameDayAdvanceBooking) {
    this.allowSameDayAdvanceBooking = allowSameDayAdvanceBooking;
  }

  public void setCheckoutDayReusable(boolean checkoutDayReusable) {
    this.checkoutDayReusable = checkoutDayReusable;
  }

  public void setBlockOverbooking(boolean blockOverbooking) {
    this.blockOverbooking = blockOverbooking;
  }

  public void setPageSize(int pageSize) {
    this.pageSize = pageSize;
  }

  public void setMaxStayNights(int maxStayNights) {
    this.maxStayNights = maxStayNights;
  }

  public void setDefaultQueueSort(String defaultQueueSort) {
    this.defaultQueueSort = defaultQueueSort;
  }

  public void setDefaultAdvanceSort(String defaultAdvanceSort) {
    this.defaultAdvanceSort = defaultAdvanceSort;
  }

  public void setDefaultReportPeriod(String defaultReportPeriod) {
    this.defaultReportPeriod = defaultReportPeriod;
  }

  public void setDefaultRecordLimit(int defaultRecordLimit) {
    this.defaultRecordLimit = defaultRecordLimit;
  }

  public void setHoldGraceMinutesOverride(Room.RoomType roomType, int value) {
    writeOverride(typeHoldGraceMinutes, typeIndex(roomType), value);
  }

  public void setInitialQueueCapacityOverride(Room.RoomType roomType, int value) {
    writeOverride(typeInitialQueueCapacity, typeIndex(roomType), value);
  }

  public void setMaxQueueLengthOverride(Room.RoomType roomType, int value) {
    writeOverride(typeMaxQueueLength, typeIndex(roomType), value);
  }

  private void writeOverride(int[] overrides, int index, int value) {
    if (overrides == null || index < 0 || index >= overrides.length) return;
    overrides[index] = value;
  }

  public boolean hasAnyOverride() {
    return countOverrides() > 0;
  }

  public int countOverrides() {
    int count = 0;
    count += countSet(typeHoldGraceMinutes);
    count += countSet(typeInitialQueueCapacity);
    count += countSet(typeMaxQueueLength);
    return count;
  }

  private int countSet(int[] overrides) {
    if (overrides == null) return 0;
    int count = 0;
    for (int value : overrides) {
      if (value != USE_HOUSE_VALUE) count++;
    }
    return count;
  }

  public void clearAllOverrides() {
    typeHoldGraceMinutes = blankOverrides();
    typeInitialQueueCapacity = blankOverrides();
    typeMaxQueueLength = blankOverrides();
  }

  // Unlimited is 0 rather than a separate flag, so the two can never disagree.
  public boolean isQueueLengthCapped() {
    return maxQueueLength > UNLIMITED_QUEUE;
  }

  public boolean isQueueLengthCapped(Room.RoomType roomType) {
    return getMaxQueueLength(roomType) > UNLIMITED_QUEUE;
  }

  @Override
  public String toString() {
    return "BookingSettings{"
        + "holdGraceMinutes="
        + holdGraceMinutes
        + ", maxStrikes="
        + maxStrikes
        + ", requeueOnLapse="
        + requeueOnLapse
        + ", initialQueueCapacity="
        + initialQueueCapacity
        + ", allowQueueExpansion="
        + allowQueueExpansion
        + ", maxQueueLength="
        + maxQueueLength
        + ", autoAssignWhenRoomFree="
        + autoAssignWhenRoomFree
        + ", enforceVipBypass="
        + enforceVipBypass
        + ", allowBypassOverride="
        + allowBypassOverride
        + ", allowNonFrontAllocation="
        + allowNonFrontAllocation
        + ", blockDuplicateAcrossLines="
        + blockDuplicateAcrossLines
        + ", advanceBookingLeadDays="
        + advanceBookingLeadDays
        + ", allowSameDayAdvanceBooking="
        + allowSameDayAdvanceBooking
        + ", checkoutDayReusable="
        + checkoutDayReusable
        + ", blockOverbooking="
        + blockOverbooking
        + ", pageSize="
        + pageSize
        + ", maxStayNights="
        + maxStayNights
        + ", defaultQueueSort='"
        + defaultQueueSort
        + "'"
        + ", defaultAdvanceSort='"
        + defaultAdvanceSort
        + "'"
        + ", defaultReportPeriod='"
        + defaultReportPeriod
        + "'"
        + ", defaultRecordLimit="
        + defaultRecordLimit
        + ", overrides="
        + countOverrides()
        + "}";
  }
}
