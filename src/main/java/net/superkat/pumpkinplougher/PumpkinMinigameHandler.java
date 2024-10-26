package net.superkat.pumpkinplougher;

import net.minecraft.server.level.ServerPlayer;
import java.util.ArrayList;
import java.util.Iterator;

public class PumpkinMinigameHandler {
    public static ArrayList<PumpkinMinigame> minigames = new ArrayList<>();

    public static void startNewMinigame(ServerPlayer player, boolean sculky) {
        PumpkinMinigame minigame = new PumpkinMinigame(player.serverLevel(), player).sculkyMode(sculky);
        minigames.add(minigame);
        minigame.start();
    }

    public static void tickMinigames() {
        Iterator<PumpkinMinigame> iterator = minigames.iterator();

        while (iterator.hasNext()) {
            PumpkinMinigame minigame = iterator.next();
            if(minigame.finished) {
                iterator.remove();
            } else {
                minigame.tick();
            }
        }
    }

    public static void finishAllMinigames() {
        minigames.forEach(PumpkinMinigame::finish);
        minigames.clear();
    }

}
