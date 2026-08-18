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

    // A file written before a field existed comes back with that field null or zero, so the record
    // is repaired the moment it is read rather than reaching a screen half filled in, and the
    // repair is written back so the migration only ever runs once.
    this.settings.normalize();
    save();
  }

  private void save() {
    fileUtil.saveToFile(settings);
  }

  public BookingSettings getSettings() {
    return settings;
  }

  // The screens edit the live object in place, so this exists to force the write once a whole
  // group of fields has been changed rather than saving after every keystroke.
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
