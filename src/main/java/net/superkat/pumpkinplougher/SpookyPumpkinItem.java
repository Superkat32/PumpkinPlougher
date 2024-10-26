package net.superkat.pumpkinplougher;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
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
        ItemStack usedItem = player.getItemInHand(usedHand);
        boolean minigamesEnabled = level.getGameRules().getBoolean(PumpkinPlougher.PUMPKIN_MINIGAME_ENABLED);
        if(!level.isClientSide && player instanceof ServerPlayer serverPlayer) {

            boolean peaceful = level.getDifficulty().equals(Difficulty.PEACEFUL);
            boolean canStart = minigamesEnabled && !peaceful;

            if(canStart) {
                PumpkinMinigameHandler.startNewMinigame(serverPlayer, sculky);
                usedItem.consume(1, player);
            } else {
                if(peaceful) {
                    serverPlayer.displayClientMessage(Component.translatable("raiseofthepumpkins.peaceful").withStyle(ChatFormatting.RED), false);
                } else {
                    serverPlayer.displayClientMessage(Component.translatable("raiseofthepumpkins.minigamedisabled").withStyle(ChatFormatting.RED), false);
                }
            }
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
