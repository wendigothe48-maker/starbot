package com.example.guildbot;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import com.google.gson.JsonParser;
import com.google.gson.JsonObject;
import org.bukkit.Bukkit;
import java.net.URI;

public class WebSocketClientImpl extends WebSocketClient {
    private GuildBotPlugin plugin;

    public WebSocketClientImpl(URI serverUri, GuildBotPlugin plugin) {
        super(serverUri);
        this.plugin = plugin;
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        plugin.getLogger().info("Connected to WebSocket server");
    }

    @Override
    public void onMessage(String message) {
        try {
            JsonObject json = JsonParser.parseString(message).getAsJsonObject();
            Bukkit.getScheduler().runTask(plugin, () -> plugin.handleMessage(json));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        plugin.getLogger().warning("WebSocket closed: " + reason);
    }

    @Override
    public void onError(Exception ex) {
        ex.printStackTrace();
    }
}
