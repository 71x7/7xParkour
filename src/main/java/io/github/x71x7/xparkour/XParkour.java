package io.github.x71x7.xparkour;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class XParkour extends JavaPlugin {

    private static final LegacyComponentSerializer LEGACY =
            LegacyComponentSerializer.builder().character('&').hexColors().build();

    private CourseManager courses;
    private SessionManager sessions;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        courses = new CourseManager(this);
        courses.load();
        sessions = new SessionManager(this);
        sessions.startTask();

        getServer().getPluginManager().registerEvents(new ParkourListener(this), this);

        PluginCommand cmd = getCommand("parkour");
        if (cmd != null) {
            ParkourCommand handler = new ParkourCommand(this);
            cmd.setExecutor(handler);
            cmd.setTabCompleter(handler);
        }
        getLogger().info("7xParkour activado: " + courses.all().size() + " parkour(s) cargado(s).");
    }

    @Override
    public void onDisable() {
        if (sessions != null) sessions.clear();
        if (courses != null) courses.saveAll(true);
    }

    public void reloadAll() {
        reloadConfig();
        sessions.clear();
        courses.load();
    }

    public CourseManager courses() { return courses; }
    public SessionManager sessions() { return sessions; }


    public static String apply(String raw, String... kv) {
        for (int i = 0; i + 1 < kv.length; i += 2) {
            raw = raw.replace(kv[i], kv[i + 1]);
        }
        return raw;
    }

    public String text(String key, String... kv) {
        return apply(getConfig().getString("messages." + key, "&cMensaje faltante: " + key), kv);
    }

    public Component component(String raw) {
        return LEGACY.deserialize(raw);
    }

    public void send(CommandSender to, String key, String... kv) {
        to.sendMessage(component(getConfig().getString("prefix", "") + text(key, kv)));
    }

    public void sendPlain(CommandSender to, String key, String... kv) {
        to.sendMessage(component(text(key, kv)));
    }

    public void sound(Player p, String key) {
        String s = getConfig().getString("sounds." + key, "");
        if (s == null || s.isEmpty()) return;
        try {
            p.playSound(p.getLocation(), s, 1f, 1f);
        } catch (RuntimeException | LinkageError ignored) {
        }
    }
}
