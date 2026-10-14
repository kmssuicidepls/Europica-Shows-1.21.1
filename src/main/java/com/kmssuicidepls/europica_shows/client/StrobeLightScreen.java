package com.kmssuicidepls.europica_shows.client;

import com.kmssuicidepls.europica_shows.block.entity.StrobeLightBlockEntity;
import com.kmssuicidepls.europica_shows.block.entity.StrobeLightBlockEntity.RedstoneMode;
import com.kmssuicidepls.europica_shows.menu.StrobeLightMenu;
import com.kmssuicidepls.europica_shows.network.UpdateStrobePayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.IntConsumer;

public class StrobeLightScreen extends AbstractContainerScreen<StrobeLightMenu> {
    private static final int GUI_WIDTH = 220;
    private static final int GUI_HEIGHT = 136;
    private static final int ROW_HEIGHT = 20;
    private static final int SLIDER_WIDTH = 150;
    private static final int BOX_WIDTH = 46;
    private static final int FULL_WIDTH = SLIDER_WIDTH + 4 + BOX_WIDTH;

    private boolean enabled = false;
    private RedstoneMode mode = RedstoneMode.IGNORED;
    private int gapTicks = 10;
    private int flashTicks = 4;

    private final List<EditBox> boxes = new ArrayList<>();
    private boolean syncing = false;
    private NumberRow gapRow;

    public StrobeLightScreen(StrobeLightMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = GUI_WIDTH;
        this.imageHeight = GUI_HEIGHT;
    }

    @Override
    protected void init() {
        super.init();
        boxes.clear();

        StrobeLightBlockEntity be = menu.getBlockEntity();
        if (be != null) {
            enabled = be.isEnabled();
            mode = be.getMode();
            gapTicks = be.getGapTicks();
            flashTicks = be.getFlashTicks();
        }

        int left = leftPos + 10;
        int top = topPos + 22;

        // Manual on / off
        addRenderableWidget(Button.builder(powerLabel(), b -> {
            enabled = !enabled;
            b.setMessage(powerLabel());
            sendUpdate();
        }).bounds(left, top, FULL_WIDTH, ROW_HEIGHT).build());

        // Redstone behaviour: Ignored -> While Powered -> Once Per Pulse
        addRenderableWidget(Button.builder(modeLabel(), b -> {
            mode = mode.next();
            b.setMessage(modeLabel());
            gapRow.setActive(mode != RedstoneMode.PER_PULSE);
            sendUpdate();
        }).bounds(left, top + 24, FULL_WIDTH, ROW_HEIGHT).build());

        // Time between flashes, and how long each flash lasts (in ticks, 20 ticks = 1 second)
        gapRow = new NumberRow(left, topPos + 82, "Gap", 1, StrobeLightBlockEntity.MAX_GAP,
                gapTicks, v -> gapTicks = v);
        new NumberRow(left, topPos + 106, "Flash", 1, StrobeLightBlockEntity.MAX_FLASH,
                flashTicks, v -> flashTicks = v);

        // Between pulses there is no "gap", so grey it out in pulse mode
        gapRow.setActive(mode != RedstoneMode.PER_PULSE);
    }

    private Component powerLabel() {
        return Component.literal("Power: " + (enabled ? "ON" : "OFF"));
    }

    private Component modeLabel() {
        return Component.literal("Redstone: " + mode.label);
    }

    private String modeHint() {
        return switch (mode) {
            case IGNORED -> "Strobes nonstop while the power is ON";
            case WHILE_POWERED -> "Strobes only while it has a redstone signal";
            case PER_PULSE -> "One flash each time a redstone pulse arrives";
        };
    }

    // ---- A slider plus a text box for exact values, kept in sync with each other ----

    private class NumberRow {
        private final ValueSlider slider;
        private final EditBox box;
        private final int min;
        private final int max;
        private final IntConsumer setter;

        NumberRow(int x, int y, String label, int min, int max, int initial, IntConsumer setter) {
            this.min = min;
            this.max = max;
            this.setter = setter;

            this.slider = new ValueSlider(x, y, SLIDER_WIDTH, ROW_HEIGHT, label, min, max, initial, v -> {
                this.setter.accept(v);
                setBoxText(v);
                sendUpdate();
            });

            this.box = new EditBox(font, x + SLIDER_WIDTH + 4, y, BOX_WIDTH, ROW_HEIGHT, Component.literal(label));
            this.box.setFilter(s -> s.matches("\\d{0,3}"));
            this.box.setValue(Integer.toString(initial));
            this.box.setResponder(this::onText);

            addRenderableWidget(slider);
            addRenderableWidget(box);
            boxes.add(box);
        }

        void setActive(boolean active) {
            slider.active = active;
            box.setEditable(active);
        }

        private void setBoxText(int value) {
            syncing = true;
            box.setValue(Integer.toString(value));
            syncing = false;
        }

        private void onText(String text) {
            if (syncing || text.isEmpty()) return;
            try {
                int value = Mth.clamp(Integer.parseInt(text), min, max);
                setter.accept(value);
                slider.setReal(value);
                sendUpdate();
            } catch (NumberFormatException ignored) {
                // not a number yet
            }
        }
    }

    private void sendUpdate() {
        PacketDistributor.sendToServer(new UpdateStrobePayload(
                menu.getPos(), enabled, mode.ordinal(), gapTicks, flashTicks));
    }

    // ---- Input ----

    // Stop typing digits / letters (like 'E') from closing the screen while a text box is focused
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode != 256 /* ESC */) {
            for (EditBox box : boxes) {
                if (box.canConsumeInput()) {
                    box.keyPressed(keyCode, scanCode, modifiers);
                    return true;
                }
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    // AbstractContainerScreen never forwards drags to widgets, so sliders could be clicked but not dragged.
    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        GuiEventListener focused = getFocused();
        if (focused != null && isDragging() && button == 0
                && focused.mouseDragged(mouseX, mouseY, button, dragX, dragY)) {
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    // ---- Rendering ----

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        g.fill(leftPos - 1, topPos - 1, leftPos + imageWidth + 1, topPos + imageHeight + 1, 0xFF000000);
        g.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF3A3A3A);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, 10, 8, 0xFFFFFF, false);
        g.drawString(font, modeHint(), 10, 70, 0xAAAAAA, false);
    }

    // ---- Slider that maps 0..1 onto a whole-number range ----

    private static class ValueSlider extends AbstractSliderButton {
        private final String label;
        private final int min;
        private final int max;
        private final IntConsumer onChange;

        ValueSlider(int x, int y, int width, int height, String label,
                    int min, int max, int start, IntConsumer onChange) {
            super(x, y, width, height, Component.empty(), (double) (start - min) / (max - min));
            this.label = label;
            this.min = min;
            this.max = max;
            this.onChange = onChange;
            updateMessage();
        }

        private int real() {
            return (int) Math.round(min + value * (max - min));
        }

        /** Moves the slider without firing the change callback (used by the text boxes). */
        void setReal(int real) {
            this.value = Mth.clamp((double) (real - min) / (max - min), 0.0, 1.0);
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            int ticks = real();
            setMessage(Component.literal(label + ": " + ticks + " ticks ("
                    + String.format(Locale.ROOT, "%.2f", ticks / 20.0) + "s)"));
        }

        @Override
        protected void applyValue() {
            onChange.accept(real());
        }
    }
}