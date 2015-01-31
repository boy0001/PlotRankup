package com.empcraft.approval;

import java.util.UUID;

import com.intellectualcrafters.plot.object.PlotId;

public class PlotWrapper implements Comparable<PlotWrapper> {
    
    Long timestamp;
    PlotId id;
    String world;
    UUID owner;
    
    public PlotWrapper(Long timestamp, PlotId id, String world, UUID owner) {
        this.timestamp = timestamp;
        this.id = id;
        this.world = world;
        this.owner = owner;
    }
    
    @Override
    public int compareTo(PlotWrapper other) {
        return (int) (timestamp == other.timestamp ? id.x - other.id.x : other.timestamp - timestamp);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        PlotWrapper other = (PlotWrapper) obj;
        return (((int) this.id.x == (int) other.id.x) && ((int) this.id.y == (int) other.id.y) && (this.world.equals(other.world)));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = (prime * result) + this.id.x;
        result = (prime * result) + this.id.y;
        result = (prime * result) + this.world.hashCode();
        return result;
    }
}
