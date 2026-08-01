package entity;

import java.io.Serializable;

// Singleton-style config record for the Housekeeping module — only one instance ever exists,
// created with sensible defaults on first run and edited in place via Settings & Configuration.
// These values are stored for reference / future use; as of now editing them here does not
// change behavior elsewhere in the module (no auto-escalation, no queue-jump enforcement, no
// max-rooms enforcement) — see HouseKeepingController's Settings screens for the current scope.
public class HousekeepingSettings implements Serializable {
  private static final long serialVersionUID = 1L;

  // --- Task Rules: standard cleaning time estimates (minutes) per room type ---
  private int cleanTimeStandardMinutes;
  private int cleanTimeSuiteMinutes;
  private int cleanTimeLuxuryMinutes;

  // --- Task Rules: overdue threshold (minutes) before a task is considered overdue ---
  private int overdueThresholdMinutes;

  // --- Task Rules: which task types are allowed to jump the queue (Add Urgent Task) ---
  private boolean queueJumpStandardClean;
  private boolean queueJumpDeepClean;
  private boolean queueJumpTurnover;
  private boolean queueJumpMaintenanceCheck;

  // --- Staff Configuration: shift schedules (free-text, e.g. "07:00 - 15:00") ---
  private String morningShiftSchedule;
  private String afternoonShiftSchedule;
  private String nightShiftSchedule;

  // --- Staff Configuration: max rooms assignable per staff per shift ---
  private int maxRoomsMorning;
  private int maxRoomsAfternoon;
  private int maxRoomsNight;

  public HousekeepingSettings() {
    this.cleanTimeStandardMinutes = 30;
    this.cleanTimeSuiteMinutes = 45;
    this.cleanTimeLuxuryMinutes = 60;

    this.overdueThresholdMinutes = 120;

    this.queueJumpStandardClean = true;
    this.queueJumpDeepClean = true;
    this.queueJumpTurnover = true;
    this.queueJumpMaintenanceCheck = true;

    this.morningShiftSchedule = "07:00 - 15:00";
    this.afternoonShiftSchedule = "15:00 - 23:00";
    this.nightShiftSchedule = "23:00 - 07:00";

    this.maxRoomsMorning = 8;
    this.maxRoomsAfternoon = 8;
    this.maxRoomsNight = 5;
  }

  public int getCleanTimeStandardMinutes() {
    return cleanTimeStandardMinutes;
  }

  public void setCleanTimeStandardMinutes(int cleanTimeStandardMinutes) {
    this.cleanTimeStandardMinutes = cleanTimeStandardMinutes;
  }

  public int getCleanTimeSuiteMinutes() {
    return cleanTimeSuiteMinutes;
  }

  public void setCleanTimeSuiteMinutes(int cleanTimeSuiteMinutes) {
    this.cleanTimeSuiteMinutes = cleanTimeSuiteMinutes;
  }

  public int getCleanTimeLuxuryMinutes() {
    return cleanTimeLuxuryMinutes;
  }

  public void setCleanTimeLuxuryMinutes(int cleanTimeLuxuryMinutes) {
    this.cleanTimeLuxuryMinutes = cleanTimeLuxuryMinutes;
  }

  public int getOverdueThresholdMinutes() {
    return overdueThresholdMinutes;
  }

  public void setOverdueThresholdMinutes(int overdueThresholdMinutes) {
    this.overdueThresholdMinutes = overdueThresholdMinutes;
  }

  public boolean isQueueJumpStandardClean() {
    return queueJumpStandardClean;
  }

  public void setQueueJumpStandardClean(boolean queueJumpStandardClean) {
    this.queueJumpStandardClean = queueJumpStandardClean;
  }

  public boolean isQueueJumpDeepClean() {
    return queueJumpDeepClean;
  }

  public void setQueueJumpDeepClean(boolean queueJumpDeepClean) {
    this.queueJumpDeepClean = queueJumpDeepClean;
  }

  public boolean isQueueJumpTurnover() {
    return queueJumpTurnover;
  }

  public void setQueueJumpTurnover(boolean queueJumpTurnover) {
    this.queueJumpTurnover = queueJumpTurnover;
  }

  public boolean isQueueJumpMaintenanceCheck() {
    return queueJumpMaintenanceCheck;
  }

  public void setQueueJumpMaintenanceCheck(boolean queueJumpMaintenanceCheck) {
    this.queueJumpMaintenanceCheck = queueJumpMaintenanceCheck;
  }

  public String getMorningShiftSchedule() {
    return morningShiftSchedule;
  }

  public void setMorningShiftSchedule(String morningShiftSchedule) {
    this.morningShiftSchedule = morningShiftSchedule;
  }

  public String getAfternoonShiftSchedule() {
    return afternoonShiftSchedule;
  }

  public void setAfternoonShiftSchedule(String afternoonShiftSchedule) {
    this.afternoonShiftSchedule = afternoonShiftSchedule;
  }

  public String getNightShiftSchedule() {
    return nightShiftSchedule;
  }

  public void setNightShiftSchedule(String nightShiftSchedule) {
    this.nightShiftSchedule = nightShiftSchedule;
  }

  public int getMaxRoomsMorning() {
    return maxRoomsMorning;
  }

  public void setMaxRoomsMorning(int maxRoomsMorning) {
    this.maxRoomsMorning = maxRoomsMorning;
  }

  public int getMaxRoomsAfternoon() {
    return maxRoomsAfternoon;
  }

  public void setMaxRoomsAfternoon(int maxRoomsAfternoon) {
    this.maxRoomsAfternoon = maxRoomsAfternoon;
  }

  public int getMaxRoomsNight() {
    return maxRoomsNight;
  }

  public void setMaxRoomsNight(int maxRoomsNight) {
    this.maxRoomsNight = maxRoomsNight;
  }
}
