package com.kmssuicidepls.europica_shows.client;

import com.kmssuicidepls.europica_shows.block.entity.BeamProjectorBlockEntity;
import com.kmssuicidepls.europica_shows.menu.BeamProjectorMenu;
import com.kmssuicidepls.europica_shows.network.UpdateBeamPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public class BeamProjectorScreen extends AbstractContainerScreen<BeamProjectorMenu> {
    private static final int GUI_WIDTH = 330;
    private static final int GUI_HEIGHT = 180;
    private static final int ROW_HEIGHT = 20;
    private static final int ROW_STEP = 24;
    private static final int SLIDER_WIDTH = 106;
    private static final int BOX_WIDTH = 38;
    private static final int COLUMN_WIDTH = SLIDER_WIDTH + 4 + BOX_WIDTH;   // slider + gap + text box
    private static final int RIGHT_COLUMN_X = 170;

    private float yaw, pitch, startDistance, offsetX, offsetY, offsetZ;
    private int red = 255, green = 255, blue = 255;

    private final List<EditBox> boxes = new ArrayList<>();
    private boolean syncing = false;

    public BeamProjectorScreen(BeamProjectorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = GUI_WIDTH;
        this.imageHeight = GUI_HEIGHT;
    }

    @Override
    protected void init() {
        super.init();
        boxes.clear();

        BeamProjectorBlockEntity be = menu.getBlockEntity();
        if (be != null) {
            yaw = be.getYaw();
            pitch = be.getPitch();
            startDistance = be.getStartDistance();
            offsetX = be.getOffsetX();
            offsetY = be.getOffsetY();
            offsetZ = be.getOffsetZ();
            int c = be.getColor();
            red = (c >> 16) & 0xFF;
            green = (c >> 8) & 0xFF;
            blue = c & 0xFF;
        }

        int left = leftPos + 10;
        int right = leftPos + RIGHT_COLUMN_X;
        int top = topPos + 22;

        // Left column: direction and start distance
        new NumberRow(left, top, "Yaw", 0, 360, yaw, v -> yaw = v);
        new NumberRow(left, top + ROW_STEP, "Pitch", -90, 90, pitch, v -> pitch = v);
        new NumberRow(left, top + ROW_STEP * 2, "Start", 0, BeamProjectorBlockEntity.MAX_START,
                startDistance, v -> startDistance = v);

        // Right column: position offset
        float max = BeamProjectorBlockEntity.MAX_OFFSET;
        new NumberRow(right, top, "Offset X", -max, max, offsetX, v -> offsetX = v);
        new NumberRow(right, top + ROW_STEP, "Offset Y", -max, max, offsetY, v -> offsetY = v);
        new NumberRow(right, top + ROW_STEP * 2, "Offset Z", -max, max, offsetZ, v -> offsetZ = v);

        // Color sliders (left, lower), preview swatch is drawn on the right
        int colorTop = topPos + 100;
        addRenderableWidget(new ValueSlider(left, colorTop, COLUMN_WIDTH, ROW_HEIGHT,
                "Red", 0, 255, red, 0, v -> { red = (int) Math.round(v); sendUpdate(); }));
        addRenderableWidget(new ValueSlider(left, colorTop + ROW_STEP, COLUMN_WIDTH, ROW_HEIGHT,
                "Green", 0, 255, green, 0, v -> { green = (int) Math.round(v); sendUpdate(); }));
        addRenderableWidget(new ValueSlider(left, colorTop + ROW_STEP * 2, COLUMN_WIDTH, ROW_HEIGHT,
                "Blue", 0, 255, blue, 0, v -> { blue = (int) Math.round(v); sendUpdate(); }));
    }

    // ---- A slider plus a text box for exact values, kept in sync with each other ----

    private class NumberRow {
        private final ValueSlider slider;
        private final EditBox box;
        private final float min;
        private final float max;
        private final Consumer<Float> setter;

        NumberRow(int x, int y, String label, float min, float max, float initial, Consumer<Float> setter) {
            this.min = min;
            this.max = max;
            this.setter = setter;

            this.slider = new ValueSlider(x, y, SLIDER_WIDTH, ROW_HEIGHT, label, min, max, initial, 1, v -> {
                float rounded = (float) (Math.round(v * 10.0) / 10.0);
                this.setter.accept(rounded);
                setBoxText(rounded);
                sendUpdate();
            });

            this.box = new EditBox(font, x + SLIDER_WIDTH + 4, y, BOX_WIDTH, ROW_HEIGHT, Component.literal(label));
            this.box.setFilter(s -> s.matches("-?\\d*\\.?\\d*"));
            this.box.setValue(format(initial));
            this.box.setResponder(this::onText);

            addRenderableWidget(slider);
            addRenderableWidget(box);
            boxes.add(box);
        }

        private void setBoxText(float value) {
            syncing = true;
            box.setValue(format(value));
            syncing = false;
        }

        private void onText(String text) {
            if (syncing) return;
            try {
                float value = Mth.clamp(Float.parseFloat(text), min, max);
                setter.accept(value);
                slider.setReal(value);
                sendUpdate();
            } catch (NumberFormatException ignored) {
                // incomplete input like "-" or "."
            }
        }
    }

    private static String format(float value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }

    private int packedColor() {
        return (red << 16) | (green << 8) | blue;
    }

    private void sendUpdate() {
        PacketDistributor.sendToServer(new UpdateBeamPayload(
                menu.getPos(), yaw, pitch, packedColor(),
                new Vector3f(offsetX, offsetY, offsetZ), startDistance));
    }

    // ---- Input ----

    // Stop typing letters (like 'E') from closing the screen while a text box is focused
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

    // AbstractContainerScreen overrides mouseDragged for item-slot dragging and never forwards it
    // to widgets, so sliders could be clicked but not dragged. Forward it ourselves.
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
        // Panel with a simple border
        g.fill(leftPos - 1, topPos - 1, leftPos + imageWidth + 1, topPos + imageHeight + 1, 0xFF000000);
        g.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF3A3A3A);

        // Live color preview (right column, under the offset rows)
        int x0 = leftPos + RIGHT_COLUMN_X;
        int y0 = topPos + 100;
        int x1 = x0 + COLUMN_WIDTH;
        int y1 = y0 + ROW_STEP * 2 + ROW_HEIGHT;
        g.fill(x0 - 1, y0 - 1, x1 + 1, y1 + 1, 0xFF000000);
        BeamProjectorBlockEntity be = menu.getBlockEntity();
        int shown = be != null && be.hasGlassColor() ? be.getGlassColor() : packedColor();
        g.fill(x0, y0, x1, y1, 0xFF000000 | shown);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, 10, 8, 0xFFFFFF, false);
        g.drawString(font, "Position offset (blocks)", RIGHT_COLUMN_X, 8, 0xAAAAAA, false);
    }

    // ---- Slider that maps 0..1 onto a real range ----

    private static class ValueSlider extends AbstractSliderButton {
        private final String label;
        private final double min;
        private final double max;
        private final int decimals;
        private final Consumer<Double> onChange;

        ValueSlider(int x, int y, int width, int height, String label,
                    double min, double max, double start, int decimals, Consumer<Double> onChange) {
            super(x, y, width, height, Component.empty(), (start - min) / (max - min));
            this.label = label;
            this.min = min;
            this.max = max;
            this.decimals = decimals;
            this.onChange = onChange;
            updateMessage();
        }

        private double real() {
            return min + value * (max - min);
        }

        /** Moves the slider without firing the change callback (used by the text boxes). */
        void setReal(double real) {
            this.value = Mth.clamp((real - min) / (max - min), 0.0, 1.0);
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.literal(label + ": " + String.format(Locale.ROOT, "%." + decimals + "f", real())));
        }

        @Override
        protected void applyValue() {
            onChange.accept(real());
        }
    }
}