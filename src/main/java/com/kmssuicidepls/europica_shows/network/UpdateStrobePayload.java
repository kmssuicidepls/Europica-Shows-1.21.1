package com.kmssuicidepls.europica_shows.network;

import com.kmssuicidepls.europica_shows.EuropicaShows;
import com.kmssuicidepls.europica_shows.block.entity.StrobeLightBlockEntity;
import com.kmssuicidepls.europica_shows.menu.StrobeLightMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Sent from the strobe light menu to the server whenever a setting changes. */
public record UpdateStrobePayload(BlockPos pos, boolean enabled, int mode,
                                  int gapTicks, int flashTicks) implements CustomPacketPayload {

    public static final Type<UpdateStrobePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(EuropicaShows.MOD_ID, "update_strobe"));

    public static final StreamCodec<RegistryFriendlyByteBuf, UpdateStrobePayload> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, UpdateStrobePayload::pos,
                    ByteBufCodecs.BOOL, UpdateStrobePayload::enabled,
                    ByteBufCodecs.INT, UpdateStrobePayload::mode,
                    ByteBufCodecs.INT, UpdateStrobePayload::gapTicks,
                    ByteBufCodecs.INT, UpdateStrobePayload::flashTicks,
                    UpdateStrobePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(UpdateStrobePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();

            // The player must have this exact strobe light's menu open and be close to it
            if (!(player.containerMenu instanceof StrobeLightMenu menu)) return;
            if (!menu.getPos().equals(payload.pos())) return;
            if (player.distanceToSqr(Vec3.atCenterOf(payload.pos())) > 64.0) return;

            if (player.level().getBlockEntity(payload.pos()) instanceof StrobeLightBlockEntity be) {
                be.setSettings(payload.enabled(), payload.mode(), payload.gapTicks(), payload.flashTicks());
            }
        });
    }
}