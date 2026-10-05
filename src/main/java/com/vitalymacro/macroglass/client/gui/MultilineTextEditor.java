package com.vitalymacro.macroglass.client.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public final class MultilineTextEditor extends ClickableWidget {
    private static final int PADDING = 8;
    private static final int LINE_HEIGHT = 10;
    private static final int MAX_LENGTH = 20000;

    private final TextRenderer textRenderer;
    private String text;
    private int caret;
    private int anchor;
    private double scrollY;
    private int preferredX = -1;
    private boolean dragging;

    public MultilineTextEditor(TextRenderer textRenderer, int x, int y, int width, int height, String initialText) {
        super(x, y, width, height, Text.literal("Macro editor"));
        this.textRenderer = textRenderer;
        this.text = normalize(initialText);
        this.caret = this.text.length();
        this.anchor = this.caret;
        this.active = true;
        this.visible = true;
    }

    public String getText() {
        return text;
    }

    public void setTextValue(String value) {
        this.text = normalize(value);
        this.caret = Math.min(caret, text.length());
        this.anchor = Math.min(anchor, text.length());
        clampScroll();
    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        int bg = isFocused() ? 0xCC11161D : 0xAA0D1218;
        int border = isFocused() ? 0xEE7D8FA0 : 0x88515B66;
        context.fill(getX(), getY(), getRight(), getBottom(), bg);
        context.fill(getX(), getY(), getRight(), getY() + 1, border);
        context.fill(getX(), getBottom() - 1, getRight(), getBottom(), border);
        context.fill(getX(), getY(), getX() + 1, getBottom(), border);
        context.fill(getRight() - 1, getY(), getRight(), getBottom(), border);

        int innerX = getX() + PADDING;
        int innerY = getY() + PADDING;
        int innerW = getWidth() - PADDING * 2;
        int innerH = getHeight() - PADDING * 2;

        List<VisualLine> lines = buildVisualLines(innerW);
        int firstVisible = Math.max(0, (int) Math.floor(scrollY / LINE_HEIGHT));
        int lastVisible = Math.min(lines.size(), firstVisible + innerH / LINE_HEIGHT + 2);

        context.enableScissor(innerX, innerY, innerX + innerW, innerY + innerH);

        for (int i = firstVisible; i < lastVisible; i++) {
            VisualLine line = lines.get(i);
            int y = innerY + i * LINE_HEIGHT - (int) scrollY;
            String lineText = text.substring(line.start, line.end);

            if (hasSelection()) {
                drawSelectionForLine(context, line, i, y);
            }

            context.drawText(textRenderer, lineText, innerX, y, 0xFFE3E8ED, false);
        }

        if (isFocused()) {
            int[] caretPos = caretScreenPosition(lines, innerX, innerY);
            int cx = caretPos[0];
            int cy = caretPos[1];
            boolean showCaret = ((System.currentTimeMillis() / 500L) & 1L) == 0L;
            if (showCaret) {
                context.fill(cx, cy - 1, cx + 1, cy + 9, 0xFFE7F0F7);
            }
        }

        context.disableScissor();

        if (lines.size() * LINE_HEIGHT > innerH) {
            int trackX = getRight() - 5;
            int trackTop = innerY;
            int trackBottom = innerY + innerH;
            context.fill(trackX, trackTop, trackX + 2, trackBottom, 0x553D4852);
            int maxScroll = Math.max(1, lines.size() * LINE_HEIGHT - innerH);
            int thumbHeight = Math.max(18, (int) ((innerH / (double) (lines.size() * LINE_HEIGHT)) * innerH));
            int thumbY = trackTop + (int) ((trackBottom - trackTop - thumbHeight) * (scrollY / maxScroll));
            context.fill(trackX, thumbY, trackX + 2, thumbY + thumbHeight, 0xB7A1ACB8);
        }
    }

    private void drawSelectionForLine(DrawContext context, VisualLine line, int visualIndex, int y) {
        int selectionStart = Math.min(anchor, caret);
        int selectionEnd = Math.max(anchor, caret);
        int from = Math.max(selectionStart, line.start);
        int to = Math.min(selectionEnd, line.end);
        if (from >= to && !(selectionStart == selectionEnd && selectionStart == line.start)) {
            return;
        }

        int x1 = getX() + PADDING + textRenderer.getWidth(text.substring(line.start, from));
        int x2 = getX() + PADDING + textRenderer.getWidth(text.substring(line.start, to));
        if (from == to && selectionStart != selectionEnd) {
            return;
        }
        context.fill(x1, y - 1, Math.max(x1 + 1, x2), y + 9, 0x80527EA5);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (click.button() != 0 || !isMouseOver(click.x(), click.y())) {
            return false;
        }

        setFocused(true);
        dragging = true;
        int position = positionFromMouse(click.x(), click.y());
        if (doubled) {
            int[] word = wordBounds(position);
            anchor = word[0];
            caret = word[1];
        } else {
            caret = position;
            anchor = position;
        }
        preferredX = -1;
        return true;
    }

    @Override
    public boolean mouseDragged(Click click, double offsetX, double offsetY) {
        if (!dragging || click.button() != 0) {
            return false;
        }
        caret = positionFromMouse(click.x(), click.y());
        ensureCaretVisible();
        return true;
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (click.button() == 0 && dragging) {
            dragging = false;
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (!isMouseOver(mouseX, mouseY)) {
            return false;
        }
        scrollY -= verticalAmount * LINE_HEIGHT * 3.0;
        clampScroll();
        return true;
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (!isFocused()) {
            return false;
        }

        int key = input.getKeycode();
        boolean ctrl = input.hasCtrlOrCmd();
        boolean shift = input.hasShift();

        if (ctrl && input.isSelectAll()) {
            anchor = 0;
            caret = text.length();
            preferredX = -1;
            return true;
        }
        if (ctrl && input.isCopy()) {
            copySelection();
            return true;
        }
        if (ctrl && input.isCut()) {
            copySelection();
            deleteSelection();
            return true;
        }
        if (ctrl && input.isPaste()) {
            insertText(MinecraftClient.getInstance().keyboard.getClipboard());
            return true;
        }

        if (input.isLeft()) {
            moveHorizontal(-1, shift, ctrl);
            return true;
        }
        if (input.isRight()) {
            moveHorizontal(1, shift, ctrl);
            return true;
        }
        if (input.isUp()) {
            moveVertical(-1, shift);
            return true;
        }
        if (input.isDown()) {
            moveVertical(1, shift);
            return true;
        }

        if (key == GLFW.GLFW_KEY_HOME) {
            setCaret(lineStart(caret), shift);
            preferredX = -1;
            return true;
        }
        if (key == GLFW.GLFW_KEY_END) {
            setCaret(lineEnd(caret), shift);
            preferredX = -1;
            return true;
        }
        if (key == GLFW.GLFW_KEY_BACKSPACE) {
            if (hasSelection()) {
                deleteSelection();
            } else if (caret > 0) {
                int from = ctrl ? wordStart(caret) : caret - 1;
                replaceRange(from, caret, "");
                caret = from;
                anchor = from;
            }
            preferredX = -1;
            return true;
        }
        if (key == GLFW.GLFW_KEY_DELETE) {
            if (hasSelection()) {
                deleteSelection();
            } else if (caret < text.length()) {
                int to = ctrl ? wordEnd(caret) : caret + 1;
                replaceRange(caret, to, "");
                anchor = caret;
            }
            preferredX = -1;
            return true;
        }
        if (input.isEnter()) {
            replaceSelection("\n");
            return true;
        }
        if (input.isTab()) {
            replaceSelection("    ");
            return true;
        }

        return false;
    }

    @Override
    public boolean charTyped(CharInput input) {
        if (!isFocused() || !input.isValidChar()) {
            return false;
        }
        String value = input.asString();
        if (value.isEmpty()) {
            return false;
        }
        insertText(value);
        return true;
    }

    @Override
    public void setFocused(boolean focused) {
        super.setFocused(focused);
        if (focused) {
            ensureCaretVisible();
        }
    }

    private void insertText(String value) {
        if (value == null || value.isEmpty()) {
            return;
        }
        value = normalize(value);
        if (value.length() > MAX_LENGTH) {
            value = value.substring(0, MAX_LENGTH);
        }
        int selected = Math.abs(caret - anchor);
        int available = MAX_LENGTH - (text.length() - selected);
        if (available <= 0) {
            return;
        }
        if (value.length() > available) {
            value = value.substring(0, available);
        }
        replaceSelection(value);
    }

    private void replaceSelection(String value) {
        int from = Math.min(caret, anchor);
        int to = Math.max(caret, anchor);
        replaceRange(from, to, value);
        caret = from + value.length();
        anchor = caret;
        preferredX = -1;
        ensureCaretVisible();
    }

    private void replaceRange(int from, int to, String value) {
        text = text.substring(0, from) + value + text.substring(to);
    }

    private void deleteSelection() {
        if (!hasSelection()) {
            return;
        }
        replaceSelection("");
    }

    private boolean hasSelection() {
        return caret != anchor;
    }

    private void copySelection() {
        if (!hasSelection()) {
            return;
        }
        int from = Math.min(caret, anchor);
        int to = Math.max(caret, anchor);
        MinecraftClient.getInstance().keyboard.setClipboard(text.substring(from, to));
    }

    private void setCaret(int newCaret, boolean extend) {
        newCaret = Math.max(0, Math.min(text.length(), newCaret));
        if (!extend) {
            anchor = newCaret;
        }
        caret = newCaret;
        ensureCaretVisible();
    }

    private void moveHorizontal(int direction, boolean extend, boolean byWord) {
        if (!extend && hasSelection()) {
            caret = direction < 0 ? Math.min(caret, anchor) : Math.max(caret, anchor);
            anchor = caret;
            return;
        }

        int next = caret;
        if (byWord) {
            next = direction < 0 ? wordStart(caret) : wordEnd(caret);
        } else {
            next = Math.max(0, Math.min(text.length(), caret + direction));
        }
        setCaret(next, extend);
        preferredX = -1;
    }

    private void moveVertical(int direction, boolean extend) {
        List<VisualLine> lines = buildVisualLines(getWidth() - PADDING * 2);
        if (lines.isEmpty()) {
            return;
        }
        int lineIndex = visualLineIndexForCaret(lines, caret);
        int target = Math.max(0, Math.min(lines.size() - 1, lineIndex + direction));
        if (target == lineIndex) {
            if (!extend) {
                anchor = caret;
            }
            return;
        }

        int currentLineStart = lines.get(lineIndex).start;
        int x = preferredX >= 0 ? preferredX : textRenderer.getWidth(text.substring(currentLineStart, caret));
        preferredX = x;
        VisualLine targetLine = lines.get(target);
        int targetCaret = characterAtX(targetLine, x);
        setCaret(targetCaret, extend);
    }

    private int positionFromMouse(double mouseX, double mouseY) {
        int innerX = getX() + PADDING;
        int innerY = getY() + PADDING;
        int innerW = getWidth() - PADDING * 2;
        List<VisualLine> lines = buildVisualLines(innerW);
        if (lines.isEmpty()) {
            return 0;
        }

        int index = (int) Math.floor((mouseY - innerY + scrollY) / LINE_HEIGHT);
        index = Math.max(0, Math.min(lines.size() - 1, index));
        return characterAtX(lines.get(index), (int) Math.round(mouseX - innerX));
    }

    private int characterAtX(VisualLine line, int relativeX) {
        if (relativeX <= 0) {
            return line.start;
        }
        String value = text.substring(line.start, line.end);
        if (value.isEmpty()) {
            return line.start;
        }

        int best = line.start;
        int bestDistance = Integer.MAX_VALUE;
        for (int i = 0; i <= value.length(); i++) {
            int width = textRenderer.getWidth(value.substring(0, i));
            int distance = Math.abs(width - relativeX);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = line.start + i;
            }
        }
        return best;
    }

    private int[] caretScreenPosition(List<VisualLine> lines, int innerX, int innerY) {
        int lineIndex = visualLineIndexForCaret(lines, caret);
        VisualLine line = lines.get(lineIndex);
        int local = Math.max(0, Math.min(caret, line.end) - line.start);
        int x = innerX + textRenderer.getWidth(text.substring(line.start, line.start + local));
        int y = innerY + lineIndex * LINE_HEIGHT - (int) scrollY;
        return new int[]{x, y};
    }

    private int visualLineIndexForCaret(List<VisualLine> lines, int position) {
        for (int i = 0; i < lines.size(); i++) {
            VisualLine line = lines.get(i);
            if (position >= line.start && position <= line.end) {
                if (position == line.end && i + 1 < lines.size() && lines.get(i + 1).start == position + 1) {
                    continue;
                }
                return i;
            }
        }
        return lines.size() - 1;
    }

    private List<VisualLine> buildVisualLines(int maxWidth) {
        List<VisualLine> lines = new ArrayList<>();
        if (text.isEmpty()) {
            lines.add(new VisualLine(0, 0));
            return lines;
        }

        int lineStart = 0;
        while (lineStart <= text.length()) {
            int newline = text.indexOf('\n', lineStart);
            int lineEnd = newline >= 0 ? newline : text.length();

            if (lineStart == lineEnd) {
                lines.add(new VisualLine(lineStart, lineEnd));
            } else {
                int pos = lineStart;
                while (pos < lineEnd) {
                    int end = pos + 1;
                    while (end <= lineEnd && textRenderer.getWidth(text.substring(pos, end)) <= maxWidth) {
                        end++;
                    }
                    end--;
                    if (end <= pos) {
                        end = pos + 1;
                    }
                    lines.add(new VisualLine(pos, end));
                    pos = end;
                }
            }

            if (newline < 0) {
                break;
            }
            lineStart = newline + 1;
            if (lineStart == text.length()) {
                lines.add(new VisualLine(lineStart, lineStart));
                break;
            }
        }
        return lines;
    }

    private void ensureCaretVisible() {
        List<VisualLine> lines = buildVisualLines(getWidth() - PADDING * 2);
        if (lines.isEmpty()) {
            return;
        }
        int lineIndex = visualLineIndexForCaret(lines, caret);
        int innerH = getHeight() - PADDING * 2;
        double top = lineIndex * LINE_HEIGHT;
        double bottom = top + LINE_HEIGHT;
        if (top < scrollY) {
            scrollY = top;
        } else if (bottom > scrollY + innerH) {
            scrollY = bottom - innerH;
        }
        clampScroll();
    }

    private void clampScroll() {
        int innerH = Math.max(1, getHeight() - PADDING * 2);
        int lineCount = buildVisualLines(Math.max(1, getWidth() - PADDING * 2)).size();
        double max = Math.max(0, lineCount * LINE_HEIGHT - innerH);
        scrollY = Math.max(0, Math.min(max, scrollY));
    }

    private int lineStart(int position) {
        int index = text.lastIndexOf('\n', Math.max(0, position - 1));
        return index < 0 ? 0 : index + 1;
    }

    private int lineEnd(int position) {
        int index = text.indexOf('\n', position);
        return index < 0 ? text.length() : index;
    }

    private int wordStart(int position) {
        int p = Math.max(0, Math.min(text.length(), position));
        while (p > 0 && Character.isWhitespace(text.charAt(p - 1))) p--;
        while (p > 0 && !Character.isWhitespace(text.charAt(p - 1))) p--;
        return p;
    }

    private int wordEnd(int position) {
        int p = Math.max(0, Math.min(text.length(), position));
        while (p < text.length() && Character.isWhitespace(text.charAt(p))) p++;
        while (p < text.length() && !Character.isWhitespace(text.charAt(p))) p++;
        return p;
    }

    private int[] wordBounds(int position) {
        int start = wordStart(position);
        int end = wordEnd(position);
        if (start == end && start < text.length()) {
            end++;
        }
        return new int[]{start, end};
    }

    private static String normalize(String value) {
        if (value == null) return "";
        return value.replace("\r\n", "\n").replace('\r', '\n');
    }

    private record VisualLine(int start, int end) {
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
        appendDefaultNarrations(builder);
    }
}
