package net.superkat.pumpkinplougher;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

//Always server side
public class PumpkinMinigame {
    public final int MAX_TICKS = 2000; //100 seconds

    public ArrayList<LivingEntity> currentMonsters = new ArrayList<>();
    public int maxMonsters = 50;
    public int monstersKilled = 0;
    public final ServerLevel level;
    public final Player starterPlayer;
    public BlockPos startPos = BlockPos.ZERO;
    public boolean finished = false;
    public boolean lost = false;
    public boolean sculky = false;

    public int ticks = 0;
    public int monsterTicks = 0;
    public int lastMonsterTicks = 0;

    public PumpkinMinigame(ServerLevel level, Player starterPlayer) {
        this.level = level;
        this.starterPlayer = starterPlayer;
        this.startPos = starterPlayer.blockPosition();
    }

    public PumpkinMinigame sculkyMode(boolean soulMode) {
        this.sculky = soulMode;
        return this;
    }

    public void start() {
        this.ticks = 0;
        this.monsterTicks = 20;
        this.lastMonsterTicks = 0;

        this.maxMonsters = sculky ? 20 : 15;
    }

    //gets called twice in one tick... sorta works out though actually
    public void tick() {
        if(endEarly() || ticks >= MAX_TICKS) {
            this.finish();
            return;
        }

        ticks++;
        monsterTicks--;
        lastMonsterTicks++;

        this.currentMonsters.removeIf(monster -> {
            boolean shouldRemove = monster == null || monster.isDeadOrDying();
            if(shouldRemove) {
                monstersKilled++;
            }
            return shouldRemove;
        });

        sendMessageToPlayers();
        if(ticks % 10 == 0) {
            this.level.sendParticles(sculky ? ParticleTypes.TRIAL_SPAWNER_DETECTED_PLAYER_OMINOUS : ParticleTypes.TRIAL_SPAWNER_DETECTED_PLAYER, startPos.getX(), startPos.getY() + 1, startPos.getZ(), 3, 0, 0.5, 0, 0);
        }

        if(monsterTicks <= 0) {
            summonMonster();
        }
    }

    public void sendMessageToPlayers() {
        if(ticks % 20 == 0) { //every second but actually every half a second because of bug but I left it lol
            int secondsLeft = (MAX_TICKS - ticks) / 20;
            ChatFormatting secondsFormatting = secondsLeft > 30 ? ChatFormatting.GOLD : ChatFormatting.YELLOW;
            Component seconds = Component.literal(String.valueOf(secondsLeft)).withStyle(secondsFormatting);

            int currentMonsters = this.currentMonsters.size();
            int monstersAvailableToSpawn = maxMonsters - currentMonsters;
            ChatFormatting monstersFormatting = monstersAvailableToSpawn < maxMonsters / 2 ?
                    (monstersAvailableToSpawn < maxMonsters / 4 ? ChatFormatting.DARK_RED : ChatFormatting.RED) : ChatFormatting.GOLD;
            Component monsters = monstersAvailableToSpawn <= 3
                    ? Component.translatable("raiseofthepumpkins.danger", String.valueOf(currentMonsters)).withStyle(monstersFormatting)
                    : Component.literal(String.valueOf(currentMonsters)).withStyle(monstersFormatting);

            ChatFormatting titleFormatting = sculky ? ChatFormatting.AQUA : ChatFormatting.GOLD;
            Component title = Component.translatable("raiseofthepumpkins.title").withStyle(titleFormatting);

            List<ServerPlayer> nearbyPlayers = getNearbyPlayers();
            for (ServerPlayer player : nearbyPlayers) {
                player.displayClientMessage(Component.translatable("raiseofthepumpkins.hud", title, seconds, monsters), true);
            }
        }
    }

    public List<ServerPlayer> getNearbyPlayers() {
        return this.level.getPlayers(player -> {
            double sqrDistance = player.distanceToSqr(startPos.getX(), startPos.getY(), startPos.getZ());
            return player.isAlive() && sqrDistance <= 32 * 32;
        });
    }

    public void summonMonster() {
        BlockPos spawnPos = findRandomSpawnPos(0, 20);
        if(spawnPos == null) return;

        Zombie zombie = EntityType.ZOMBIE.create(this.level);
        if(zombie == null) return;

        zombie.setPos(spawnPos.getX() + 0.5, spawnPos.getY() + 1, spawnPos.getZ() + 0.5);
        zombie.finalizeSpawn(this.level, this.level.getCurrentDifficultyAt(spawnPos), MobSpawnType.EVENT, null);
        zombie.setOnGround(true);
        zombie.setItemSlot(EquipmentSlot.HEAD, Items.CARVED_PUMPKIN.getDefaultInstance());

        setZombieGear(zombie);

        int secondsLeft = (MAX_TICKS - ticks) / 20;
        this.level.addFreshEntity(zombie);
        this.currentMonsters.add(zombie);
        int minTicks = (secondsLeft > 30 ? 80 : 32) / (sculky ? 4 : 1);
        int maxTicks = (secondsLeft > 30 ? 180 : 120) / (sculky ? 4 : 1);
        this.monsterTicks = this.level.random.nextInt(minTicks, maxTicks);
        this.lastMonsterTicks = 0;

        this.level.playSound(null, spawnPos.getX(), spawnPos.getY(), spawnPos.getZ(), SoundEvents.TRIDENT_RIPTIDE_1, SoundSource.HOSTILE, 2f, 1.5f);
    }

    private void setZombieGear(Zombie zombie) {
        Item weaponStack = null;
        int weapon = this.level.random.nextInt(7);
        if(weapon >= 4) {
            switch (weapon) {
                case 4 -> weaponStack = sculky ? Items.DIAMOND_SHOVEL : Items.GOLDEN_SHOVEL;
                case 5 -> weaponStack = sculky ? Items.DIAMOND_SWORD : Items.GOLDEN_SWORD;
                case 6 -> weaponStack = sculky ? Items.NETHERITE_SWORD : Items.DIAMOND_SWORD;
                case 7 -> weaponStack = sculky ? Items.NETHERITE_AXE : Items.DIAMOND_AXE;
            }
        }

        int secondsLeft = (MAX_TICKS - ticks) / 20;

        Item funItem = Items.PUMPKIN_PIE;
        if(sculky) {
            int funItemIndex = this.level.random.nextInt(5);
            switch (funItemIndex) {
                case 4 -> funItem = Items.SCULK;
                case 5 -> funItem = Items.CARVED_PUMPKIN;
            }
        }

        if(weaponStack != null) {
            zombie.setItemSlot(EquipmentSlot.MAINHAND, weaponStack.getDefaultInstance());
            zombie.setDropChance(EquipmentSlot.MAINHAND, 0.05f);
            zombie.setItemSlot(EquipmentSlot.OFFHAND, funItem.getDefaultInstance());
            if(secondsLeft <= 30) {
                zombie.setGuaranteedDrop(EquipmentSlot.OFFHAND);
            } else {
                zombie.setDropChance(EquipmentSlot.OFFHAND, 0.20f);
            }
        } else {
            zombie.setItemSlot(EquipmentSlot.MAINHAND, funItem.getDefaultInstance());
            if(secondsLeft <= 30) {
                zombie.setGuaranteedDrop(EquipmentSlot.MAINHAND);
            } else {
                zombie.setDropChance(EquipmentSlot.MAINHAND, 0.20f);
            }
        }
    }

    @Nullable
    private BlockPos findRandomSpawnPos(int offsetMultiplier, int maxTry) {
        int i = offsetMultiplier == 0 ? 2 : 2 - offsetMultiplier;
        BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos();
        SpawnPlacementType spawnType = SpawnPlacements.getPlacementType(EntityType.RAVAGER);

        for (int i1 = 0; i1 < maxTry; i1++) {
            float f = this.level.random.nextFloat() * (float) (Math.PI * 2);
            int j = this.startPos.getX() + Mth.floor(Mth.cos(f) * 5f * (float)i) + this.level.random.nextInt(5);
            int l = this.startPos.getZ() + Mth.floor(Mth.sin(f) * 5f * (float)i) + this.level.random.nextInt(5);
            int k = this.level.getHeight(Heightmap.Types.WORLD_SURFACE, j, l);
            mutableBlockPos.set(j, k, l);
            if (!this.level.isVillage(mutableBlockPos) || offsetMultiplier >= 2) {
                int j1 = 10;
                if (this.level
                        .hasChunksAt(
                                mutableBlockPos.getX() - j1,
                                mutableBlockPos.getZ() - j1,
                                mutableBlockPos.getX() + j1,
                                mutableBlockPos.getZ() + j1
                        )
                        && this.level.isPositionEntityTicking(mutableBlockPos)
                        && (
                        spawnType.isSpawnPositionOk(this.level, mutableBlockPos, EntityType.RAVAGER)
                                || this.level.getBlockState(mutableBlockPos.below()).is(Blocks.SNOW)
                                && this.level.getBlockState(mutableBlockPos).isAir()
                )) {
                    return mutableBlockPos;
                }
            }
        }

        return null;
    }

    public boolean endEarly() {
        boolean shouldEndEarly = this.level.getDifficulty() == Difficulty.PEACEFUL || currentMonsters.size() > maxMonsters;
        if(shouldEndEarly) {
            this.lost = true;
        }
        return shouldEndEarly;
    }

    public void finish() {
        this.getNearbyPlayers().forEach(player -> {
            if(!lost) {
                if(sculky) player.giveExperiencePoints(5 * monstersKilled);
                Component text = Component.translatable("raiseofthepumpkins.victory").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);
                player.connection.send(new ClientboundSetTitleTextPacket(text));
            } else {
                Component text = Component.translatable("raiseofthepumpkins.defeat").withStyle(ChatFormatting.RED, ChatFormatting.BOLD);
                player.connection.send(new ClientboundSetTitleTextPacket(text));
            }
        });

        if(!lost) {
            this.level.playSound(null, this.startPos, SoundEvents.TRIAL_SPAWNER_ABOUT_TO_SPAWN_ITEM, SoundSource.MASTER, 2f, 1f);
            this.level.playSound(null, this.startPos, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.MASTER, 1f, 1.5f);
        } else {
            this.level.playSound(null, this.startPos, SoundEvents.TRIDENT_THUNDER.value(), SoundSource.MASTER, 1f, 0.5f);
        }

        this.currentMonsters.forEach(Entity::discard);
        this.finished = true;
    }

}
