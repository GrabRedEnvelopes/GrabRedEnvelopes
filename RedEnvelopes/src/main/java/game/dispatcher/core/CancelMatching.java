package game.dispatcher.core;

import game.dispatcher.event.MatchEvent;
import jsa.event.EventListener;
import org.springframework.web.socket.WebSocketSession;

public class CancelMatching implements EventListener<MatchEvent> {

    @Override
    public void onEvent(MatchEvent matchEvent) {
        // Channel Id
        long channelId = matchEvent.getChannelId();
        // UserId
        Long userId = matchEvent.getUserId();
        // Get user information
        UserModel userModel = UserContext.getUser(userId);
        if (userModel != null) {
            // Get matching queue
            Integer type = userModel.getType();
            if (type == null) {
                DebugUtil.logError("error!!! CancelMatching type is null , userId : " + userId);
                return;
            }
            ConcurrentLinkedDeque<UserModel> queue = MatchingAction.CHANNEL_MATCHING_QUEUES.get(channelId).get(type);
            if (queue != null) {
                // Matchmaking Queue Cancel Matchmaking
                queue.remove(userModel);
                // User set idle state
                userModel.setState(UserState.Free.index);
                WebSocketSession session = userModel.getSession();
                if (session != null && session.isOpen() && userModel.isOnline()) {
                    try {
                        ResultUtils.sendClintMessage(Cmd.S_C_CancelMatching, "", session);
                    } catch (Exception e) {
                        e.printStackTrace();
                        DebugUtil.logError("error!!! S_C_CancelMatching cmd : " + Cmd.S_C_CancelMatching + "  ," + e.getMessage());
                    }
                }
            } else {
                DebugUtil.logError("error!!! CancelMatching queue is null , userId : " + userId);
            }
        }
    }
}
