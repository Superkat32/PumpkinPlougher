package net.superkat.pumpkinplougher.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.superkat.pumpkinplougher.PumpkinPlougherPlayer;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin implements PumpkinPlougherPlayer {
    public Vec3 previousDeltaMovement = Vec3.ZERO;
    public int boomTicks = 0;

    @Override
    public Vec3 pumpkinplougher$previousDeltaMovement() {
        return previousDeltaMovement;
    }

    @Override
    public void pumpkinplougher$setPreviousDeltaMovement(Vec3 movement) {
        this.previousDeltaMovement = movement;
    }

    @Override
    public int pumpkinplougher$getBoomTicks() {
        return boomTicks;
    }

    @Override
    public void pumpkinplougher$setBoomTicks(int ticks) {
        this.boomTicks = ticks;
    }
}
