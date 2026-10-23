package pl.xmib.radar;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

/** Obrazki do powiadomien: "karta gracza" ze skinem albo zwykly screenshot. Dziala poza watkiem renderowania. */
public class CardBuilder {

    public static byte[] card(Notifier.Info i, BufferedImage skin) throws Exception {
        int w = 640, h = 330;
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        g.setColor(new Color(0x0A0F11)); g.fillRect(0, 0, w, h);
        g.setColor(new Color(0x0F1517)); g.fillRoundRect(10, 10, w - 20, h - 20, 26, 26);
        g.setColor(new Color(0x1E2A2C)); g.drawRoundRect(10, 10, w - 21, h - 21, 26, 26);

        g.setColor(new Color(0xFF4D4D)); g.fillOval(34, 34, 12, 12);
        g.setFont(new Font("SansSerif", Font.BOLD, 15));
        g.setColor(new Color(0xFF6B6B));
        g.drawString("KTO\u015A JEST W POBLI\u017BU", 54, 46);
        g.setFont(new Font("SansSerif", Font.PLAIN, 15));
        g.setColor(new Color(0x8A9896));
        String right = i.time() + " \u00B7 " + i.server();
        g.drawString(right, w - 34 - g.getFontMetrics().stringWidth(right), 46);

        int bx = 34, by = 66, bw = 150, bh = 232;
        g.setColor(new Color(0x151D1F)); g.fillRoundRect(bx, by, bw, bh, 18, 18);
        g.setColor(new Color(0x24312F)); g.drawRoundRect(bx, by, bw - 1, bh - 1, 18, 18);
        if (skin != null) {
            double s = Math.min((bw - 24.0) / skin.getWidth(), (bh - 24.0) / skin.getHeight());
            int sw = (int) (skin.getWidth() * s), sh = (int) (skin.getHeight() * s);
            g.drawImage(skin, bx + (bw - sw) / 2, by + (bh - sh) / 2, sw, sh, null);
        } else {
            g.setFont(new Font("SansSerif", Font.BOLD, 60));
            g.setColor(new Color(0x2B3A3C));
            g.drawString("?", bx + 56, by + 135);
        }

        int tx = 214, maxW = w - tx - 34;
        g.setFont(new Font("SansSerif", Font.BOLD, 36));
        g.setColor(Color.WHITE);
        g.drawString(fit(g, i.name(), maxW), tx, 112);
        g.setFont(new Font("SansSerif", Font.PLAIN, 20));
        g.setColor(new Color(0x8A9896));
        g.drawString(fit(g, Notifier.kratek(i.dist()) + " \u2022 " + i.dirLong(), maxW), tx, 146);
        g.setColor(new Color(0xFF7A8C));
        g.drawString("Serca (max): " + Notifier.fmt(i.hearts()), tx, 190);
        g.setColor(new Color(0xE9F1EF));
        g.drawString(fit(g, "W r\u0119ce: " + i.hand(), maxW), tx, 222);
        String armor = i.pieces().isEmpty() ? "brak zbroi" : String.join(", ", i.pieces());
        g.drawString(fit(g, "Zbroja: " + armor, maxW), tx, 254);
        g.dispose();
        return jpeg(out, 0.9f);
    }

    public static byte[] shot(BufferedImage shot) throws Exception {
        int w = Math.min(shot.getWidth(), 1280);
        int h = shot.getHeight() * w / shot.getWidth();
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(shot, 0, 0, w, h, null);
        g.dispose();
        return jpeg(out, 0.82f);
    }

    private static String fit(Graphics2D g, String s, int maxW) {
        FontMetrics fm = g.getFontMetrics();
        if (fm.stringWidth(s) <= maxW) return s;
        while (s.length() > 1 && fm.stringWidth(s + "\u2026") > maxW) s = s.substring(0, s.length() - 1);
        return s + "\u2026";
    }

    private static byte[] jpeg(BufferedImage img, float q) throws Exception {
        ImageWriter wr = ImageIO.getImageWritersByFormatName("jpeg").next();
        ImageWriteParam p = wr.getDefaultWriteParam();
        p.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        p.setCompressionQuality(q);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (MemoryCacheImageOutputStream ios = new MemoryCacheImageOutputStream(bos)) {
            wr.setOutput(ios);
            wr.write(null, new IIOImage(img, null, null), p);
        }
        wr.dispose();
        return bos.toByteArray();
    }
}
