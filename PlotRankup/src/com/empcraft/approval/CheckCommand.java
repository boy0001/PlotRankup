package com.empcraft.approval;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;

import com.intellectualcrafters.plot.PlotMain;
import com.intellectualcrafters.plot.commands.SubCommand;
import com.intellectualcrafters.plot.config.C;
import com.intellectualcrafters.plot.database.DBFunc;
import com.intellectualcrafters.plot.flag.Flag;
import com.intellectualcrafters.plot.object.Plot;
import com.intellectualcrafters.plot.util.PlayerFunctions;

import org.bukkit.entity.Player;

public class CheckCommand extends SubCommand {

    public CheckCommand() {
        super("check", "plots.check", "Check the status of your plot in the queue", "check", "status", CommandCategory.INFO, true);
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
        if (flag==null || flag.getValue().equals("true")) {
            Main.sendMessage(player, "&cThis plot is not in the queue!");
            return false;
        }
        Long time;
        try {
            time = (System.currentTimeMillis()/1000)-Long.parseLong(flag.getValue());
        }
        catch (Exception e) {
            time = System.currentTimeMillis()/1000;
        }
        Main.sendMessage(player, "&6Plot&7: &a"+plot.id+"\n&6Submitted&7: &a"+secToTime(time)+"&7ago\n");
        
        ArrayList<PlotWrapper> plots = getPlots();
        Integer index = plots.indexOf(new PlotWrapper(0L, plot.id, plot.world, plot.owner));
        if (index==null) {
            Main.sendMessage(player, "&6Queue rank&7: &aUnknown");    
        }
        else {
            Main.sendMessage(player, "&6Queue rank&7: &a"+index);  
        }
        return true;
    }
    
    private ArrayList<PlotWrapper> getPlots() {
            
            ArrayList<PlotWrapper> plots = new ArrayList<PlotWrapper>();
            
            for (Plot plot : PlotMain.getPlots()) {
                if (plot.hasOwner()) {
                    Flag flag = plot.settings.getFlag("done");
                    if (flag!=null) {
                        if (!flag.getValue().equals("true")) {
                            Long timestamp;
                            try {
                                timestamp = Long.parseLong(flag.getValue());
                            }
                            catch (Exception e) {
                                timestamp = 0L;
                            }
                            PlotWrapper wrap = new PlotWrapper(timestamp, plot.id, plot.world, plot.owner);
                            plots.add(wrap);
                        }
                    }
                }
            }
            Collections.sort(plots);
            return plots;
    }

    private String secToTime(Long time) {
        StringBuilder toreturn = new StringBuilder();
        int years = 0;
        int weeks = 0;
        int days = 0;
        int hours = 0;
        int minutes = 0;
        if (time>=33868800) {
            years = (int) (time/33868800);
            time-=years*33868800;
            if (time>1) {
                toreturn.append(years+" years ");
            }
            else {
                toreturn.append(years+" year ");
            }
        }
        if (time>=604800) {
            weeks = (int) (time/604800);
            time-=weeks*604800;
            if (time>1) {
                toreturn.append(weeks+" weeks ");
            }
            else {
                toreturn.append(weeks+" week ");
            }
        }
        if (time>=86400) {
            days = (int) (time/86400);
            time-=days*86400;
            if (time>1) {
                toreturn.append(days+" days ");
            }
            else {
                toreturn.append(days+" day ");
            }
        }
        if (time>=3600) {
            hours = (int) (time/3600);
            time-=hours*3600;
            if (time>1) {
                toreturn.append(hours+" hours ");
            }
            else {
                toreturn.append(hours+" hour ");
            }
        }
        if (time>=60) {
            minutes = (int) (time/60);
            time-=minutes*60;
            if (time>1) {
                toreturn.append(minutes+" minutes ");
            }
            else {
                toreturn.append(minutes+" minute ");
            }
        }
        if (toreturn.equals("")||time>0){
            if (time>1) {
                toreturn.append((time)+" seconds ");
            }
            else {
                toreturn.append((time)+" second ");
            }
        }
        return toreturn.toString();
    }
}
