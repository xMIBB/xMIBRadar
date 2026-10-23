package pl.xmib.radar;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.*;

/** Skanuje graczy w poblizu, odpala alerty lokalne (czat, dzwiek) i powiadomienia na telefon. */
public class Tracker {
    public record Nearby(UUID id, String name, double dist, double right, double fwd, int dir, AbstractClientPlayerEntity player) {}

    public static final String[] SHORT = {"N", "NE", "E", "SE", "S", "SW", "W", "NW"};
    public static final String[] LONG = {"północ", "północny wschód", "wschód", "południowy wschód",
            "południe", "południowy zachód", "zachód", "północny zachód"};
    public static final String[] SOUND_NAMES = {"Pling", "Dzwonek", "Chime", "Orb", "Ding"};

    private static volatile List<Nearby> nearby = List.of();
    private static final Set<UUID> present = new HashSet<>();
    private static final Map<UUID, String> names = new HashMap<>();
    private static final Map<UUID, Long> lastPush = new HashMap<>();
    private static Object lastWorld;
    private static long primeUntil = 0;
    private static long lastAnyPush = 0;

    public static List<Nearby> nearby() { return nearby; }

    public static void tick(MinecraftClient mc) {
        Config c = Config.get();
        if (mc.player == null || mc.world == null) {
            nearby = List.of(); present.clear(); lastWorld = null;
            return;
        }
        long now = System.currentTimeMillis();
        if (mc.world != lastWorld) {
            lastWorld = mc.world;
            present.clear(); names.clear();
            primeUntil = now + 3000; // chwile po wejsciu na swiat tylko zapamietujemy graczy
        }

        double yaw = Math.toRadians(mc.player.getYaw());
        double sin = Math.sin(yaw), cos = Math.cos(yaw);
        List<Nearby> list = new ArrayList<>();
        for (AbstractClientPlayerEntity p : mc.world.getPlayers()) {
            if (p == mc.player || p.isSpectator()) continue;
            double dist = mc.player.distanceTo(p);
            if (dist > c.range) continue;
            String nick = p.getName().getString();
            if (c.ignoreNames.stream().anyMatch(n -> n.equalsIgnoreCase(nick))) continue;
            double dx = p.getX() - mc.player.getX();
            double dz = p.getZ() - mc.player.getZ();
            double right = -dx * cos - dz * sin;
            double fwd = -dx * sin + dz * cos;
            double bearing = Math.toDegrees(Math.atan2(dx, -dz));
            if (bearing < 0) bearing += 360;
            int dir = (int) Math.round(bearing / 45.0) % 8;
            list.add(new Nearby(p.getUuid(), nick, dist, right, fwd, dir, p));
        }
        list.sort(Comparator.comparingDouble(Nearby::dist));
        nearby = list;

        if (!c.radarEnabled) { present.clear(); return; }

        Set<UUID> nowIds = new HashSet<>();
        for (Nearby n : list) nowIds.add(n.id());
        List<Nearby> appeared = new ArrayList<>();
        for (Nearby n : list) if (!present.contains(n.id())) appeared.add(n);
        List<UUID> left = new ArrayList<>();
        for (UUID id : present) if (!nowIds.contains(id)) left.add(id);
        boolean primed = now > primeUntil;

        if (primed) {
            if (!appeared.isEmpty()) {
                if (c.chatMsg) {
                    if (appeared.size() <= 5) for (Nearby n : appeared) chat(mc, n.name(), " (" + (int) n.dist() + "m, " + SHORT[n.dir()] + ")", Formatting.GREEN);
                    else chat(mc, "Wykryto " + appeared.size() + " graczy", " w pobliżu", Formatting.GREEN);
                }
                if (c.overlay) {
                    Nearby f = appeared.get(0);
                    mc.inGameHud.setOverlayMessage(Text.literal("Gracz w pobliżu: " + f.name() + " (" + (int) f.dist() + "m " + SHORT[f.dir()] + ")").formatted(Formatting.GREEN), false);
                }
                if (c.sound) playSound(mc);
            }
            if (c.leaveInfo) for (UUID id : left) chat(mc, names.getOrDefault(id, "?"), " oddalił się", Formatting.GRAY);
        }

        present.clear(); present.addAll(nowIds);
        for (Nearby n : list) names.put(n.id(), n.name());

        // powiadomienia na telefon
        if (primed && c.phoneEnabled && list.size() <= c.maxNearbyForPush
                && !(c.onlyBackground && mc.isWindowFocused())) {
            for (Nearby n : list) {
                if (now - lastAnyPush < 3000) break; // nie spamujemy telefonu
                Long last = lastPush.get(n.id());
                if (last != null && now - last < c.cooldownSeconds * 1000L) continue;
                lastPush.put(n.id(), now);
                lastAnyPush = now;
                Notifier.push(mc, n);
            }
        }
    }

    private static void chat(MinecraftClient mc, String who, String rest, Formatting color) {
        mc.player.sendMessage(Text.literal("[xMIBRadar] ").formatted(Formatting.DARK_GREEN)
                .append(Text.literal(who).formatted(Formatting.WHITE))
                .append(Text.literal(rest).formatted(color)), false);
    }

    public static void playSound(MinecraftClient mc) {
        Config c = Config.get();
        float vol = c.volume / 100f;
        if (vol <= 0f) return;
        SoundEvent ev = Compat.sound(Math.floorMod(c.soundType, SOUND_NAMES.length));
        mc.getSoundManager().play(PositionedSoundInstance.master(ev, 1.0f, vol));
    }

    public static void localTest(MinecraftClient mc) {
        if (mc.player != null) chat(mc, "Test", " - radar działa", Formatting.GREEN);
        playSound(mc);
    }
}
