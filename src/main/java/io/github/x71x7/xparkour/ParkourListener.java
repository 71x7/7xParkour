package io.github.x71x7.xparkour;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.event.player.PlayerToggleFlightEvent;

import java.util.List;

public final class ParkourListener implements Listener {

    private final XParkour plugin;

    public ParkourListener(XParkour plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent e) {
        Location to = e.getTo();
        if (to == null) return;
        Player p = e.getPlayer();
        SessionManager sm = plugin.sessions();
        Session s = sm.get(p);

        if (s != null && to.getY() < s.getCourse().getFallY()) {
            sm.respawn(p, s);
            return;
        }

        Location from = e.getFrom();
        if (from.getBlockX() == to.getBlockX()
                && from.getBlockY() == to.getBlockY()
                && from.getBlockZ() == to.getBlockZ()) {
            return;
        }

        if (s != null) {
            handleRunning(p, s, to);
            return;
        }

        if (!p.hasPermission("xparkour.play")) return;
        for (Course c : plugin.courses().all()) {
            if (c.isReady() && Course.sameBlock(to, c.getStart())) {
                sm.tryBegin(p, c, to);
                return;
            }
        }
    }

    private void handleRunning(Player p, Session s, Location to) {
        SessionManager sm = plugin.sessions();
        Course c = s.getCourse();

        if (Course.sameBlock(to, c.getStart())) {
            sm.tryBegin(p, c, to);
            return;
        }

        if (Course.sameBlock(to, c.getEnd())) {
            int total = c.getCheckpoints().size();
            if (s.getCheckpoint() >= total - 1) {
                sm.finish(p, s);
            } else if (s.canWarn()) {
                plugin.send(p, "missing-checkpoints", "%missing%", String.valueOf(total - 1 - s.getCheckpoint()));
                plugin.sound(p, "error");
            }
            return;
        }

        List<Location> cps = c.getCheckpoints();
        for (int i = 0; i < cps.size(); i++) {
            if (!Course.sameBlock(to, cps.get(i))) continue;
            if (i == s.getCheckpoint() + 1) {
                sm.reach(p, s, i, to);
            } else if (i > s.getCheckpoint() + 1 && s.canWarn()) {
                plugin.send(p, "skipped");
                plugin.sound(p, "error");
            }
            return;
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        plugin.sessions().remove(e.getPlayer().getUniqueId());
    }

    @EventHandler(ignoreCancelled = true)
    public void onToggleFlight(PlayerToggleFlightEvent e) {
        Player p = e.getPlayer();
        if (e.isFlying() && plugin.sessions().get(p) != null) {
            e.setCancelled(true);
            plugin.sessions().cancel(p, "fly-blocked");
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onGameMode(PlayerGameModeChangeEvent e) {
        GameMode gm = e.getNewGameMode();
        if (gm != GameMode.SURVIVAL && gm != GameMode.ADVENTURE) {
            plugin.sessions().cancel(e.getPlayer(), "gamemode-blocked");
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent e) {
        Player p = e.getPlayer();
        Session s = plugin.sessions().get(p);
        if (s == null) return;
        TeleportCause cause = e.getCause();
        if (cause != TeleportCause.PLUGIN && cause != TeleportCause.UNKNOWN) {
            plugin.sessions().cancel(p, "cancelled");
            return;
        }
        Location dest = e.getTo();
        if (dest != null && dest.getWorld() != null
                && s.getCourse().getStart().getWorld() != null
                && !dest.getWorld().equals(s.getCourse().getStart().getWorld())) {
            plugin.sessions().cancel(p, "cancelled");
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;
        Session s = plugin.sessions().get(p);
        if (s == null) return;

        if (plugin.getConfig().getBoolean("respawn-on-hazard", true)) {
            switch (e.getCause()) {
                case VOID, LAVA, FIRE, FIRE_TICK -> {
                    e.setCancelled(true);
                    plugin.sessions().respawn(p, s);
                    return;
                }
                default -> { }
            }
        }
        if (plugin.getConfig().getBoolean("cancel-damage", true)) {
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onHunger(FoodLevelChangeEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;
        if (plugin.getConfig().getBoolean("cancel-hunger", true) && plugin.sessions().get(p) != null) {
            e.setCancelled(true);
        }
    }
}
