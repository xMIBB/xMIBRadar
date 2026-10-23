package pl.xmib.radar;

import net.minecraft.client.gui.DrawContext;

import java.util.List;

/** Rysuje radar w srodku GUI (nie ma juz radaru na ekranie gry). */
public class Radar {
    public static void draw(DrawContext ctx, int cx, int cy, int R, int range, List<Tracker.Nearby> list) {
        for (int dy = -R; dy <= R; dy++) {
            int dx = (int) Math.sqrt(R * R - dy * dy);
            ctx.fill(cx - dx, cy + dy, cx + dx + 1, cy + dy + 1, 0xFF0A1A14);
        }
        ctx.fill(cx - R, cy, cx + R + 1, cy + 1, 0xFF173A29);
        ctx.fill(cx, cy - R, cx + 1, cy + R + 1, 0xFF173A29);
        int[] radii = {R, R * 2 / 3, R / 3};
        for (int i = 0; i < radii.length; i++) {
            int col = i == 0 ? 0xFF2A7A52 : 0xFF1F4D35;
            for (int a = 0; a < 360; a++) {
                double rad = Math.toRadians(a);
                int px = cx + (int) Math.round(radii[i] * Math.cos(rad));
                int py = cy + (int) Math.round(radii[i] * Math.sin(rad));
                ctx.fill(px, py, px + 1, py + 1, col);
            }
        }
        // sweep
        double head = (System.currentTimeMillis() % 3000L) / 3000.0 * 360.0;
        for (int k = 0; k < 70; k += 2) {
            double ang = Math.toRadians(head - k);
            int alpha = (int) (80 * (1.0 - k / 70.0));
            int col = (alpha << 24) | 0x2EE87E;
            for (int r = 0; r < R; r++) {
                int px = cx + (int) Math.round(r * Math.sin(ang));
                int py = cy - (int) Math.round(r * Math.cos(ang));
                ctx.fill(px, py, px + 2, py + 2, col);
            }
        }
        double s = R / (double) range;
        for (Tracker.Nearby n : list) {
            double x = n.right() * s, y = n.fwd() * s;
            double len = Math.hypot(x, y);
            if (len > R - 3) { x = x / len * (R - 3); y = y / len * (R - 3); }
            int px = cx + (int) Math.round(x), py = cy - (int) Math.round(y);
            int col = n.dist() < 15 ? 0xFFFF6B3D : 0xFFFFB347;
            ctx.fill(px - 2, py - 2, px + 3, py + 3, 0x55000000 | (col & 0xFFFFFF));
            ctx.fill(px - 1, py - 1, px + 2, py + 2, col);
        }
        ctx.fill(cx - 1, cy - 1, cx + 2, cy + 2, 0xFFFFFFFF);
    }
}
