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

  public static String textLimit(String text, int limit, int wordLimit) {
    if (text.length() <= limit) {
      return text;
    }

    int lastSpace = text.lastIndexOf(" ", limit - 3);
    if ((limit - 3) - (lastSpace + 1) <= wordLimit) {
      text = text.substring(0, lastSpace);
    } else {
      text = text.substring(0, limit - 3);
    }

    return text.replaceAll("\\s*$", "") + "...";
  }

  public static String textLimit(String text, int limit) {
    return textLimit(text, limit, 5);
  }

  protected SubCommandResult querySubCommand(@NotNull ArrayList<SubCommand> subCommands, @NotNull String[] arguments) {
    if (arguments.length > 0) {
      for (SubCommand subCommand : subCommands) {
        if (subCommand.name.equals(arguments[0]) || Arrays.asList(subCommand.aliases).contains(arguments[0])) {
          if (arguments.length > 1) {
            if (subCommand.acceptOverflows) {
              return new SubCommandResult(subCommand, arguments, 1);
            } else {
              SubCommandResult result = this.querySubCommand(subCommand.subcommands,
                  Arrays.copyOfRange(arguments, 1, arguments.length));
              if (result.subCommand != null) {
                return new SubCommandResult(result.subCommand, arguments, result.currentArgumentIndex + 1,
                    result.isValid, result.isUsage, result.currentUsageIndex);
              } else {
                boolean valid = true;
                int i;
                for (i = 0; i < subCommand.usage.length && i < arguments.length - 1; i++) {
                  List<String> usage = Arrays.asList(subCommand.usage[i]);
                  String argument = arguments[i + 1];

                  if (!containsUnofficialArgumentKeywords(usage)
                      && !((usage.contains(argument) && !isOfficialArgumentKeyword(argument))
                          || (usage.contains("%number%") && isStringInteger(argument))
                          || (usage.contains("%decimal%") && isStringDouble(argument))
                          || (usage.contains("%player%") && Bukkit.getPlayer(argument) != null))) {
                    valid = false;
                    break;
                  }
                }

                if (valid && arguments.length - 1 <= subCommand.usage.length) {
                  return new SubCommandResult(subCommand, arguments, i, true, true, i - 1);
                } else {
                  return new SubCommandResult(subCommand, arguments, i + 1, false, true, i - (valid ? 1 : 0));
                }
              }
            }
          } else {
            return new SubCommandResult(subCommand, arguments, 0);
          }
        }
      }
    }
    return new SubCommandResult(null, arguments, 0);
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
      if (subCommand.senderHasPermission(sender)) {
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
            for (String usage : result.subCommand.usage[0]) {
              if (usage.equals("%player%")) {
                for (Player player : Bukkit.getServer().getOnlinePlayers()) {
                  options.add(player.getName());
                }
              } else if (!isArgumentKeyword(usage)) {
                options.add(usage);
              }
            }
          }
        } else if (result.currentUsageIndex + 1 < result.subCommand.usage.length) {
          for (String usage : result.subCommand.usage[result.currentUsageIndex + 1]) {
            if (usage.equals("%player%")) {
              for (Player player : Bukkit.getServer().getOnlinePlayers()) {
                options.add(player.getName());
              }
            } else if (!isArgumentKeyword(usage)) {
              options.add(usage);
            }
          }
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
}
