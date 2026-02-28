package com.example.guildbot;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import com.google.gson.JsonObject;
import java.net.URI;

public class GuildBotPlugin extends JavaPlugin implements Listener {
    private WebSocketClientImpl wsClient;
    private String wsUrl = "ws://localhost:3000";

    @Override
    public void onEnable() {
        saveDefaultConfig();
        if (getConfig().contains("ws-url")) {
            wsUrl = getConfig().getString("ws-url");
        } else {
            getConfig().set("ws-url", wsUrl);
            saveConfig();
        }

        getServer().getPluginManager().registerEvents(this, this);
        connectWebSocket();
        getLogger().info("GuildBot Plugin Enabled!");
    }

    @Override
    public void onDisable() {
        if (wsClient != null) {
            wsClient.close();
        }
    }

    private void connectWebSocket() {
        try {
            wsClient = new WebSocketClientImpl(new URI(wsUrl), this);
            wsClient.connect();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (wsClient != null && wsClient.isOpen()) {
            JsonObject json = new JsonObject();
            json.addProperty("type", "PLAYER_JOIN");
            json.addProperty("username", player.getName());
            wsClient.send(json.toString());
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player player = (Player) sender;

        if (command.getName().equalsIgnoreCase("getmycode")) {
            if (wsClient != null && wsClient.isOpen()) {
                JsonObject json = new JsonObject();
                json.addProperty("type", "GET_CODE");
                json.addProperty("username", player.getName());
                wsClient.send(json.toString());
            }
            return true;
        }
        
        if (command.getName().equalsIgnoreCase("invite")) {
            if (args.length == 1) {
                JsonObject json = new JsonObject();
                json.addProperty("type", "INVITE");
                json.addProperty("inviter", player.getName());
                json.addProperty("invitee", args[0]);
                wsClient.send(json.toString());
                player.sendMessage(ChatColor.GREEN + "Invite sent to " + args[0]);
            } else {
                player.sendMessage(ChatColor.RED + "Usage: /invite <player>");
            }
            return true;
        }

        if (command.getName().equalsIgnoreCase("join")) {
            if (args.length == 1) {
                JsonObject json = new JsonObject();
                json.addProperty("type", "JOIN");
                json.addProperty("username", player.getName());
                json.addProperty("guildName", args[0]);
                wsClient.send(json.toString());
            } else {
                player.sendMessage(ChatColor.RED + "Usage: /join <guildname>");
            }
            return true;
        }

        return false;
    }

    public void handleMessage(JsonObject json) {
        String type = json.get("type").getAsString();
        
        if (type.equals("CODE_GENERATED")) {
            String username = json.get("username").getAsString();
            String code = json.get("code").getAsString();
            Player player = Bukkit.getPlayer(username);
            if (player != null) {
                player.sendMessage(ChatColor.STRIKETHROUGH + "------------------------------------");
                player.sendMessage(ChatColor.GOLD + "--------GUILD VERSION 1.0--------");
                player.sendMessage(ChatColor.YELLOW + "--------Your Verificatio code-------");
                player.sendMessage(ChatColor.AQUA + "-------------" + code + "-----------------");
                player.sendMessage(ChatColor.RED + "-----Don't Share with anyone------");
                player.sendMessage(ChatColor.STRIKETHROUGH + "------------------------------------");
            }
        }
        
        if (type.equals("VERIFIED")) {
            String username = json.get("username").getAsString();
            Player player = Bukkit.getPlayer(username);
            if (player != null) {
                player.sendMessage(ChatColor.GREEN + "You have been successfully verified!");
            }
        }
        
        if (type.equals("INVITE_RECEIVED")) {
            String invitee = json.get("invitee").getAsString();
            String guildName = json.get("guildName").getAsString();
            Player player = Bukkit.getPlayer(invitee);
            if (player != null) {
                player.sendMessage(ChatColor.GREEN + "You have been invited to join guild: " + guildName);
                player.sendMessage(ChatColor.AQUA + "Type /join " + guildName + " to accept.");
            }
        }

        if (type.equals("JOIN_SUCCESS")) {
            String username = json.get("username").getAsString();
            String guildName = json.get("guildName").getAsString();
            Player player = Bukkit.getPlayer(username);
            if (player != null) {
                player.sendMessage(ChatColor.GREEN + "You have successfully joined the guild: " + guildName);
            }
        }

        if (type.equals("PRE_APPROVED")) {
            String username = json.get("username").getAsString();
            String guildName = json.get("guildName").getAsString();
            Player player = Bukkit.getPlayer(username);
            if (player != null) {
                player.sendMessage(ChatColor.YELLOW + "You have requested to join " + guildName + ". If the owner invites you within 6 hours, you will join automatically.");
            }
        }

        if (type.equals("ERROR")) {
            String username = json.get("username").getAsString();
            String message = json.get("message").getAsString();
            Player player = Bukkit.getPlayer(username);
            if (player != null) {
                player.sendMessage(ChatColor.RED + "Error: " + message);
            }
        }
    }
}
