package nl.oxod.oxlib;

import java.util.ArrayList;
import java.util.logging.Logger;

import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import lombok.Getter;
import net.kyori.adventure.text.minimessage.MiniMessage;
import nl.oxod.oxlib.api.commands.CommandManager;
import nl.oxod.oxlib.config.Config;

@edu.umd.cs.findbugs.annotations.SuppressFBWarnings({ "UWF_UNWRITTEN_FIELD", "ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD", "MS_EXPOSE_REP" })
public class OxLib extends JavaPlugin implements Listener {
  @Getter
  private CommandManager commandManager;

  @Getter
  private static final MiniMessage miniMessage = MiniMessage.miniMessage();
  @Getter
  private static Config libConfig;
  @Getter
  private static OxLib instance;

  private static final Logger logger = Logger.getLogger("oxlib");

  public static Logger logger() {
    return logger;
  }

  @Override
  public void onEnable() {
    saveDefaultConfig();
    instance = this;
    libConfig = new Config();
    libConfig.load();
  }
}
