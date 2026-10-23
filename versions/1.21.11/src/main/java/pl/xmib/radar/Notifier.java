package pl.xmib.radar;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Notifier {
    public record Info(String name, int dist, String dirLong, int x, int y, int z, double hearts,
                       String hand, List<String> pieces, String server, String time) {}

    public static volatile String status = "Jeszcze nic nie wys\u0142ano";
    public static volatile boolean ok = false, error = false;

    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static final ExecutorService POOL = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "xMIBRadar-sender"); t.setDaemon(true); return t;
    });
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");

    /** Wolane z glownego watku (tick). */
    public static void push(MinecraftClient mc, Tracker.Nearby n) {
        Config c = Config.get();
        AbstractClientPlayerEntity p = n.player();

        List<String> pieces = new ArrayList<>();
        for (EquipmentSlot s : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack st = p.getEquippedStack(s);
            if (!st.isEmpty()) pieces.add(st.getName().getString());
        }
        ItemStack hand = p.getMainHandStack();
        ServerInfo si = mc.getCurrentServerEntry();
        Info info = new Info(n.name(), (int) Math.round(n.dist()), Tracker.LONG[n.dir()],
                p.getBlockX(), p.getBlockY(), p.getBlockZ(),
                Compat.maxHealth(p) / 2.0,
                hand.isEmpty() ? "pusta r\u0119ka" : hand.getName().getString(),
                pieces, si != null ? si.address : "singleplayer", LocalTime.now().format(TIME));

        final int mode = c.imageMode;
        final String url = c.ntfyUrl();
        if (mode == 1) Compat.screenshot(mc, shot -> submit(info, shot, mode, url));
        else submit(info, null, mode, url);
    }

    private static void submit(Info info, BufferedImage shot, int mode, String url) {
        POOL.execute(() -> {
            try {
                byte[] img = null;
                if (mode == 1 && shot != null) img = CardBuilder.shot(shot);
                else if (mode == 0 || mode == 1) img = CardBuilder.card(info, fetchSkin(info.name()));

                String title = "\uD83D\uDEA8 " + info.name() + " jest w pobli\u017cu! (" + kratek(info.dist()) + ")";
                String msg = "\uD83D\uDCCD " + info.dirLong() + " \u2022 X " + info.x() + " Y " + info.y() + " Z " + info.z() + "\\n"
                        + "\u2694\uFE0F " + info.hand() + "\\n"
                        + "\uD83D\uDEE1\uFE0F " + (info.pieces().isEmpty() ? "brak zbroi" : String.join(", ", info.pieces())) + "\\n"
                        + "\u2764\uFE0F maks. " + fmt(info.hearts()) + "\\n"
                        + "\uD83C\uDF10 " + info.server();
                send(url, title, msg, img);
            } catch (Throwable e) {
                fail(e.toString());
            }
        });
    }

    public static void sendTest() {
        final String url = Config.get().ntfyUrl();
        POOL.execute(() -> send(url, "xMIBRadar \u2013 test", "Dzia\u0142a! Powiadomienia z Minecrafta docieraj\u0105 na telefon.", null));
    }

    private static void send(String url, String title, String msg, byte[] image) {
        try {
            HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(25))
                    .header("Title", mime(title))
                    .header("Priority", "4");
            HttpRequest req;
            if (image != null) {
                req = b.header("Message", mime(msg)).header("Filename", "radar.jpg")
                        .PUT(HttpRequest.BodyPublishers.ofByteArray(image)).build();
            } else {
                req = b.POST(HttpRequest.BodyPublishers.ofString(msg.replace("\\n", "\n"), StandardCharsets.UTF_8)).build();
            }
            long t0 = System.nanoTime();
            HttpResponse<String> res = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
            double sec = (System.nanoTime() - t0) / 1e9;
            if (res.statusCode() / 100 == 2) {
                status = "Wys\u0142ano o " + LocalTime.now().format(TIME) + " (" + String.format(Locale.forLanguageTag("pl-PL"), "%.2f", sec) + " s)";
                ok = true; error = false;
            } else {
                fail("HTTP " + res.statusCode());
            }
        } catch (Exception e) {
            fail(e.toString());
        }
    }

    private static void fail(String msg) {
        status = "B\u0142\u0105d: " + (msg.length() > 40 ? msg.substring(0, 40) : msg);
        error = true; ok = false;
        System.err.println("[xMIBRadar] ntfy error: " + msg);
    }

    private static BufferedImage fetchSkin(String name) {
        try {
            HttpRequest r = HttpRequest.newBuilder(URI.create("https://minotar.net/armor/body/"
                    + URLEncoder.encode(name, StandardCharsets.UTF_8) + "/220.png"))
                    .timeout(Duration.ofSeconds(8)).build();
            HttpResponse<byte[]> res = HTTP.send(r, HttpResponse.BodyHandlers.ofByteArray());
            if (res.statusCode() == 200) return ImageIO.read(new ByteArrayInputStream(res.body()));
        } catch (Exception ignored) {}
        return null;
    }

    private static String mime(String s) {
        return "=?UTF-8?B?" + Base64.getEncoder().encodeToString(s.getBytes(StandardCharsets.UTF_8)) + "?=";
    }

    public static String fmt(double d) {
        return d == Math.floor(d) ? String.valueOf((int) d) : String.format("%.1f", d);
    }

    public static String kratek(int n) {
        int m10 = n % 10, m100 = n % 100;
        String w = n == 1 ? "kratka" : (m10 >= 2 && m10 <= 4 && !(m100 >= 12 && m100 <= 14)) ? "kratki" : "kratek";
        return n + " " + w;
    }
}
