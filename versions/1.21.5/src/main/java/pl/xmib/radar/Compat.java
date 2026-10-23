package pl.xmib.radar;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import org.lwjgl.glfw.GLFW;

import java.awt.image.BufferedImage;
import java.util.function.Consumer;

/** Roznice miedzy wersjami Minecrafta zebrane w jednym miejscu. */
public final class Compat {
    private Compat() {}

    public static String username(MinecraftClient mc) { return mc.getSession().getUsername(); }

    public static double maxHealth(PlayerEntity p) { return p.getAttributeValue(EntityAttributes.MAX_HEALTH); }

    public static SoundEvent sound(int idx) {
        return switch (idx) {
            case 0 -> SoundEvents.BLOCK_NOTE_BLOCK_PLING.value();
            case 1 -> SoundEvents.BLOCK_NOTE_BLOCK_BELL.value();
            case 2 -> SoundEvents.BLOCK_NOTE_BLOCK_CHIME.value();
            case 3 -> SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP;
            default -> SoundEvents.ENTITY_ARROW_HIT_PLAYER;
        };
    }

    public static void screenshot(MinecraftClient mc, Consumer<BufferedImage> cb) {
        BufferedImage bi = null;
        try (NativeImage img = ScreenshotRecorder.takeScreenshot(mc.getFramebuffer())) {
            bi = convert(img);
        } catch (Throwable e) {
            System.err.println("[xMIBRadar] screenshot failed: " + e);
        }
        cb.accept(bi);
    }

    private static BufferedImage convert(NativeImage img) {
        BufferedImage bi = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < img.getHeight(); y++)
            for (int x = 0; x < img.getWidth(); x++)
                bi.setRGB(x, y, img.getColorArgb(x, y) & 0xFFFFFF);
        return bi;
    }

    public static void pushScaled(DrawContext ctx, float ox, float oy, float s) {
        var m = ctx.getMatrices();
        m.push();
        m.translate(ox, oy, 0f);
        m.scale(s, s, 1f);
    }

    public static void pop(DrawContext ctx) { ctx.getMatrices().pop(); }

    public static KeyBinding keyBinding() {
        return new KeyBinding("key.xmibradar.config", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_K, "category.xmibradar");
    }
}
