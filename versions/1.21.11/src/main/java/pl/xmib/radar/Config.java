package pl.xmib.radar;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

public class Config {
    // zakladka Radar
    public boolean radarEnabled = true;
    public int range = 128;            // kratki (16-128)
    public boolean chatMsg = true;
    public boolean overlay = true;
    public boolean leaveInfo = true;
    public boolean sound = true;
    public int soundType = 0;
    public int volume = 100;

    // zakladka Telefon
    public boolean phoneEnabled = true;
    public int imageMode = 0;          // 0 karta gracza, 1 screenshot, 2 brak
    public boolean onlyBackground = false;
    public int cooldownSeconds = 60;   // ten sam gracz najwyzej co X s
    public int maxNearbyForPush = 30;  // powyzej tylu graczy w poblizu NIE wysylamy powiadomien na telefon
    public String ntfyServer = "https://ntfy.sh";
    public String channel = "";
    public List<String> ignoreNames = new ArrayList<>(); // znajomi - bez powiadomien

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("xmibradar.json");
    private static Config instance = new Config();

    public static Config get() { return instance; }

    public String ntfyUrl() {
        String s = ntfyServer.trim();
        while (s.endsWith("/")) s = s.substring(0, s.length() - 1);
        return s + "/" + channel;
    }

    /** Kanal w formacie: nickwminecraft-xmib-radar-cyferki */
    public static String randomChannel() {
        String nick = "gracz";
        try { nick = Compat.username(MinecraftClient.getInstance()); } catch (Throwable ignored) {}
        if (nick == null || nick.isBlank()) nick = "gracz";
        nick = nick.replaceAll("[^A-Za-z0-9_]", "_");
        SecureRandom r = new SecureRandom();
        StringBuilder sb = new StringBuilder(nick).append("-xmib-radar-");
        for (int i = 0; i < 8; i++) sb.append(r.nextInt(10));
        return sb.toString();
    }

    public static void load() {
        try {
            if (Files.exists(FILE)) {
                Config c = GSON.fromJson(Files.readString(FILE), Config.class);
                if (c != null) instance = c;
            }
        } catch (Exception e) {
            System.err.println("[xMIBRadar] config load failed: " + e);
        }
        if (instance.channel == null || instance.channel.isBlank() || instance.channel.startsWith("radar-"))
            instance.channel = randomChannel();
        if (instance.ntfyServer == null || instance.ntfyServer.isBlank()) instance.ntfyServer = "https://ntfy.sh";
        if (instance.ignoreNames == null) instance.ignoreNames = new ArrayList<>();
        save();
    }

    public static void save() {
        try {
            Files.writeString(FILE, GSON.toJson(instance));
        } catch (Exception e) {
            System.err.println("[xMIBRadar] config save failed: " + e);
        }
    }
}
