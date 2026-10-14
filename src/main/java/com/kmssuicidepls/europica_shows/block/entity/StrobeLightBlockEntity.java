package com.kmssuicidepls.europica_shows.block.entity;

import com.kmssuicidepls.europica_shows.block.StrobeLightBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class StrobeLightBlockEntity extends BlockEntity {
    public static final int MAX_GAP = 200;     // ticks between flashes (10 s)
    public static final int MAX_FLASH = 100;   // ticks per flash (5 s)

    public enum RedstoneMode {
        IGNORED("Ignored"),                 // never looks at redstone, strobes whenever it is on
        WHILE_POWERED("While Powered"),     // strobes continuously for as long as it has a signal
        PER_PULSE("Once Per Pulse");        // one flash each time a signal arrives

        public final String label;

        RedstoneMode(String label) {
            this.label = label;
        }

        public RedstoneMode next() {
            return values()[(ordinal() + 1) % values().length];
        }

        public static RedstoneMode byId(int id) {
            return values()[Mth.clamp(id, 0, values().length - 1)];
        }
    }

    // ---- Saved settings ----
    private boolean enabled = false;                     // manual on/off switch (starts OFF until a player turns it on)
    private RedstoneMode mode = RedstoneMode.IGNORED;
    private int gapTicks = 10;                           // time between flashes
    private int flashTicks = 4;                          // how long each flash lasts

    // ---- Runtime state (server only, not saved) ----
    private boolean running = false;   // cycle modes: a strobe cycle is in progress; pulse mode: a flash is in progress
    private int timer = 0;             // ticks left in the current phase

    public StrobeLightBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.STROBE_LIGHT.get(), pos, state);
    }

    public boolean isEnabled() { return enabled; }
    public RedstoneMode getMode() { return mode; }
    public int getGapTicks() { return gapTicks; }
    public int getFlashTicks() { return flashTicks; }

    /** Validates and stores settings sent from the menu, then syncs them to clients. Server side only. */
    public void setSettings(boolean newEnabled, int modeId, int newGap, int newFlash) {
        RedstoneMode newMode = RedstoneMode.byId(modeId);
        boolean restart = newEnabled != enabled || newMode != mode;

        this.enabled = newEnabled;
        this.mode = newMode;
        this.gapTicks = Mth.clamp(newGap, 1, MAX_GAP);
        this.flashTicks = Mth.clamp(newFlash, 1, MAX_FLASH);

        if (restart) stop();   // switching off / changing mode ends whatever was happening

        setChanged();
        if (level != null) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_ALL);
        }
    }

    /** Called by the block when a redstone signal newly arrives. */
    public void onPulse() {
        if (!enabled || mode != RedstoneMode.PER_PULSE) return;
        running = true;
        timer = flashTicks;    // a pulse during a flash restarts the flash
        setLit(true);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, StrobeLightBlockEntity be) {
        be.tick();
    }

    private void tick() {
        if (level == null) return;

        // Manually switched off: never strobe, never react to redstone
        if (!enabled) {
            stop();
            return;
        }

        switch (mode) {
            case IGNORED -> cycle(true);
            case WHILE_POWERED -> cycle(level.getBlockState(worldPosition).getValue(StrobeLightBlock.POWERED));
            case PER_PULSE -> pulseTick();
        }
    }

    /** Flash for flashTicks, wait gapTicks, repeat, for as long as `active` is true. */
    private void cycle(boolean active) {
        if (!active) {
            stop();
            return;
        }
        if (!running) {                       // start of a new run: begin with a flash
            running = true;
            timer = flashTicks;
            setLit(true);
            return;
        }
        if (--timer <= 0) {
            if (isLit()) {
                setLit(false);
                timer = gapTicks;
            } else {
                setLit(true);
                timer = flashTicks;
            }
        }
    }

    /** Pulse mode: just count down the flash that onPulse() started. */
    private void pulseTick() {
        if (!running) {
            if (isLit()) setLit(false);       // e.g. world was saved mid-flash
            return;
        }
        if (--timer <= 0) {
            running = false;
            setLit(false);
        }
    }

    private void stop() {
        running = false;
        timer = 0;
        if (isLit()) setLit(false);
    }

    private boolean isLit() {
        return level != null && level.getBlockState(worldPosition).getOptionalValue(StrobeLightBlock.LIT).orElse(false);
    }

    private void setLit(boolean lit) {
        if (level == null) return;
        BlockState state = level.getBlockState(worldPosition);
        if (state.getBlock() instanceof StrobeLightBlock && state.getValue(StrobeLightBlock.LIT) != lit) {
            level.setBlock(worldPosition, state.setValue(StrobeLightBlock.LIT, lit), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("enabled", enabled);
        tag.putInt("mode", mode.ordinal());
        tag.putInt("gap", gapTicks);
        tag.putInt("flash", flashTicks);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("enabled")) enabled = tag.getBoolean("enabled");
        if (tag.contains("mode")) mode = RedstoneMode.byId(tag.getInt("mode"));
        if (tag.contains("gap")) gapTicks = Mth.clamp(tag.getInt("gap"), 1, MAX_GAP);
        if (tag.contains("flash")) flashTicks = Mth.clamp(tag.getInt("flash"), 1, MAX_FLASH);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}