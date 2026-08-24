package repo;

import entity.BookingSettings;
import util.BinaryFileUtil;

public class BookingSettingsRepo {
  private final BinaryFileUtil<BookingSettings> fileUtil;
  private BookingSettings settings;

  public BookingSettingsRepo() {
    this.fileUtil = new BinaryFileUtil<>("booking_settings.dat");
    load();
  }

  private void load() {
    this.settings = fileUtil.retrieveFromFile();
    if (this.settings == null) {
      this.settings = new BookingSettings();
      save();
      return;
    }

    // An older file returns new fields null or zero, so it is repaired on read and saved once.
    this.settings.normalize();
    save();
  }

  private void save() {
    fileUtil.saveToFile(settings);
  }

  public BookingSettings getSettings() {
    return settings;
  }

  // The screens edit the live object, so the write is forced once a group has changed.
  public boolean updateSettings(BookingSettings updatedSettings) {
    if (updatedSettings == null) return false;
    this.settings = updatedSettings;
    save();
    return true;
  }

  public void resetToDefaults() {
    settings.resetToDefaults();
    save();
  }
}
