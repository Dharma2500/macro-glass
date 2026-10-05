package com.vitalymacro.macroglass.client;

import com.vitalymacro.macroglass.client.config.MacroConfig;
import com.vitalymacro.macroglass.client.gui.MacroScreen;
import com.vitalymacro.macroglass.client.macro.MacroExecutor;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.util.Identifier;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public final class MacroGlassClient implements ClientModInitializer {
    public static final String MOD_ID = "macro_glass";

    private static KeyBinding openScreenKey;
    private static MacroConfig config;
    private static MacroExecutor executor;

    @Override
    public void onInitializeClient() {
        config = MacroConfig.load();
        executor = new MacroExecutor(config);
        ClientTickEvents.END_CLIENT_TICK.register(executor::tick);

        openScreenKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.macro_glass.open",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_CONTROL,
                new KeyBinding.Category(Identifier.of(MOD_ID, "keybindings"))
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openScreenKey.wasPressed()) {
                if (client.currentScreen instanceof MacroScreen) {
                    client.setScreen(null);
                } else if (client.world != null) {
                    client.setScreen(new MacroScreen(config, executor));
                }
            }
        });
    }

    public static MacroConfig getConfig() {
        return config;
    }

    public static MacroExecutor getExecutor() {
        return executor;
    }
}
