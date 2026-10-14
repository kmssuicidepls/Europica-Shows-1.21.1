package com.kmssuicidepls.europica_shows.spout;

import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.DustColorTransitionOptions;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Knows which particle types the water spout can use, and builds the ParticleOptions for them.
 * Particles that need extra data (dust, block, item, ...) are built from the spout's saved settings.
 * Types that need a target position (vibration, trail) or other special data (sculk_charge, shriek)
 * are left out of the list.
 */
public final class SpoutParticles {

    public enum Kind { SIMPLE, DUST, DUST_TRANSITION, ENTITY_EFFECT, BLOCK, ITEM }

    public static final String DEFAULT_PARTICLE = "minecraft:falling_water";
    public static final String DEFAULT_BLOCK = "minecraft:stone";
    public static final String DEFAULT_ITEM = "minecraft:snowball";

    private static List<ResourceLocation> allowed;

    private SpoutParticles() {
    }

    /** What kind of data this particle needs, or null if the spout can't use it. */
    public static Kind kindOf(ResourceLocation id) {
        if (id == null) return null;
        ParticleType<?> type = BuiltInRegistries.PARTICLE_TYPE.getOptional(id).orElse(null);
        if (type == null) return null;
        if (type instanceof SimpleParticleType) return Kind.SIMPLE;
        return switch (id.toString()) {
            case "minecraft:dust" -> Kind.DUST;
            case "minecraft:dust_color_transition" -> Kind.DUST_TRANSITION;
            case "minecraft:entity_effect" -> Kind.ENTITY_EFFECT;
            case "minecraft:block", "minecraft:block_marker",
                 "minecraft:falling_dust", "minecraft:dust_pillar" -> Kind.BLOCK;
            case "minecraft:item" -> Kind.ITEM;
            default -> null;
        };
    }

    public static boolean isAllowed(ResourceLocation id) {
        return kindOf(id) != null;
    }

    /** Every usable particle id (including ones added by other mods), sorted by name. */
    public static List<ResourceLocation> allowedIds() {
        if (allowed == null) {
            List<ResourceLocation> list = new ArrayList<>();
            for (ResourceLocation id : BuiltInRegistries.PARTICLE_TYPE.keySet()) {
                if (kindOf(id) != null) list.add(id);
            }
            list.sort(Comparator.comparing(ResourceLocation::toString));
            allowed = list;
        }
        return allowed;
    }

    public static boolean isValidBlock(String id) {
        ResourceLocation rl = ResourceLocation.tryParse(id);
        return rl != null && BuiltInRegistries.BLOCK.getOptional(rl).filter(b -> b != Blocks.AIR).isPresent();
    }

    public static boolean isValidItem(String id) {
        ResourceLocation rl = ResourceLocation.tryParse(id);
        return rl != null && BuiltInRegistries.ITEM.getOptional(rl).filter(i -> i != Items.AIR).isPresent();
    }

    private static Block blockOf(String id) {
        ResourceLocation rl = ResourceLocation.tryParse(id);
        Block block = rl == null ? null : BuiltInRegistries.BLOCK.getOptional(rl).orElse(null);
        return block == null || block == Blocks.AIR ? Blocks.STONE : block;
    }

    private static Item itemOf(String id) {
        ResourceLocation rl = ResourceLocation.tryParse(id);
        Item item = rl == null ? null : BuiltInRegistries.ITEM.getOptional(rl).orElse(null);
        return item == null || item == Items.AIR ? Items.SNOWBALL : item;
    }

    private static Vector3f toVector(int rgb) {
        return new Vector3f(((rgb >> 16) & 0xFF) / 255.0f, ((rgb >> 8) & 0xFF) / 255.0f, (rgb & 0xFF) / 255.0f);
    }

    /** Builds the options to spawn. Falls back to falling water if the id isn't usable. */
    @SuppressWarnings("unchecked")
    public static ParticleOptions build(String idString, int colorA, int colorB, float size,
                                        String blockId, String itemId) {
        ResourceLocation id = ResourceLocation.tryParse(idString);
        Kind kind = kindOf(id);
        if (kind == null) return ParticleTypes.FALLING_WATER;

        ParticleType<?> type = BuiltInRegistries.PARTICLE_TYPE.getOptional(id).orElse(null);
        if (type == null) return ParticleTypes.FALLING_WATER;

        float clampedSize = Mth.clamp(size, 0.1f, 4.0f);
        return switch (kind) {
            case SIMPLE -> (SimpleParticleType) type;
            case DUST -> new DustParticleOptions(toVector(colorA), clampedSize);
            case DUST_TRANSITION -> new DustColorTransitionOptions(toVector(colorA), toVector(colorB), clampedSize);
            case ENTITY_EFFECT -> {
                Vector3f c = toVector(colorA);
                yield ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, c.x(), c.y(), c.z());
            }
            case BLOCK -> new BlockParticleOption((ParticleType<BlockParticleOption>) type,
                    blockOf(blockId).defaultBlockState());
            case ITEM -> new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(itemOf(itemId)));
        };
    }
}