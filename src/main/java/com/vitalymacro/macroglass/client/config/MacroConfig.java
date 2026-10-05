package com.vitalymacro.macroglass.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;

public final class MacroConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("macro_glass.json");

    private MacroSlot[] slots = new MacroSlot[7];

    public MacroConfig() {
        for (int i = 0; i < slots.length; i++) {
            slots[i] = new MacroSlot();
        }
    }

    public MacroSlot getSlot(int index) {
        return slots[index];
    }

    public MacroSlot[] getSlots() {
        return slots;
    }

    public void save() {
        try {
            Files.createDirectories(PATH.getParent());
            Files.writeString(
                    PATH,
                    GSON.toJson(this),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE
            );
        } catch (IOException ignored) {
            // Configuration persistence should never break the client.
        }
    }

    public static MacroConfig load() {
        if (!Files.exists(PATH)) {
            MacroConfig config = new MacroConfig();
            config.save();
            return config;
        }

        try {
            MacroConfig loaded = GSON.fromJson(Files.readString(PATH), MacroConfig.class);
            if (loaded == null || loaded.slots == null || loaded.slots.length != 7) {
                throw new IllegalStateException("Invalid macro_glass.json");
            }

            for (MacroSlot slot : loaded.slots) {
                if (slot == null) {
                    throw new IllegalStateException("Null macro slot");
                }
                slot.sanitize();
            }
            return loaded;
        } catch (Exception ignored) {
            MacroConfig fallback = new MacroConfig();
            fallback.save();
            return fallback;
        }
    }

    public static final class MacroSlot {
        private String text = "";
        private int repetitions = 1;
        private int delayMs = 1000;

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text == null ? "" : text.replace("\r\n", "\n").replace('\r', '\n');
        }

        public int getRepetitions() {
            return repetitions;
        }

        public void setRepetitions(int repetitions) {
            this.repetitions = clamp(repetitions, 1, 1_000_000);
        }

        public int getDelayMs() {
            return delayMs;
        }

        public void setDelayMs(int delayMs) {
            this.delayMs = clamp(delayMs, 0, 86_400_000);
        }

        private void sanitize() {
            setText(text);
            setRepetitions(repetitions);
            setDelayMs(delayMs);
        }

        private static int clamp(int value, int min, int max) {
            return Math.max(min, Math.min(max, value));
        }
    }
}
