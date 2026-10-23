package pl.xmib.radar;

import net.minecraft.client.gui.screen.Screen;

public class ConfigScreen extends ConfigScreenBase {
    public ConfigScreen(Screen parent) { super(parent); }

    @Override public boolean mouseClicked(double mx, double my, int button) { return handleClick(mx, my, button) || super.mouseClicked(mx, my, button); }
    @Override public boolean mouseDragged(double mx, double my, int button, double dx, double dy) { return handleDrag(mx) || super.mouseDragged(mx, my, button, dx, dy); }
    @Override public boolean mouseReleased(double mx, double my, int button) { return handleRelease() || super.mouseReleased(mx, my, button); }
    @Override public boolean keyPressed(int keyCode, int scanCode, int modifiers) { return handleKey(keyCode) || super.keyPressed(keyCode, scanCode, modifiers); }
}
