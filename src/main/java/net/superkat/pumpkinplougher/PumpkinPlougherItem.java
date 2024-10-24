package net.superkat.pumpkinplougher;

import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.level.Level;
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

public class PumpkinPlougherItem extends Item implements GeoItem {
    public static final RawAnimation IDLE_ANIM = RawAnimation.begin().thenLoop("animation.pumpkinplougher.idle");
    public static final RawAnimation PLOUGH_ANIM = RawAnimation.begin().thenLoop("animation.pumpkinplougher.plough");
    public static final RawAnimation PLOUGH_FIRST_PERSON_ANIM = RawAnimation.begin().thenLoop("animation.pumpkinplougher.ploughfirstperson");
    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    public PumpkinPlougherItem() {
        super(new Item.Properties().stacksTo(1));

        SingletonGeoAnimatable.registerSyncedAnimatable(this);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if(level instanceof ServerLevel serverLevel) {
            triggerAnim(player, GeoItem.getOrAssignId(player.getItemInHand(hand), serverLevel), "controller", "plough");
        }
        return ItemUtils.startUsingInstantly(level, player, hand);
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity livingEntity, int timeCharged) {
        if(level instanceof ServerLevel serverLevel && livingEntity instanceof Player player) {
            triggerAnim(player, GeoItem.getOrAssignId(stack, serverLevel), "controller", "idle");
        }
        super.releaseUsing(stack, level, livingEntity, timeCharged);
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 1200;
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
                }
            }).setParticleKeyframeHandler(event -> {
                Player player = ClientUtil.getClientPlayer();
                if(player != null) {
                    player.level().addParticle(ParticleTypes.SMOKE, player.getX(), player.getY(), player.getZ(), 0, 0.1, 0);
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
