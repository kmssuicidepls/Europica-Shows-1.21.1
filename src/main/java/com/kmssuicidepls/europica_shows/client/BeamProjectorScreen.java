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

import java.util.Locale;
import java.util.function.Consumer;

public class BeamProjectorScreen extends AbstractContainerScreen<BeamProjectorMenu> {
    private static final int GUI_WIDTH = 200;
    private static final int GUI_HEIGHT = 176;
    private static final int SLIDER_HEIGHT = 20;

    private float yaw;
    private float pitch;
    private int red = 255, green = 255, blue = 255;

    private ValueSlider yawSlider, pitchSlider;
    private EditBox yawBox, pitchBox;
    private boolean syncing = false;

    public BeamProjectorScreen(BeamProjectorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = GUI_WIDTH;
        this.imageHeight = GUI_HEIGHT;
    }

    @Override
    protected void init() {
        super.init();

        BeamProjectorBlockEntity be = menu.getBlockEntity();
        if (be != null) {
            yaw = be.getYaw();
            pitch = be.getPitch();
            int c = be.getColor();
            red = (c >> 16) & 0xFF;
            green = (c >> 8) & 0xFF;
            blue = c & 0xFF;
        }

        int x = leftPos + 10;
        int sliderWidth = 136;
        int boxX = x + sliderWidth + 6;
        int boxWidth = 38;

        // --- Direction: slider for quick changes, text box for exact values ---
        int yawY = topPos + 22;
        yawSlider = addRenderableWidget(new ValueSlider(x, yawY, sliderWidth, SLIDER_HEIGHT,
                "Yaw", 0, 360, yaw, 1, v -> {
                    yaw = (float) (Math.round(v * 10.0) / 10.0);
                    setBoxText(yawBox, yaw);
                    sendUpdate();
                }));
        yawBox = addRenderableWidget(new EditBox(font, boxX, yawY, boxWidth, SLIDER_HEIGHT, Component.literal("Yaw")));
        yawBox.setFilter(s -> s.matches("-?\\d*\\.?\\d*"));
        yawBox.setValue(format(yaw));
        yawBox.setResponder(this::onYawText);

        int pitchY = topPos + 46;
        pitchSlider = addRenderableWidget(new ValueSlider(x, pitchY, sliderWidth, SLIDER_HEIGHT,
                "Pitch", -90, 90, pitch, 1, v -> {
                    pitch = (float) (Math.round(v * 10.0) / 10.0);
                    setBoxText(pitchBox, pitch);
                    sendUpdate();
                }));
        pitchBox = addRenderableWidget(new EditBox(font, boxX, pitchY, boxWidth, SLIDER_HEIGHT, Component.literal("Pitch")));
        pitchBox.setFilter(s -> s.matches("-?\\d*\\.?\\d*"));
        pitchBox.setValue(format(pitch));
        pitchBox.setResponder(this::onPitchText);

        // --- Color ---
        int fullWidth = GUI_WIDTH - 20;
        addRenderableWidget(new ValueSlider(x, topPos + 78, fullWidth, SLIDER_HEIGHT,
                "Red", 0, 255, red, 0, v -> { red = (int) Math.round(v); sendUpdate(); }));
        addRenderableWidget(new ValueSlider(x, topPos + 102, fullWidth, SLIDER_HEIGHT,
                "Green", 0, 255, green, 0, v -> { green = (int) Math.round(v); sendUpdate(); }));
        addRenderableWidget(new ValueSlider(x, topPos + 126, fullWidth, SLIDER_HEIGHT,
                "Blue", 0, 255, blue, 0, v -> { blue = (int) Math.round(v); sendUpdate(); }));
    }

    // ---- Text box handling ----

    private void onYawText(String text) {
        if (syncing) return;
        try {
            yaw = Mth.clamp(Float.parseFloat(text), 0.0f, 360.0f);
        } catch (NumberFormatException e) {
            return;   // incomplete input like "-" or "."
        }
        yawSlider.setReal(yaw);
        sendUpdate();
    }

    private void onPitchText(String text) {
        if (syncing) return;
        try {
            pitch = Mth.clamp(Float.parseFloat(text), -90.0f, 90.0f);
        } catch (NumberFormatException e) {
            return;
        }
        pitchSlider.setReal(pitch);
        sendUpdate();
    }

    private void setBoxText(EditBox box, float value) {
        if (box == null) return;
        syncing = true;
        box.setValue(format(value));
        syncing = false;
    }

    private static String format(float value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }

    private int packedColor() {
        return (red << 16) | (green << 8) | blue;
    }

    private void sendUpdate() {
        PacketDistributor.sendToServer(new UpdateBeamPayload(menu.getPos(), yaw, pitch, packedColor()));
    }

    // ---- Input: stop typing letters (like 'E') from closing the screen while a box is focused ----

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode != 256 /* ESC */ && (yawBox.canConsumeInput() || pitchBox.canConsumeInput())) {
            (yawBox.canConsumeInput() ? yawBox : pitchBox).keyPressed(keyCode, scanCode, modifiers);
            return true;
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

        // Live color preview
        int swatchY = topPos + 152;
        g.fill(leftPos + 9, swatchY - 1, leftPos + imageWidth - 9, swatchY + 17, 0xFF000000);
        g.fill(leftPos + 10, swatchY, leftPos + imageWidth - 10, swatchY + 16, 0xFF000000 | packedColor());
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, 10, 8, 0xFFFFFF, false);
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