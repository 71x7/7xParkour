package io.github.x71x7.xparkour;

import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.time.Duration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public final class SessionManager {

    private final XParkour plugin;
    private final Map<UUID, Session> sessions = new HashMap<>();
    private BukkitTask task;

    public SessionManager(XParkour plugin) {
        this.plugin = plugin;
    }

    public void startTask() {
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 2L, 2L);
    }

    public void clear() {
        sessions.clear();
    }

    public Session get(Player p) { return sessions.get(p.getUniqueId()); }
    public void remove(UUID id) { sessions.remove(id); }

    private void tick() {
        String format = plugin.getConfig().getString("actionbar", "");
        if (format == null || format.isEmpty()) return;
        for (Map.Entry<UUID, Session> e : sessions.entrySet()) {
            Player p = Bukkit.getPlayer(e.getKey());
            if (p == null) continue;
            Session s = e.getValue();
            int total = s.getCourse().getCheckpoints().size();
            String line = XParkour.apply(format,
                    "%course%", s.getCourse().getName(),
                    "%time%", TimeUtil.format(s.elapsedMs()),
                    "%cp%", String.valueOf(s.getCheckpoint() + 1),
                    "%total%", String.valueOf(total));
            p.sendActionBar(plugin.component(line));
        }
    }


    public void tryBegin(Player p, Course c, Location at) {
        GameMode gm = p.getGameMode();
        if (gm != GameMode.SURVIVAL && gm != GameMode.ADVENTURE) {
            plugin.send(p, "gamemode-blocked");
            plugin.sound(p, "error");
            return;
        }
        if (p.getAllowFlight() || p.isFlying()) {
            plugin.send(p, "fly-start-blocked");
            plugin.sound(p, "error");
            return;
        }
        boolean had = sessions.containsKey(p.getUniqueId());
        Location respawn = Course.center(c.getStart(), at.getYaw(), at.getPitch());
        sessions.put(p.getUniqueId(), new Session(c, respawn));
        plugin.send(p, had ? "restarted" : "started", "%course%", c.getName());
        plugin.sound(p, "start");
    }

    public void reach(Player p, Session s, int index, Location at) {
        s.setCheckpoint(index);
        s.setRespawn(Course.center(s.getCourse().getCheckpoints().get(index), at.getYaw(), at.getPitch()));
        plugin.send(p, "checkpoint",
                "%n%", String.valueOf(index + 1),
                "%total%", String.valueOf(s.getCourse().getCheckpoints().size()),
                "%time%", TimeUtil.format(s.elapsedMs()));
        plugin.sound(p, "checkpoint");
    }

    public void respawn(Player p, Session s) {
        teleport(p, s.getRespawn());
        plugin.sound(p, "respawn");
    }

    public void restart(Player p, Session s) {
        Course c = s.getCourse();
        Location l = Course.center(c.getStart(), p.getLocation().getYaw(), p.getLocation().getPitch());
        teleport(p, l);
        sessions.put(p.getUniqueId(), new Session(c, l));
        plugin.send(p, "restarted");
        plugin.sound(p, "start");
    }

    public void finish(Player p, Session s) {
        Course c = s.getCourse();
        CourseManager cm = plugin.courses();
        long ms = s.elapsedMs();
        String time = TimeUtil.format(ms);

        CourseManager.Entry top = cm.record(c.getName());
        CourseManager.Entry old = cm.best(c.getName(), p.getUniqueId());
        boolean pb = cm.submit(c.getName(), p.getUniqueId(), p.getName(), ms);
        boolean wr = pb && (top == null || ms < top.ms());
        sessions.remove(p.getUniqueId());

        p.showTitle(Title.title(
                plugin.component(plugin.text("title-finish")),
                plugin.component(plugin.text("subtitle-finish", "%time%", time)),
                Title.Times.times(Duration.ofMillis(300), Duration.ofSeconds(3), Duration.ofMillis(600))));
        plugin.send(p, "finished", "%course%", c.getName(), "%time%", time);
        if (pb) {
            if (old == null) {
                plugin.send(p, "first-time");
            } else {
                plugin.send(p, "personal-best", "%diff%", "-" + TimeUtil.format(old.ms() - ms));
            }
        }
        plugin.sound(p, "finish");

        if (wr && plugin.getConfig().getBoolean("broadcast-records", true)) {
            Bukkit.getServer().sendMessage(plugin.component(plugin.getConfig().getString("prefix", "")
                    + plugin.text("record-broadcast", "%player%", p.getName(), "%course%", c.getName(), "%time%", time)));
        }
    }

    public void cancel(Player p, String messageKey) {
        if (sessions.remove(p.getUniqueId()) != null && messageKey != null) {
            plugin.send(p, messageKey);
            plugin.sound(p, "error");
        }
    }

    public void cancelCourse(String courseName) {
        Iterator<Map.Entry<UUID, Session>> it = sessions.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Session> e = it.next();
            if (e.getValue().getCourse().getName().equalsIgnoreCase(courseName)) {
                it.remove();
                Player p = Bukkit.getPlayer(e.getKey());
                if (p != null) plugin.send(p, "cancelled");
            }
        }
    }

    public void teleport(Player p, Location l) {
        p.teleport(l);
        p.setFallDistance(0f);
        p.setVelocity(new Vector(0, 0, 0));
        p.setFireTicks(0);
    }
}
