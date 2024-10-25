package net.superkat.pumpkinplougher;

import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.SingletonGeoAnimatable;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.util.ClientUtil;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.function.Consumer;
import java.util.stream.Stream;

public class PumpkinPlougherItem extends Item implements GeoItem {
    public static final RawAnimation IDLE_ANIM = RawAnimation.begin().thenLoop("animation.pumpkinplougher.idle");
    public static final RawAnimation PLOUGH_ANIM = RawAnimation.begin().thenLoop("animation.pumpkinplougher.plough");
    public static final RawAnimation PLOUGH_FIRST_PERSON_ANIM = RawAnimation.begin().thenLoop("animation.pumpkinplougher.ploughfirstperson");
    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    public static final int USE_DURATION = 1200;

    public PumpkinPlougherItem() {
        super(new Item.Properties().stacksTo(1));

        SingletonGeoAnimatable.registerSyncedAnimatable(this);
    }

    public void tickUse(Player player) {
        if(player.level().isClientSide) {
            Vec3 playerVel = player.getDeltaMovement();
            int playerVelSqr = (int) (Math.ceil(playerVel.x * playerVel.x) + Math.ceil(playerVel.z * playerVel.z));
            int time = (int) player.level().getDayTime();

            int particleTime = 15 - (playerVelSqr * 5);

            if(particleTime > 0 && time % particleTime == 0) {
                Vec3 handPos = player.getHandHoldingItemAngle(Items.FIREWORK_ROCKET);
                player.level().addParticle(ParticleTypes.SCULK_SOUL, player.getX() + handPos.x, player.getY() + 0.25, player.getZ() + handPos.z, -playerVel.x, 0.35, -playerVel.y);
                if(player.getRandom().nextInt(10) == 0) {
                    player.level().addParticle(ParticleTypes.LAVA, player.getX() + handPos.x, player.getY() + 0.55, player.getZ() + handPos.z, -playerVel.x, 1.35, -playerVel.y);
                    player.playSound(SoundEvents.LAVA_EXTINGUISH, 0.3f, 1.3f);
                }
            }
        } else {
            Vec3 playerVel = player.getKnownMovement();
            if(player instanceof ServerPlayer serverPlayer) {
                PumpkinPlougherPlayer pumpkinPlougherPlayer = (PumpkinPlougherPlayer) serverPlayer;
                Vec3 previousDeltaMovement = pumpkinPlougherPlayer.pumpkinplougher$previousDeltaMovement();
                pumpkinPlougherPlayer.pumpkinplougher$setPreviousDeltaMovement(playerVel);
                int boomTicks = pumpkinPlougherPlayer.pumpkinplougher$getBoomTicks();
                pumpkinPlougherPlayer.pumpkinplougher$setBoomTicks(boomTicks + 1);

                double speedDifference = playerVel.length() - previousDeltaMovement.length();

                if((boomTicks >= 40 && speedDifference >= 0.2) || boomTicks >= 300) {
                    showSonicBoom(serverPlayer);
                    pumpkinPlougherPlayer.pumpkinplougher$setBoomTicks(0);
                }

            }

            player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(1.5), entity -> {
                boolean notSpectator = !entity.isSpectator();
                boolean notAllied = !player.isAlliedTo(entity);
                boolean notArmorStand = !(entity instanceof ArmorStand);
                boolean notAttacker = entity != player;
//                boolean distance = entity.distanceToSqr(player) <= Math.pow(3.5, 2.0);
                return notSpectator && notAllied && notArmorStand && notAttacker;
            }).forEach(entity -> {
                entity.knockback(Math.abs(playerVel.length() * 3), -playerVel.x, -playerVel.z);
                float damage = (float) (playerVel.length() * 7);
                if((entity instanceof Monster && entity.isInvertedHealAndHarm()) || entity.getItemBySlot(EquipmentSlot.HEAD).is(Items.CARVED_PUMPKIN)) {
                    damage *= 50f;
                }
                boolean entityWasHurt = entity.hurt(player.damageSources().playerAttack(player), damage);
                if(entityWasHurt && entity.isDeadOrDying()) {
                    player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.TRIDENT_THUNDER, SoundSource.PLAYERS, 1f, 1.7f);
                    ((ServerLevel)player.level()).sendParticles(ParticleTypes.SCULK_SOUL, entity.getX(), entity.getEyeY(), entity.getZ(), 3, 0, 0, 0, 0.1);
                    ((ServerLevel)player.level()).sendParticles(ParticleTypes.SOUL_FIRE_FLAME, entity.getX(), entity.getEyeY(), entity.getZ(), 7, 0, 0, 0, 0.35);
                    ((ServerLevel)player.level()).sendParticles(ParticleTypes.TRIAL_SPAWNER_DETECTED_PLAYER_OMINOUS, entity.getX(), entity.getEyeY(), entity.getZ(), 7, 0, 0, 0, 0.1);
                }
            });

            if(player.level().getGameRules().getBoolean(PumpkinPlougher.PLOUGHABLE_PUMPKINS)) {
                int searchX = 2;
                int searchZ = 2;
                BlockPos searchPos = player.getOnPos();
                for (int x = -searchX; x < searchX; x++) {
                    for (int z = -searchZ; z < searchZ; z++) {
                        searchPos = player.blockPosition().offset(x, 0, z);
                        BlockState blockState = player.level().getBlockState(searchPos);
                        //find pumpkin
                        if(blockState.is(Blocks.PUMPKIN)) {
                            player.level().destroyBlock(searchPos, true, player);
                        }
                    }
                }
            }

        }
    }

    private void showSonicBoom(ServerPlayer player) {
        player.serverLevel().sendParticles(ParticleTypes.SONIC_BOOM, player.getX(), player.getEyeY(), player.getZ(), 1, 0, 0, 0, 0);
        player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.WARDEN_SONIC_CHARGE, SoundSource.PLAYERS, 1, 1.7f);
        player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 2, 0.8f);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if(level instanceof ServerLevel serverLevel) {
            triggerAnim(player, GeoItem.getOrAssignId(player.getItemInHand(hand), serverLevel), "controller", "plough");
        }
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, USE_DURATION, 99, false, false, false), player);
        return ItemUtils.startUsingInstantly(level, player, hand);
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity livingEntity, int timeCharged) {
        if(level instanceof ServerLevel serverLevel && livingEntity instanceof Player player) {
            triggerAnim(player, GeoItem.getOrAssignId(stack, serverLevel), "controller", "idle");
        }
        //likely causes bug by removing a potion speed but whatever
        livingEntity.removeEffect(MobEffects.MOVEMENT_SPEED);
        super.releaseUsing(stack, level, livingEntity, timeCharged);
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return USE_DURATION;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 5, state -> {
                ItemDisplayContext displayContext = state.getData(DataTickets.ITEM_RENDER_PERSPECTIVE);
                boolean firstPerson = displayContext.firstPerson();

                if(state.isCurrentAnimation(PLOUGH_ANIM) && firstPerson) {
                    state.setAndContinue(PLOUGH_FIRST_PERSON_ANIM);
                }

                return state.setAndContinue(IDLE_ANIM);
            }).triggerableAnim("plough", PLOUGH_ANIM).setSoundKeyframeHandler(event -> {
                Player player = ClientUtil.getClientPlayer();
                if(player != null) {
                    player.playSound(SoundEvents.MINECART_RIDING, 0.1f, 1.7f);
                    if(player.getRandom().nextInt(5) == 0) {
                        player.playSound(SoundEvents.CHAIN_STEP, 0.5f, 0.8f);
                        player.level().addParticle(ParticleTypes.DUST_PLUME, player.getX(), player.getY(), player.getZ(), 0, 0.1, 0);
                    }
                }
            }).setParticleKeyframeHandler(event -> {
                Player player = ClientUtil.getClientPlayer();
                if(player != null) {
                    player.level().addParticle(ParticleTypes.LARGE_SMOKE, player.getX(), player.getY(), player.getZ(), 0, 0.1, 0);
                }
            }).triggerableAnim("plough_first", PLOUGH_FIRST_PERSON_ANIM).triggerableAnim("idle", IDLE_ANIM)
        );
    }

    @Override
    public boolean isPerspectiveAware() {
        return true;
    }

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private PumpkinPlougherItemRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getGeoItemRenderer() {
                if(this.renderer == null)
                    this.renderer = new PumpkinPlougherItemRenderer();

                return this.renderer;
            }
        });
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geoCache;
    }
}
