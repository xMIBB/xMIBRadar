package pl.xmib.radar;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;

public class XMIBRadarClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        System.setProperty("java.awt.headless", "true"); // AWT tylko do rysowania obrazka powiadomienia
        Config.load();

        KeyBinding open = KeyBindingHelper.registerKeyBinding(Compat.keyBinding());

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            while (open.wasPressed()) mc.setScreen(new ConfigScreen(mc.currentScreen));
            Tracker.tick(mc);
        });
    }
}
