package nl.oxod.oxlib.api.commands;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@edu.umd.cs.findbugs.annotations.SuppressFBWarnings("URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
public abstract class SubCommand {

  CommandManager manager;
  public final ArrayList<SubCommand> subcommands = new ArrayList<>();

  public String name = "";
  public String info = "";
  public String[] aliases = new String[0];
  public String[][] usage = new String[0][];
  public Boolean acceptOverflows = false;

  public Plugin getPlugin() {
    if (this.manager != null) {
      return this.manager.plugin;
    } else {
      return null;
    }
  }

  public @NotNull SubCommand addSubCommand(@NotNull SubCommand subcommand) {
    subcommand.manager = this.manager;
    subcommands.add(subcommand);
    return this;
  }

  public ArrayList<SubCommand> getPermittedSubCommands(@NotNull CommandSender sender) {
    ArrayList<SubCommand> permittedList = new ArrayList<>();
    for (SubCommand subcommand : subcommands) {
      if (subcommand.onPermission(sender)) {
        permittedList.add(subcommand);
      }
    }
    return permittedList;
  }

  public Boolean onPermission(CommandSender sender) {
    return true;
  }

  public Boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
      SubCommandResult result) {
    return false;
  }

  public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
      @NotNull String alias, SubCommandResult result) {
    return null;
  }
}
