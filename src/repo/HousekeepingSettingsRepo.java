package repo;

import entity.HousekeepingSettings;
import util.BinaryFileUtil;

public class HousekeepingSettingsRepo {
  private final BinaryFileUtil<HousekeepingSettings> fileUtil;
  private HousekeepingSettings settings;

  public HousekeepingSettingsRepo() {
    this.fileUtil = new BinaryFileUtil<>("housekeeping_settings.dat");
    load();
  }

  private void load() {
    this.settings = fileUtil.retrieveFromFile();
    if (this.settings == null) {
      this.settings = new HousekeepingSettings();
      save();
    }
  }

  private void save() {
    fileUtil.saveToFile(settings);
  }

  public HousekeepingSettings getSettings() {
    return settings;
  }

  public void updateCleaningTimes(int standardMinutes, int suiteMinutes, int luxuryMinutes) {
    settings.setCleanTimeStandardMinutes(standardMinutes);
    settings.setCleanTimeSuiteMinutes(suiteMinutes);
    settings.setCleanTimeLuxuryMinutes(luxuryMinutes);
    save();
  }

  public void updateOverdueThreshold(int minutes) {
    settings.setOverdueThresholdMinutes(minutes);
    save();
  }

  public void updateQueueJumpTypes(
      boolean standardClean, boolean deepClean, boolean turnover, boolean maintenanceCheck) {
    settings.setQueueJumpStandardClean(standardClean);
    settings.setQueueJumpDeepClean(deepClean);
    settings.setQueueJumpTurnover(turnover);
    settings.setQueueJumpMaintenanceCheck(maintenanceCheck);
    save();
  }

  public void updateShiftSchedules(String morning, String afternoon, String night) {
    settings.setMorningShiftSchedule(morning);
    settings.setAfternoonShiftSchedule(afternoon);
    settings.setNightShiftSchedule(night);
    save();
  }

  public void updateMaxRooms(int morning, int afternoon, int night) {
    settings.setMaxRoomsMorning(morning);
    settings.setMaxRoomsAfternoon(afternoon);
    settings.setMaxRoomsNight(night);
    save();
  }
}
