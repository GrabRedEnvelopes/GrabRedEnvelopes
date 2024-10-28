package game.dispatcher.core;

import com.alibaba.fastjson2.JSONObject;
import game.dispatcher.event.PlayEvent;
import jsa.event.EventListener;
import org.springframework.web.socket.WebSocketSession;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class RedEnvelopesCore implements EventListener<PlayEvent> {
    private static final int TOTAL_ROUNDS = 5;
    private static final Random random = new Random();
    @Override
    public void onEvent(PlayEvent playEvent) {

        long roomId = playEvent.getRoomId();
        RoomInfo roomInfo = RoomContext.ROOM_MAP.get(roomId);
        // Basic Information
        List<UserModel> users = roomInfo.getUsers();
        int type = roomInfo.getType();
        // Initial flop sequence
        List<UserModel> currentOrder = new ArrayList<>(users);
        Collections.shuffle(currentOrder);
        // Setting the location
        for (int i = 0; i < currentOrder.size(); i++) {
            UserModel userModel = currentOrder.get(i);
            userModel.setPosition(i + 1);
        }
        Map<Integer, List<UserModel>> userOrderMap = new ConcurrentHashMap<>();
        // Card play order generation
        for (int round = 1; round <= TOTAL_ROUNDS; round++) {
            userOrderMap.put(round, new ArrayList<>(currentOrder));
            UserModel first = currentOrder.remove(0);
            currentOrder.add(first);
        }
        // Set the player
        UserModel userModel = userOrderMap.get(1).get(0);
        userModel.setHolding(true);
        userModel.setHoldingTime(System.currentTimeMillis() + 2000);
        roomInfo.setUserOrderMap(userOrderMap);
        // Assign random points (each card has a points greater than 0)
        Map<Integer, List<Integer>> cardPointsOrderMap = new ConcurrentHashMap<>();
        for (int round = 1; round <= TOTAL_ROUNDS; round++) {
            List<Integer> cardPoints = generateRandomPoints(type, users.size());
            cardPointsOrderMap.put(round, cardPoints);
        }
        roomInfo.setCardPointsOrderMap(cardPointsOrderMap);

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("round", 1);
        List<UserModel> userModels = userOrderMap.get(1);
        jsonObject.put("currentOrder", userModels);
        String Body = jsonObject.toJSONString();
        for (UserModel model : userModels) {
            WebSocketSession mySession = model.getSession();
            if (mySession != null && mySession.isOpen() && model.isOnline()) {
                try {
                    ResultUtils.sendClintMessage(Cmd.S_C_Shuffle, Body, mySession);
                } catch (Exception e) {
                    e.printStackTrace();
                    DebugUtil.logError("Shuffle error!!! S_C_Shuffle cmd : " + Cmd.S_C_Shuffle + "  ," + e.getMessage());
                }
            }
        }
    }

    // Generates random scores, the sum of which is totalPoints, the number of which is numPlayers and each score is greater than 0
    private static List<Integer> generateRandomPoints(int totalPoints, int numPlayers) {
        List<Integer> points = new ArrayList<>();
        // Remaining points
        int remainingPoints = totalPoints;
        // Minimum Limit
        int minPoint = 1;
        // Randomly generate numPlayers - 1 score
        for (int i = 0; i < numPlayers - 1; i++) {
            // Update maxPoint each time
            int maxPoint = (int) (0.75 * remainingPoints);
            // Make sure there are enough points left to share with other players
            int maxAvailable = Math.min(remainingPoints - (numPlayers - i - 1) * minPoint, maxPoint);
            // Randomly generate a score within a range
            int point = random.nextInt(maxAvailable - minPoint + 1) + minPoint;
            points.add(point);
            remainingPoints -= point;
        }
        // The last score is all the remaining scores
        points.add(remainingPoints);

        // Shuffle
        Collections.shuffle(points);
        return points;
    }

}
