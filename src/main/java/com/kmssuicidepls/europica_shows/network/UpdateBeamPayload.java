package com.kmssuicidepls.europica_shows.network;

import com.kmssuicidepls.europica_shows.EuropicaShows;
import com.kmssuicidepls.europica_shows.block.entity.BeamProjectorBlockEntity;
import com.kmssuicidepls.europica_shows.menu.BeamProjectorMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Sent from the client menu to the server whenever a slider or text box changes. */
public record UpdateBeamPayload(BlockPos pos, float yaw, float pitch, int color) implements CustomPacketPayload {

    public static final Type<UpdateBeamPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(EuropicaShows.MOD_ID, "update_beam"));

    public static final StreamCodec<RegistryFriendlyByteBuf, UpdateBeamPayload> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, UpdateBeamPayload::pos,
                    ByteBufCodecs.FLOAT, UpdateBeamPayload::yaw,
                    ByteBufCodecs.FLOAT, UpdateBeamPayload::pitch,
                    ByteBufCodecs.INT, UpdateBeamPayload::color,
                    UpdateBeamPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(UpdateBeamPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();

            // The player must have this exact projector's menu open and be close to it
            if (!(player.containerMenu instanceof BeamProjectorMenu menu)) return;
            if (!menu.getPos().equals(payload.pos())) return;
            if (player.distanceToSqr(Vec3.atCenterOf(payload.pos())) > 64.0) return;

            if (player.level().getBlockEntity(payload.pos()) instanceof BeamProjectorBlockEntity be) {
                be.setSettings(payload.yaw(), payload.pitch(), payload.color());
            }
        });
    }
}