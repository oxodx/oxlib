package nl.oxod.oxlib.api.commands;

import java.util.List;

import org.bukkit.Bukkit;
import org.jetbrains.annotations.Nullable;

public sealed interface CmdArg<T> {
  String name();

  Class<T> type();

  @Nullable T parse(String raw);

  default List<String> complete() {
    return List.of();
  }

  record Player(String name) implements CmdArg<org.bukkit.entity.Player> {
    @Override
    public Class<org.bukkit.entity.Player> type() {
      return org.bukkit.entity.Player.class;
    }

    @Override
    public @Nullable org.bukkit.entity.Player parse(String raw) {
      return Bukkit.getPlayer(raw);
    }

    @Override
    public List<String> complete() {
      return Bukkit.getOnlinePlayers().stream().map(org.bukkit.entity.Player::getName).toList();
    }
  }

  record Text(String name) implements CmdArg<String> {
    @Override
    public Class<String> type() {
      return String.class;
    }

    @Override
    public String parse(String raw) {
      return raw;
    }
  }

  record Int(String name) implements CmdArg<java.lang.Integer> {
    @Override
    public Class<java.lang.Integer> type() {
      return java.lang.Integer.class;
    }

    @Override
    public @Nullable java.lang.Integer parse(String raw) {
      try {
        return java.lang.Integer.parseInt(raw);
      } catch (NumberFormatException e) {
        return null;
      }
    }
  }

  record Decimal(String name) implements CmdArg<Double> {
    @Override
    public Class<Double> type() {
      return Double.class;
    }

    @Override
    public @Nullable Double parse(String raw) {
      try {
        return Double.parseDouble(raw);
      } catch (NumberFormatException e) {
        return null;
      }
    }
  }

  @edu.umd.cs.findbugs.annotations.SuppressFBWarnings("EI_EXPOSE_REP")
  record Literal(String name, List<String> options) implements CmdArg<String> {
    public Literal(String name, String... options) {
      this(name, List.of(options));
    }

    @Override
    public Class<String> type() {
      return String.class;
    }

    @Override
    public @Nullable String parse(String raw) {
      return options.contains(raw) ? raw : null;
    }

    @Override
    public List<String> complete() {
      return options;
    }
  }
}
