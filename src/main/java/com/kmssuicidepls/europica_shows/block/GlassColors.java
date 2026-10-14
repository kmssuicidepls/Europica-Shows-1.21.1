package com.kmssuicidepls.europica_shows.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BeaconBeamBlock;

/** Reads the color of colored glass touching a block. */
public final class GlassColors {
    private GlassColors() {
    }

    /**
     * Averages the colors of every colored glass block (or pane) directly touching `pos`.
     * Anything that tints beacon beams counts, which includes stained glass from other mods
     * that implements BeaconBeamBlock. Clear and tinted glass have no color and are ignored.
     *
     * @return 0xRRGGBB, or -1 if no colored glass is touching
     */
    public static int averageAround(BlockGetter level, BlockPos pos) {
        int red = 0, green = 0, blue = 0, count = 0;
        for (Direction direction : Direction.values()) {
            if (level.getBlockState(pos.relative(direction)).getBlock() instanceof BeaconBeamBlock beam) {
                int rgb = beam.getColor().getTextureDiffuseColor() & 0xFFFFFF;
                red += (rgb >> 16) & 0xFF;
                green += (rgb >> 8) & 0xFF;
                blue += rgb & 0xFF;
                count++;
            }
        }
        if (count == 0) return -1;
        return ((red / count) << 16) | ((green / count) << 8) | (blue / count);
    }
}