package game.dispatcher.core;

import com.alibaba.fastjson2.JSONObject;
import game.dispatcher.event.HeartBeatEvent;
import jsa.event.EventListener;
import org.springframework.web.socket.WebSocketSession;

public class HeartBeat implements EventListener<HeartBeatEvent> {
    @Override
    public void onEvent(HeartBeatEvent heartBeatEvent) {
        WebSocketSession session = heartBeatEvent.getSession();
        // Get userId
        Long userId = UserContext.getUserIdBySessionId(session.getId());
        if (userId != null) {
            // Get the data model
            UserModel user = UserContext.getUser(userId);
            if (user != null) {
                user.setOnlineTime(System.currentTimeMillis());
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("sysTime", System.currentTimeMillis());
                String Body = jsonObject.toJSONString();
                try {
                    ResultUtils.sendClintMessage(Cmd.S_C_HeartBeat, Body, session);
                } catch (Exception e) {
                    e.printStackTrace();
                    DebugUtil.logError("error!!! S_C_HeartBeat cmd : " + Cmd.S_C_HeartBeat + "  ," + e.getMessage());
                }
            }
        }
    }
}
