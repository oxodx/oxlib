package nl.oxod.oxlib.api.commands;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class SubCommand {
  CommandManager manager;
  final List<SubCommand> children = new ArrayList<>();

  private String cmdName = "";
  private String description = "";
  private List<CmdArg<?>> cmdArgs = List.of();
  private List<String> aliasList = List.of();

  protected SubCommand() {}

  protected SubCommand(String name) {

    this.cmdName = name;
  }

  protected SubCommand(String name, String description, List<CmdArg<?>> args, String... aliases) {
    this.cmdName = name;
    this.description = description;
    this.cmdArgs = args;
    this.aliasList = List.of(aliases);
  }

  public Plugin getPlugin() {
    return manager != null ? manager.plugin : null;
  }

  public @NotNull SubCommand addChild(@NotNull SubCommand child) {
    child.manager = this.manager;
    children.add(child);
    return this;
  }

  public boolean onPermission(CommandSender sender) {
    return true;
  }

  public void execute(CmdContext ctx) {
  }

  public @Nullable List<String> onTabComplete(CommandSender sender, String[] args, int argIndex) {
    return null;
  }

  ArrayList<SubCommand> getPermittedChildren(CommandSender sender) {
    ArrayList<SubCommand> result = new ArrayList<>();
    for (SubCommand child : children) {
      if (child.onPermission(sender)) {
        result.add(child);
      }
    }
    return result;
  }

  String name() {
    return cmdName;
  }

  String description() {
    return description;
  }

  List<CmdArg<?>> args() {
    return cmdArgs;
  }

  List<String> aliases() {
    return aliasList;
  }
}
