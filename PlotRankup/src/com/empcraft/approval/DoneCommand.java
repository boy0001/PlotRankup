package com.empcraft.approval;

import com.intellectualcrafters.plot.PlotMain;
import com.intellectualcrafters.plot.commands.SubCommand;
import com.intellectualcrafters.plot.config.C;
import com.intellectualcrafters.plot.flag.Flag;
import com.intellectualcrafters.plot.flag.FlagManager;
import com.intellectualcrafters.plot.generator.HybridPlotManager;
import com.intellectualcrafters.plot.generator.HybridPlotWorld;
import com.intellectualcrafters.plot.object.Plot;
import com.intellectualcrafters.plot.object.PlotWorld;
import com.intellectualcrafters.plot.util.PlayerFunctions;
import org.bukkit.entity.Player;

public class DoneCommand extends SubCommand {

    public DoneCommand() {
        super("done", "plots.done", "Mark your plot as finished", "done", "finish", CommandCategory.ACTIONS, true);
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
        Flag flag = FlagManager.getPlotFlag(plot, "done");
        if (flag!=null) {
            if (!flag.getValue().equals("true")) {
                Main.sendMessage(player, "&7This plot is already marked as &cdone&7 and is waiting for approval.");
            }
            else {
                Main.sendMessage(player, "&7This plot is already marked as &cdone&7 and has been approved.");
            }
            return false;
        }
        
        int coolTime = Main.config.getInt("reapproval-wait-time-sec");
        
        Long playerCool = Main.cooldown.get(player.getName());
        if (playerCool != null) {
            long diff = (System.currentTimeMillis()/1000) - playerCool;
            if (diff < coolTime) {
                Main.sendMessage(player, "&cSorry... &7you need to wait a bit longer until you can resubmit your build (&a"+(coolTime-diff)+" seconds&7)");
                return false;
            }
            Main.cooldown.remove(player.getName());
        }
        Integer goal = Main.worldChanged.get(player.getWorld().getName());
        if (goal == null) {
        	goal = 0;
        }
        boolean changed = hasChanged(plot, goal);
        if (!changed) {
            Main.sendMessage(player, "&7You've changed an &cinsignificant&7 number of blocks in your current build. It has potential, and if you put some more time into it you'll definitely have something special.\n&8 - &7You can resubmit this build in &a"+coolTime+"&7 seconds. Good luck!");
            Main.cooldown.put(player.getName(), System.currentTimeMillis()/1000);
            return false;
        }
        
        for (String user : Main.toRemove) {
            if ((System.currentTimeMillis()/1000) - Main.cooldown.get(user) >= coolTime) {
                Main.cooldown.remove(user);
                Main.toRemove.remove(user);
            }
        }
        FlagManager.addPlotFlag(plot, new Flag(FlagManager.getFlag("done"), ""+(System.currentTimeMillis()/1000)));
        Main.sendMessage(player, "&7Your plot has been marked as &adone&7 and should be approved shortly.");
        
        return true;    
    }
    
    private boolean hasChanged(Plot plot, int goal) {
    	if (goal == 0) {
    		return true;
    	}
        PlotWorld plotworld = PlotMain.getWorldSettings(plot.world);
        if (plotworld instanceof HybridPlotWorld) {
        	return HybridPlotManager.checkModified(plot, goal);
        }
        return true;
    }
}
