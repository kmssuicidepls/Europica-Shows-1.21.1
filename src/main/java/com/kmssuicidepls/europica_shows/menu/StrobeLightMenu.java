package com.kmssuicidepls.europica_shows.menu;

import com.kmssuicidepls.europica_shows.block.StrobeLightBlock;
import com.kmssuicidepls.europica_shows.block.entity.StrobeLightBlockEntity;
import com.kmssuicidepls.europica_shows.registry.ModMenuTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class StrobeLightMenu extends AbstractContainerMenu {
    private final BlockPos pos;
    private final Level level;

    // Server side, called from the block
    public StrobeLightMenu(int containerId, Inventory inventory, BlockPos pos) {
        super(ModMenuTypes.STROBE_LIGHT_MENU.get(), containerId);
        this.pos = pos;
        this.level = inventory.player.level();
    }

    // Client side, called by the MenuType factory with the data written in the block
    public StrobeLightMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(containerId, inventory, buf.readBlockPos());
    }

    public BlockPos getPos() {
        return pos;
    }

    public StrobeLightBlockEntity getBlockEntity() {
        return level.getBlockEntity(pos) instanceof StrobeLightBlockEntity be ? be : null;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;   // no slots
    }

    @Override
    public boolean stillValid(Player player) {
        return ContainerLevelAccess.create(level, pos).evaluate((lvl, p) ->
                lvl.getBlockState(p).getBlock() instanceof StrobeLightBlock
                        && player.distanceToSqr(p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5) <= 64.0, true);
    }
}