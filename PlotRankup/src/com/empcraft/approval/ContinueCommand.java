package com.empcraft.approval;

import com.intellectualcrafters.plot.commands.SubCommand;
import com.intellectualcrafters.plot.config.C;
import com.intellectualcrafters.plot.flag.Flag;
import com.intellectualcrafters.plot.flag.FlagManager;
import com.intellectualcrafters.plot.object.Plot;
import com.intellectualcrafters.plot.object.PlotPlayer;
import com.intellectualcrafters.plot.util.MainUtil;

public class ContinueCommand extends SubCommand {

    public ContinueCommand() {
        super("continue", "plots.continue", "Continue editing your plot", "continue", "undone", CommandCategory.ACTIONS, true);
    }

    @Override
    public boolean execute(final PlotPlayer player, final String... args) {
        final Plot plot = MainUtil.getPlot(player.getLocation());
        if (plot == null) {
            sendMessage(player, C.NOT_IN_PLOT);
            return false;
        }
        if (!plot.isAdded(player.getUUID())) {
            sendMessage(player, C.NO_PLOT_PERMS);
            return false;
        }

        final Flag flag = FlagManager.getPlotFlag(plot, "done");
        if (flag == null) {
            MainUtil.sendMessage(player, "&7This plot is already in &cbuild&7 mode.");
            return false;
        }

        if (flag.getValue().equals("true")) {
            MainUtil.sendMessage(player, "&7This plot has been &a approved &7 and &c locked &7 by an admin.");
            return false;
        }
        FlagManager.removePlotFlag(plot, "done");
        MainUtil.sendMessage(player, "&7You may now &acontinue &7building.");

        return true;
    }
}
