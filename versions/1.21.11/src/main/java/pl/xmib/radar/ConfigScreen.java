package pl.xmib.radar;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.KeyInput;

public class ConfigScreen extends ConfigScreenBase {
    public ConfigScreen(Screen parent) { super(parent); }

    @Override public boolean mouseClicked(Click click, boolean doubled) { return handleClick(click.x(), click.y(), click.button()) || super.mouseClicked(click, doubled); }
    @Override public boolean mouseDragged(Click click, double offsetX, double offsetY) { return handleDrag(click.x()) || super.mouseDragged(click, offsetX, offsetY); }
    @Override public boolean mouseReleased(Click click) { return handleRelease() || super.mouseReleased(click); }
    @Override public boolean keyPressed(KeyInput input) { return handleKey(input.getKeycode()) || super.keyPressed(input); }
}
