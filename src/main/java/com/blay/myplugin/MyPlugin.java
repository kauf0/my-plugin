package com.blay.myplugin;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.Server;
import org.bukkit.plugin.PluginManager;
import org.bukkit.event.Event;
import org.bukkit.util.config.Configuration;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.command.CommandSender;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerListener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.Random;
import java.io.File;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.FileWriter;
import java.util.ArrayList;

public class MyPlugin extends JavaPlugin {
    public static class OnPlayerJoinListener extends PlayerListener {
        public void onPlayerJoin(PlayerJoinEvent event) {
            Player player = event.getPlayer();
            if (player.getName().equalsIgnoreCase("CONSOLE")) {
                player.kickPlayer("§cInvalid username!");
            } else if (MyPlugin.onJoinMessages.length > 0) {
                player.sendMessage(MyPlugin.onJoinMessages[MyPlugin.random.nextInt(MyPlugin.onJoinMessages.length)].replace("%%", "\uf420").replace("%p", player.getDisplayName()).replace('\uf420', '\\'));
            }
        }
    }

    public static class BtwMessagesTask implements Runnable {
        public void run() {
            String message = MyPlugin.btwMessages[MyPlugin.random.nextInt(MyPlugin.btwMessages.length)];
            for (Player player : MyPlugin.server.getOnlinePlayers()) {
                player.sendMessage(message);
            }
        }
    }

    private static Server server;
    private static final Random random = new Random();

    private String[] infoLines;
    private static String[] onJoinMessages;
    private static String[] btwMessages;
    private int btwMessagesTaskId = -1;

    private void reloadConfig() {
        File pluginFolder = new File("./plugins/MyPlugin");
        if (!pluginFolder.exists()) {
            pluginFolder.mkdirs();
        }

        File configFile = new File(pluginFolder, "config.yml");
        if (!configFile.exists()) {
            StringBuilder defaultConfig = new StringBuilder();

            try {
                InputStreamReader defaultConfigReader = new InputStreamReader(getClass().getResourceAsStream("/config.yml"));
                for (int character = defaultConfigReader.read(); character != -1; character = defaultConfigReader.read()) {
                    defaultConfig.append((char)character);
                }
                defaultConfigReader.close();
            } catch (IOException e) {
                throw new RuntimeException("Failed to read the default config for MyPlugin: " + e);
            }

            try {
                FileWriter configFileWriter = new FileWriter(configFile);
                configFileWriter.write(defaultConfig.toString());
                configFileWriter.close();
            } catch (IOException e) {
                throw new RuntimeException("Failed to write the default config for MyPlugin: " + e);
            }
        }

        Configuration config = new Configuration(configFile);
        config.load();

        infoLines = config.getStringList("infoLines", new ArrayList<>()).toArray(new String[0]);
        onJoinMessages = config.getStringList("onJoinMessages", new ArrayList<>()).toArray(new String[0]);

        String[] btwMessages = config.getStringList("btwMessages", new ArrayList<>()).toArray(new String[0]);
        long btwMessagesInterval = config.getInt("btwMessagesInterval", 0);

        BukkitScheduler scheduler = server.getScheduler();
        if (btwMessagesTaskId != -1) {
            scheduler.cancelTask(btwMessagesTaskId);
        }
        if (!(btwMessages.length == 0 || btwMessagesInterval < 1)) {
            btwMessagesTaskId = scheduler.scheduleSyncRepeatingTask(this, new BtwMessagesTask(), 0, btwMessagesInterval);
            MyPlugin.btwMessages = btwMessages;
        }
    }

    public void onEnable() {
        server = getServer();
        server.getLogger().info("[MyPlugin] Siema!");

        reloadConfig();

        BlockLogger blockLogger = new BlockLogger();

        PluginManager pluginManager = server.getPluginManager();

        pluginManager.registerEvent(Event.Type.BLOCK_BREAK, blockLogger, Event.Priority.Normal, this);
        pluginManager.registerEvent(Event.Type.BLOCK_BURN, blockLogger, Event.Priority.Normal, this);
        pluginManager.registerEvent(Event.Type.BLOCK_FADE, blockLogger, Event.Priority.Normal, this);
        pluginManager.registerEvent(Event.Type.LEAVES_DECAY, blockLogger, Event.Priority.Normal, this);
        pluginManager.registerEvent(Event.Type.BLOCK_PLACE, blockLogger, Event.Priority.Normal, this);

        pluginManager.registerEvent(Event.Type.PLAYER_INTERACT, new BlockLogger.OnPlayerInteractListener(), Event.Priority.Normal, this);
        pluginManager.registerEvent(Event.Type.WORLD_SAVE, new BlockLogger.OnWorldSaveListener(), Event.Priority.Normal, this);

        pluginManager.registerEvent(Event.Type.PLAYER_JOIN, new OnPlayerJoinListener(), Event.Priority.Normal, this);
    }

    public void onDisable() {}

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        switch (label) {
            case "blocklog":
            case "blocklogger":
            case "inspect":
                BlockLogger.handleCommand(sender, args, server);
                break;
            case "players":
            case "list":
            case "kto":
                ArrayList<String> toSend = new ArrayList<>();

                Player[] players = server.getOnlinePlayers();
                if (players.length == 0) {
                    toSend.add("There currently are no players online.");
                } else {
                    if (label.equals("kto")) {
                        toSend.add("Sposrod " + players.length + " osob przebywajacych obecnie w swiecie Arkadii, znane tobie to:");
                    } else {
                        toSend.add("Online players (" + players.length + "):");
                    }
                    for (int i = 0; i < players.length - 1; i++) {
                        toSend.add(players[i].getName() + ",");
                    }
                    toSend.add(players[players.length - 1].getName());
                }

                for (String line : toSend) {
                    sender.sendMessage(line);
                }
                break;
            case "myplugin-reload":
            case "reload-myplugin":
                if (sender.isOp()) {
                    server.broadcast("[MyPlugin] (" + sender.getName() + ") Reloading plugin config...", server.BROADCAST_CHANNEL_ADMINISTRATIVE);
                    reloadConfig();
                    server.broadcast("[MyPlugin] (" + sender.getName() + ") Reloaded plugin config successfully!", server.BROADCAST_CHANNEL_ADMINISTRATIVE);
                } else {
                    sender.sendMessage("§cYou do not have the permission to do that!");
                }
                break;
            default:
                for (String line : infoLines) {
                    sender.sendMessage(line);
                }
        }
        return true;
    }
}
