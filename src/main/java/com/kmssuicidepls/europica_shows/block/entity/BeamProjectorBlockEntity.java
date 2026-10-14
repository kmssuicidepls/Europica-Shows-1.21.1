package com.kmssuicidepls.europica_shows.block.entity;

import com.kmssuicidepls.europica_shows.block.GlassColors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class BeamProjectorBlockEntity extends BlockEntity {
    public static final float BEAM_LENGTH = 32.0f;
    public static final float MAX_OFFSET = 16.0f;   // X / Y / Z offset range: -16 to +16 blocks
    public static final float MAX_START = 16.0f;    // start distance along the beam: 0 to 16 blocks

    private float yaw = 0.0f;       // 0-360, Minecraft convention: 0 = south, 90 = west
    private float pitch = 90.0f;    // -90 to 90, positive = up (default: straight up like a beacon)
    private int color = 0xFFFFFF;   // RGB
    private float offsetX = 0.0f;   // world-axis offset of where the beam / cone is drawn from
    private float offsetY = 0.0f;
    private float offsetZ = 0.0f;
    private float startDistance = 0.0f;   // pushes the start point out along the beam direction
    private int glassColor = -1;          // average color of touching colored glass, -1 if none (not user-set)

    public BeamProjectorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BEAM_PROJECTOR.get(), pos, state);
    }

    public float getYaw() { return yaw; }
    public float getPitch() { return pitch; }
    public int getColor() { return color; }
    public float getOffsetX() { return offsetX; }
    public float getOffsetY() { return offsetY; }
    public float getOffsetZ() { return offsetZ; }
    public float getStartDistance() { return startDistance; }

    /** The color actually drawn: touching colored glass wins over the menu color. */
    public int getEffectiveColor() { return glassColor >= 0 ? glassColor : color; }
    public boolean hasGlassColor() { return glassColor >= 0; }
    public int getGlassColor() { return glassColor; }

    /** Re-reads the glass around the block and syncs to clients if it changed. Server side only. */
    public void updateGlassColor() {
        if (level == null || level.isClientSide) return;
        int newColor = GlassColors.averageAround(level, worldPosition);
        if (newColor == glassColor) return;
        glassColor = newColor;
        setChanged();
        BlockState state = getBlockState();
        level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_ALL);
    }

    /** Validates and stores new settings, then syncs them to clients. Server side only. */
    public void setSettings(float newYaw, float newPitch, int newColor,
                            float newOffX, float newOffY, float newOffZ, float newStart) {
        this.yaw = Mth.clamp(clean(newYaw), 0.0f, 360.0f);
        this.pitch = Mth.clamp(clean(newPitch), -90.0f, 90.0f);
        this.color = newColor & 0xFFFFFF;
        this.offsetX = Mth.clamp(clean(newOffX), -MAX_OFFSET, MAX_OFFSET);
        this.offsetY = Mth.clamp(clean(newOffY), -MAX_OFFSET, MAX_OFFSET);
        this.offsetZ = Mth.clamp(clean(newOffZ), -MAX_OFFSET, MAX_OFFSET);
        this.startDistance = Mth.clamp(clean(newStart), 0.0f, MAX_START);
        setChanged();
        if (level != null) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_ALL);
        }
    }

    private static float clean(float value) {
        return Float.isNaN(value) || Float.isInfinite(value) ? 0.0f : value;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putFloat("yaw", yaw);
        tag.putFloat("pitch", pitch);
        tag.putInt("color", color);
        tag.putFloat("offsetX", offsetX);
        tag.putFloat("offsetY", offsetY);
        tag.putFloat("offsetZ", offsetZ);
        tag.putFloat("startDistance", startDistance);
        tag.putInt("glassColor", glassColor);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("yaw")) yaw = tag.getFloat("yaw");
        if (tag.contains("pitch")) pitch = tag.getFloat("pitch");
        if (tag.contains("color")) color = tag.getInt("color");
        if (tag.contains("offsetX")) offsetX = tag.getFloat("offsetX");
        if (tag.contains("offsetY")) offsetY = tag.getFloat("offsetY");
        if (tag.contains("offsetZ")) offsetZ = tag.getFloat("offsetZ");
        if (tag.contains("startDistance")) startDistance = tag.getFloat("startDistance");
        if (tag.contains("glassColor")) glassColor = tag.getInt("glassColor");
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