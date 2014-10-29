package com.empcraft.approval;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.intellectualcrafters.plot.C;
import com.intellectualcrafters.plot.Flag;
import com.intellectualcrafters.plot.FlagManager;
import com.intellectualcrafters.plot.PlayerFunctions;
import com.intellectualcrafters.plot.Plot;
import com.intellectualcrafters.plot.PlotBlock;
import com.intellectualcrafters.plot.PlotHelper;
import com.intellectualcrafters.plot.PlotMain;
import com.intellectualcrafters.plot.PlotWorld;
import com.intellectualcrafters.plot.commands.SubCommand;
import com.intellectualcrafters.plot.database.DBFunc;
import com.intellectualcrafters.plot.generator.DefaultPlotWorld;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
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
        Flag flag = plot.settings.getFlag("done");
        if (flag!=null) {
            if (flag.getValue().equals("false")) {
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
        
        int changed = hasChanged(player.getWorld(), plot);
        if (changed!=-1) {
            Main.sendMessage(player, "&7You've changed &c"+changed+" blocks for your current build. It has potential, and if you put some more time into it you'll definitely have something special.\n&8 - &7You can resubmit this build in &a"+coolTime+"&7 seconds. Good luck!");
            Main.cooldown.put(player.getName(), System.currentTimeMillis()/1000);
        }
        
        for (String user : Main.toRemove) {
            if ((System.currentTimeMillis()/1000) - Main.cooldown.get(user) >= coolTime) {
                Main.cooldown.remove(user);
                Main.toRemove.remove(user);
            }
        }
        
        Set<Flag> flags = plot.settings.getFlags();
        flags.add(new Flag(FlagManager.getFlag("done"), "false"));
        plot.settings.setFlags(flags.toArray(new Flag[0]));
        
        DBFunc.setFlags(player.getWorld().getName(), plot, plot.settings.getFlags().toArray(new Flag[0]));
        
        Main.sendMessage(player, "&7Your plot has been marked as &adone&7 and should be approved shortly.");
        
        return true;
    }
    
    private int hasChanged(World world, Plot plot) {
        PlotWorld plotworld = PlotMain.getWorldSettings(world);
        if (plotworld instanceof DefaultPlotWorld) {
            int goal = Main.worldChanged.get(world.getName());
            if (goal==0) {
                return -1;
            }
            int count = 0;
            DefaultPlotWorld dpw = (DefaultPlotWorld) plotworld;
            Location bot = PlotHelper.getPlotBottomLoc(world, plot.id).add(1, 0, 1);
            Location top = PlotHelper.getPlotBottomLoc(world, plot.id);
            
            Set<PlotBlock> floor = new HashSet<PlotBlock>(Arrays.asList(dpw.TOP_BLOCK));
            Set<PlotBlock> main = new HashSet<PlotBlock>(Arrays.asList(dpw.MAIN_BLOCK));
            
            int maxy = world.getMaxHeight();
            
            for (int x = bot.getBlockX(); x <= top.getBlockX(); x++) {
                for (int z = bot.getBlockZ(); z <= top.getBlockZ(); z++) {
                    for (int y = dpw.PLOT_HEIGHT + 1; y <= maxy; y++) {
                        Block block = world.getBlockAt(x, y, z);
                        if (block.getTypeId() != 0) {
                            count++;
                        }
                    }
                    if (count>=goal) {
                        return -1;
                    }
                }
            }
            
            
            for (int x = bot.getBlockX(); x <= top.getBlockX(); x++) {
                for (int z = bot.getBlockZ(); z <= top.getBlockZ(); z++) {
                    for (int y = dpw.PLOT_HEIGHT; y <= dpw.PLOT_HEIGHT; y++) {
                        Block block = world.getBlockAt(x, y, z);
                        PlotBlock plotblock = new PlotBlock((short) block.getTypeId(), block.getData());
                        if (!floor.contains(plotblock)) {
                            count++;
                        }
                    }
                    if (count>=goal) {
                        return -1;
                    }
                }
            }
            
            for (int x = bot.getBlockX(); x <= top.getBlockX(); x++) {
                for (int z = bot.getBlockZ(); z <= top.getBlockZ(); z++) {
                    for (int y = 1; y < dpw.PLOT_HEIGHT; y++) {
                        Block block = world.getBlockAt(x, y, z);
                        PlotBlock plotblock = new PlotBlock((short) block.getTypeId(), block.getData());
                        if (!main.contains(plotblock)) {
                            count++;
                        }
                    }
                    if (count>=goal) {
                        return -1;
                    }
                }
            }
            return count;
        }
        else {
            return -1;
        }
    }
}
