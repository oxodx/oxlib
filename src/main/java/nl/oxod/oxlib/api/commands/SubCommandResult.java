package nl.oxod.oxlib.api.commands;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SubCommandResult {
  public SubCommand subCommand;
  public String[] arguments;
  public Integer currentArgumentIndex;
  public Integer currentUsageIndex;
  public Boolean isValid = true;
  public Boolean isUsage = false;

  public SubCommandResult(@Nullable SubCommand subCommand, @NotNull String[] arguments,
      @Nullable Integer currentArgumentIndex) {
    this.subCommand = subCommand;
    this.arguments = arguments;
    this.currentArgumentIndex = currentArgumentIndex;
  }

  public SubCommandResult(@Nullable SubCommand subcommand, @NotNull String[] arguments,
      @Nullable Integer currentArgumentIndex, @NotNull Boolean isValid, @NotNull Boolean isUsage,
      @Nullable Integer currentUsageIndex) {
    this(subcommand, arguments, currentArgumentIndex);
    this.isValid = isValid;
    this.isUsage = isUsage;
    this.currentUsageIndex = currentUsageIndex;
  }

  public @Nullable String getCurrentArgument() {
    try {
      return arguments[currentArgumentIndex];
    } catch (Exception e) {
      return null;
    }
  }

  public Boolean isValid() {
    return isValid && subCommand != null && this.getCurrentArgument() != null
        && !(isUsage && subCommand.usage.length <= 0);
  }
}
