package com.kmssuicidepls.europica_shows.menu;

import com.kmssuicidepls.europica_shows.block.ModBlocks;
import com.kmssuicidepls.europica_shows.block.entity.BeamProjectorBlockEntity;
import com.kmssuicidepls.europica_shows.registry.ModMenuTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class BeamProjectorMenu extends AbstractContainerMenu {
    private final BlockPos pos;
    private final Level level;

    // Server side, called from the block
    public BeamProjectorMenu(int containerId, Inventory inventory, BlockPos pos) {
        super(ModMenuTypes.BEAM_PROJECTOR_MENU.get(), containerId);
        this.pos = pos;
        this.level = inventory.player.level();
    }

    // Client side, called by the MenuType factory with the data written in the block
    public BeamProjectorMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(containerId, inventory, buf.readBlockPos());
    }

    public BlockPos getPos() {
        return pos;
    }

    public BeamProjectorBlockEntity getBlockEntity() {
        return level.getBlockEntity(pos) instanceof BeamProjectorBlockEntity be ? be : null;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;   // no slots
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(ContainerLevelAccess.create(level, pos), player, ModBlocks.BEAM_PROJECTOR.get());
    }
}