package com.empcraft.approval;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import com.intellectualcrafters.plot.PlotMain;
import com.intellectualcrafters.plot.commands.MainCommand;
import com.intellectualcrafters.plot.events.PlotFlagAddEvent;
import com.intellectualcrafters.plot.events.PlotFlagRemoveEvent;
import com.intellectualcrafters.plot.flag.AbstractFlag;
import com.intellectualcrafters.plot.flag.Flag;
import com.intellectualcrafters.plot.flag.FlagManager;
import com.intellectualcrafters.plot.object.Plot;
import com.intellectualcrafters.plot.object.PlotId;
import com.intellectualcrafters.plot.util.PlayerFunctions;

public class Main extends JavaPlugin implements Listener {

    public String version;
    public Main plugin;
    
    
    public static boolean vaultFeatures = false;
    public static HashMap<String, Long> cooldown = new HashMap<String, Long>();
    public static HashSet<String> toRemove = new HashSet<String>();
    public static FileConfiguration config;
    
    public static HashMap<String, Integer> worldChanged = new HashMap<String, Integer>();
    
    @Override
    public void onEnable() {
        this.version = getDescription().getVersion();
        this.plugin = this;
        setupPlotSquared();
        setupVault();
        setupConfig();
        setupFlags();
        Main.config = this.getConfig();
        setupPlots();
    }
    
    private static void setupPlots() {
        for (Plot plot : PlotMain.getPlots()) {
            Flag flag = FlagManager.getPlotFlag(plot, "done");
            if (flag!=null) {
                if (flag.getValue().equals("true")) {
                    plot.countsTowardsMax = false;
                }
            }
        }
    }
    
    private void setupConfig() {
        getConfig().options().copyDefaults(true);
        final Map<String, Object> options = new HashMap<String, Object>();
        getConfig().set("version", version);
        options.put("reapproval-wait-time-sec", 300);
        options.put("build-while-approved", false);
        for (String world: PlotMain.getPlotWorlds()) {
            options.put(world+".approval.min-required-changed-blocks", 0);
            List<String> actions = Arrays.asList("1:manuadd %player% rank1", "2:manuadd %player% %nextrank%");
            options.put(world+".approval.actions", actions); 
            List<String> rankLadder = Arrays.asList("rank1", "rank2");
            options.put(world+".approval.rankLadder", rankLadder); 
        }
        for (final Entry<String, Object> node : options.entrySet()) {
            if (!getConfig().contains(node.getKey())) {
                getConfig().set(node.getKey(), node.getValue());
            }
        }
        saveConfig();
        for (World world: Bukkit.getWorlds()) {
            worldChanged.put(world.getName(), getConfig().getInt(world.getName()+".approval.min-required-changed-blocks"));
        }
    }
    
    private void setupPlotSquared() {
        final Plugin plotsquared = Bukkit.getServer().getPluginManager().getPlugin("PlotSquared");
        if(plotsquared == null || !plotsquared.isEnabled()) {
            sendMessage(null, "&c[PlotApproval] Could not find PlotSquared! Disabling plugin...");
            Bukkit.getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        Bukkit.getServer().getPluginManager().registerEvents(this, this);
        MainCommand.subCommands.add(new DoneCommand());
        MainCommand.subCommands.add(new ContinueCommand());
        MainCommand.subCommands.add(new ApproveCommand());
        MainCommand.subCommands.add(new CheckCommand());
    }
    
    private void setupVault() {
        final Plugin vaultPlugin = Bukkit.getServer().getPluginManager().getPlugin("Vault");
        if (vaultPlugin != null && vaultPlugin.isEnabled()) {
            VaultListener vault = new VaultListener(this, vaultPlugin);
            Bukkit.getServer().getPluginManager().registerEvents(vault, this);
            sendMessage(null, "&a[PlotApproval] Detected vault. Additional features enabled");
            Main.vaultFeatures = true;
        }
        else {
            sendMessage(null, "&a[PlotApproval] Detected vault. Additional features enabled");
        }
    }
    
    private static String colorise(final String mystring) {
        return ChatColor.translateAlternateColorCodes('&', mystring);
    }
    
    public static void sendMessage(final Player player, final String mystring) {
        if (ChatColor.stripColor(mystring).equals("")) {
            return;
        }
        if (player == null) {
            Bukkit.getServer().getConsoleSender().sendMessage(colorise(mystring));
        } else {
            player.sendMessage(colorise(mystring));
        }
    }
    
    private static void setupFlags() {
        AbstractFlag doneFlag = new AbstractFlag("done") {
            @Override
            public String getValueDesc() {
                return "Value must be a boolean 'true' or 'false'; which determines whether your build has been finalized.";
            }
            @Override
            public String parseValue(String value) {
                switch(value) {
                    case "true":
                        return "0";
                    case "false":
                        return "false";
                    default:
                        try {
                            return Long.parseLong(value)+"";
                        }
                        catch (Exception e) {
                            return null;
                        }
                }
            }
        };
        FlagManager.addFlag(doneFlag);
    }
    
    @EventHandler(ignoreCancelled=true, priority=EventPriority.HIGH)
    private static void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        World world = player.getWorld();
        if (!PlotMain.isPlotWorld(world)) {
            return;
        }
        Location loc = event.getBlock().getLocation();
        PlotId id = PlayerFunctions.getPlot(loc);
        if (id==null) {
            return;
        }
        Plot plot = PlotMain.getPlots(world).get(id) ;
        if (plot==null) {
            return;
        }
        boolean rights = plot.hasRights(player);
        if (!rights) {
            return;
        }
        Flag flag = FlagManager.getPlotFlag(plot, "done"); 
        if (flag == null) {
            return;
        }
        if (PlotMain.hasPermission(player, "plots.admin")) {
            return;
        }
        if (flag.getValue().equals(true)) {
            if (!config.getBoolean("build-while-approved")) {
                if (!flag.getValue().equals("true")) {
                    sendMessage(player,"&7Your plot has been marked as done. To remove it from the queue and continue building please use:\n&a/plots continue");
                }
                else {
                    sendMessage(player,"&7Your plot has been approved. To continue building, please get an admin to unapprove the plot.");
                }
                event.setCancelled(true);
            }
        }
    }
    
    
    @EventHandler(ignoreCancelled=true, priority=EventPriority.HIGH)
    private static void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        World world = player.getWorld();
        if (!PlotMain.isPlotWorld(world)) {
            return;
        }
        Location loc = event.getBlock().getLocation();
        PlotId id = PlayerFunctions.getPlot(loc);
        if (id==null) {
            return;
        }
        Plot plot = PlotMain.getPlots(world).get(id) ;
        if (plot==null) {
            return;
        }
        boolean rights = plot.hasRights(player);
        if (!rights) {
            return;
        }
        Flag flag = FlagManager.getPlotFlag(plot, "done"); 
        if (flag == null) {
            return;
        }
        if (PlotMain.hasPermission(player, "plots.admin")) {
            return;
        }
        if (flag.getValue().equals(true)) {
            if (!config.getBoolean("build-while-approved")) {
                if (!flag.getValue().equals("true")) {
                    sendMessage(player,"&7Your plot has been marked as done. To remove it from the queue and continue building please use:\n&a/plots continue");
                }
                else {
                    sendMessage(player,"&7Your plot has been approved. To continue building, please get an admin to unapprove the plot.");
                }
                event.setCancelled(true);
            }
        }
    }
    
    @EventHandler(ignoreCancelled=true, priority=EventPriority.HIGH)
    private static void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        World world = player.getWorld();
        if (!PlotMain.isPlotWorld(world)) {
            return;
        }
        Block block = event.getClickedBlock();
        if (block==null) {
            return;
        }
        Location loc = block.getLocation();
        PlotId id = PlayerFunctions.getPlot(loc);
        if (id==null) {
            return;
        }
        Plot plot = PlotMain.getPlots(world).get(id) ;
        if (plot==null) {
            return;
        }
        boolean rights = plot.hasRights(player);
        if (!rights) {
            return;
        }
        Flag flag = FlagManager.getPlotFlag(plot, "done"); 
        if (flag == null) {
            return;
        }
        if (PlotMain.hasPermission(player, "plots.admin")) {
            return;
        }
        if (flag.getValue().equals(true)) {
            if (!config.getBoolean("build-while-approved")) {
                if (!flag.getValue().equals("true")) {
                    sendMessage(player,"&7Your plot has been marked as done. To remove it from the queue and continue building please use:\n&a/plots continue");
                }
                else {
                    sendMessage(player,"&7Your plot has been approved. To continue building, please get an admin to unapprove the plot.");
                }
                event.setCancelled(true);
            }
        }
    }
    
    @EventHandler
    private static void onJoin(PlayerJoinEvent event) {
        if (toRemove.contains(event.getPlayer().getName())) {
            toRemove.remove(event.getPlayer().getName());
        }
    }
    
    @EventHandler
    private static void onQuit(PlayerQuitEvent event) {
        if (cooldown.containsKey(event.getPlayer().getName())) {
            toRemove.add(event.getPlayer().getName());
        }
    }
    
    
    
}
