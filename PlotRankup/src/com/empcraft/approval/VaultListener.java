package com.empcraft.approval;

import java.util.List;
import java.util.UUID;

import net.milkbowl.vault.Vault;
import net.milkbowl.vault.chat.Chat;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;

public class VaultListener implements Listener {

    Main main;
    
    private static Permission perms = null;
    
    public VaultListener(Main main, Plugin vaultPlugin) {
        this.main = main;
        setupPermissions();
    }
    private boolean setupPermissions() {
        RegisteredServiceProvider<Permission> rsp = main.getServer().getServicesManager().getRegistration(Permission.class);
        perms = rsp.getProvider();
        return perms != null;
    }
    
    public static String getGroup(World world, UUID uuid) {
        return perms.getPrimaryGroup(world.getName(), Bukkit.getOfflinePlayer(uuid));
    }
    
    public static String getNextRank(World world, String group) {
        List<String> ranks = Main.config.getStringList(world.getName()+".approval.rankLadder");
        for (int i = 0; i < ranks.size()-1; i++) {
            String rank = ranks.get(i);
            if (rank.equals(group)) {
                return ranks.get(i+1);
            }
        }
        
        return "INVALID_RANK["+world.getName()+":"+group+"]";
    }

    // TODO events
    
}
