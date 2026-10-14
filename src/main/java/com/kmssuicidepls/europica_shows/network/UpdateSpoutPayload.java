package com.kmssuicidepls.europica_shows.network;

import com.kmssuicidepls.europica_shows.EuropicaShows;
import com.kmssuicidepls.europica_shows.block.entity.WaterSpoutBlockEntity;
import com.kmssuicidepls.europica_shows.menu.WaterSpoutMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Sent from the water spout menu to the server whenever a setting changes. */
public record UpdateSpoutPayload(BlockPos pos, CompoundTag data) implements CustomPacketPayload {

    public static final Type<UpdateSpoutPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(EuropicaShows.MOD_ID, "update_spout"));

    public static final StreamCodec<RegistryFriendlyByteBuf, UpdateSpoutPayload> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, UpdateSpoutPayload::pos,
                    ByteBufCodecs.COMPOUND_TAG, UpdateSpoutPayload::data,
                    UpdateSpoutPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(UpdateSpoutPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();

            // The player must have this exact spout's menu open and be close to it
            if (!(player.containerMenu instanceof WaterSpoutMenu menu)) return;
            if (!menu.getPos().equals(payload.pos())) return;
            if (player.distanceToSqr(Vec3.atCenterOf(payload.pos())) > 64.0) return;

            if (player.level().getBlockEntity(payload.pos()) instanceof WaterSpoutBlockEntity be) {
                be.applySettings(payload.data());   // validates and clamps every value
            }
        });
    }
}