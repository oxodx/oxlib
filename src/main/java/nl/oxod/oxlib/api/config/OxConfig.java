package nl.oxod.oxlib.api.config;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import lombok.Getter;
import nl.oxod.oxlib.OxLib;
import nl.oxod.oxlib.api.config.decorators.IgnoreField;

@edu.umd.cs.findbugs.annotations.SuppressFBWarnings("EI_EXPOSE_REP")
public abstract class OxConfig {
  @IgnoreField
  private final File file;

  @IgnoreField
  private final YamlConfiguration rawConfiguration;

  @Getter
  private int configVersion = 0;

  public OxConfig(File file, Map<String, Object> migrationParams) {
    this.file = file;
    this.rawConfiguration = new YamlConfiguration();

    try {
      this.rawConfiguration.load(file);
      applyMigrations(migrationParams);
    } catch (FileNotFoundException e) {
      OxLib.logger().severe("Config file not found: " + file.getName());
    } catch (IOException e) {
      e.printStackTrace();
    } catch (InvalidConfigurationException e) {
      OxLib.logger().severe("Invalid configuration in file: " + file.getName());
      e.printStackTrace();
    }
  }

  @edu.umd.cs.findbugs.annotations.SuppressFBWarnings("CT_CONSTRUCTOR_THROW")
  public OxConfig(File file) {
    this(file, null);
  }

  private void applyMigrations(Map<String, Object> migrationParams) {
    int from = rawConfiguration.getInt("config-version", 0);
    var steps = migrationParams == null ? getMigrationSteps() : getMigrationSteps(migrationParams);
    if (steps.size() <= from) {
      return;
    }

    for (var migration : steps.subList(from, steps.size())) {
      migration.accept(rawConfiguration);
    }
    try {
      rawConfiguration.save(file);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  protected List<Consumer<YamlConfiguration>> getMigrationSteps() {
    return List.of();
  }

  protected List<Consumer<YamlConfiguration>> getMigrationSteps(Map<String, Object> params) {
    return List.of();
  }

  public void saveChanges() {
    ConfigManager.save(this, rawConfiguration, file);
  }

  public CompletableFuture<Void> saveChangesAsync() {
    return CompletableFuture.runAsync(() -> {
      ConfigManager.save(this, rawConfiguration, file);
    });
  }

  public YamlConfiguration getRawConfig() {
    return rawConfiguration;
  }

  public void load() {
    ConfigManager.load(this, rawConfiguration);
  }
}
