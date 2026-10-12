package com.kmssuicidepls.europica_shows.block;

import com.kmssuicidepls.europica_shows.EuropicaShows;
import com.kmssuicidepls.europica_shows.block.custom.RedstoneConduitBlock;
import com.kmssuicidepls.europica_shows.block.custom.WaterSpoutBlock;
import com.kmssuicidepls.europica_shows.items.ModItems;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.redstone.Redstone;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;
import com.kmssuicidepls.europica_shows.block.BeamProjectorBlock;

import java.util.function.Supplier;

public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(EuropicaShows.MOD_ID);

    // public static final DeferredBlock<Block> EXAMPLE_BLOCK = registerBlock("example_block",
    //        () -> new DropExperienceBlock(UniformInt.of(100, 3000),
    //              BlockBehaviour.Properties.of().strength(1f).sound(SoundType.BONE_BLOCK)));

    public static final DeferredBlock<WaterSpoutBlock> WATER_SPOUT =
            BLOCKS.registerBlock("water_spout", WaterSpoutBlock::new,
                    BlockBehaviour.Properties.of().strength(2.0f));

    public static final DeferredBlock<BeamProjectorBlock> BEAM_PROJECTOR =
            BLOCKS.registerBlock("beam_projector", BeamProjectorBlock::new,
                    BlockBehaviour.Properties.of().strength(2.0f));

    public static final DeferredBlock<SpotlightBlock> SPOTLIGHT =
            BLOCKS.registerBlock("spotlight", SpotlightBlock::new,
                    BlockBehaviour.Properties.of().strength(2.0f));

    public static final DeferredBlock<RedstoneConduitBlock> STONE_CONDUIT =
            BLOCKS.registerBlock("stone_conduit", RedstoneConduitBlock::new,
                    BlockBehaviour.Properties.of()
                            .strength(1.5f)
                            .isRedstoneConductor((state, level, pos) -> false));



    private static <T extends Block> DeferredBlock<T> registerBlock(String name, Supplier<T> block) {
        DeferredBlock<T> toReturn = BLOCKS.register(name, block);

        registerBlockItem(name, toReturn);
        return toReturn;
    }

    private static <T extends Block> void registerBlockItem (String name, DeferredBlock<T> block) {
        ModItems.ITEMS.register(name, () -> new BlockItem(block.get(),new Item.Properties()));
    }


    public static void register (IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}
