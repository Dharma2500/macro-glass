package com.vitalymacro.macroglass.client.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;
import net.minecraft.client.MinecraftClient;

public final class GlassButtonWidget extends ClickableWidget {
    @FunctionalInterface
    public interface PressAction {
        void onPress();
    }

    private final PressAction action;
    private boolean selected;

    public GlassButtonWidget(int x, int y, int width, int height, Text message, PressAction action) {
        super(x, y, width, height, message);
        this.action = action;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        int background = !active ? 0x55333840 : selected ? 0xCC344351 : hovered ? 0xCC27313B : 0xAA1A2027;
        int border = !active ? 0x665E6670 : selected ? 0xE0AEBECC : hovered ? 0xDDB9C7D5 : 0x885F6A76;
        int textColor = active ? 0xFFF1F5F9 : 0xFF7D8792;

        context.fill(getX(), getY(), getRight(), getBottom(), background);
        context.fill(getX(), getY(), getRight(), getY() + 1, border);
        context.fill(getX(), getBottom() - 1, getRight(), getBottom(), border);
        context.fill(getX(), getY(), getX() + 1, getBottom(), border);
        context.fill(getRight() - 1, getY(), getRight(), getBottom(), border);

        MinecraftClient client = MinecraftClient.getInstance();
        int textWidth = client.textRenderer.getWidth(getMessage());
        int textX = getX() + (getWidth() - textWidth) / 2;
        int textY = getY() + (getHeight() - 8) / 2;
        context.drawText(client.textRenderer, getMessage(), textX, textY, textColor, false);
    }

    @Override
    public void onClick(net.minecraft.client.gui.Click click, boolean doubled) {
        if (active && click.button() == 0) {
            action.onPress();
        }
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
        appendDefaultNarrations(builder);
    }
}
