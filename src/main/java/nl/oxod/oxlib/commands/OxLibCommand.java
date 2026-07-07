package nl.oxod.oxlib.commands;

import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginDescriptionFile;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import nl.oxod.oxlib.api.commands.CmdContext;
import nl.oxod.oxlib.api.commands.SubCommand;

public class OxLibCommand extends SubCommand {
  public OxLibCommand() {
    super("about", "Shows more details about the plugin.", List.of());
  }

  @Override
  public void execute(CmdContext ctx) {
    if (!(ctx.sender() instanceof Player player)) {
      return;
    }

    PluginDescriptionFile info = getPlugin().getDescription();

    TextComponent.Builder message = Component.text()
        .append(Component.text()
            .append(Component.newline())
            .append(Component.text("["))
            .append(Component.text(getPlugin().getName(), null, TextDecoration.BOLD))
            .append(Component.space())
            .append(Component.text("- About]"))
            .color(NamedTextColor.DARK_AQUA))
        .append(Component.newline());

    message
        .append(Component.text(" Plugin: "))
        .append(Component.text(info.getName(), NamedTextColor.DARK_AQUA, TextDecoration.ITALIC))
        .append(Component.newline());

    if (!info.getVersion().isEmpty()) {
      message
          .append(Component.text(" Version: "))
          .append(Component.text(info.getVersion()))
          .append(Component.newline())
          .append(Component.newline());
    }

    String description = info.getDescription();
    if (description != null && !description.isEmpty()) {
      message
          .append(Component.text(" Description: "))
          .append(Component.text(description, NamedTextColor.GRAY))
          .append(Component.newline())
          .append(Component.newline());
    }

    List<String> authors = info.getAuthors();
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

    List<String> contributors = info.getContributors();
    if (!contributors.isEmpty()) {
      message.append(Component.text(contributors.size() > 1 ? " Contributors:" : " Contributor:"));
      boolean first = true;
      for (String c : contributors) {
        if (first) {
          first = false;
        } else {
          message.append(Component.text(","));
        }
        message.append(Component.text(" " + c, NamedTextColor.DARK_AQUA));
      }
      message.append(Component.newline());
    }

    String website = info.getWebsite();
    if (website != null && !website.isEmpty()) {
      if (!website.toLowerCase().matches("^https?://")) {
        website = "http://" + website;
      }
      message
          .append(Component.text(" Website: "))
          .append(Component.text(website, NamedTextColor.DARK_AQUA, TextDecoration.ITALIC)
              .hoverEvent(HoverEvent.showText(
                  Component.text()
                      .append(Component.text("Click here to open the url "))
                      .append(Component.text(website, NamedTextColor.DARK_AQUA, TextDecoration.ITALIC))))
              .clickEvent(ClickEvent.openUrl(website)))
          .append(Component.newline());
    }

    player.sendMessage(message);
  }
}
