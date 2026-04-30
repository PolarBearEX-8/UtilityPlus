package com.example.utilityplus.managers;

import com.example.utilityplus.UtilityPlus;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.HeightMap;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class SpawnManager {

    private final UtilityPlus plugin;
    private File dataFile;
    private FileConfiguration dataConfig;
    private Location spawnLocation;

    // If the world wasn't loaded yet during onEnable, we store its name here
    // and resolve it lazily on first use or via WorldLoadEvent
    private String pendingWorldName = null;

    // Config flags
    private boolean tpOnFirstJoin;
    private boolean tpOnDeath;
    private boolean tpNoRespawnPoint;
    private int cooldownSeconds;
    private int warmupSeconds;

    // Random Spawn Settings
    private boolean rtpEnabled;
    private int rtpMinRadius;
    private int rtpMaxRadius;
    private int rtpAttempts;
    private String rtpWorldName;

    private static final Set<Material> UNSAFE_RANDOM_RESPAWN_GROUND = EnumSet.of(
            Material.CACTUS,
            Material.CAMPFIRE,
            Material.FIRE,
            Material.LAVA,
            Material.MAGMA_BLOCK,
            Material.POWDER_SNOW,
            Material.SOUL_CAMPFIRE,
            Material.SOUL_FIRE,
            Material.SWEET_BERRY_BUSH,
            Material.WATER
    );

    // Cooldown tracker: UUID -> last /spawn use timestamp (ms)
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    // Warmup task tracker: UUID -> active warmup task
    private final Map<UUID, ScheduledTask> warmupTasks = new HashMap<>();

    // Tracks players who have joined before (persisted in spawn.yml)
    // We store them as a list so first-join detection survives restarts
    private final java.util.Set<UUID> knownPlayers = new java.util.HashSet<>();

    public SpawnManager(UtilityPlus plugin) {
        this.plugin = plugin;
        readConfig();
        loadData();
    }

    private void readConfig() {
        FileConfiguration cfg = plugin.getConfig();
        this.tpOnFirstJoin    = cfg.getBoolean("spawn.tp-spawn-first-join",      true);
        this.tpOnDeath        = cfg.getBoolean("spawn.tp-spawn-on-death",         true);
        this.tpNoRespawnPoint = cfg.getBoolean("spawn.tp-spawn-no-respawn-point", true);
        this.cooldownSeconds  = cfg.getInt    ("spawn.tp-spawn-cooldown",         30);
        this.warmupSeconds    = cfg.getInt    ("spawn.tp-spawn-warmup",           5);

        this.rtpEnabled   = cfg.getBoolean("random-respawn.enabled", true);
        this.rtpMinRadius = Math.max(0, cfg.getInt("random-respawn.min-radius", 50));
        this.rtpMaxRadius = Math.max(rtpMinRadius, cfg.getInt("random-respawn.max-radius", 1000));
        this.rtpAttempts  = Math.max(1, cfg.getInt("random-respawn.attempts", 48));
        this.rtpWorldName = cfg.getString("random-respawn.world", "world");
    }

    // ---------------------------------------------------------------
    // Data persistence
    // ---------------------------------------------------------------

    private void loadData() {
        dataFile = new File(plugin.getDataFolder(), "spawn.yml");
        if (!dataFile.exists()) {
            try {
                plugin.getDataFolder().mkdirs();
                dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("[SpawnManager] Could not create spawn.yml!");
            }
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);

        // Load spawn location
        if (dataConfig.contains("spawn.world")) {
            String worldName = dataConfig.getString("spawn.world");
            World world = Bukkit.getWorld(worldName);
            if (world != null) {
                spawnLocation = buildLocation(world);
                plugin.getLogger().info("[SpawnManager] Spawn loaded at " + worldName);
            } else {
                // World not loaded yet — save name and resolve later
                pendingWorldName = worldName;
                plugin.getLogger().warning("[SpawnManager] World '" + worldName
                        + "' not loaded yet — spawn will be resolved when the world loads.");
            }
        }

        // Load known players (for first-join detection)
        if (dataConfig.contains("known-players")) {
            for (String uuidStr : dataConfig.getStringList("known-players")) {
                try { knownPlayers.add(UUID.fromString(uuidStr)); }
                catch (IllegalArgumentException ignored) {}
            }
        }
    }

    /**
     * Called by SpawnListener on WorldLoadEvent.
     * Resolves the spawn location if the world was not ready during onEnable.
     */
    public boolean tryResolvePendingWorld(String loadedWorldName) {
        if (pendingWorldName == null) return false;
        if (!pendingWorldName.equalsIgnoreCase(loadedWorldName)) return false;

        World world = Bukkit.getWorld(loadedWorldName);
        if (world == null) return false;

        spawnLocation = buildLocation(world);
        pendingWorldName = null;
        plugin.getLogger().info("[SpawnManager] Spawn location resolved after world load: " + loadedWorldName);
        return true;
    }

    public boolean hasPendingWorld() {
        return pendingWorldName != null;
    }

    private Location buildLocation(World world) {
        return new Location(
                world,
                dataConfig.getDouble("spawn.x"),
                dataConfig.getDouble("spawn.y"),
                dataConfig.getDouble("spawn.z"),
                (float) dataConfig.getDouble("spawn.yaw"),
                (float) dataConfig.getDouble("spawn.pitch")
        );
    }

    public void saveData() {
        if (spawnLocation != null) {
            dataConfig.set("spawn.world", spawnLocation.getWorld().getName());
            dataConfig.set("spawn.x",     spawnLocation.getX());
            dataConfig.set("spawn.y",     spawnLocation.getY());
            dataConfig.set("spawn.z",     spawnLocation.getZ());
            dataConfig.set("spawn.yaw",   (double) spawnLocation.getYaw());
            dataConfig.set("spawn.pitch", (double) spawnLocation.getPitch());
        }

        // Persist known-players list
        java.util.List<String> uuidList = new java.util.ArrayList<>();
        for (UUID uuid : knownPlayers) uuidList.add(uuid.toString());
        dataConfig.set("known-players", uuidList);

        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("[SpawnManager] Could not save spawn.yml!");
        }
    }

    // ---------------------------------------------------------------
    // Spawn location
    // ---------------------------------------------------------------

    public void setSpawn(Location location) {
        this.spawnLocation = location;
        saveData();
    }

    public Location getSpawn() {
        return spawnLocation;
    }

    public boolean hasSpawn() {
        return spawnLocation != null;
    }

    // ---------------------------------------------------------------
    // Cooldown
    // ---------------------------------------------------------------

    public boolean isOnCooldown(UUID uuid) {
        return getCooldownSecondsLeft(uuid) > 0;
    }

    public long getCooldownSecondsLeft(UUID uuid) {
        Long last = cooldowns.get(uuid);
        if (last == null || cooldownSeconds <= 0) return 0;
        long elapsed = (System.currentTimeMillis() - last) / 1000L;
        return Math.max(0L, cooldownSeconds - elapsed);
    }

    public void applyCooldown(UUID uuid) {
        if (cooldownSeconds > 0) {
            cooldowns.put(uuid, System.currentTimeMillis());
        }
    }

    public int getCooldownSeconds() {
        return cooldownSeconds;
    }

    public int getWarmupSeconds() {
        return warmupSeconds;
    }

    // ---------------------------------------------------------------
    // Warmup task management
    // ---------------------------------------------------------------

    public void startWarmup(UUID uuid, ScheduledTask task) {
        cancelWarmup(uuid);
        warmupTasks.put(uuid, task);
    }

    public void cancelWarmup(UUID uuid) {
        ScheduledTask t = warmupTasks.remove(uuid);
        if (t != null) t.cancel();
    }

    public boolean hasWarmup(UUID uuid) {
        return warmupTasks.containsKey(uuid);
    }

    public UtilityPlus getPlugin() {
        return plugin;
    }

    /** Called by ReloadCommand — re-reads spawn.yml and config values. */
    public void reload() {
        spawnLocation   = null;
        pendingWorldName = null;
        knownPlayers.clear();
        readConfig();
        loadData();
        plugin.getLogger().info("[SpawnManager] Reloaded.");
    }

    // ---------------------------------------------------------------
    // Config flag getters
    // ---------------------------------------------------------------

    public boolean isTpOnFirstJoin()    { return tpOnFirstJoin; }
    public boolean isTpOnDeath()        { return tpOnDeath; }
    public boolean isTpNoRespawnPoint() { return tpNoRespawnPoint; }
    public boolean isRtpEnabled()       { return rtpEnabled; }

    public Location getRandomLocation() {
        World world = Bukkit.getWorld(rtpWorldName);
        if (world == null) {
            world = Bukkit.getWorlds().get(0);
        }

        Location center = getRandomRespawnCenter(world);
        ThreadLocalRandom random = ThreadLocalRandom.current();

        for (int attempt = 0; attempt < rtpAttempts; attempt++) {
            // Polar coordinates for uniform distribution in the ring
            // θ: 0 to 2π, r: rtpMinRadius to rtpMaxRadius
            double theta = random.nextDouble(0, Math.PI * 2);
            double r = randomRadius(random);

            int x = center.getBlockX() + (int) Math.round(r * Math.cos(theta));
            int z = center.getBlockZ() + (int) Math.round(r * Math.sin(theta));
            if (!isInsideWorldBorder(world, x, z)) {
                continue;
            }

            Location loc = world.getHighestBlockAt(x, z, HeightMap.MOTION_BLOCKING_NO_LEAVES)
                    .getLocation()
                    .add(0.5, 1, 0.5);

            if (isSafeLocation(loc)) {
                loc.setYaw(random.nextFloat() * 360.0F);
                loc.setPitch(0.0F);
                return loc;
            }
        }

        return spawnLocation != null ? spawnLocation : world.getSpawnLocation();
    }

    private Location getRandomRespawnCenter(World world) {
        if (spawnLocation != null && spawnLocation.getWorld() != null
                && spawnLocation.getWorld().getName().equals(world.getName())) {
            return spawnLocation;
        }

        return world.getSpawnLocation();
    }

    private double randomRadius(ThreadLocalRandom random) {
        if (rtpMaxRadius <= rtpMinRadius) {
            return rtpMinRadius;
        }

        double minSquared = (double) rtpMinRadius * rtpMinRadius;
        double maxSquared = (double) rtpMaxRadius * rtpMaxRadius;
        return Math.sqrt(random.nextDouble(minSquared, maxSquared));
    }

    private boolean isInsideWorldBorder(World world, int x, int z) {
        WorldBorder border = world.getWorldBorder();
        Location center = border.getCenter();
        double radius = border.getSize() / 2.0D;

        return x + 0.5D >= center.getX() - radius
                && x + 0.5D <= center.getX() + radius
                && z + 0.5D >= center.getZ() - radius
                && z + 0.5D <= center.getZ() + radius;
    }

    private boolean isSafeLocation(Location loc) {
        World world = loc.getWorld();
        if (world == null) {
            return false;
        }

        int y = loc.getBlockY();
        if (y <= world.getMinHeight() || y + 1 >= world.getMaxHeight()) {
            return false;
        }

        Block feet = loc.getBlock();
        Block head = loc.clone().add(0, 1, 0).getBlock();
        Block ground = loc.clone().add(0, -1, 0).getBlock();
        Material groundType = ground.getType();

        return feet.isPassable()
                && head.isPassable()
                && groundType.isSolid()
                && !ground.isLiquid()
                && !UNSAFE_RANDOM_RESPAWN_GROUND.contains(groundType);
    }

    // ---------------------------------------------------------------
    // First-join tracking
    // ---------------------------------------------------------------

    /** Returns true if this is the player's first time joining the server. */
    public boolean isFirstJoin(UUID uuid) {
        return !knownPlayers.contains(uuid);
    }

    /** Mark a player as having joined before. */
    public void markKnown(UUID uuid) {
        if (knownPlayers.add(uuid)) {
            saveData(); // persist immediately so restarts don't re-trigger
        }
    }
}
