package nl.oxod.oxlib.commands;

import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginDescriptionFile;
import org.jetbrains.annotations.NotNull;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import nl.oxod.oxlib.api.commands.SubCommand;
import nl.oxod.oxlib.api.commands.SubCommandResult;

public class OxLibCommand extends SubCommand {
  public OxLibCommand() {
    this.name = "about";
    this.info = "Shows more details about the plugin.";
  }

  @Override
  public Boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
      SubCommandResult result) {
    if (sender instanceof Player player) {
      PluginDescriptionFile pluginInfo = this.getPlugin().getDescription();

      TextComponent.Builder message = Component.text()
          .append(
              Component.text()
                  .append(Component.newline())
                  .append(Component.text("["))
                  .append(Component.text(this.getPlugin().getName(), null, TextDecoration.BOLD))
                  .append(Component.space())
                  .append(Component.text("- About]"))
                  .color(NamedTextColor.DARK_AQUA))
          .append(Component.newline());

      message
          .append(Component.text(" Plugin: "))
          .append(Component.text(pluginInfo.getName(), NamedTextColor.DARK_AQUA, TextDecoration.ITALIC))
          .append(Component.newline());

      if (pluginInfo.getVersion().length() > 0) {
        message
            .append(Component.text(" Version: "))
            .append(Component.text(pluginInfo.getVersion()))
            .append(Component.newline())
            .append(Component.newline());
      }

      String description = pluginInfo.getDescription();
      if (description != null && description.length() > 0) {
        message
            .append(Component.text(" Description: "))
            .append(Component.text(description, NamedTextColor.GRAY))
            .append(Component.newline())
            .append(Component.newline());
      }

      List<String> authors = pluginInfo.getAuthors();
      if (!authors.isEmpty()) {
        message.append(Component.text(authors.size() > 1 ? " Authors:" : " Author:"));
        boolean first = true;
        for (String author : authors) {
          if (first) {
            first = false;
          } else {
            message.append(Component.text(","));
          }
          message.append(Component.text(" " + author, NamedTextColor.DARK_AQUA));
        }
        message.append(Component.newline());
      }

      List<String> contributors = pluginInfo.getContributors();
      if (!contributors.isEmpty()) {
        message.append(Component.text(contributors.size() > 1 ? " Contributors:" : " Contributor:"));
        boolean first = true;
        for (String author : contributors) {
          if (first) {
            first = false;
          } else {
            message.append(Component.text(","));
          }
          message.append(Component.text(" " + author, NamedTextColor.DARK_AQUA));
        }
        message.append(Component.newline());
      }

      String website = pluginInfo.getWebsite();
      if (website != null && website.length() > 0) {
        if (!website.toLowerCase().matches("^https?://")) {
          website = "http://" + website;
        }
        message
            .append(Component.text(" Website: "))
            .append(
                Component.text(website, NamedTextColor.DARK_AQUA, TextDecoration.ITALIC)
                    .hoverEvent(HoverEvent.showText(
                        Component.text()
                            .append(Component.text("Click here to open the url "))
                            .append(Component.text(website, NamedTextColor.DARK_AQUA, TextDecoration.ITALIC))))
                    .clickEvent(ClickEvent.openUrl(website)))
            .append(Component.newline());
      }

      player.sendMessage(message);
    }

    return true;
  }
}
