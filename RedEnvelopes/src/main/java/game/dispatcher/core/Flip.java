package game.dispatcher.core;

import com.alibaba.fastjson2.JSONObject;
import game.dispatcher.event.PlayEvent;
import jsa.event.EventListener;
import org.springframework.web.socket.WebSocketSession;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Flip implements EventListener<PlayEvent> {

    @Override
    public void onEvent(PlayEvent playEvent) {
        // Channel ID
        long channelId = playEvent.getChannelId();
        // Flop Position
        String cardIndex = playEvent.getCardIndex();
        // Room ID
        Long roomId = playEvent.getRoomId();
        RoomInfo roomInfo = RoomContext.ROOM_MAP.get(roomId);
        if (roomInfo == null) {
            return;
        }
        // Round
        Integer round = roomInfo.getRound();
        int playIndex = roomInfo.getPlayIndex();
        // Users
        Map<Integer, List<UserModel>> userOrderMap = roomInfo.getUserOrderMap();
        List<UserModel> userModels = userOrderMap.get(round);
        UserModel userModel = userModels.get(playIndex);
        // If you don't have a license, you won't be allowed to operate.
        if (!userModel.isHolding()) {
            return;
        }

        // Points
        Map<Integer, List<Integer>> cardPointsOrderMap = roomInfo.getCardPointsOrderMap();
        List<Integer> cardPoints = cardPointsOrderMap.get(round);
        // Get a card points
        int points = cardPoints.remove(0);
        userModel.addScore(points);
        // Store turned cards
        Map<Integer, Map<String, Integer>> cardFlipped = roomInfo.getCardFlipped();
        Map<String, Integer> nowFlipped = cardFlipped.computeIfAbsent(round, ConcurrentHashMap::new);
        nowFlipped.put(cardIndex, points);
        // Increased rounds
        playIndex += 1;
        if (playIndex > 4) {
            round += 1;
            playIndex = 0;
        }
        // Trigger end
        if (round == 6) {
            // Record game records
            int fee = roomInfo.getFee();
            int type = roomInfo.getType() + fee;
            int totalFee = roomInfo.getTotalFee();
            double ratio = roomInfo.getRatio();
            SysBattleRecords sysBattleRecords = new SysBattleRecords(channelId, roomId, type, totalFee, ratio);

            // All players are prohibited from playing cards
            userModel.setHolding(false);
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("round", round);
            jsonObject.put("currentOrder", userModels);
            jsonObject.put("flipped", nowFlipped);
            String Body = jsonObject.toJSONString();
            // Destroy the room
            RoomContext.ROOM_MAP.remove(roomId);
            // Game Tasks
            int index = 0;
            for (UserModel model : userModels) {
                // Store user ID and user final points
                index++;
                sysBattleRecords.setUserAndScore(index, model.getUserId(), model.getScore());
                // Record the number of games
                UserExtraInfoModel.modifyBattleTimes(model.getUserId(), 1);
                // Points ID
                long pointId = Long.parseLong(ServerConfigContext.getServerConfigValue("PointId"));
                // User deposit
                HttpServerUtils.addPoint(model.getUserId(), pointId, model.getScore());
                // Complete the game task
                int PlayActionId = Integer.parseInt(ServerConfigContext.getServerConfigValue("PlayActionId"));
                TaskModel.checkAndDoTask(model.getUserId(), PlayActionId, 1);
                try {
                    WebSocketSession mySession = model.getSession();
                    if (mySession != null && mySession.isOpen() && model.isOnline()) {
                        ResultUtils.sendClintMessage(Cmd.S_C_PlayEnd, Body, mySession);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    DebugUtil.logError("Flip error!!! S_C_PlayEnd cmd : " + Cmd.S_C_PlayEnd + "  ," + e.getMessage());
                }
            }
            // Reset
            for (UserModel model : userModels) {
                model.reset();
            }
            // Record game records
            MapperContext.sysBattleRecordsMapper.insert(sysBattleRecords);
            return;
        }
        // Update game match information
        roomInfo.setRound(round);
        roomInfo.setPlayIndex(playIndex);
        // Update player card order
        userModel.setHolding(false);
        List<UserModel> nextList = userOrderMap.get(round);
        UserModel nextUser = nextList.get(playIndex);
        nextUser.setHolding(true);
        nextUser.setHoldingTime(System.currentTimeMillis());

        // Send message
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("round", round);
        jsonObject.put("currentOrder", nextList);
        jsonObject.put("flipped", nowFlipped);
        String Body = jsonObject.toJSONString();
        for (UserModel model : nextList) {
            try {
                WebSocketSession mySession = model.getSession();
                if (mySession != null && mySession.isOpen() && model.isOnline()) {
                    ResultUtils.sendClintMessage(Cmd.S_C_Play, Body, mySession);
                }
            } catch (Exception e) {
                e.printStackTrace();
                DebugUtil.logError("Flip error!!! S_C_Play cmd : " + Cmd.S_C_Play + "  ," + e.getMessage());
            }
        }
    }
}
