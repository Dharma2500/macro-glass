package com.vitalymacro.macroglass.client.macro;

import com.vitalymacro.macroglass.client.config.MacroConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;

import java.util.ArrayList;
import java.util.List;

public final class MacroExecutor {
    private final MacroConfig config;

    private boolean running;
    private int slotIndex;
    private int targetRepetitions;
    private int completedRepetitions;
    private int lineIndex;
    private long nextActionAt;
    private List<String> actions = List.of();

    public MacroExecutor(MacroConfig config) {
        this.config = config;
    }

    public void start(int slotIndex) {
        if (slotIndex < 0 || slotIndex >= config.getSlots().length) {
            return;
        }

        MacroConfig.MacroSlot slot = config.getSlot(slotIndex);
        List<String> parsed = parseActions(slot.getText());
        if (parsed.isEmpty()) {
            stop();
            return;
        }

        this.slotIndex = slotIndex;
        this.targetRepetitions = slot.getRepetitions();
        this.completedRepetitions = 0;
        this.lineIndex = 0;
        this.actions = parsed;
        this.nextActionAt = System.currentTimeMillis();
        this.running = true;
    }

    public void stop() {
        running = false;
        actions = List.of();
        lineIndex = 0;
        completedRepetitions = 0;
    }

    public void tick(MinecraftClient client) {
        if (!running) {
            return;
        }

        if (client.player == null || client.getNetworkHandler() == null) {
            stop();
            return;
        }

        long now = System.currentTimeMillis();
        if (now < nextActionAt) {
            return;
        }

        if (lineIndex < actions.size()) {
            execute(client.getNetworkHandler(), actions.get(lineIndex));
            lineIndex++;
            nextActionAt = now;
            return;
        }

        completedRepetitions++;
        if (completedRepetitions >= targetRepetitions) {
            stop();
            return;
        }

        lineIndex = 0;
        nextActionAt = now + config.getSlot(slotIndex).getDelayMs();
    }

    public boolean isRunning() {
        return running;
    }

    public int getRunningSlot() {
        return slotIndex;
    }

    public int getCompletedRepetitions() {
        return completedRepetitions;
    }

    public int getTargetRepetitions() {
        return targetRepetitions;
    }

    private void execute(ClientPlayNetworkHandler networkHandler, String action) {
        if (action.startsWith("/")) {
            String command = action.substring(1).trim();
            if (!command.isEmpty()) {
                networkHandler.sendChatCommand(command);
            }
        } else {
            networkHandler.sendChatMessage(action);
        }
    }

    private static List<String> parseActions(String text) {
        List<String> result = new ArrayList<>();
        String normalized = text == null ? "" : text.replace("\r\n", "\n").replace('\r', '\n');
        for (String raw : normalized.split("\n", -1)) {
            String action = raw.trim();
            if (!action.isEmpty()) {
                result.add(action);
            }
        }
        return result;
    }
}
