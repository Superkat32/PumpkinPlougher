package net.superkat.pumpkinplougher;

import net.minecraft.world.phys.Vec3;

public interface PumpkinPlougherPlayer {
    Vec3 pumpkinplougher$previousDeltaMovement();
    void pumpkinplougher$setPreviousDeltaMovement(Vec3 movement);
    int pumpkinplougher$getBoomTicks();
    void pumpkinplougher$setBoomTicks(int ticks);
}
