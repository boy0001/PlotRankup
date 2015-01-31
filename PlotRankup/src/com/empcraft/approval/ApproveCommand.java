package com.empcraft.approval;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.intellectualcrafters.plot.PlotMain;
import com.intellectualcrafters.plot.commands.SubCommand;
import com.intellectualcrafters.plot.config.C;
import com.intellectualcrafters.plot.database.DBFunc;
import com.intellectualcrafters.plot.flag.Flag;
import com.intellectualcrafters.plot.flag.FlagManager;
import com.intellectualcrafters.plot.object.Plot;
import com.intellectualcrafters.plot.object.PlotId;
import com.intellectualcrafters.plot.util.PlayerFunctions;
import com.intellectualcrafters.plot.util.UUIDHandler;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

public class ApproveCommand extends SubCommand {

    public ApproveCommand() {
        super("approve", "plots.admin", "Used to approve player's plots", "approve", "approval", CommandCategory.ACTIONS, true);
    }
    @Override
    public boolean execute(Player player, String... args) {
        List<String> validArgs = Arrays.asList("approve","list","next","listworld","deny");
        if (args.length==0 || !validArgs.contains(args[0].toLowerCase())) {
            Main.sendMessage(player, "&7Syntax: &c/plots approval <approve|deny|list|listworld|next>");
            return false;
        }
        args[0] = args[0].toLowerCase();
        if (args[0].equals("approve")) {
            
            if(!PlayerFunctions.isInPlot(player)) {
                sendMessage(player, C.NOT_IN_PLOT);
                return false;
            }
            PlotId id = PlayerFunctions.getPlot(player.getLocation());
            
            World world = player.getWorld();
            Plot plot = PlotMain.getPlots(world).get(id); 
            if (plot==null || !plot.hasOwner()) {
                sendMessage(player, C.NOT_IN_PLOT);
                return false;
            }
            
            Flag flag = FlagManager.getPlotFlag(plot, "done");
            if (flag==null || flag.getValue().equals("true")) {
                if (flag==null) {
                    Main.sendMessage(player, "&7This plot is not &cpending&7 for approval.");
                }
                else {
                    Main.sendMessage(player, "&7This plot has already been approved.");
                }
                return false;
            }
            FlagManager.addPlotFlag(plot, new Flag(FlagManager.getFlag("done"), "true"));
            Player owner = Bukkit.getPlayer(plot.owner);
            if (owner!=null) {
                if (plot.settings.getAlias() != null && !plot.settings.getAlias().equals("")) {
                    Main.sendMessage(owner, "&7Your plot &a"+plot.id+" &7/ &a"+plot.settings.getAlias()+" has been approved!");
                }
                else {
                    Main.sendMessage(owner, "&7Your plot &a"+plot.id+"&7 has been approved!");
                }
            }
            
            int count = countApproved(plot.owner, world);
            
            for (String commandargs : Main.config.getStringList(world.getName()+".approval.actions")) {
                try {
                    int required = Integer.parseInt(commandargs.split(":")[0]);
                    if (required==count) {
                        String ownername = UUIDHandler.getName(plot.owner);
                        if (ownername==null) {
                            ownername = "";
                        }
                        String cmd = commandargs.substring(commandargs.indexOf(":")+1);
                        if (cmd.contains("%player%")) {
                            cmd = cmd.replaceAll("%player%", ownername);
                        }
                        cmd = cmd.replaceAll("%world%", world.getName());
                        
                        if (Main.vaultFeatures) {
                            if (cmd.contains("%nextrank%")) {
                                cmd.replaceAll("%nextrank%", VaultListener.getNextRank(world, VaultListener.getGroup(world, plot.owner)));
                            }
                        }
                        Main.sendMessage(null, "Console: "+cmd);
                        Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), cmd);
                    }
                }
                catch (Exception e) {
                    Main.sendMessage(null, "[PlotApproval] &cInvalid approval command "+commandargs+"!");
                    Main.sendMessage(player, "[PlotApproval] &cInvalid approval command "+commandargs+"!");
                    return true;
                }
            }
            Main.sendMessage(player, "&aSuccessfully approved plot!");
            return true;
        }
        if (args[0].equals("listworld")) { // Plots are sorted in claim order.
            World world = player.getWorld();
            
            ArrayList<PlotWrapper> plots = getPlots(world);
            if (plots.size()==0) {
                Main.sendMessage(player, "&7There are currently &c0&7 plots pending for approval.");
                return true;
            }
            Main.sendMessage(player, "&7There are currently &c"+plots.size()+"&7 plots pending for approval.");
            for (PlotWrapper current : plots) {
                String ownername = UUIDHandler.getName(current.owner);
                if (ownername==null) {
                    ownername = "unknown";
                }
                Main.sendMessage(player, "&8 - &3"+current.world+"&7;&3"+current.id.x+"&7;&3"+current.id.y+" &7: "+ownername);
            }
            return true;
        }
        if (args[0].equals("list")) {
            ArrayList<PlotWrapper> plots = getPlots();
            if (plots.size()==0) {
                Main.sendMessage(player, "&7There are currently &c0&7 plots pending for approval.");
                return true;
            }
            Main.sendMessage(player, "&7There are currently &c"+plots.size()+"&7 plots pending for approval.");
            for (PlotWrapper current : plots) {
                String ownername = UUIDHandler.getName(current.owner);
                if (ownername==null) {
                    ownername = "unknown";
                }
                Main.sendMessage(player, "&8 - &3"+current.world+"&7;&3"+current.id.x+"&7;&3"+current.id.y+" &7: "+ownername);
            }
            return true;
        }
        if (args[0].equals("next")) {

            PlotId id = PlayerFunctions.getPlot(player.getLocation());
            World world = player.getWorld();
            String worldname = world.getName();
            
            ArrayList<PlotWrapper> plots = getPlots();
            if (plots.size() > 0) {
                if (id!=null) {
                    Plot plot = PlotMain.getPlots(world).get(id); 
                    if (plot!=null && plot.hasOwner()) {
                        for (int i = 0; i < plots.size(); i++) {
                            if (plots.get(i).id.equals(id) && plots.get(i).world.equals(worldname)) {
                                if (i < plots.size()-1) {
                                    PlotWrapper wrap = plots.get(i+1);
                                    Plot p2 = PlotMain.getPlots(wrap.world).get(wrap.id);
                                    PlotMain.teleportPlayer(player, player.getLocation(), p2);
                                }
                                break;
                            }
                        }
                    }
                }
                PlotWrapper wrap = plots.get(0);
                Plot p2 = PlotMain.getPlots(wrap.world).get(wrap.id);
                PlotMain.teleportPlayer(player, player.getLocation(), p2);
                return true;
            }
            Main.sendMessage(player, "&7There are currently &c0&7 plots pending for approval.");
            return true;
        }
        if (args[0].equals("deny")) {
            
            if(!PlayerFunctions.isInPlot(player)) {
                sendMessage(player, C.NOT_IN_PLOT);
                return false;
            }
            PlotId id = PlayerFunctions.getPlot(player.getLocation());
            
            World world = player.getWorld();
            Plot plot = PlotMain.getPlots(world).get(id); 
            if (plot==null || !plot.hasOwner()) {
                sendMessage(player, C.NOT_IN_PLOT);
                return false;
            }
            
            Flag flag = FlagManager.getPlotFlag(plot, "done");
            if (flag==null) {
                Main.sendMessage(player, "&7This plot is not &cpending&7 for approval.");
                return false;
            }
            FlagManager.removePlotFlag(plot, "done");
            String owner = UUIDHandler.getName(plot.owner);
            if (owner!=null) {
                Main.cooldown.put(owner, (System.currentTimeMillis()/1000));
            }
            Main.sendMessage(player, "&aSuccessfully unapproved plot!");
            return true;
        }
        return true;
    }
    private ArrayList<PlotWrapper> getPlots() {
        
        ArrayList<PlotWrapper> plots = new ArrayList<PlotWrapper>();
        
        for (Plot plot : PlotMain.getPlots()) {
            if (plot.hasOwner()) {
                Flag flag = FlagManager.getPlotFlag(plot, "done");
                if (flag!=null) {
                    if (!(flag.getValue() instanceof Boolean) && flag.getValue() instanceof Long) {
                        Long timestamp = (Long) flag.getValue();
                        PlotWrapper wrap = new PlotWrapper(timestamp, plot.id, plot.world, plot.owner);
                        plots.add(wrap);
                    }
                }
            }
        }
        Collections.sort(plots);
        return plots;
    }
    private ArrayList<PlotWrapper> getPlots(World world) {
        
        ArrayList<PlotWrapper> plots = new ArrayList<PlotWrapper>();
        
        for (Plot plot : PlotMain.getPlots(world).values()) {
            if (plot.hasOwner()) {
                Flag flag = FlagManager.getPlotFlag(plot, "done");
                if (flag!=null) {
                    if (!(flag.getValue() instanceof Boolean) && flag.getValue() instanceof Long) {
                        Long timestamp = (Long) flag.getValue();
                        PlotWrapper wrap = new PlotWrapper(timestamp, plot.id, plot.world, plot.owner);
                        plots.add(wrap);
                    }
                }
            }
        }
        Collections.sort(plots);
        return plots;
    }
    private int countApproved(UUID owner, World world) {
        int count = 0;
        for (Plot plot : PlotMain.getPlots(world).values()) {
            if (plot.owner.equals(owner)) {
                Flag flag = FlagManager.getPlotFlag(plot, "done");
                if (flag!=null) {
                    if (flag.getValue().equals("true")) {
                        count++;
                    }
                }
            }
        }
        return count;
    }
}