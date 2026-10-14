package com.kmssuicidepls.europica_shows.client;

import com.kmssuicidepls.europica_shows.block.entity.WaterSpoutBlockEntity;
import com.kmssuicidepls.europica_shows.menu.WaterSpoutMenu;
import com.kmssuicidepls.europica_shows.network.UpdateSpoutPayload;
import com.kmssuicidepls.europica_shows.spout.SpoutParticles;
import com.kmssuicidepls.europica_shows.spout.SpoutParticles.Kind;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public class WaterSpoutScreen extends AbstractContainerScreen<WaterSpoutMenu> {
    private enum Page { MAIN, OPTIONS, PICK_PARTICLE, PICK_BLOCK, PICK_ITEM }

    private static final int GUI_WIDTH = 330;
    private static final int ROW_HEIGHT = 20;
    private static final int ROW_STEP = 24;
    private static final int SLIDER_WIDTH = 106;
    private static final int BOX_WIDTH = 38;
    private static final int COLUMN_WIDTH = SLIDER_WIDTH + 4 + BOX_WIDTH;
    private static final int RIGHT_COLUMN_X = 170;
    private static final int LIST_ROWS = 11;

    // Lists for the block / item pickers, built once on first use
    private static List<String> blockIds;
    private static List<String> itemIds;

    // ---- Current settings (mirrors the block entity) ----
    private String particleId = SpoutParticles.DEFAULT_PARTICLE;
    private String blockId = SpoutParticles.DEFAULT_BLOCK;
    private String itemId = SpoutParticles.DEFAULT_ITEM;
    private final int[] colorA = {255, 255, 255};   // defaults to white
    private final int[] colorB = {255, 255, 255};
    private float size = 1.0f;
    private int count = 1;
    private float spread = 0.1f;
    private float offsetX, offsetY, offsetZ, startHeight;

    // ---- UI state ----
    private Page page = Page.MAIN;
    private boolean loaded = false;
    private boolean editingTo = false;
    private boolean syncing = false;
    private final List<EditBox> boxes = new ArrayList<>();
    private ListPicker picker;

    public WaterSpoutScreen(WaterSpoutMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    // =====================================================================
    //  Layout
    // =====================================================================

    @Override
    protected void init() {
        this.imageWidth = GUI_WIDTH;
        this.imageHeight = switch (page) {
            case MAIN -> 130;
            case OPTIONS -> 180;
            default -> 200;
        };
        super.init();

        boxes.clear();
        picker = null;

        if (!loaded) {
            loadFromBlockEntity();
            loaded = true;
        }

        switch (page) {
            case MAIN -> buildMain();
            case OPTIONS -> buildOptions();
            case PICK_PARTICLE -> buildPicker(particleNames(), id -> {
                particleId = id;
                editingTo = false;
                page = kind() == Kind.SIMPLE ? Page.MAIN : Page.OPTIONS;   // specialised ones open their options
                sendUpdate();
                rebuildWidgets();
            }, Page.MAIN);
            case PICK_BLOCK -> buildPicker(blockNames(), id -> {
                blockId = id;
                page = Page.OPTIONS;
                sendUpdate();
                rebuildWidgets();
            }, Page.OPTIONS);
            case PICK_ITEM -> buildPicker(itemNames(), id -> {
                itemId = id;
                page = Page.OPTIONS;
                sendUpdate();
                rebuildWidgets();
            }, Page.OPTIONS);
        }
    }

    private void loadFromBlockEntity() {
        WaterSpoutBlockEntity be = menu.getBlockEntity();
        if (be == null) return;
        particleId = be.getParticleId();
        blockId = be.getBlockId();
        itemId = be.getItemId();
        unpack(be.getColor(), colorA);
        unpack(be.getColor2(), colorB);
        size = be.getSize();
        count = be.getCount();
        spread = be.getSpread();
        offsetX = be.getOffsetX();
        offsetY = be.getOffsetY();
        offsetZ = be.getOffsetZ();
        startHeight = be.getStartHeight();
    }

    private void buildMain() {
        int left = leftPos + 10;
        int right = leftPos + RIGHT_COLUMN_X;
        int top = topPos + 22;

        // Left column: particle choice + how it spawns
        addRenderableWidget(Button.builder(Component.literal("Particle: " + shortName(particleId)), b -> {
            page = Page.PICK_PARTICLE;
            rebuildWidgets();
        }).bounds(left, top, COLUMN_WIDTH, ROW_HEIGHT).build());

        new NumberRow(left, top + ROW_STEP, "Count", 1, WaterSpoutBlockEntity.MAX_COUNT, 0, count,
                v -> count = Math.round(v));
        new NumberRow(left, top + ROW_STEP * 2, "Spread", 0, WaterSpoutBlockEntity.MAX_SPREAD, 2, spread,
                v -> spread = v);
        new NumberRow(left, top + ROW_STEP * 3, "Start", 0, WaterSpoutBlockEntity.MAX_START, 1, startHeight,
                v -> startHeight = v);

        // Right column: position offset
        float max = WaterSpoutBlockEntity.MAX_OFFSET;
        new NumberRow(right, top, "Offset X", -max, max, 1, offsetX, v -> offsetX = v);
        new NumberRow(right, top + ROW_STEP, "Offset Y", -max, max, 1, offsetY, v -> offsetY = v);
        new NumberRow(right, top + ROW_STEP * 2, "Offset Z", -max, max, 1, offsetZ, v -> offsetZ = v);

        Kind kind = kind();
        Button options = Button.builder(
                Component.literal(kind == Kind.SIMPLE ? "No extra options" : "Particle options..."), b -> {
                    page = Page.OPTIONS;
                    rebuildWidgets();
                }).bounds(right, top + ROW_STEP * 3, COLUMN_WIDTH, ROW_HEIGHT).build();
        options.active = kind != Kind.SIMPLE;
        addRenderableWidget(options);
    }

    private void buildOptions() {
        int left = leftPos + 10;
        int top = topPos + 22;
        Kind kind = kind();

        switch (kind) {
            case DUST, DUST_TRANSITION, ENTITY_EFFECT -> {
                int row = 0;
                if (kind == Kind.DUST_TRANSITION) {
                    addRenderableWidget(Button.builder(
                            Component.literal("Editing: " + (editingTo ? "To color" : "From color")), b -> {
                                editingTo = !editingTo;
                                rebuildWidgets();
                            }).bounds(left, top, COLUMN_WIDTH, ROW_HEIGHT).build());
                    row = 1;
                }
                int[] target = editingTo ? colorB : colorA;
                addColorSlider(left, top + ROW_STEP * row, "Red", target, 0);
                addColorSlider(left, top + ROW_STEP * (row + 1), "Green", target, 1);
                addColorSlider(left, top + ROW_STEP * (row + 2), "Blue", target, 2);
                if (kind != Kind.ENTITY_EFFECT) {
                    new NumberRow(left, top + ROW_STEP * (row + 3), "Size", 0.1f, 4.0f, 1, size, v -> size = v);
                }
            }
            case BLOCK -> addRenderableWidget(Button.builder(
                    Component.literal("Block: " + shortName(blockId)), b -> {
                        page = Page.PICK_BLOCK;
                        rebuildWidgets();
                    }).bounds(left, top, COLUMN_WIDTH, ROW_HEIGHT).build());
            case ITEM -> addRenderableWidget(Button.builder(
                    Component.literal("Item: " + shortName(itemId)), b -> {
                        page = Page.PICK_ITEM;
                        rebuildWidgets();
                    }).bounds(left, top, COLUMN_WIDTH, ROW_HEIGHT).build());
            default -> { }
        }

        addRenderableWidget(Button.builder(Component.literal("Done"), b -> {
            page = Page.MAIN;
            rebuildWidgets();
        }).bounds(left, topPos + imageHeight - 30, 100, ROW_HEIGHT).build());
    }

    private void addColorSlider(int x, int y, String label, int[] target, int index) {
        addRenderableWidget(new ValueSlider(x, y, COLUMN_WIDTH, ROW_HEIGHT, label, 0, 255, target[index], 0, v -> {
            target[index] = (int) Math.round(v);
            sendUpdate();
        }));
    }

    private void buildPicker(List<String> ids, Consumer<String> onPick, Page cancelPage) {
        int left = leftPos + 10;
        int width = GUI_WIDTH - 20;

        EditBox search = new EditBox(font, left, topPos + 20, width, 18, Component.literal("Search"));
        search.setHint(Component.literal("Type to search..."));
        search.setMaxLength(64);
        picker = new ListPicker(left, topPos + 42, width, ListPicker.ROW_H * LIST_ROWS, ids, onPick);
        search.setResponder(picker::setFilter);

        addRenderableWidget(search);
        addRenderableWidget(picker);
        boxes.add(search);
        setFocused(search);
        search.setFocused(true);

        addRenderableWidget(Button.builder(Component.literal("Cancel"), b -> {
            page = cancelPage;
            rebuildWidgets();
        }).bounds(left, topPos + imageHeight - 26, 100, ROW_HEIGHT).build());
    }

    // =====================================================================
    //  Helpers
    // =====================================================================

    private Kind kind() {
        Kind kind = SpoutParticles.kindOf(ResourceLocation.tryParse(particleId));
        return kind == null ? Kind.SIMPLE : kind;
    }

    private static String shortName(String id) {
        return id.startsWith("minecraft:") ? id.substring("minecraft:".length()) : id;
    }

    private static void unpack(int rgb, int[] out) {
        out[0] = (rgb >> 16) & 0xFF;
        out[1] = (rgb >> 8) & 0xFF;
        out[2] = rgb & 0xFF;
    }

    private static int pack(int[] rgb) {
        return (rgb[0] << 16) | (rgb[1] << 8) | rgb[2];
    }

    private static List<String> particleNames() {
        List<String> names = new ArrayList<>();
        for (ResourceLocation id : SpoutParticles.allowedIds()) names.add(id.toString());
        return names;
    }

    private static List<String> blockNames() {
        if (blockIds == null) {
            List<String> names = new ArrayList<>();
            for (ResourceLocation id : BuiltInRegistries.BLOCK.keySet()) {
                if (!id.equals(BuiltInRegistries.BLOCK.getKey(Blocks.AIR))) names.add(id.toString());
            }
            names.sort(String::compareTo);
            blockIds = names;
        }
        return blockIds;
    }

    private static List<String> itemNames() {
        if (itemIds == null) {
            List<String> names = new ArrayList<>();
            for (ResourceLocation id : BuiltInRegistries.ITEM.keySet()) {
                if (!id.equals(BuiltInRegistries.ITEM.getKey(Items.AIR))) names.add(id.toString());
            }
            names.sort(String::compareTo);
            itemIds = names;
        }
        return itemIds;
    }

    private void sendUpdate() {
        CompoundTag tag = new CompoundTag();
        tag.putString("particle", particleId);
        tag.putInt("color", pack(colorA));
        tag.putInt("color2", pack(colorB));
        tag.putFloat("size", size);
        tag.putString("block", blockId);
        tag.putString("item", itemId);
        tag.putInt("count", count);
        tag.putFloat("spread", spread);
        tag.putFloat("offsetX", offsetX);
        tag.putFloat("offsetY", offsetY);
        tag.putFloat("offsetZ", offsetZ);
        tag.putFloat("start", startHeight);
        PacketDistributor.sendToServer(new UpdateSpoutPayload(menu.getPos(), tag));
    }

    // =====================================================================
    //  Input
    // =====================================================================

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Enter in the search box picks the first match
        if ((keyCode == 257 || keyCode == 335) && picker != null) {
            picker.pickFirst();
            return true;
        }
        // Stop typing letters (like 'E') from closing the screen while a text box is focused
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

    // AbstractContainerScreen never forwards drags to widgets, so sliders could not be dragged
    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        GuiEventListener focused = getFocused();
        if (focused != null && isDragging() && button == 0
                && focused.mouseDragged(mouseX, mouseY, button, dragX, dragY)) {
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (picker != null) {
            picker.scrollBy(-scrollY * 3);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    // =====================================================================
    //  Rendering
    // =====================================================================

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        g.fill(leftPos - 1, topPos - 1, leftPos + imageWidth + 1, topPos + imageHeight + 1, 0xFF000000);
        g.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF3A3A3A);

        // Color preview on the options page
        Kind kind = kind();
        if (page == Page.OPTIONS
                && (kind == Kind.DUST || kind == Kind.DUST_TRANSITION || kind == Kind.ENTITY_EFFECT)) {
            int x0 = leftPos + RIGHT_COLUMN_X;
            int y0 = topPos + 22;
            int x1 = x0 + COLUMN_WIDTH;
            int y1 = y0 + ROW_STEP * 3 + ROW_HEIGHT;
            g.fill(x0 - 1, y0 - 1, x1 + 1, y1 + 1, 0xFF000000);
            WaterSpoutBlockEntity be = menu.getBlockEntity();
            boolean glass = be != null && be.hasGlassColor();   // touching colored glass overrides both colors
            int shownA = glass ? be.getGlassColor() : pack(colorA);
            int shownB = glass ? be.getGlassColor() : pack(colorB);
            if (kind == Kind.DUST_TRANSITION) {
                int mid = (x0 + x1) / 2;
                g.fill(x0, y0, mid, y1, 0xFF000000 | shownA);
                g.fill(mid, y0, x1, y1, 0xFF000000 | shownB);
            } else {
                g.fill(x0, y0, x1, y1, 0xFF000000 | shownA);
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        switch (page) {
            case MAIN -> {
                g.drawString(font, title, 10, 8, 0xFFFFFF, false);
                g.drawString(font, "Position offset (blocks)", RIGHT_COLUMN_X, 8, 0xAAAAAA, false);
            }
            case OPTIONS -> g.drawString(font, "Particle options: " + shortName(particleId), 10, 8, 0xFFFFFF, false);
            case PICK_PARTICLE -> g.drawString(font, "Choose a particle", 10, 8, 0xFFFFFF, false);
            case PICK_BLOCK -> g.drawString(font, "Choose a block", 10, 8, 0xFFFFFF, false);
            case PICK_ITEM -> g.drawString(font, "Choose an item", 10, 8, 0xFFFFFF, false);
        }
    }

    // =====================================================================
    //  A slider plus a text box for exact values, kept in sync
    // =====================================================================

    private class NumberRow {
        private ValueSlider slider;   // not final: the slider callback refers to it before the constructor finishes
        private final EditBox box;
        private final float min;
        private final float max;
        private final int decimals;
        private final double scale;
        private final Consumer<Float> setter;

        NumberRow(int x, int y, String label, float min, float max, int decimals,
                  float initial, Consumer<Float> setter) {
            this.min = min;
            this.max = max;
            this.decimals = decimals;
            this.scale = Math.pow(10, decimals);
            this.setter = setter;

            this.slider = new ValueSlider(x, y, SLIDER_WIDTH, ROW_HEIGHT, label, min, max, initial, decimals, v -> {
                float rounded = round(v);
                this.setter.accept(rounded);
                this.slider.setReal(rounded);   // snap the thumb to the rounded value
                setBoxText(rounded);
                sendUpdate();
            });

            this.box = new EditBox(font, x + SLIDER_WIDTH + 4, y, BOX_WIDTH, ROW_HEIGHT, Component.literal(label));
            this.box.setFilter(s -> s.matches(decimals == 0 ? "-?\\d*" : "-?\\d*\\.?\\d*"));
            this.box.setValue(format(initial));
            this.box.setResponder(this::onText);

            addRenderableWidget(slider);
            addRenderableWidget(box);
            boxes.add(box);
        }

        private float round(double value) {
            return (float) (Math.round(value * scale) / scale);
        }

        private String format(float value) {
            return String.format(Locale.ROOT, "%." + decimals + "f", value);
        }

        private void setBoxText(float value) {
            syncing = true;
            box.setValue(format(value));
            syncing = false;
        }

        private void onText(String text) {
            if (syncing) return;
            try {
                float value = round(Mth.clamp(Float.parseFloat(text), min, max));
                setter.accept(value);
                slider.setReal(value);
                sendUpdate();
            } catch (NumberFormatException ignored) {
                // incomplete input like "-" or "."
            }
        }
    }

    // =====================================================================
    //  Slider that maps 0..1 onto a real range
    // =====================================================================

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

        /** Moves the slider without firing the change callback. */
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

    // =====================================================================
    //  Scrollable, filterable list used for the particle / block / item pickers
    // =====================================================================

    private static class ListPicker extends AbstractWidget {
        static final int ROW_H = 12;

        private final List<String> all;
        private final Consumer<String> onPick;
        private List<String> filtered;
        private int scroll = 0;

        ListPicker(int x, int y, int width, int height, List<String> all, Consumer<String> onPick) {
            super(x, y, width, height, Component.empty());
            this.all = all;
            this.filtered = all;
            this.onPick = onPick;
        }

        void setFilter(String text) {
            String query = text.trim().toLowerCase(Locale.ROOT);
            if (query.isEmpty()) {
                filtered = all;
            } else {
                List<String> result = new ArrayList<>();
                for (String s : all) {
                    if (s.contains(query)) result.add(s);
                }
                filtered = result;
            }
            scroll = 0;
        }

        private int visibleRows() {
            return height / ROW_H;
        }

        private int maxScroll() {
            return Math.max(0, filtered.size() - visibleRows());
        }

        void scrollBy(double rows) {
            scroll = Mth.clamp(scroll + (int) Math.round(rows), 0, maxScroll());
        }

        void pickFirst() {
            if (!filtered.isEmpty()) onPick.accept(filtered.get(0));
        }

        @Override
        protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            Font font = Minecraft.getInstance().font;
            g.fill(getX() - 1, getY() - 1, getX() + width + 1, getY() + height + 1, 0xFF000000);
            g.fill(getX(), getY(), getX() + width, getY() + height, 0xFF1E1E1E);

            if (filtered.isEmpty()) {
                g.drawString(font, "No matches", getX() + 4, getY() + 4, 0xAAAAAA, false);
                return;
            }

            int rows = visibleRows();
            for (int i = 0; i < rows; i++) {
                int index = scroll + i;
                if (index >= filtered.size()) break;
                int rowY = getY() + i * ROW_H;
                boolean hovered = mouseX >= getX() && mouseX < getX() + width - 6
                        && mouseY >= rowY && mouseY < rowY + ROW_H;
                if (hovered) {
                    g.fill(getX(), rowY, getX() + width - 6, rowY + ROW_H, 0xFF505050);
                }
                g.drawString(font, shortName(filtered.get(index)), getX() + 4, rowY + 2, 0xFFFFFF, false);
            }

            // Scroll bar
            if (maxScroll() > 0) {
                int trackX = getX() + width - 5;
                g.fill(trackX, getY(), trackX + 4, getY() + height, 0xFF2E2E2E);
                int thumbHeight = Math.max(12, height * rows / filtered.size());
                int thumbY = getY() + (height - thumbHeight) * scroll / maxScroll();
                g.fill(trackX, thumbY, trackX + 4, thumbY + thumbHeight, 0xFF909090);
            }
        }

        @Override
        public void onClick(double mouseX, double mouseY) {
            int row = (int) ((mouseY - getY()) / ROW_H);
            int index = scroll + row;
            if (row >= 0 && row < visibleRows() && index < filtered.size()) {
                onPick.accept(filtered.get(index));
            }
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
        }
    }
}