package game.dispatcher.core;

import game.dispatcher.event.MatchEvent;
import game.dispatcher.event.RoomEvent;
import jsa.event.EventContainer;
import jsa.event.EventListener;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Matching implements EventListener<MatchEvent> {
    public static final long BACKUP_THRESHOLD_MS = 20000;
    public static final Random random = new Random();
    @Override
    public void onEvent(MatchEvent matchEvent) {
        // Channel ID
        Long channelId = matchEvent.getChannelId();
        // Get Queue
        Map.Entry<Integer, ConcurrentLinkedDeque<UserModel>> entry = matchEvent.getEntry();
        Integer type = entry.getKey();
        ConcurrentLinkedDeque<UserModel> queue = entry.getValue();
        // Current time
        long currentTimeMillis = System.currentTimeMillis();
        // If there is no player, the latest time will be recorded to prevent the next single match from instantly entering the preparation mechanism
        if (queue.isEmpty()) {
            // Update the latest game time
            Map<Integer, Long> typeTime = MatchingAction.channelLastMatchTime.computeIfAbsent(channelId, k -> new ConcurrentHashMap<>());
            typeTime.put(type, currentTimeMillis);
            return;
        }
        // Game on
        boolean gameStarted = false;
        // If there are more than 5 people, the game will start
        while (queue.size() >= 5) {
            // 开始游戏
            startGame(channelId, type, queue);
            gameStarted = true;
        }
        // Record the last time the game was opened
        if (gameStarted) {
            Map<Integer, Long> typeTimeMap = MatchingAction.channelLastMatchTime.computeIfAbsent(channelId, k -> new ConcurrentHashMap<>());
            typeTimeMap.put(type, currentTimeMillis);
        }
        // Initialize the Map corresponding to channelId, if channelId does not exist
        MatchingAction.channelLastMatchTime.putIfAbsent(channelId, new ConcurrentHashMap<>());
        // Get the Map<Integer, Long> corresponding to the channelId
        Map<Integer, Long> typeTimeMap = MatchingAction.channelLastMatchTime.get(channelId);
        // Initialize the timestamp corresponding to type. If type does not exist, set it to the current time.
        typeTimeMap.putIfAbsent(type, currentTimeMillis);
        // Get the last game time
        Long lastMatchTime = MatchingAction.channelLastMatchTime.get(channelId).get(type);
        // Preliminary game mechanics restrictions
        int limitRank = Integer.parseInt(ServerConfigContext.getServerConfigValue("LimitRank"));
        // If the timeout period is exceeded, the preparation mechanism will be performed.
        if (queue.size() < 5 && currentTimeMillis - lastMatchTime >= BACKUP_THRESHOLD_MS && type < limitRank) {
            startBackupGame(channelId, type, queue);
            // 更新最新游戏时间
            Map<Integer, Long> typeTime = MatchingAction.channelLastMatchTime.computeIfAbsent(channelId, k -> new ConcurrentHashMap<>());
            typeTime.put(type, currentTimeMillis);
        }
    }

    private void startGame(Long channelId, Integer type, ConcurrentLinkedDeque<UserModel> queue) {
        // Room Users
        List<UserModel> playersForGame = new ArrayList<>();
        // Match 5 people to start a game
        int baseNum = 5;
        // Matching Users
        for (int i = 0; i < baseNum; i++) {
            UserModel player = queue.poll();
            if (player != null) {
                playersForGame.add(player);
            }
        }
        // Create a room
        RoomEvent roomEvent = new RoomEvent(RoomEvent.Create_Room, null);
        roomEvent.setBindingId(random.nextInt(16));
        roomEvent.setUsers(playersForGame);
        roomEvent.setChannelId(channelId);
        roomEvent.setGameType(type);
        EventContainer.dispatchEvent(roomEvent);
    }

    private void startBackupGame(Long channelId, Integer type, ConcurrentLinkedDeque<UserModel> queue) {
        // Adjust the order of players in the queue
        queue = UserModel.adjustQueue(queue);
        // Start the game
        startGame(channelId, type, queue);
    }
}
