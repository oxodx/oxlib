package nl.oxod.oxlib.api.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import nl.oxod.oxlib.OxLib;

public class CommandManager implements TabExecutor {
  protected final OxLib plugin;
  private final List<SubCommand> roots = new ArrayList<>();
  private SubCommand defaultCommand;
  public final PluginCommand command;

  private record Resolve(SubCommand command, CmdContext context, @Nullable String error) {
  }

  private record Match(SubCommand command, int depth) {
  }

  @edu.umd.cs.findbugs.annotations.SuppressFBWarnings("EI_EXPOSE_REP2")
  public CommandManager(OxLib plugin, String commandName) {
    this.plugin = plugin;
    this.command = plugin.getServer().getPluginCommand(commandName);
    if (this.command != null) {
      this.command.setExecutor(this);
    }
  }

  public @NotNull CommandManager addSubCommand(@NotNull SubCommand subCommand) {
    subCommand.manager = this;
    roots.add(subCommand);
    return this;
  }

  public @NotNull CommandManager setMainSubCommand(@NotNull SubCommand subCommand) {
    subCommand.manager = this;
    defaultCommand = subCommand;
    return this;
  }

  public @Nullable SubCommand getSubCommand(String name) {
    return findSubCommand(roots, name);
  }

  @Override
  public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label,
      @NotNull String[] args) {
    if (args.length == 0 && defaultCommand != null) {
      defaultCommand.execute(new CmdContext(sender, label, Map.of(), List.of()));
      return true;
    }

    if (args.length == 0) {
      return true;
    }

    Resolve resolved = resolve(roots, sender, args, 0);
    if (resolved == null) {
      sendError(sender, label, "Unknown command", args);
      return true;
    }

    if (resolved.error() != null) {
      sendError(sender, label, resolved.error(), args);
      return true;
    }

    resolved.command().execute(resolved.context());
    return true;
  }

  @Override
  public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd,
      @NotNull String alias, @NotNull String[] args) {
    if (args.length == 1) {
      return filterNames(roots, sender, args[0]);
    }

    String[] path = Arrays.copyOf(args, args.length - 1);
    Match match = findMatch(roots, sender, path, 0);

    if (match == null) {
      return args.length == 2 ? filterNames(roots, sender, args[args.length - 1]) : List.of();
    }

    int argIndex = path.length - match.depth();
    SubCommand target = match.command();

    if (argIndex < 0) {
      return filterNames(target.children, sender, args[args.length - 1]);
    }

    List<String> suggestions = new ArrayList<>();

    if (argIndex < target.args().size()) {
      suggestions.addAll(target.args().get(argIndex).complete());
    }

    List<String> custom = target.onTabComplete(sender, args, argIndex);
    if (custom != null) {
      suggestions.addAll(custom);
    }

    String partial = args[args.length - 1].toLowerCase();
    suggestions.removeIf(s -> !s.toLowerCase().contains(partial));
    return suggestions;
  }

  @Nullable
  private Match findMatch(List<SubCommand> candidates, CommandSender sender, String[] args, int offset) {
    if (offset >= args.length) {
      return null;
    }
    SubCommand matched = findSubCommand(candidates, args[offset]);
    if (matched == null || !matched.onPermission(sender)) {
      return null;
    }

    int next = offset + 1;
    if (next < args.length && !matched.children.isEmpty()) {
      Match deeper = findMatch(matched.children, sender, args, next);
      if (deeper != null) {
        return deeper;
      }
    }

    return new Match(matched, next);
  }

  @Nullable
  private Resolve resolve(List<SubCommand> candidates, CommandSender sender, String[] args, int offset) {
    SubCommand matched = findSubCommand(candidates, args[offset]);
    if (matched == null) {
      return null;
    }

    if (!matched.onPermission(sender)) {
      return null;
    }

    int next = offset + 1;
    if (next < args.length && !matched.children.isEmpty()) {
      Resolve nested = resolve(matched.children, sender, args, next);
      if (nested != null) {
        return nested;
      }
    }

    int argCount = args.length - next;
    List<CmdArg<?>> cmdArgs = matched.args();

    if (argCount > cmdArgs.size()) {
      return new Resolve(matched, null, "Too many arguments");
    }

    Map<String, Object> named = new LinkedHashMap<>();
    List<Object> ordered = new ArrayList<>();

    for (int i = 0; i < argCount; i++) {
      CmdArg<?> argDef = cmdArgs.get(i);
      Object value = argDef.parse(args[next + i]);
      if (value == null) {
        return new Resolve(matched, null, "Invalid " + describe(argDef) + ": " + args[next + i]);
      }
      named.put(argDef.name(), value);
      ordered.add(value);
    }

    return new Resolve(matched, new CmdContext(sender, command.getLabel(), named, ordered), null);
  }

  private static String describe(CmdArg<?> arg) {
    return switch (arg) {
      case CmdArg.Player p -> "player '" + p.name() + "'";
      case CmdArg.Int i -> "number '" + i.name() + "'";
      case CmdArg.Decimal d -> "decimal '" + d.name() + "'";
      case CmdArg.Text t -> "text '" + t.name() + "'";
      case CmdArg.Literal l -> "option '" + l.name() + "'";
    };
  }

  @Nullable
  private SubCommand findSubCommand(List<SubCommand> candidates, String name) {
    for (SubCommand sub : candidates) {
      if (sub.name().equals(name) || sub.aliases().contains(name)) {
        return sub;
      }
    }
    return null;
  }

  private List<String> filterNames(List<SubCommand> candidates, CommandSender sender, String partial) {
    List<String> result = new ArrayList<>();
    for (SubCommand sub : candidates) {
      if (sub.onPermission(sender) && !sub.name().isEmpty()
          && sub.name().toLowerCase().startsWith(partial.toLowerCase())) {
        result.add(sub.name());
      }
    }
    return result;
  }

  private void sendError(CommandSender sender, String label, String message, @Nullable String[] args) {
    if (!(sender instanceof Player player)) {
      sender.sendMessage("[" + plugin.getName() + "] " + message);
      return;
    }

    Component msg = Component.text()
        .append(Component.text("[" + plugin.getName() + "]", NamedTextColor.RED, TextDecoration.BOLD)
            .clickEvent(ClickEvent.runCommand("/" + label + " help")))
        .append(Component.text(" " + message, NamedTextColor.RED))
        .build();

    if (args != null && args.length > 0) {
      String full = "/" + label + " " + String.join(" ", args);
      msg = msg.append(Component.newline())
          .append(Component.text(full, NamedTextColor.GRAY)
              .clickEvent(ClickEvent.suggestCommand(full)));
    }

    player.sendMessage(msg);
  }
}
