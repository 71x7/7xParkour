package io.github.x71x7.xparkour;

import org.bukkit.Location;

public final class Session {

    private final Course course;
    private final long startNanos = System.nanoTime();
    private int checkpoint = -1;
    private Location respawn;
    private long lastWarn;

    public Session(Course course, Location respawn) {
        this.course = course;
        this.respawn = respawn;
    }

    public Course getCourse() { return course; }
    public int getCheckpoint() { return checkpoint; }
    public void setCheckpoint(int checkpoint) { this.checkpoint = checkpoint; }
    public Location getRespawn() { return respawn; }
    public void setRespawn(Location respawn) { this.respawn = respawn; }

    public long elapsedMs() {
        return (System.nanoTime() - startNanos) / 1_000_000L;
    }

    public boolean canWarn() {
        long now = System.currentTimeMillis();
        if (now - lastWarn < 1500) return false;
        lastWarn = now;
        return true;
    }
}
