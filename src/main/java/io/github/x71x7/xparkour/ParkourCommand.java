package io.github.x71x7.xparkour;

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public final class ParkourCommand implements CommandExecutor, TabCompleter {

    private static final Pattern NAME = Pattern.compile("[A-Za-z0-9_-]{1,24}");
    private static final List<String> PLAYER_SUBS =
            List.of("help", "list", "join", "leave", "checkpoint", "restart", "top", "info");
    private static final List<String> ADMIN_SUBS =
            List.of("create", "delete", "setstart", "setend", "addcp", "delcp", "setspawn", "setfall",
                    "resettimes", "reload");
    private static final List<String> WITH_COURSE = List.of(
            "join", "top", "info", "delete", "setstart", "setend", "addcp", "delcp", "setspawn", "setfall",
            "resettimes");

    private final XParkour plugin;

    public ParkourCommand(XParkour plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            help(sender);
            return true;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);

        if (ADMIN_SUBS.contains(sub) && !sender.hasPermission("xparkour.admin")) {
            plugin.send(sender, "no-permission");
            return true;
        }
        if (PLAYER_SUBS.contains(sub) && !sub.equals("help") && !sender.hasPermission("xparkour.play")) {
            plugin.send(sender, "no-permission");
            return true;
        }

        switch (sub) {
            case "help" -> help(sender);
            case "list" -> list(sender);
            case "top" -> top(sender, args);
            case "info" -> info(sender, args);
            case "join" -> join(sender, args);
            case "leave" -> leave(sender);
            case "checkpoint", "cp" -> checkpoint(sender);
            case "restart" -> restart(sender);
            case "create" -> create(sender, args);
            case "delete" -> delete(sender, args);
            case "setstart", "setend", "setspawn", "setfall", "addcp", "delcp" -> edit(sender, sub, args);
            case "resettimes" -> resetTimes(sender, args);
            case "reload" -> {
                plugin.reloadAll();
                plugin.send(sender, "reloaded");
            }
            default -> plugin.send(sender, "unknown");
        }
        return true;
    }


    private void help(CommandSender s) {
        for (String line : plugin.getConfig().getStringList("help")) s.sendMessage(plugin.component(line));
        if (s.hasPermission("xparkour.admin")) {
            for (String line : plugin.getConfig().getStringList("help-admin")) s.sendMessage(plugin.component(line));
        }
    }

    private Player player(CommandSender s) {
        if (s instanceof Player p) return p;
        plugin.send(s, "players-only");
        return null;
    }

    private Course course(CommandSender s, String[] args, String usage) {
        if (args.length < 2) {
            plugin.send(s, "usage", "%usage%", usage);
            return null;
        }
        Course c = plugin.courses().get(args[1]);
        if (c == null) plugin.send(s, "course-not-found");
        return c;
    }

    private static Location blockOf(Player p) {
        return p.getLocation().getBlock().getLocation();
    }


    private void list(CommandSender s) {
        if (plugin.courses().all().isEmpty()) {
            plugin.sendPlain(s, "list-empty");
            return;
        }
        plugin.sendPlain(s, "list-header");
        for (Course c : plugin.courses().all()) {
            String status = c.isReady() ? "&aOK" : "&cincompleto";
            s.sendMessage(plugin.component(plugin.text("list-line",
                    "%course%", c.getName(),
                    "%cps%", String.valueOf(c.getCheckpoints().size()),
                    "%status%", status)));
        }
    }

    private void top(CommandSender s, String[] args) {
        Course c = course(s, args, "/pk top <parkour>");
        if (c == null) return;
        plugin.sendPlain(s, "top-header", "%course%", c.getName());
        List<CourseManager.Entry> top = plugin.courses().top(c.getName(), 10);
        if (top.isEmpty()) {
            plugin.sendPlain(s, "top-empty");
            return;
        }
        int pos = 1;
        for (CourseManager.Entry e : top) {
            plugin.sendPlain(s, "top-line",
                    "%pos%", String.valueOf(pos++), "%player%", e.name(), "%time%", TimeUtil.format(e.ms()));
        }
    }

    private void info(CommandSender s, String[] args) {
        Course c = course(s, args, "/pk info <parkour>");
        if (c == null) return;
        String none = plugin.text("none");
        String best = none;
        if (s instanceof Player p) {
            CourseManager.Entry b = plugin.courses().best(c.getName(), p.getUniqueId());
            if (b != null) best = TimeUtil.format(b.ms());
        }
        CourseManager.Entry rec = plugin.courses().record(c.getName());
        plugin.sendPlain(s, "info-line1", "%course%", c.getName(), "%cps%", String.valueOf(c.getCheckpoints().size()));
        plugin.sendPlain(s, "info-line2", "%best%", best);
        plugin.sendPlain(s, "info-line3", "%record%",
                rec == null ? none : TimeUtil.format(rec.ms()) + " &7(" + rec.name() + ")");
    }

    private void join(CommandSender s, String[] args) {
        Player p = player(s);
        if (p == null) return;
        Course c = course(s, args, "/pk join <parkour>");
        if (c == null) return;
        if (!c.isReady()) {
            plugin.send(s, "course-not-ready");
            return;
        }
        plugin.sessions().remove(p.getUniqueId());
        Location dest = c.getSpawn() != null && c.getSpawn().getWorld() != null
                ? c.getSpawn()
                : Course.center(c.getStart(), p.getLocation().getYaw(), p.getLocation().getPitch());
        plugin.sessions().teleport(p, dest);
        plugin.send(p, "joined", "%course%", c.getName());
    }

    private void leave(CommandSender s) {
        Player p = player(s);
        if (p == null) return;
        if (plugin.sessions().get(p) == null) {
            plugin.send(p, "not-in-session");
            return;
        }
        plugin.sessions().remove(p.getUniqueId());
        plugin.send(p, "left");
    }

    private void checkpoint(CommandSender s) {
        Player p = player(s);
        if (p == null) return;
        Session session = plugin.sessions().get(p);
        if (session == null) {
            plugin.send(p, "not-in-session");
            return;
        }
        plugin.sessions().respawn(p, session);
    }

    private void restart(CommandSender s) {
        Player p = player(s);
        if (p == null) return;
        Session session = plugin.sessions().get(p);
        if (session == null) {
            plugin.send(p, "not-in-session");
            return;
        }
        plugin.sessions().restart(p, session);
    }


    private void create(CommandSender s, String[] args) {
        if (args.length < 2) {
            plugin.send(s, "usage", "%usage%", "/pk create <nombre>");
            return;
        }
        String name = args[1];
        if (!NAME.matcher(name).matches()) {
            plugin.send(s, "name-invalid");
            return;
        }
        if (plugin.courses().exists(name)) {
            plugin.send(s, "course-exists");
            return;
        }
        plugin.courses().create(name);
        plugin.send(s, "course-created", "%course%", name);
    }

    private void delete(CommandSender s, String[] args) {
        Course c = course(s, args, "/pk delete <parkour>");
        if (c == null) return;
        plugin.sessions().cancelCourse(c.getName());
        plugin.courses().delete(c.getName());
        plugin.send(s, "course-deleted", "%course%", c.getName());
    }

    private void edit(CommandSender s, String sub, String[] args) {
        Player p = player(s);
        if (p == null) return;
        Course c = course(s, args, "/pk " + sub + " <parkour>");
        if (c == null) return;

        String what;
        switch (sub) {
            case "setstart" -> {
                c.setStart(blockOf(p));
                what = "Inicio";
            }
            case "setend" -> {
                c.setEnd(blockOf(p));
                what = "Meta";
            }
            case "setspawn" -> {
                c.setSpawn(p.getLocation());
                what = "Spawn";
            }
            case "setfall" -> {
                c.setFallY(p.getLocation().getY());
                what = "Altura de caída";
            }
            case "addcp" -> {
                c.getCheckpoints().add(blockOf(p));
                plugin.courses().saveCourses(false);
                plugin.send(p, "checkpoint-added", "%n%", String.valueOf(c.getCheckpoints().size()),
                        "%course%", c.getName());
                return;
            }
            default -> {
                if (c.getCheckpoints().isEmpty()) {
                    plugin.send(p, "no-checkpoints");
                    return;
                }
                c.getCheckpoints().remove(c.getCheckpoints().size() - 1);
                plugin.sessions().cancelCourse(c.getName());
                plugin.courses().saveCourses(false);
                plugin.send(p, "checkpoint-removed", "%course%", c.getName());
                return;
            }
        }
        plugin.courses().saveCourses(false);
        plugin.send(p, "point-set", "%what%", what, "%course%", c.getName());
    }

    private void resetTimes(CommandSender s, String[] args) {
        Course c = course(s, args, "/pk resettimes <parkour>");
        if (c == null) return;
        plugin.courses().clearTimes(c.getName());
        plugin.send(s, "times-reset", "%course%", c.getName());
    }


    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            List<String> subs = new ArrayList<>(PLAYER_SUBS);
            if (sender.hasPermission("xparkour.admin")) subs.addAll(ADMIN_SUBS);
            filter(subs, args[0], out);
        } else if (args.length == 2) {
            String sub = args[0].toLowerCase(Locale.ROOT);
            boolean allowed = !ADMIN_SUBS.contains(sub) || sender.hasPermission("xparkour.admin");
            if (allowed && WITH_COURSE.contains(sub)) {
                List<String> names = new ArrayList<>();
                for (Course c : plugin.courses().all()) names.add(c.getName());
                filter(names, args[1], out);
            }
        }
        return out;
    }

    private static void filter(List<String> options, String prefix, List<String> out) {
        String p = prefix.toLowerCase(Locale.ROOT);
        for (String o : options) {
            if (o.toLowerCase(Locale.ROOT).startsWith(p)) out.add(o);
        }
    }
}
