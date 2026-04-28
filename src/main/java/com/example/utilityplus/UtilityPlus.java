package com.example.utilityplus;

import com.example.utilityplus.commands.*;
import com.example.utilityplus.listeners.*;
import com.example.utilityplus.managers.*;
import com.example.utilityplus.listeners.JoinMessageListener;
import com.example.utilityplus.tabcomplete.TabCompleterManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;
import java.util.List;

public class UtilityPlus extends JavaPlugin {

    private SpawnManager spawnManager;
    private HomeManager  homeManager;
    private TPAManager   tpaManager;
    private ChatManager  chatManager;
    private TeamManager  teamManager;
    private StatsManager statsManager;
    private VanishCommand vanishCommand;
    private TickMonitor tickMonitor;
    private CpuMonitor  cpuMonitor;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        // ── Managers ─────────────────────────────────────────────────
        spawnManager = new SpawnManager(this);
        homeManager  = new HomeManager(this);
        tpaManager   = new TPAManager(this);
        chatManager  = new ChatManager();
        teamManager  = new TeamManager(this);
        statsManager = new StatsManager(this);
        tickMonitor  = new TickMonitor(this);
        cpuMonitor   = new CpuMonitor(this);

        // ── Executors ─────────────────────────────────────────────────
        // Spawn
        SpawnCommand spawnCmd = new SpawnCommand(spawnManager);
        getCommand("setspawn").setExecutor(spawnCmd);
        getCommand("spawn").setExecutor(spawnCmd);

        // Home
        HomeCommand homeCmd = new HomeCommand(homeManager);
        getCommand("sethome").setExecutor(homeCmd);
        getCommand("home").setExecutor(homeCmd);
        getCommand("delhome").setExecutor(homeCmd);

        // TPA
        TPACommand tpaCmd = new TPACommand(tpaManager, this);
        getCommand("tpa").setExecutor(tpaCmd);
        getCommand("tpahere").setExecutor(tpaCmd);
        getCommand("tpaccept").setExecutor(tpaCmd);
        getCommand("tpdeny").setExecutor(tpaCmd);
        getCommand("tpcancel").setExecutor(tpaCmd);
        getCommand("tpaon").setExecutor(tpaCmd);
        getCommand("tpaoff").setExecutor(tpaCmd);

        // Chat
        ChatCommand chatCmd = new ChatCommand(chatManager);
        getCommand("chat").setExecutor(chatCmd);
        getCommand("chatsettings").setExecutor(chatCmd);

        // PM
        PMCommand pmCmd = new PMCommand(chatManager);
        for (String c : Arrays.asList("msg","tell","w","whisper","dm","pm","r","reply")) {
            getCommand(c).setExecutor(pmCmd);
        }

        // Team
        getCommand("team").setExecutor(new TeamCommand(teamManager, chatManager));

        // Reload
        getCommand("upreload").setExecutor(new ReloadCommand(this));

        // Vanish
        vanishCommand = new VanishCommand(this);
        getCommand("v").setExecutor(vanishCommand);

        // Broadcast
        BroadcastCommand bcCmd = new BroadcastCommand(this);
        getCommand("bc").setExecutor(bcCmd);
        getCommand("broadcast").setExecutor(bcCmd);

        // Gamemode
        GamemodeCommand gmCmd = new GamemodeCommand();
        getCommand("gmc").setExecutor(gmCmd);
        getCommand("gms").setExecutor(gmCmd);
        getCommand("gmsp").setExecutor(gmCmd);
        getCommand("gma").setExecutor(gmCmd);

        // Kill
        getCommand("kill").setExecutor(new KillCommand(this));

        // Summon
        getCommand("s").setExecutor(new STapwarp());

        // Help
        getCommand("helps").setExecutor(new HelpsCommand(this));

        // Stats
        StatsCommand statsCmd = new StatsCommand(statsManager);
        getCommand("stats").setExecutor(statsCmd);
        getCommand("topstats").setExecutor(statsCmd);

        // TPS More
        TPSMoreCommand tpsMoreCmd = new TPSMoreCommand(tickMonitor, cpuMonitor);
        getCommand("tpsmore").setExecutor(tpsMoreCmd);
        getCommand("tps").setExecutor(tpsMoreCmd);

        // ── Tab Completers ────────────────────────────────────────────
        TabCompleterManager tab = new TabCompleterManager(homeManager, teamManager);
        List<String> allCmds = Arrays.asList(
            "setspawn","spawn",
            "sethome","home","delhome",
            "tpa","tpahere","tpaccept","tpdeny","tpcancel","tpaon","tpaoff",
            "chat","chatsettings",
            "msg","tell","w","whisper","dm","pm","r","reply",
            "team","upreload",
            "v","bc","broadcast","gmc","gms","gmsp","gma","kill","s","helps",
            "stats", "topstats", "tpsmore", "tps"
        );
        for (String c : allCmds) getCommand(c).setTabCompleter(tab);

        // ── Listeners ─────────────────────────────────────────────────
        getServer().getPluginManager().registerEvents(new SpawnListener(spawnManager), this);
        getServer().getPluginManager().registerEvents(new HomeGUIListener(homeManager), this);
        getServer().getPluginManager().registerEvents(new TPAListener(tpaManager), this);
        getServer().getPluginManager().registerEvents(new ChatListener(chatManager, teamManager), this);
        getServer().getPluginManager().registerEvents(new AnvilListener(), this);
        getServer().getPluginManager().registerEvents(new TeamListener(teamManager), this);
        getServer().getPluginManager().registerEvents(new JoinMessageListener(this), this);
        getServer().getPluginManager().registerEvents(new StatsListener(statsManager), this);
        getServer().getPluginManager().registerEvents(new StatsGUIListener(), this);
        getServer().getPluginManager().registerEvents(new VanishListener(this, vanishCommand), this);

        getLogger().info("UtilityPlus enabled!");
    }

    @Override
    public void onDisable() {
        if (spawnManager != null) spawnManager.saveData();
        if (homeManager  != null) homeManager.saveData();
        if (teamManager  != null) teamManager.saveData();
        if (statsManager != null) statsManager.saveData();
        getLogger().info("UtilityPlus disabled!");
    }

    public SpawnManager getSpawnManager() { return spawnManager; }
    public HomeManager  getHomeManager()  { return homeManager; }
    public TPAManager   getTpaManager()   { return tpaManager; }
    public ChatManager  getChatManager()  { return chatManager; }
    public TeamManager  getTeamManager()  { return teamManager; }
    public StatsManager getStatsManager() { return statsManager; }
}
