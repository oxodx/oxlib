package nl.oxod.oxlib.api.commands;

import java.util.List;
import java.util.Map;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CmdContext {
  private final CommandSender sender;
  private final String label;
  private final Map<String, Object> named;
  private final List<Object> ordered;

  CmdContext(CommandSender sender, String label, Map<String, Object> named, List<Object> ordered) {
    this.sender = sender;
    this.label = label;
    this.named = named;
    this.ordered = ordered;
  }

  @SuppressWarnings("unchecked")
  public <T> T arg(String name) {
    return (T) named.get(name);
  }

  @SuppressWarnings("unchecked")
  public <T> T arg(int index) {
    return (T) ordered.get(index);
  }

  public boolean has(String name) {
    return named.containsKey(name);
  }

  public int size() {
    return ordered.size();
  }

  public CommandSender sender() {
    return sender;
  }

  public Player player() {
    if (sender instanceof Player player) {
      return player;
    }
    throw new IllegalStateException("Sender is not a player");
  }

  public String label() {
    return label;
  }
}
