package com.kmssuicidepls.europica_shows.client;

import com.kmssuicidepls.europica_shows.block.entity.BeamProjectorBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BeaconRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

public class BeamProjectorRenderer implements BlockEntityRenderer<BeamProjectorBlockEntity> {

    public BeamProjectorRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(BeamProjectorBlockEntity be, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int light, int overlay) {
        Level level = be.getLevel();
        if (level == null) return;

        pose.pushPose();

        // Pivot around the center of the block
        pose.translate(0.5, 0.5, 0.5);

        // Applied to the beam bottom-up: tilt from vertical by (90 - pitch), then turn by yaw.
        // Yaw follows Minecraft's convention (0 = south, 90 = west); pitch is positive upward.
        pose.mulPose(Axis.YP.rotationDegrees(-be.getYaw()));
        pose.mulPose(Axis.XP.rotationDegrees(90.0f - be.getPitch()));

        // Vanilla's renderBeaconBeam shifts by (0.5, 0, 0.5) itself, so cancel that out
        pose.translate(-0.5, 0.0, -0.5);

        BeaconRenderer.renderBeaconBeam(
                pose, buffers, BeaconRenderer.BEAM_LOCATION,
                partialTick, 1.0f, level.getGameTime(),
                0, (int) BeamProjectorBlockEntity.BEAM_LENGTH,   // start offset, beam length
                0xFF000000 | be.getColor(),                       // ARGB
                0.2f, 0.25f);                                     // beam radius, glow radius

        pose.popPose();
    }

    // Keeps the beam visible when the block itself is off-screen
    @Override
    public AABB getRenderBoundingBox(BeamProjectorBlockEntity be) {
        return new AABB(be.getBlockPos()).inflate(BeamProjectorBlockEntity.BEAM_LENGTH);
    }

    @Override
    public boolean shouldRenderOffScreen(BeamProjectorBlockEntity be) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 256;
    }
}