package com.vitalymacro.macroglass.client.gui;

import com.vitalymacro.macroglass.client.config.MacroConfig;
import com.vitalymacro.macroglass.client.macro.MacroExecutor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public final class MacroScreen extends Screen {
    private static final int TAB_COUNT = 7;
    private static final int SIDE = 18;
    private static final int TAB_GAP = 4;
    private static final int TAB_WIDTH = 42;
    private static final int TAB_HEIGHT = 22;
    private static final int FOOTER_HEIGHT = 58;

    private final MacroConfig config;
    private final MacroExecutor executor;

    private int selectedTab = 0;
    private int panelX;
    private int panelY;
    private int panelW;
    private int panelH;

    private MultilineTextEditor editor;
    private TextFieldWidget repetitionsField;
    private TextFieldWidget delayField;

    public MacroScreen(MacroConfig config, MacroExecutor executor) {
        super(Text.literal("Macro Glass"));
        this.config = config;
        this.executor = executor;
    }

    @Override
    protected void init() {
        calculatePanel();
        rebuildWidgets();
    }

    private void calculatePanel() {
        panelW = Math.max(420, Math.min(width - 24, (int) (width * 0.50f)));
        panelH = Math.max(300, Math.min(height - 24, (int) (height * 0.50f)));
        panelX = (width - panelW) / 2;
        panelY = (height - panelH) / 2;
    }

    private void rebuildWidgets() {
        clearChildren();

        int editorX = panelX + SIDE;
        int editorY = panelY + 66;
        int editorW = panelW - SIDE * 2;
        int editorH = Math.max(96, panelH - 66 - FOOTER_HEIGHT);

        editor = new MultilineTextEditor(
                textRenderer,
                editorX,
                editorY,
                editorW,
                editorH,
                config.getSlot(selectedTab).getText()
        );
        addDrawableChild(editor);

        int fieldY = panelY + panelH - 46;
        int fieldW = 86;
        int buttonH = 22;
        int gap = 6;

        repetitionsField = new TextFieldWidget(textRenderer, panelX + SIDE, fieldY, fieldW, buttonH, Text.literal(""));
        repetitionsField.setMaxLength(7);
        repetitionsField.setTextPredicate(value -> value.matches("\\d*"));
        repetitionsField.setDrawsBackground(false);
        repetitionsField.setText(String.valueOf(config.getSlot(selectedTab).getRepetitions()));
        addDrawableChild(repetitionsField);

        int delayX = panelX + SIDE + fieldW + 112;
        delayField = new TextFieldWidget(textRenderer, delayX, fieldY, fieldW, buttonH, Text.literal(""));
        delayField.setMaxLength(9);
        delayField.setTextPredicate(value -> value.matches("\\d*"));
        delayField.setDrawsBackground(false);
        delayField.setText(String.valueOf(config.getSlot(selectedTab).getDelayMs()));
        addDrawableChild(delayField);

        int buttonsW = 170;
        int buttonX = panelX + panelW - SIDE - buttonsW;
        int half = (buttonsW - gap) / 2;

        GlassButtonWidget runButton = new GlassButtonWidget(
                buttonX,
                fieldY,
                half,
                buttonH,
                Text.literal("▶  Запустить"),
                this::startMacro
        );
        addDrawableChild(runButton);

        GlassButtonWidget stopButton = new GlassButtonWidget(
                buttonX + half + gap,
                fieldY,
                half,
                buttonH,
                Text.literal("■  Остановить"),
                executor::stop
        );
        addDrawableChild(stopButton);

        for (int i = 0; i < TAB_COUNT; i++) {
            final int tab = i;
            int x = panelX + SIDE + i * (TAB_WIDTH + TAB_GAP);
            GlassButtonWidget tabButton = new GlassButtonWidget(
                    x,
                    panelY + 36,
                    TAB_WIDTH,
                    TAB_HEIGHT,
                    Text.literal(String.valueOf(i + 1)),
                    () -> switchTab(tab)
            );
            tabButton.setSelected(i == selectedTab);
            addDrawableChild(tabButton);
        }

        if (editor != null) {
            editor.setFocused(true);
        }
    }

    private void startMacro() {
        saveCurrentSlot();
        executor.start(selectedTab);
    }

    private void switchTab(int newTab) {
        if (newTab == selectedTab) {
            return;
        }
        saveCurrentSlot();
        selectedTab = newTab;
        rebuildWidgets();
    }

    private void saveCurrentSlot() {
        if (editor == null || repetitionsField == null || delayField == null) {
            return;
        }

        MacroConfig.MacroSlot slot = config.getSlot(selectedTab);
        slot.setText(editor.getText());
        slot.setRepetitions(parseInt(repetitionsField.getText(), slot.getRepetitions(), 1, 1_000_000));
        slot.setDelayMs(parseInt(delayField.getText(), slot.getDelayMs(), 0, 86_400_000));
        config.save();
    }

    private static int parseInt(String value, int fallback, int min, int max) {
        try {
            if (value == null || value.isBlank()) {
                return fallback;
            }
            return Math.max(min, Math.min(max, Integer.parseInt(value)));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderDarkOverlay(context);
        drawPanel(context);
        drawFieldBackgrounds(context);
        super.render(context, mouseX, mouseY, delta);
        drawLabels(context);
        drawStatus(context);
    }

    private void renderDarkOverlay(DrawContext context) {
        context.fill(0, 0, width, height, 0xB9000000);
        int vignette = Math.max(1, Math.min(width, height) / 7);
        for (int i = 0; i < vignette; i += 8) {
            int alpha = Math.max(0, 34 - i / 4);
            context.fill(0, i, width, i + 8, (alpha << 24));
            context.fill(0, height - i - 8, width, height - i, (alpha << 24));
        }
    }

    private void drawPanel(DrawContext context) {
        context.fill(panelX, panelY, panelX + panelW, panelY + panelH, 0xD7121820);
        context.fill(panelX, panelY, panelX + panelW, panelY + 1, 0xE7768798);
        context.fill(panelX, panelY + panelH - 1, panelX + panelW, panelY + panelH, 0xA04D5965);
        context.fill(panelX, panelY, panelX + 1, panelY + panelH, 0xA04D5965);
        context.fill(panelX + panelW - 1, panelY, panelX + panelW, panelY + panelH, 0xA04D5965);

        // Subtle glass highlight and section separators.
        context.fill(panelX + 1, panelY + 1, panelX + panelW - 1, panelY + 2, 0x401F2A34);
        context.fill(panelX + SIDE, panelY + 62, panelX + panelW - SIDE, panelY + 63, 0x523A4652);
        context.fill(panelX + SIDE, panelY + panelH - FOOTER_HEIGHT, panelX + panelW - SIDE, panelY + panelH - FOOTER_HEIGHT + 1, 0x523A4652);
    }

    private void drawFieldBackgrounds(DrawContext context) {
        drawField(context, repetitionsField);
        drawField(context, delayField);
    }

    private void drawLabels(DrawContext context) {
        int labelY = panelY + panelH - 41;
        context.drawText(textRenderer, "Повторы", panelX + SIDE, labelY - 9, 0xFFA4AFBA, false);
        context.drawText(textRenderer, "Разрыв, мс", panelX + SIDE + 198, labelY - 9, 0xFFA4AFBA, false);

        context.drawText(textRenderer, "Макрос", panelX + SIDE, panelY + 13, 0xFFE9EEF3, false);
        context.drawText(textRenderer, "ПКМ здесь не нужен: открой / закрой окно клавишей Right Ctrl", panelX + SIDE, panelY + 23, 0xFF66737F, false);
    }

    private void drawField(DrawContext context, TextFieldWidget field) {
        if (field == null) return;
        int x = field.getX();
        int y = field.getY();
        context.fill(x, y, field.getRight(), field.getBottom(), field.isFocused() ? 0xCC182028 : 0x99202830);
        context.fill(x, y, field.getRight(), y + 1, field.isFocused() ? 0xB7A7B8C8 : 0x664E5A65);
        context.fill(x, field.getBottom() - 1, field.getRight(), field.getBottom(), 0x664E5A65);
    }

    private void drawStatus(DrawContext context) {
        String status;
        if (executor.isRunning()) {
            status = "● ВЫПОЛНЕНИЕ  ·  вкладка " + (executor.getRunningSlot() + 1) + "  ·  "
                    + executor.getCompletedRepetitions() + "/" + executor.getTargetRepetitions();
        } else {
            status = "● ГОТОВ";
        }
        context.drawText(textRenderer, status, panelX + panelW - SIDE - textRenderer.getWidth(status), panelY + panelH - 14, 0xFF8996A2, false);
    }

    @Override
    protected void setInitialFocus() {
        if (editor != null) {
            setInitialFocus(editor);
        }
    }

    @Override
    public void close() {
        saveCurrentSlot();
        MinecraftClient.getInstance().setScreen(null);
    }

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyInput input) {
        if (input.isEscape()) {
            close();
            return true;
        }
        if (input.getKeycode() == GLFW.GLFW_KEY_RIGHT_CONTROL) {
            close();
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public void removed() {
        saveCurrentSlot();
        config.save();
        super.removed();
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
