package chip.uhc.util;

import org.bukkit.Bukkit;
//import org.bukkit.GameMode;
//import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.command.CommandException;
import org.bukkit.ChatColor;
import org.bukkit.scoreboard.Score;
import java.util.LinkedHashMap;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.*;
import dev.jorel.commandapi.arguments.ScoreHolderArgument.ScoreHolderType;
import dev.jorel.commandapi.wrappers.IntegerRange;
import dev.jorel.commandapi.wrappers.FunctionWrapper;
import dev.jorel.commandapi.CommandPermission;

public class App extends JavaPlugin {
    @Override
    public void onLoad() {
        // /dispatch, /cmd: runs a command, used to run any plugin command in /execute and functions
        LinkedHashMap<String, Argument> dispatchArgs = new LinkedHashMap<>();
        dispatchArgs.put("command", new GreedyStringArgument());

        new CommandAPICommand("dispatch")
            .withArguments(dispatchArgs)
            .withPermission(CommandPermission.OP)
            .withAliases("cmd")
            .executes((sender, args) -> {
                String cmd = (String) args[0];
                Bukkit.dispatchCommand(sender, cmd);
            })
            .register();

        // /regen <seed>: regens dims game & game_nether with provided seed
        LinkedHashMap<String, Argument> regenWorldArgs = new LinkedHashMap<>();
        regenWorldArgs.put("seed", new GreedyStringArgument());

        new CommandAPICommand("regen")
            .withArguments(regenWorldArgs)
            .withPermission(CommandPermission.OP)
            .executes((sender, args) -> {
                String seed = (String) args[0];
                if (seed.contains(" ")) seed = "\"" + seed + "\"";
                if (Bukkit.getWorld("game") != null && Bukkit.getWorld("game_nether") != null) {
                    sender.sendMessage("Reloading dimensions...");
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "mvregen game -s " + seed);
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "mvconfirm");
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "mvregen game_nether -s " + seed);
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "mvconfirm");
                    sender.sendMessage(ChatColor.GREEN + "Both dimensions have been regenerated successfully.");
                } else {
                    CommandAPI.fail("Dimensions game and game_nether must both exist in order to run this command.");
                }
            })
            .register();

        // /for <var> in m..n run <cmd>
        // /for <var> in m..n step s run <cmd>

        // examples:
        // /for i in 0..10 run say $i         # (0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
        // /for i in 0..10 step 2 run say $i  # (0, 2, 4, 6, 8, 10)
        LinkedHashMap<String, Argument> forArgs = new LinkedHashMap<>();
        forArgs.put("var", new StringArgument());
        forArgs.put("in", new LiteralArgument("in"));
        forArgs.put("range", new IntegerRangeArgument());
        forArgs.put("run", new LiteralArgument("run"));
        forArgs.put("cmd", new GreedyStringArgument());

        LinkedHashMap<String, Argument> forStepArgs = new LinkedHashMap<>();
        forStepArgs.put("var", new StringArgument());
        forStepArgs.put("in", new LiteralArgument("in"));
        forStepArgs.put("range", new IntegerRangeArgument());
        forStepArgs.put("steplit", new LiteralArgument("step"));
        forStepArgs.put("step", new IntegerArgument(1));
        forStepArgs.put("run", new LiteralArgument("run"));
        forStepArgs.put("cmd", new GreedyStringArgument());

        new CommandAPICommand("for")
            .withArguments(forArgs)
            .withPermission(CommandPermission.OP)
            .executes((sender, args) -> {
                String vname = (String) args[0];
                IntegerRange vrange = (IntegerRange) args[1];
                String cmd = (String) args[2];

                try {
                    for (int i = vrange.getLowerBound(); i <= vrange.getUpperBound(); i++) {
                        String itercmd = cmd;
                        itercmd = itercmd.replaceAll("(?<!\\\\)\\$" + vname, Integer.toString(i));
                        itercmd = itercmd.replaceAll("\\\\\\$", "\\$");
                        Bukkit.dispatchCommand(sender, itercmd);
                    }
                } catch (CommandException e) {
                    CommandAPI.fail("An error occurred while running the commands: " + e.getMessage());
                }
            })
            .register();

            new CommandAPICommand("for")
            .withArguments(forStepArgs)
            .withPermission(CommandPermission.OP)
            .executes((sender, args) -> {
                String vname = (String) args[0];
                IntegerRange vrange = (IntegerRange) args[1];
                int step = (int) args[2];
                String cmd = (String) args[3];

                try {
                    for (int i = vrange.getLowerBound(); i <= vrange.getUpperBound(); i += step) {
                        String itercmd = cmd;
                        itercmd = itercmd.replaceAll("(?<!\\\\)\\$" + vname, Integer.toString(i));
                        itercmd = itercmd.replaceAll("\\\\\\$", "\\$");
                        Bukkit.dispatchCommand(sender, itercmd);
                    }
                } catch (CommandException e) {
                    sender.sendMessage(ChatColor.RED + "An error occurred while running the commands: " + e.getMessage());
                }
            })
            .register();

            // /runfn <fn> with <player> <objective> as <value> || /runfn <fn> with <player> <objective> as <player> <objective>
            // this fn sets scoreboard entry to <value> / <player> <objective>, runs the function, and returns the value on said scoreboard entry.
            // this simplifies syntax for functions with args and allows them to be used with /for
            // uhhhh, if you want to use multiple arguments just curry I guess
            LinkedHashMap<String, Argument> runfnArgs = new LinkedHashMap<>();
            runfnArgs.put("function", new FunctionArgument());
            runfnArgs.put("with", new LiteralArgument("with"));
            runfnArgs.put("player", new ScoreHolderArgument(ScoreHolderType.SINGLE));
            runfnArgs.put("objective", new ObjectiveArgument());
            runfnArgs.put("as", new LiteralArgument("as"));
            runfnArgs.put("value", new IntegerArgument());

            LinkedHashMap<String, Argument> runfnEntryArgs = new LinkedHashMap<>();
            runfnEntryArgs.put("function", new FunctionArgument());
            runfnEntryArgs.put("with", new LiteralArgument("with"));
            runfnEntryArgs.put("player", new ScoreHolderArgument(ScoreHolderType.SINGLE));
            runfnEntryArgs.put("objective", new ObjectiveArgument());
            runfnEntryArgs.put("as", new LiteralArgument("as"));
            runfnEntryArgs.put("player value", new ScoreHolderArgument(ScoreHolderType.SINGLE));
            runfnEntryArgs.put("objective value", new ObjectiveArgument());

            new CommandAPICommand("runfunction")
            .withArguments(runfnEntryArgs)
            .withPermission(CommandPermission.OP)
            .withAliases("runfn")
            .executes((sender, args) -> {
                FunctionWrapper[] fns = (FunctionWrapper[]) args[0];
                String inputPl = (String) args[1];
                String inputOb = (String) args[2];
                String valuePl = (String) args[3];
                String valueOb = (String) args[4];
                
                // get (pl obj) slot and set it to val, effectively acting as the arg
                Score inputScore = Bukkit.getScoreboardManager().getMainScoreboard().getObjective(inputOb).getScore(inputPl);
                int inputVal = Bukkit.getScoreboardManager().getMainScoreboard().getObjective(valueOb).getScore(valuePl).getScore();
                
                inputScore.setScore(inputVal);
                // run fns after arg has been set
                for (FunctionWrapper fn : fns) {
                    fn.run();
                }
                // return score for use w/ execute store
                try {
                    int retVal = inputScore.getScore();
                    sender.sendMessage("Function returned " + Integer.toString(retVal));
                    return retVal; 
                } catch (IllegalArgumentException | IllegalStateException e) {
                    CommandAPI.fail("Entry no longer exists");
                }
                return 0;
            })
            .register();
        
    }
}
