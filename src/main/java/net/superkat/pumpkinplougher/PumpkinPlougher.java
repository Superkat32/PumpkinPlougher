package net.superkat.pumpkinplougher;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameRules;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(PumpkinPlougher.MODID)
public class PumpkinPlougher {
    public static final String MODID = "pumpkinplougher";
    private static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
//    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);
    public static final DeferredHolder<Item, PumpkinPlougherItem> PUMPKIN_PLOUGHER_ITEM = ITEMS.register("pumpkinplougher", PumpkinPlougherItem::new);
    public static final DeferredHolder<Item, SpookyPumpkinItem> SPOOKY_PUMPKIN_ITEM = ITEMS.register("spooky_pumpkin", SpookyPumpkinItem::new);
    public static final DeferredHolder<Item, SpookyPumpkinItem> SPOOKY_SCULKY_PUMPKIN_ITEM = ITEMS.register("spooky_sculky_pumpkin", SpookyPumpkinItem.SpookySculkyPumpkinItem::new);

    public static final GameRules.Key<GameRules.BooleanValue> PLOUGHABLE_PUMPKINS = GameRules.register(
            "ploughablePumpkins", GameRules.Category.PLAYER, GameRules.BooleanValue.create(true)
    );

    public static final GameRules.Key<GameRules.BooleanValue> PLOUGHABLE_PLAYERS = GameRules.register(
            "ploughablePlayers", GameRules.Category.PLAYER, GameRules.BooleanValue.create(true)
    );

    public static final GameRules.Key<GameRules.BooleanValue> PUMPKIN_MINIGAME_ENABLED = GameRules.register(
            "pumpkinMinigameEnabled", GameRules.Category.PLAYER, GameRules.BooleanValue.create(true)
    );

    public PumpkinPlougher(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);

        ITEMS.register(modEventBus);
//        CREATIVE_MODE_TABS.register(modEventBus);

        NeoForge.EVENT_BUS.register(this);
        NeoForge.EVENT_BUS.addListener(false, this::pumpkinPlougherItemTick);
        NeoForge.EVENT_BUS.addListener(this::tickPumpkinMinigames);
        NeoForge.EVENT_BUS.addListener(this::finishPumpkinMinigamesOnServerClose);
        NeoForge.EVENT_BUS.addListener(this::clearPumpkinMinigamesOnServerOpen);

        // Register the item to a creative tab
        modEventBus.addListener(this::addCreative);

        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.COMBAT)
            event.accept(PUMPKIN_PLOUGHER_ITEM.get());

        if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            event.accept(SPOOKY_PUMPKIN_ITEM.get());
            event.accept(SPOOKY_SCULKY_PUMPKIN_ITEM.get());
        }
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
    }

    @SubscribeEvent
    public void pumpkinPlougherItemTick(LivingEntityUseItemEvent.Tick event) {
        if(event.getEntity() instanceof Player player) {
            ItemStack useItemStack = event.getItem();
            if(useItemStack.getItem() instanceof PumpkinPlougherItem pumpkinPlougherItem) {
                pumpkinPlougherItem.tickUse(player);
            }
        }
    }

    @SubscribeEvent
    public void tickPumpkinMinigames(ServerTickEvent.Post event) {
        PumpkinMinigameHandler.tickMinigames();
    }

    @SubscribeEvent
    public void finishPumpkinMinigamesOnServerClose(ServerStoppingEvent event) {
        PumpkinMinigameHandler.finishAllMinigames();
    }

    @SubscribeEvent
    public void clearPumpkinMinigamesOnServerOpen(ServerStartingEvent event) {
        PumpkinMinigameHandler.minigames.clear();
    }

    @EventBusSubscriber(modid = MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
        }
    }
}
