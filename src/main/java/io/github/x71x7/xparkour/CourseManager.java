package io.github.x71x7.xparkour;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

public final class CourseManager {

    public record Entry(String name, long ms) {}

    private final XParkour plugin;
    private final Map<String, Course> courses = new LinkedHashMap<>();
    private final Map<String, Map<UUID, Entry>> times = new HashMap<>();
    private final File coursesFile;
    private final File timesFile;

    public CourseManager(XParkour plugin) {
        this.plugin = plugin;
        this.coursesFile = new File(plugin.getDataFolder(), "courses.yml");
        this.timesFile = new File(plugin.getDataFolder(), "times.yml");
    }

    private static String key(String name) {
        return name.toLowerCase(Locale.ROOT);
    }


    public void load() {
        courses.clear();
        times.clear();

        YamlConfiguration cy = YamlConfiguration.loadConfiguration(coursesFile);
        ConfigurationSection root = cy.getConfigurationSection("courses");
        if (root != null) {
            for (String name : root.getKeys(false)) {
                ConfigurationSection cs = root.getConfigurationSection(name);
                if (cs == null) continue;
                Course c = new Course(name);
                c.setStart(cs.getLocation("start"));
                c.setEnd(cs.getLocation("end"));
                c.setSpawn(cs.getLocation("spawn"));
                if (cs.contains("fall-y")) c.setFallY(cs.getDouble("fall-y"));
                for (Object o : cs.getList("checkpoints", Collections.emptyList())) {
                    if (o instanceof Location loc) c.getCheckpoints().add(loc);
                }
                courses.put(key(name), c);
            }
        }

        YamlConfiguration ty = YamlConfiguration.loadConfiguration(timesFile);
        ConfigurationSection troot = ty.getConfigurationSection("times");
        if (troot != null) {
            for (String course : troot.getKeys(false)) {
                ConfigurationSection cs = troot.getConfigurationSection(course);
                if (cs == null) continue;
                Map<UUID, Entry> map = new HashMap<>();
                for (String id : cs.getKeys(false)) {
                    try {
                        UUID uuid = UUID.fromString(id);
                        map.put(uuid, new Entry(cs.getString(id + ".name", "?"), cs.getLong(id + ".ms")));
                    } catch (IllegalArgumentException ignored) {
                    }
                }
                times.put(key(course), map);
            }
        }
    }


    public void saveAll(boolean sync) {
        saveCourses(sync);
        saveTimes(sync);
    }

    public void saveCourses(boolean sync) {
        YamlConfiguration y = new YamlConfiguration();
        for (Course c : courses.values()) {
            String p = "courses." + c.getName();
            y.set(p + ".start", c.getStart());
            y.set(p + ".end", c.getEnd());
            y.set(p + ".spawn", c.getSpawn());
            y.set(p + ".fall-y", c.getRawFallY());
            y.set(p + ".checkpoints", new ArrayList<>(c.getCheckpoints()));
        }
        write(coursesFile, y.saveToString(), sync);
    }

    public void saveTimes(boolean sync) {
        YamlConfiguration y = new YamlConfiguration();
        for (Map.Entry<String, Map<UUID, Entry>> course : times.entrySet()) {
            for (Map.Entry<UUID, Entry> e : course.getValue().entrySet()) {
                String p = "times." + course.getKey() + "." + e.getKey();
                y.set(p + ".name", e.getValue().name());
                y.set(p + ".ms", e.getValue().ms());
            }
        }
        write(timesFile, y.saveToString(), sync);
    }

    private synchronized void writeNow(File file, String data) {
        try {
            File parent = file.getParentFile();
            if (parent != null) parent.mkdirs();
            Files.writeString(file.toPath(), data, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            plugin.getLogger().warning("No se pudo guardar " + file.getName() + ": " + ex.getMessage());
        }
    }

    private void write(File file, String data, boolean sync) {
        if (sync || !plugin.isEnabled()) {
            writeNow(file, data);
        } else {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> writeNow(file, data));
        }
    }


    public Course get(String name) { return courses.get(key(name)); }
    public Collection<Course> all() { return courses.values(); }
    public boolean exists(String name) { return courses.containsKey(key(name)); }

    public Course create(String name) {
        Course c = new Course(name);
        courses.put(key(name), c);
        saveCourses(false);
        return c;
    }

    public void delete(String name) {
        courses.remove(key(name));
        times.remove(key(name));
        saveAll(false);
    }


    public Entry best(String course, UUID id) {
        Map<UUID, Entry> m = times.get(key(course));
        return m == null ? null : m.get(id);
    }

    public boolean submit(String course, UUID id, String playerName, long ms) {
        Map<UUID, Entry> m = times.computeIfAbsent(key(course), k -> new HashMap<>());
        Entry old = m.get(id);
        if (old == null || ms < old.ms()) {
            m.put(id, new Entry(playerName, ms));
            saveTimes(false);
            return true;
        }
        return false;
    }

    public List<Entry> top(String course, int limit) {
        Map<UUID, Entry> m = times.get(key(course));
        if (m == null) return Collections.emptyList();
        List<Entry> list = new ArrayList<>(m.values());
        list.sort(Comparator.comparingLong(Entry::ms));
        return list.size() > limit ? list.subList(0, limit) : list;
    }

    public Entry record(String course) {
        List<Entry> t = top(course, 1);
        return t.isEmpty() ? null : t.get(0);
    }

    public void clearTimes(String course) {
        times.remove(key(course));
        saveTimes(false);
    }
}
