package repo;

import entity.VipSystemConfig;
import util.BinaryFileUtil;

public class VipSystemConfigRepo {
  private final BinaryFileUtil<VipSystemConfig> fileUtil;
  private VipSystemConfig config;

  public VipSystemConfigRepo() {
    this.fileUtil = new BinaryFileUtil<>("vip_system_config.dat");
    load();
  }

  private void load() {
    this.config = fileUtil.retrieveFromFile();
    if (this.config == null) {
      this.config = new VipSystemConfig();
      save();
    }
  }

  private void save() {
    fileUtil.saveToFile(config);
  }

  public boolean updateConfig(VipSystemConfig updatedConfig) {
    if (updatedConfig == null) return false;
    this.config = updatedConfig;
    save();
    return true;
  }

  public VipSystemConfig getConfig() {
    return config;
  }
}
