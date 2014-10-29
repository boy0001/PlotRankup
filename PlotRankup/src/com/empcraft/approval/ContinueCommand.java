package com.empcraft.approval;

import java.util.Set;

import com.intellectualcrafters.plot.C;
import com.intellectualcrafters.plot.Flag;
import com.intellectualcrafters.plot.FlagManager;
import com.intellectualcrafters.plot.PlayerFunctions;
import com.intellectualcrafters.plot.Plot;
import com.intellectualcrafters.plot.commands.SubCommand;
import com.intellectualcrafters.plot.database.DBFunc;

import org.bukkit.entity.Player;

public class ContinueCommand extends SubCommand {

    public ContinueCommand() {
        super("continue", "plots.continue", "Continue editing your plot", "continue", "undone", CommandCategory.ACTIONS, true);
    }

    @Override
    public boolean execute(Player player, String... args) {
        if(!PlayerFunctions.isInPlot(player)) {
            sendMessage(player, C.NOT_IN_PLOT);
            return false;
        }
        Plot plot = PlayerFunctions.getCurrentPlot(player);
        if(!plot.hasRights(player)) {
            sendMessage(player, C.NO_PLOT_PERMS);
            return false;
        }
        
        Flag flag = plot.settings.getFlag("done");
        if (flag==null) {
            Main.sendMessage(player, "&7This plot is already in &cbuild&7 mode.");
            return false;
        }
        
        if (flag.getValue().equals("true")) {
            Main.sendMessage(player, "&7This plot has been &a approved &7 and &c locked &7 by an admin.");
            return false; 
        }
        
        Set<Flag> flags = plot.settings.getFlags();
        flags.remove(flag);
        plot.settings.setFlags(flags.toArray(new Flag[0]));
        
        DBFunc.setFlags(player.getWorld().getName(), plot, plot.settings.getFlags().toArray(new Flag[0]));
        
        Main.sendMessage(player, "&7You may now &acontinue &7building.");
        
        return true;
    }
}
