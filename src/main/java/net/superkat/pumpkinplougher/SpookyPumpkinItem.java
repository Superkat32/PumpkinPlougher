package net.superkat.pumpkinplougher;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class SpookyPumpkinItem extends Item {
    protected boolean sculky = false;
    public SpookyPumpkinItem() {
        super(new Item.Properties());
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        boolean minigamesEnabled = level.getGameRules().getBoolean(PumpkinPlougher.PUMPKIN_MINIGAME_ENABLED);
        if(!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            if(minigamesEnabled) {
                PumpkinMinigameHandler.startNewMinigame(serverPlayer, sculky);
            } else {
                serverPlayer.displayClientMessage(Component.translatable("raiseofthepumpkins.minigamedisabled"), false);
            }
        }
        if(minigamesEnabled) {
            player.getItemInHand(usedHand).consume(1, player);
        }
        return super.use(level, player, usedHand);
    }

    public static class SpookySculkyPumpkinItem extends SpookyPumpkinItem {
        public SpookySculkyPumpkinItem() {
            super();
            sculky = true;
        }
    }
}
