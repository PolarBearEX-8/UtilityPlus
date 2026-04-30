package com.example.utilityplus;

import com.example.utilityplus.commands.*;
import com.example.utilityplus.listeners.*;
import com.example.utilityplus.managers.*;
import com.example.utilityplus.tabcomplete.TabCompleterManager;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public class UtilityPlus extends JavaPlugin {

    private SpawnManager spawnManager;
    private HomeManager  homeManager;
    private TPAManager   tpaManager;
    private ChatManager  chatManager;
    private TeamManager  teamManager;
    private StatsManager statsManager;
    private TabListManager tabListManager;
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
        chatManager  = new ChatManager(this);
        teamManager  = new TeamManager(this);
        statsManager = new StatsManager(this);
        tabListManager = new TabListManager(this);
        tickMonitor  = new TickMonitor(this);
        cpuMonitor   = new CpuMonitor(this);

        // ── Executors ─────────────────────────────────────────────────
        // Spawn
        SpawnCommand spawnCmd = new SpawnCommand(spawnManager);
        registerCommands(spawnCmd, "setspawn", "spawn");

        // Home
        HomeCommand homeCmd = new HomeCommand(homeManager);
        registerCommands(homeCmd, "sethome", "home", "delhome");

        // TPA
        TPACommand tpaCmd = new TPACommand(tpaManager, this);
        registerCommands(tpaCmd, "tpa", "tpahere", "tpaccept", "tpdeny", "tpcancel", "tpaon", "tpaoff");

        // Chat
        ChatCommand chatCmd = new ChatCommand(chatManager);
        registerCommands(chatCmd, "chat", "chatsettings");

        // PM
        PMCommand pmCmd = new PMCommand(chatManager);
        registerCommands(pmCmd, "msg", "tell", "w", "whisper", "dm", "pm", "r", "reply", "l", "last");

        TwoBTwoTCommand twoBTwoTCommand = new TwoBTwoTCommand(this, chatManager);
        registerCommands(twoBTwoTCommand,
                "ignore", "ignorehard", "ignorelist", "ignoredeathmsgs",
                "togglechat", "toggleprivatemsgs", "toggledeathmsgs", "toggledeathmsgshard",
                "queue");

        // Team
        registerCommand("team", new TeamCommand(teamManager, chatManager));

        // Reload
        registerCommand("upreload", new ReloadCommand(this));

        // Vanish
        vanishCommand = new VanishCommand(this);
        registerCommand("v", vanishCommand);

        // Broadcast
        BroadcastCommand bcCmd = new BroadcastCommand(this);
        registerCommands(bcCmd, "bc", "broadcast");

        // Gamemode
        GamemodeCommand gmCmd = new GamemodeCommand();
        registerCommands(gmCmd, "gmc", "gms", "gmsp", "gma");

        // Kill
        registerCommand("kill", new KillCommand(this));

        // Summon
        registerCommand("s", new STapwarp());

        // Help
        registerCommand("helps", new HelpsCommand(this));

        // Stats
        StatsCommand statsCmd = new StatsCommand(statsManager);
        registerCommands(statsCmd, "stats", "topstats");

        // TPS More
        TPSMoreCommand tpsMoreCmd = new TPSMoreCommand(tickMonitor, cpuMonitor);
        registerCommands(tpsMoreCmd, "tpsmore", "tps");

        // ── Tab Completers ────────────────────────────────────────────
        TabCompleterManager tab = new TabCompleterManager(homeManager, teamManager);
        List<String> allCmds = List.of(
            "setspawn","spawn",
            "sethome","home","delhome",
            "tpa","tpahere","tpaccept","tpdeny","tpcancel","tpaon","tpaoff",
            "chat","chatsettings",
            "msg","tell","w","whisper","dm","pm","r","reply","l","last",
            "ignore","ignorehard","ignorelist","ignoredeathmsgs",
            "togglechat","toggleprivatemsgs","toggledeathmsgs","toggledeathmsgshard","queue",
            "team","upreload",
            "v","bc","broadcast","gmc","gms","gmsp","gma","kill","s","helps",
            "stats", "topstats", "tpsmore", "tps"
        );
        registerTabCompleters(tab, allCmds);

        // ── Listeners ─────────────────────────────────────────────────
        getServer().getPluginManager().registerEvents(new SpawnListener(spawnManager), this);
        getServer().getPluginManager().registerEvents(new HomeGUIListener(homeManager), this);
        getServer().getPluginManager().registerEvents(new TPAListener(tpaManager), this);
        getServer().getPluginManager().registerEvents(new ChatListener(chatManager, teamManager), this);
        getServer().getPluginManager().registerEvents(new AnvilListener(), this);
        getServer().getPluginManager().registerEvents(new TeamListener(teamManager), this);
        getServer().getPluginManager().registerEvents(new JoinMessageListener(this), this);
        getServer().getPluginManager().registerEvents(new StatsListener(statsManager, chatManager, this), this);
        getServer().getPluginManager().registerEvents(new StatsGUIListener(), this);
        getServer().getPluginManager().registerEvents(new TabListListener(this, tabListManager), this);
        getServer().getPluginManager().registerEvents(new VanishListener(this, vanishCommand), this);

        getLogger().info("UtilityPlus enabled!");
    }

    @Override
    public void onDisable() {
        if (spawnManager != null) spawnManager.saveData();
        if (homeManager  != null) homeManager.saveData();
        if (chatManager != null) chatManager.saveData();
        if (teamManager  != null) teamManager.saveData();
        if (statsManager != null) statsManager.saveData();
        if (tabListManager != null) tabListManager.stop();
        if (vanishCommand != null) vanishCommand.saveData();
        getLogger().info("UtilityPlus disabled!");
    }

    public SpawnManager getSpawnManager() { return spawnManager; }
    public HomeManager  getHomeManager()  { return homeManager; }
    public TPAManager   getTpaManager()   { return tpaManager; }
    public ChatManager  getChatManager()  { return chatManager; }
    public TeamManager  getTeamManager()  { return teamManager; }
    public StatsManager getStatsManager() { return statsManager; }
    public TabListManager getTabListManager() { return tabListManager; }

    private void registerCommands(CommandExecutor executor, String... names) {
        for (String name : names) {
            registerCommand(name, executor);
        }
    }

    private void registerCommand(String name, CommandExecutor executor) {
        command(name).setExecutor(executor);
    }

    private void registerTabCompleters(TabCompleter completer, List<String> names) {
        for (String name : names) {
            command(name).setTabCompleter(completer);
        }
    }

    private PluginCommand command(String name) {
        PluginCommand command = getCommand(name);
        if (command == null) {
            throw new IllegalStateException("Command '/" + name + "' is missing from plugin.yml");
        }
        return command;
    }
}
