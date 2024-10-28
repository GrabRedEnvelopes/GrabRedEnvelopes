package game.dispatcher.core;

import com.alibaba.fastjson2.JSONObject;
import game.dispatcher.event.MatchEvent;
import jsa.event.EventListener;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

public class JoinMatch implements EventListener<MatchEvent> {

    @Override
    public void onEvent(MatchEvent matchEvent) {
        // Channel ID
        long channelId = matchEvent.getChannelId();
        // userId
        Long userId = matchEvent.getUserId();
        // Rank ID
        int rank = matchEvent.getRank();
        // Get bets
        int type = RankType.getTypeByRank(rank);
        UserModel userModel = UserContext.getUser(userId);
        WebSocketSession session = userModel.getSession();
        if (session != null && userModel.isOnline()) {
            // Join Match
            long currentTimeMillis = System.currentTimeMillis();
            UserModel player = UserContext.getUser(userId);
            player.setPriority(currentTimeMillis);
            player.setState(UserState.Match.index);
            player.setType(type);
            Map<Integer, ConcurrentLinkedDeque<UserModel>> rankQueueMap = MatchingAction.CHANNEL_MATCHING_QUEUES.computeIfAbsent(channelId, k -> new ConcurrentHashMap<>());
            ConcurrentLinkedDeque<UserModel> queue = rankQueueMap.computeIfAbsent(type, k -> new ConcurrentLinkedDeque<>());
            queue.add(player);
            // Get the last game time
            long lastMatchTime = System.currentTimeMillis();
            Map<Integer, Long> integerLongMap = MatchingAction.channelLastMatchTime.get(channelId);
            if (integerLongMap != null) {
                Long time = integerLongMap.get(type);
                if (time != null) {
                    lastMatchTime = time;
                }
            }
            // Estimated matching time
            long matchingTime = Math.abs(Matching.BACKUP_THRESHOLD_MS - (System.currentTimeMillis() - lastMatchTime));
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("matchingTime", matchingTime);
            String Body = jsonObject.toJSONString();
            try {
                if (session.isOpen() && userModel.isOnline()) {
                    ResultUtils.sendClintMessage(Cmd.S_C_JoinMatching, Body, session);
                }
            } catch (Exception e) {
                e.printStackTrace();
                DebugUtil.logError("JoinMatch error!!! S_C_JoinMatching cmd : " + Cmd.S_C_JoinMatching + "  ," + e.getMessage());
            }
        }
    }
}
