package pl.xmib.radar;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleConsumer;

public abstract class ConfigScreenBase extends Screen {
    static final int W = 460, H = 272;
    static final int LX = 14, LW = 200, RX = 236, RW = 210;
    static final int BG = 0xF00B1113, BORDER = 0xFF222E30, CARD = 0xFF10171A, CARD_B = 0xFF1E2A2C;
    static final int TXT = 0xFFE9F1EF, GRAY = 0xFF8A9896, GREEN = 0xFF34E58A, GREEN_D = 0xFF123A27;
    static final int RED = 0xFFFF5C5C, TRACK = 0xFF243032, WHITE = 0xFFFFFFFF;
    static final String[] IMAGE_MODES = {"Karta gracza", "Screenshot", "Brak"};

    private static class Region {
        int x, y, w, h; Runnable click; DoubleConsumer drag;
        boolean contains(int mx, int my) { return mx >= x && mx < x + w && my >= y && my < y + h; }
    }

    private final Screen parent;
    private final List<Region> regions = new ArrayList<>();
    private Region activeDrag;
    private int tab = 0;
    private double scale = 1.0;
    private int ox, oy, lmx, lmy;

    protected ConfigScreenBase(Screen parent) {
        super(Text.literal("xMIBRadar"));
        this.parent = parent;
    }

    @Override public boolean shouldPause() { return false; }

    // ---------- helpers ----------
    static void rr(DrawContext c, int x, int y, int w, int h, int r, int col) {
        r = Math.min(r, Math.min(w, h) / 2);
        if (r <= 0) { c.fill(x, y, x + w, y + h, col); return; }
        c.fill(x, y + r, x + w, y + h - r, col);
        for (int i = 0; i < r; i++) {
            double dy = r - i - 0.5;
            int inset = (int) Math.round(r - Math.sqrt(r * r - dy * dy));
            c.fill(x + inset, y + i, x + w - inset, y + i + 1, col);
            c.fill(x + inset, y + h - 1 - i, x + w - inset, y + h - i, col);
        }
    }

    static void box(DrawContext c, int x, int y, int w, int h, int r, int fill, int border) {
        rr(c, x, y, w, h, r, border);
        rr(c, x + 1, y + 1, w - 2, h - 2, Math.max(0, r - 1), fill);
    }

    private void t(DrawContext c, String s, int x, int y, int col) { c.drawText(textRenderer, s, x, y, col, false); }
    private void tr(DrawContext c, String s, int xRight, int y, int col) { c.drawText(textRenderer, s, xRight - textRenderer.getWidth(s), y, col, false); }

    private Region reg(int x, int y, int w, int h, Runnable click, DoubleConsumer drag) {
        Region r = new Region();
        r.x = x; r.y = y; r.w = w; r.h = h; r.click = click; r.drag = drag;
        regions.add(r);
        return r;
    }

    private void toggle(DrawContext c, int y, String label, boolean on, Runnable flip) {
        t(c, label, LX, y + 6, TXT);
        int px = LX + LW - 28;
        rr(c, px, y + 3, 28, 14, 7, on ? GREEN : TRACK);
        rr(c, on ? px + 16 : px + 2, y + 5, 10, 10, 5, WHITE);
        reg(LX, y, LW, 20, flip, null);
    }

    private void slider(DrawContext c, int y, String label, String value, double frac, DoubleConsumer set) {
        frac = Math.max(0, Math.min(1, frac));
        t(c, label, LX, y + 1, TXT);
        tr(c, value, LX + LW, y + 1, GREEN);
        rr(c, LX, y + 14, LW, 4, 2, TRACK);
        int fw = (int) Math.round(frac * LW);
        if (fw > 0) rr(c, LX, y + 14, fw, 4, 2, GREEN);
        rr(c, LX + fw - 4, y + 12, 8, 8, 4, WHITE);
        reg(LX, y, LW, 24, null, set);
    }

    private void selector(DrawContext c, int y, String label, String value, int boxW, Runnable prev, Runnable next) {
        t(c, label, LX, y + 6, TXT);
        int bx = LX + LW - boxW;
        box(c, bx, y + 1, boxW, 18, 5, 0xFF0D1416, CARD_B);
        t(c, "\u2039", bx + 6, y + 6, GRAY);
        t(c, "\u203A", bx + boxW - 11, y + 6, GRAY);
        center(c, value, bx + boxW / 2, y + 6, TXT);
        reg(bx, y + 1, boxW / 2, 18, prev, null);
        reg(bx + boxW / 2, y + 1, boxW - boxW / 2, 18, next, null);
    }

    private void button(DrawContext c, int x, int y, int w, int h, String label, boolean primary, Runnable click) {
        boolean hover = lmx >= x && lmx < x + w && lmy >= y && lmy < y + h;
        if (primary) {
            rr(c, x, y, w, h, 6, hover ? 0xFF5CF0A5 : GREEN);
            center(c, label, x + w / 2, y + (h - 8) / 2, 0xFF06150D);
        } else {
            box(c, x, y, w, h, 6, hover ? 0xFF16221F : 0xFF0D1416, hover ? GREEN : CARD_B);
            center(c, label, x + w / 2, y + (h - 8) / 2, TXT);
        }
        reg(x, y, w, h, click, null);
    }

    private void center(DrawContext c, String s, int cx, int y, int col) {
        c.drawText(textRenderer, s, cx - textRenderer.getWidth(s) / 2, y, col, true);
    }

    private static int cyc(int v, int d, int n) { return Math.floorMod(v + d, n); }

    // ---------- render ----------
    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        regions.clear();
        scale = Math.min(1.0, Math.min((height - 8) / (double) H, (width - 8) / (double) W));
        ox = (int) ((width - W * scale) / 2);
        oy = (int) ((height - H * scale) / 2);
        lmx = (int) Math.floor((mx - ox) / scale);
        lmy = (int) Math.floor((my - oy) / scale);

        Compat.pushScaled(ctx, (float) ox, (float) oy, (float) scale);
        drawAll(ctx);
        Compat.pop(ctx);
    }

    private void drawAll(DrawContext c) {
        Config cfg = Config.get();
        List<Tracker.Nearby> list = Tracker.nearby();
        MinecraftClient mc = MinecraftClient.getInstance();

        box(c, 0, 0, W, H, 8, BG, BORDER);

        // header
        rr(c, 14, 11, 22, 22, 11, GREEN_D);
        rr(c, 18, 15, 14, 14, 7, GREEN);
        rr(c, 19, 16, 12, 12, 6, GREEN_D);
        rr(c, 23, 20, 4, 4, 2, GREEN);
        c.drawText(textRenderer, Text.literal("Radar graczy").formatted(Formatting.BOLD), 44, 12, TXT, false);
        t(c, "Zasi\u0119g " + cfg.range + " kratek \u2022 " + list.size() + " w pobli\u017cu", 44, 25, GRAY);

        int tx = 228, tw = 110;
        box(c, tx, 13, tw, 20, 6, 0xFF0D1416, CARD_B);
        tabButton(c, tx + 2, 15, tw / 2 - 2, 16, "Radar", 0);
        tabButton(c, tx + tw / 2, 15, tw / 2 - 2, 16, "Telefon", 1);

        String st; int stCol;
        if (tab == 0) { st = cfg.radarEnabled ? "AKTYWNY" : "WY\u0141\u0104CZONY"; stCol = cfg.radarEnabled ? GREEN : GRAY; }
        else if (!cfg.phoneEnabled) { st = "WY\u0141\u0104CZONE"; stCol = GRAY; }
        else if (Notifier.error) { st = "B\u0141\u0104D"; stCol = RED; }
        else if (Notifier.ok) { st = "PO\u0141\u0104CZONY"; stCol = GREEN; }
        else { st = "GOTOWY"; stCol = GREEN; }
        int pw = textRenderer.getWidth(st) + 22;
        int px = W - 14 - pw;
        box(c, px, 14, pw, 18, 9, stCol == RED ? 0xFF2A1414 : GREEN_D, stCol == RED ? 0xFF5A2222 : 0xFF1F6B45);
        rr(c, px + 8, 21, 4, 4, 2, stCol);
        t(c, st, px + 15, 19, stCol);
        c.fill(1, 44, W - 1, 45, 0xFF1B2527);

        if (tab == 0) radarTab(c, cfg, list); else phoneTab(c, cfg);

        // footer
        c.fill(1, H - 36, W - 1, H - 35, 0xFF1B2527);
        t(c, "K / ESC - zamknij", 14, H - 23, GRAY);
        button(c, W - 14 - 74, H - 30, 74, 20, "Gotowe", true, this::close);
        if (tab == 0) button(c, W - 14 - 74 - 8 - 74, H - 30, 74, 20, "Testuj", false, () -> Tracker.localTest(mc));
        else button(c, W - 14 - 74 - 8 - 90, H - 30, 90, 20, "Wy\u015blij test", false, Notifier::sendTest);
    }

    private void tabButton(DrawContext c, int x, int y, int w, int h, String label, int idx) {
        boolean active = tab == idx;
        if (active) rr(c, x, y, w, h, 5, 0xFF14402B);
        center(c, label, x + w / 2, y + 4, active ? GREEN : GRAY);
        reg(x, y, w, h, () -> tab = idx, null);
    }

    private void radarTab(DrawContext c, Config cfg, List<Tracker.Nearby> list) {
        int y = 54;
        toggle(c, y, "Radar", cfg.radarEnabled, () -> cfg.radarEnabled = !cfg.radarEnabled); y += 21;
        slider(c, y, "Zasi\u0119g", cfg.range + " kratek", (cfg.range - 16) / 112.0, f -> cfg.range = 16 + (int) Math.round(f * 112)); y += 27;
        toggle(c, y, "Wiadomo\u015b\u0107 na czacie", cfg.chatMsg, () -> cfg.chatMsg = !cfg.chatMsg); y += 21;
        toggle(c, y, "Powiadomienie", cfg.overlay, () -> cfg.overlay = !cfg.overlay); y += 21;
        toggle(c, y, "Info o odej\u015bciu", cfg.leaveInfo, () -> cfg.leaveInfo = !cfg.leaveInfo); y += 21;
        toggle(c, y, "D\u017awi\u0119k", cfg.sound, () -> cfg.sound = !cfg.sound); y += 21;
        int n = Tracker.SOUND_NAMES.length;
        selector(c, y, "Rodzaj d\u017awi\u0119ku", Tracker.SOUND_NAMES[Math.floorMod(cfg.soundType, n)], 90,
                () -> cfg.soundType = cyc(cfg.soundType, -1, n), () -> cfg.soundType = cyc(cfg.soundType, 1, n)); y += 21;
        slider(c, y, "G\u0142o\u015bno\u015b\u0107", cfg.volume + "%", cfg.volume / 100.0, f -> cfg.volume = (int) Math.round(f * 20) * 5);

        // radar
        box(c, RX, 54, RW, 112, 8, CARD, CARD_B);
        t(c, "RADAR", RX + 10, 62, GRAY);
        t(c, "\u2191 prz\u00f3d", RX + 10, 152, GRAY);
        Radar.draw(c, RX + RW / 2 + 14, 110, 46, Math.max(1, cfg.range), list);

        // lista
        box(c, RX, 172, RW, 58, 8, CARD, CARD_B);
        if (list.isEmpty()) {
            t(c, "Nikogo w pobli\u017cu", RX + 10, 192, GRAY);
        } else {
            int shown = Math.min(3, list.size());
            for (int i = 0; i < shown; i++) {
                Tracker.Nearby nb = list.get(i);
                int ly = 178 + i * 11;
                t(c, textRenderer.trimToWidth(nb.name(), 120), RX + 10, ly, TXT);
                tr(c, (int) nb.dist() + "m " + Tracker.SHORT[nb.dir()], RX + RW - 10, ly, RED);
            }
            if (list.size() > shown) t(c, "+" + (list.size() - shown) + " wi\u0119cej", RX + 10, 178 + shown * 11, GRAY);
        }
    }

    private void phoneTab(DrawContext c, Config cfg) {
        int y = 54;
        toggle(c, y, "Powiadomienia na telefon", cfg.phoneEnabled, () -> cfg.phoneEnabled = !cfg.phoneEnabled); y += 21;
        int n = IMAGE_MODES.length;
        selector(c, y, "Obraz", IMAGE_MODES[Math.floorMod(cfg.imageMode, n)], 100,
                () -> cfg.imageMode = cyc(cfg.imageMode, -1, n), () -> cfg.imageMode = cyc(cfg.imageMode, 1, n)); y += 21;
        toggle(c, y, "Tylko gdy gra jest w tle", cfg.onlyBackground, () -> cfg.onlyBackground = !cfg.onlyBackground); y += 21;
        int cd = cfg.cooldownSeconds;
        String cdText = "co " + (cd < 60 ? cd + " s" : Math.round(cd / 60.0) + " min");
        slider(c, y, "Ten sam gracz najwy\u017cej", cdText, (cd - 5) / 595.0, f -> cfg.cooldownSeconds = 5 + (int) Math.round(f * 595)); y += 27;
        t(c, "Kana\u0142", LX, y + 5, TXT);
        button(c, LX + LW - 50 - 6 - 50, y, 50, 18, "Kopiuj", false, () -> MinecraftClient.getInstance().keyboard.setClipboard(cfg.channel));
        button(c, LX + LW - 50, y, 50, 18, "Losuj", false, () -> cfg.channel = Config.randomChannel());

        box(c, RX, 54, RW, 176, 8, CARD, CARD_B);
        t(c, "JAK PO\u0141\u0104CZY\u0106 TELEFON", RX + 12, 62, GRAY);
        step(c, 1, 78);
        t(c, "Zainstaluj aplikacj\u0119 ntfy", RX + 32, 78, TXT);
        t(c, "Google Play / App Store", RX + 32, 89, GRAY);
        step(c, 2, 106);
        t(c, "Kliknij + i wpisz kana\u0142:", RX + 32, 106, TXT);
        int cw = textRenderer.getWidth(cfg.channel) + 14;
        box(c, RX + 32, 118, cw, 16, 5, 0xFF0D1416, 0xFF1F6B45);
        t(c, cfg.channel, RX + 39, 123, GREEN);
        step(c, 3, 144);
        List<OrderedText> lines = textRenderer.wrapLines(Text.literal("W\u0142\u0105cz \u201ePowiadomienia na telefon\u201d i kliknij Test"), RW - 44);
        int ly = 144;
        for (OrderedText l : lines) { c.drawText(textRenderer, l, RX + 32, ly, TXT, false); ly += 11; }
        c.fill(RX + 12, 200, RX + RW - 12, 201, 0xFF1B2527);
        int dot = Notifier.error ? RED : GREEN;
        rr(c, RX + 12, 210, 5, 5, 2, dot);
        t(c, textRenderer.trimToWidth(Notifier.status, RW - 40), RX + 22, 208, dot);
    }

    private void step(DrawContext c, int n, int y) {
        rr(c, RX + 12, y - 1, 14, 14, 4, GREEN_D);
        t(c, String.valueOf(n), RX + 17, y + 2, GREEN);
    }

    // ---------- input ----------
    private double fracFor(Region r, int mx) {
        return Math.max(0, Math.min(1, (mx - r.x) / (double) r.w));
    }

    protected boolean handleClick(double mx, double my, int button) {
        if (button != 0) return false;
        int x = (int) Math.floor((mx - ox) / scale), y = (int) Math.floor((my - oy) / scale);
        for (int i = regions.size() - 1; i >= 0; i--) {
            Region r = regions.get(i);
            if (!r.contains(x, y)) continue;
            if (r.drag != null) { activeDrag = r; r.drag.accept(fracFor(r, x)); }
            else if (r.click != null) { r.click.run(); Config.save(); }
            return true;
        }
        return true;
    }

    protected boolean handleDrag(double mx) {
        if (activeDrag == null) return false;
        int x = (int) Math.floor((mx - ox) / scale);
        activeDrag.drag.accept(fracFor(activeDrag, x));
        return true;
    }

    protected boolean handleRelease() {
        if (activeDrag == null) return false;
        activeDrag = null;
        Config.save();
        return true;
    }

    protected boolean handleKey(int keyCode) {
        if (keyCode == GLFW.GLFW_KEY_K || keyCode == GLFW.GLFW_KEY_ESCAPE) { close(); return true; }
        return false;
    }

    @Override public void removed() { Config.save(); }
    @Override public void close() { client.setScreen(parent); }
}
