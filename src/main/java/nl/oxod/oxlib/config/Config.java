package nl.oxod.oxlib.config;

import java.io.File;

import lombok.Getter;
import nl.oxod.oxlib.OxLib;
import nl.oxod.oxlib.api.config.OxConfig;

@Getter
public class Config extends OxConfig {
  private Boolean debug = false;

  public Config() {
    super(new File(OxLib.getInstance().getDataFolder(), "config.yml"));
  }
}
