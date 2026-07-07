package nl.oxod.oxlib.api.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bukkit.Bukkit;
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

  private SubCommand mainSubCommand;

  public SubCommand getMainSubCommand() {

    return mainSubCommand;
  }

  public final ArrayList<SubCommand> subCommands = new ArrayList<>();
  public final PluginCommand command;

  private static final String[] argumentKeywords = new String[] { "%number%", "%decimal%", "%player%" };

  @edu.umd.cs.findbugs.annotations.SuppressFBWarnings("EI_EXPOSE_REP2")
  public CommandManager(OxLib plugin, String command) {
    this.plugin = plugin;

    this.command = this.plugin.getServer().getPluginCommand(command);
    if (this.command != null) {
      this.command.setExecutor(this);
    }
  }

  public @NotNull CommandManager addSubCommand(@NotNull SubCommand subCommand) {
    subCommand.manager = this;
    subCommands.add(subCommand);
    return this;
  }

  public @NotNull CommandManager setMainSubCommand(@NotNull SubCommand subCommand) {
    subCommand.manager = this;
    mainSubCommand = subCommand;
    return this;
  }

  public @Nullable SubCommand getSubCommand(String name) {
    for (SubCommand subCommand : subCommands) {
      if (subCommand.name.equals(name) || Arrays.asList(subCommand.aliases).contains(name)) {
        return subCommand;
      }
    }
    return null;
  }

  public static boolean isStringDouble(String text) {
    try {
      Double.parseDouble(text);
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  public static boolean isStringInteger(String text) {
    try {
      Integer.parseInt(text);
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  public static boolean isArgumentKeyword(String keyword) {
    return keyword.matches("^%.*%$");
  }

  public static boolean isOfficialArgumentKeyword(String keyword) {
    return Arrays.asList(argumentKeywords).contains(keyword);
  }

  public static boolean isUnofficialArgumentKeyword(String keyword) {
    return isArgumentKeyword(keyword) && !isOfficialArgumentKeyword(keyword);
  }

  public static boolean containsUnofficialArgumentKeywords(List<String> array) {
    for (String text : array) {
      if (isUnofficialArgumentKeyword(text)) {
        return true;
      }
    }
    return false;
  }

  protected SubCommandResult querySubCommand(@NotNull ArrayList<SubCommand> subCommands, @NotNull String[] arguments) {
    if (arguments.length == 0) {
      return new SubCommandResult(null, arguments, 0);
    }

    SubCommand matched = findSubCommand(subCommands, arguments[0]);
    if (matched == null) {
      return new SubCommandResult(null, arguments, 0);
    }

    if (arguments.length == 1) {
      return new SubCommandResult(matched, arguments, 0);
    }

    if (matched.acceptOverflows) {
      return new SubCommandResult(matched, arguments, 1);
    }

    SubCommandResult nested = this.querySubCommand(matched.subcommands,
        Arrays.copyOfRange(arguments, 1, arguments.length));
    if (nested.subCommand != null) {
      return new SubCommandResult(nested.subCommand, arguments, nested.currentArgumentIndex + 1,
          nested.isValid, nested.isUsage, nested.currentUsageIndex);
    }

    return checkUsageAgainstArguments(matched, arguments);
  }

  @Nullable
  private SubCommand findSubCommand(@NotNull ArrayList<SubCommand> subCommands, @NotNull String name) {
    for (SubCommand subCommand : subCommands) {
      if (subCommand.name.equals(name) || Arrays.asList(subCommand.aliases).contains(name)) {
        return subCommand;
      }
    }
    return null;
  }

  private SubCommandResult checkUsageAgainstArguments(@NotNull SubCommand subCommand, @NotNull String[] arguments) {
    int argLimit = Math.min(subCommand.usage.length, arguments.length - 1);
    for (int i = 0; i < argLimit; i++) {
      if (!matchesUsageOption(Arrays.asList(subCommand.usage[i]), arguments[i + 1])) {
        return new SubCommandResult(subCommand, arguments, i + 1, false, true, i);
      }
    }

    boolean allConsumed = arguments.length - 1 <= subCommand.usage.length;
    return new SubCommandResult(subCommand, arguments, allConsumed ? argLimit : argLimit + 1,
        allConsumed, true, argLimit - 1);
  }

  private boolean matchesUsageOption(List<String> options, String arg) {
    if (containsUnofficialArgumentKeywords(options)) {
      return true;
    }
    return (options.contains(arg) && !isOfficialArgumentKeyword(arg))
        || (options.contains("%number%") && isStringInteger(arg))
        || (options.contains("%decimal%") && isStringDouble(arg))
        || (options.contains("%player%") && Bukkit.getPlayer(arg) != null);
  }

  protected void sendErrorMessage(@NotNull CommandSender sender, String[] arguments, Integer currentArgumentIndex) {
    String rightCommand = String.join(" ", Arrays.copyOfRange(arguments, 0, currentArgumentIndex));
    String wrongCommand = String.join(" ", Arrays.copyOfRange(arguments, currentArgumentIndex, arguments.length));
    if (sender instanceof Player) {
      Player player = (Player) sender;
      player.sendMessage(
          Component.text()
              .append(Component.text("[" + this.plugin.getName() + "]", NamedTextColor.RED, TextDecoration.BOLD)
                  .clickEvent(ClickEvent.runCommand("/" + this.command.getName() + " help")))
              .append(Component.text(" Incorrect argument for command:", NamedTextColor.RED, TextDecoration.BOLD))
              .append(Component.newline())
              .append(Component.text()
                  .append(Component.text(
                      "/" + this.plugin.getName() + " " + rightCommand + (currentArgumentIndex > 0 ? " " : ""),
                      NamedTextColor.GRAY))
                  .append(Component.text(wrongCommand, NamedTextColor.RED, TextDecoration.UNDERLINED))
                  .clickEvent(ClickEvent.suggestCommand("/" + this.plugin.getName() + " " + rightCommand
                      + (currentArgumentIndex > 0 ? " " : "") + wrongCommand)))
              .build());
    }
  }

  public ArrayList<SubCommand> getPermittedSubCommands(CommandSender sender) {
    ArrayList<SubCommand> permittedList = new ArrayList<>();
    for (SubCommand subCommand : subCommands) {
      if (subCommand.onPermission(sender)) {
        permittedList.add(subCommand);
      }
    }
    return permittedList;
  }

  @Override
  public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
      @NotNull String[] arguments) {
    if (arguments.length > 0) {
      SubCommandResult result = this.querySubCommand(this.getPermittedSubCommands(sender), arguments);

      if (result.isValid()) {
        return result.subCommand.onCommand(sender, command, label, result);
      } else {
        this.sendErrorMessage(sender, arguments, result.currentArgumentIndex);
      }
    } else if (this.mainSubCommand != null) {
      return this.mainSubCommand.onCommand(sender, command, label,
          new SubCommandResult(this.mainSubCommand, arguments, null));
    }

    return true;
  }

  @Override
  public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
      @NotNull String alias, @NotNull String[] arguments) {
    List<String> options = new ArrayList<>();

    if (arguments.length > 1) {
      SubCommandResult result = this.querySubCommand(this.getPermittedSubCommands(sender),
          Arrays.copyOfRange(arguments, 0, arguments.length - 1));

      if (result.isValid()
          && !(result.isUsage && !(result.currentUsageIndex + 1 <= result.subCommand.usage.length - 1))) {
        if (!result.isUsage) {
          for (SubCommand subCommand : result.subCommand.getPermittedSubCommands(sender)) {
            if (subCommand.name.length() > 0) {
              options.add(subCommand.name);
            }
          }
          if (result.subCommand.usage.length > 0) {
            addUsageOptions(options, result.subCommand.usage[0]);
          }
        } else if (result.currentUsageIndex + 1 < result.subCommand.usage.length) {
          addUsageOptions(options, result.subCommand.usage[result.currentUsageIndex + 1]);
        }

        List<String> tabResult = result.subCommand.onTabComplete(sender, command, alias, result);
        if (tabResult != null) {
          options.addAll(tabResult);
        }
      }
    } else {
      for (SubCommand subCommand : this.getPermittedSubCommands(sender)) {
        if (subCommand.name.length() > 0) {
          options.add(subCommand.name);
        }
      }
    }

    for (int i = options.size() - 1; i >= 0; i--) {
      if (!options.get(i).toLowerCase().contains(arguments[arguments.length - 1].toLowerCase())) {
        options.remove(i);
      }
    }

    return options;
  }

  private void addUsageOptions(List<String> options, String[] usage) {
    for (String u : usage) {
      if (u.equals("%player%")) {
        for (Player player : Bukkit.getServer().getOnlinePlayers()) {
          options.add(player.getName());
        }
      } else if (!isArgumentKeyword(u)) {
        options.add(u);
      }
    }
  }
}
