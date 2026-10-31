package io.github.x71x7.xparkour;

import org.bukkit.Location;

import java.util.ArrayList;
import java.util.List;

public final class Course {

    private final String name;
    private Location start;
    private Location end;
    private Location spawn;
    private Double fallY;
    private final List<Location> checkpoints = new ArrayList<>();

    public Course(String name) {
        this.name = name;
    }

    public String getName() { return name; }
    public Location getStart() { return start; }
    public void setStart(Location start) { this.start = start; }
    public Location getEnd() { return end; }
    public void setEnd(Location end) { this.end = end; }
    public Location getSpawn() { return spawn; }
    public void setSpawn(Location spawn) { this.spawn = spawn; }
    public Double getRawFallY() { return fallY; }
    public void setFallY(Double fallY) { this.fallY = fallY; }
    public List<Location> getCheckpoints() { return checkpoints; }

    public boolean isReady() {
        return start != null && end != null && start.getWorld() != null && end.getWorld() != null;
    }

    public double getFallY() {
        if (fallY != null) return fallY;
        return start != null ? start.getY() - 20 : -64;
    }

    public static boolean sameBlock(Location a, Location b) {
        if (a == null || b == null || a.getWorld() == null) return false;
        return a.getBlockX() == b.getBlockX()
                && a.getBlockY() == b.getBlockY()
                && a.getBlockZ() == b.getBlockZ()
                && a.getWorld().equals(b.getWorld());
    }

    public static Location center(Location block, float yaw, float pitch) {
        return new Location(block.getWorld(), block.getBlockX() + 0.5, block.getBlockY(),
                block.getBlockZ() + 0.5, yaw, pitch);
    }
}
