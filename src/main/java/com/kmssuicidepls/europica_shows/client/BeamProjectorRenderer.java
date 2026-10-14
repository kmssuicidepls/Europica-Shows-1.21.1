package com.kmssuicidepls.europica_shows.client;

import com.kmssuicidepls.europica_shows.EuropicaShows;
import com.kmssuicidepls.europica_shows.block.BeamProjectorBlock;
import com.kmssuicidepls.europica_shows.block.SpotlightBlock;
import com.kmssuicidepls.europica_shows.block.entity.BeamProjectorBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BeaconRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public class BeamProjectorRenderer implements BlockEntityRenderer<BeamProjectorBlockEntity> {

    private static final ResourceLocation SPOTLIGHT_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(EuropicaShows.MOD_ID, "textures/misc/spotlight_beam.png");

    private static final float SPOTLIGHT_LENGTH = 24.0f;
    private static final float SPOTLIGHT_END_RADIUS = 7.0f;
    private static final int SEGMENTS = 20;
    private static final int U_REPEATS = 4;          // texture repeats around the cone
    private static final float TILE_BLOCKS = 6.0f;   // blocks of beam length per texture repeat

    public BeamProjectorRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(BeamProjectorBlockEntity be, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int light, int overlay) {
        Level level = be.getLevel();
        if (level == null) return;

        // Only draw while the block is receiving redstone power. When the block is broken, the
        // block entity can render one last frame after the position is already air, which has no
        // POWERED property, so check hasProperty first.
        BlockState currentState = level.getBlockState(be.getBlockPos());
        if (!currentState.hasProperty(BeamProjectorBlock.POWERED)
                || !currentState.getValue(BeamProjectorBlock.POWERED)) return;

        pose.pushPose();

        // Pivot around the center of the block
        pose.translate(0.5, 0.5, 0.5);

        // Position offset in world axes (set in the menu)
        pose.translate(be.getOffsetX(), be.getOffsetY(), be.getOffsetZ());

        // Applied bottom-up: tilt from vertical by (90 - pitch), then turn by yaw.
        // Yaw follows Minecraft's convention (0 = south, 90 = west); pitch is positive upward.
        pose.mulPose(Axis.YP.rotationDegrees(-be.getYaw()));
        pose.mulPose(Axis.XP.rotationDegrees(90.0f - be.getPitch()));

        // Start distance: slide the start point out along the direction the beam points
        pose.translate(0.0, be.getStartDistance(), 0.0);

        if (be.getBlockState().getBlock() instanceof SpotlightBlock) {
            renderCone(be, level, partialTick, pose, buffers);
        } else {
            // Vanilla's renderBeaconBeam shifts by (0.5, 0, 0.5) itself, so cancel that out
            pose.translate(-0.5, 0.0, -0.5);
            BeaconRenderer.renderBeaconBeam(
                    pose, buffers, BeaconRenderer.BEAM_LOCATION,
                    partialTick, 1.0f, level.getGameTime(),
                    0, (int) BeamProjectorBlockEntity.BEAM_LENGTH,
                    0xFF000000 | be.getEffectiveColor(),
                    0.2f, 0.25f);
        }

        pose.popPose();
    }

    /** Two nested translucent cones (bright core + fainter outer glow) with a scrolling texture. */
    private void renderCone(BeamProjectorBlockEntity be, Level level, float partialTick,
                            PoseStack pose, MultiBufferSource buffers) {
        VertexConsumer consumer = buffers.getBuffer(RenderType.beaconBeam(SPOTLIGHT_TEXTURE, true));
        PoseStack.Pose last = pose.last();
        int rgb = be.getEffectiveColor() & 0xFFFFFF;

        // Scroll loops seamlessly because the texture tiles exactly once per unit of V
        long time = level.getGameTime();
        float innerScroll = ((time % 50L) + partialTick) * 0.02f;
        float outerScroll = ((time % 100L) + partialTick) * 0.01f;

        drawCone(consumer, last, SPOTLIGHT_LENGTH, SPOTLIGHT_END_RADIUS * 0.55f, 255, 40, rgb, innerScroll);
        drawCone(consumer, last, SPOTLIGHT_LENGTH, SPOTLIGHT_END_RADIUS, 150, 0, rgb, outerScroll);
    }

    private static void drawCone(VertexConsumer consumer, PoseStack.Pose pose, float length, float radius,
                                 int apexAlpha, int baseAlpha, int rgb, float scroll) {
        int apexColor = (apexAlpha << 24) | rgb;   // alpha fades from apex to the far end per vertex
        int baseColor = (baseAlpha << 24) | rgb;
        float vApex = -scroll;
        float vBase = length / TILE_BLOCKS - scroll;

        for (int i = 0; i < SEGMENTS; i++) {
            float a0 = Mth.TWO_PI * i / SEGMENTS;
            float a1 = Mth.TWO_PI * (i + 1) / SEGMENTS;
            float x0 = Mth.cos(a0) * radius, z0 = Mth.sin(a0) * radius;
            float x1 = Mth.cos(a1) * radius, z1 = Mth.sin(a1) * radius;
            float u0 = (float) i * U_REPEATS / SEGMENTS;
            float u1 = (float) (i + 1) * U_REPEATS / SEGMENTS;

            // One quad per segment; the two apex vertices coincide, which makes it a triangle
            vertex(consumer, pose, 0.0f, 0.0f, 0.0f, u0, vApex, apexColor);
            vertex(consumer, pose, 0.0f, 0.0f, 0.0f, u1, vApex, apexColor);
            vertex(consumer, pose, x1, length, z1, u1, vBase, baseColor);
            vertex(consumer, pose, x0, length, z0, u0, vBase, baseColor);
        }
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose,
                               float x, float y, float z, float u, float v, int argb) {
        consumer.addVertex(pose, x, y, z)
                .setColor(argb)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(pose, 0.0f, 1.0f, 0.0f);
    }

    // Keeps the beam visible when the block itself is off-screen
    @Override
    public AABB getRenderBoundingBox(BeamProjectorBlockEntity be) {
        return new AABB(be.getBlockPos()).inflate(BeamProjectorBlockEntity.BEAM_LENGTH
                + BeamProjectorBlockEntity.MAX_OFFSET + BeamProjectorBlockEntity.MAX_START);
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