package com.kmssuicidepls.europica_shows.block.entity;

import com.kmssuicidepls.europica_shows.block.GlassColors;
import com.kmssuicidepls.europica_shows.spout.SpoutParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class WaterSpoutBlockEntity extends BlockEntity {
    public static final int MAX_COUNT = 8;
    public static final float MAX_SPREAD = 2.0f;
    public static final float MAX_OFFSET = 16.0f;
    public static final float MAX_START = 16.0f;

    private String particleId = SpoutParticles.DEFAULT_PARTICLE;
    private int color = 0xFFFFFF;          // white by default
    private int color2 = 0xFFFFFF;         // second color for dust_color_transition
    private float size = 1.0f;
    private String blockId = SpoutParticles.DEFAULT_BLOCK;
    private String itemId = SpoutParticles.DEFAULT_ITEM;
    private int count = 1;
    private float spread = 0.1f;
    private float offsetX = 0.0f;
    private float offsetY = 0.0f;
    private float offsetZ = 0.0f;
    private float startHeight = 0.0f;
    private int glassColor = -1;           // average color of touching colored glass, -1 if none (not user-set)

    private ParticleOptions cachedOptions;   // rebuilt whenever settings change

    public WaterSpoutBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WATER_SPOUT.get(), pos, state);
    }

    public String getParticleId() { return particleId; }
    public int getColor() { return color; }
    public int getColor2() { return color2; }
    public float getSize() { return size; }
    public String getBlockId() { return blockId; }
    public String getItemId() { return itemId; }
    public int getCount() { return count; }
    public float getSpread() { return spread; }
    public float getOffsetX() { return offsetX; }
    public float getOffsetY() { return offsetY; }
    public float getOffsetZ() { return offsetZ; }
    public float getStartHeight() { return startHeight; }

    public boolean hasGlassColor() { return glassColor >= 0; }
    public int getGlassColor() { return glassColor; }

    public ParticleOptions getParticleOptions() {
        if (cachedOptions == null) {
            // Touching colored glass replaces both the "from" and "to" colors
            int from = glassColor >= 0 ? glassColor : color;
            int to = glassColor >= 0 ? glassColor : color2;
            cachedOptions = SpoutParticles.build(particleId, from, to, size, blockId, itemId);
        }
        return cachedOptions;
    }

    /** Re-reads the glass around the block. Server side only. */
    public void updateGlassColor() {
        if (level == null || level.isClientSide) return;
        int newColor = GlassColors.averageAround(level, worldPosition);
        if (newColor == glassColor) return;
        glassColor = newColor;
        cachedOptions = null;
        setChanged();
        BlockState state = getBlockState();
        level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_ALL);
    }

    /** Validates and stores settings sent from the menu, then syncs them to clients. Server side only. */
    public void applySettings(CompoundTag tag) {
        readSettings(tag);
        setChanged();
        if (level != null) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_ALL);
        }
    }

    /** Reads whatever keys are present, validating and clamping each one. */
    private void readSettings(CompoundTag tag) {
        if (tag.contains("particle")) {
            ResourceLocation id = ResourceLocation.tryParse(tag.getString("particle"));
            if (id != null && SpoutParticles.isAllowed(id)) particleId = id.toString();
        }
        if (tag.contains("color")) color = tag.getInt("color") & 0xFFFFFF;
        if (tag.contains("color2")) color2 = tag.getInt("color2") & 0xFFFFFF;
        if (tag.contains("size")) size = Mth.clamp(clean(tag.getFloat("size")), 0.1f, 4.0f);
        if (tag.contains("block") && SpoutParticles.isValidBlock(tag.getString("block"))) {
            blockId = tag.getString("block");
        }
        if (tag.contains("item") && SpoutParticles.isValidItem(tag.getString("item"))) {
            itemId = tag.getString("item");
        }
        if (tag.contains("count")) count = Mth.clamp(tag.getInt("count"), 1, MAX_COUNT);
        if (tag.contains("spread")) spread = Mth.clamp(clean(tag.getFloat("spread")), 0.0f, MAX_SPREAD);
        if (tag.contains("offsetX")) offsetX = Mth.clamp(clean(tag.getFloat("offsetX")), -MAX_OFFSET, MAX_OFFSET);
        if (tag.contains("offsetY")) offsetY = Mth.clamp(clean(tag.getFloat("offsetY")), -MAX_OFFSET, MAX_OFFSET);
        if (tag.contains("offsetZ")) offsetZ = Mth.clamp(clean(tag.getFloat("offsetZ")), -MAX_OFFSET, MAX_OFFSET);
        if (tag.contains("start")) startHeight = Mth.clamp(clean(tag.getFloat("start")), 0.0f, MAX_START);
        cachedOptions = null;
    }

    private static float clean(float value) {
        return Float.isNaN(value) || Float.isInfinite(value) ? 0.0f : value;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putString("particle", particleId);
        tag.putInt("color", color);
        tag.putInt("color2", color2);
        tag.putFloat("size", size);
        tag.putString("block", blockId);
        tag.putString("item", itemId);
        tag.putInt("count", count);
        tag.putFloat("spread", spread);
        tag.putFloat("offsetX", offsetX);
        tag.putFloat("offsetY", offsetY);
        tag.putFloat("offsetZ", offsetZ);
        tag.putFloat("start", startHeight);
        tag.putInt("glass", glassColor);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        readSettings(tag);
        if (tag.contains("glass")) {
            glassColor = tag.getInt("glass");
            cachedOptions = null;
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}