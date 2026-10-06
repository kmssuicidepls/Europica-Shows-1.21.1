package com.kmssuicidepls.europica_shows.block.entity;

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

    private float yaw = 0.0f;       // 0-360, Minecraft convention: 0 = south, 90 = west
    private float pitch = 90.0f;    // -90 to 90, positive = up (default: straight up like a beacon)
    private int color = 0xFFFFFF;   // RGB

    public BeamProjectorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BEAM_PROJECTOR.get(), pos, state);
    }

    public float getYaw() { return yaw; }
    public float getPitch() { return pitch; }
    public int getColor() { return color; }

    /** Validates and stores new settings, then syncs them to clients. Server side only. */
    public void setSettings(float newYaw, float newPitch, int newColor) {
        if (Float.isNaN(newYaw)) newYaw = 0.0f;
        if (Float.isNaN(newPitch)) newPitch = 0.0f;
        this.yaw = Mth.clamp(newYaw, 0.0f, 360.0f);
        this.pitch = Mth.clamp(newPitch, -90.0f, 90.0f);
        this.color = newColor & 0xFFFFFF;
        setChanged();
        if (level != null) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_ALL);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putFloat("yaw", yaw);
        tag.putFloat("pitch", pitch);
        tag.putInt("color", color);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("yaw")) yaw = tag.getFloat("yaw");
        if (tag.contains("pitch")) pitch = tag.getFloat("pitch");
        if (tag.contains("color")) color = tag.getInt("color");
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