package game.dispatcher.core;

import game.dispatcher.event.SendMassageEvent;
import jsa.event.EventListener;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.nio.ByteBuffer;

public class SendMassage implements EventListener<SendMassageEvent> {
    @Override
    public void onEvent(SendMassageEvent sendMassageEvent) {
        WebSocketSession session = sendMassageEvent.getSession();

        if (session != null && session.isOpen()) {
            Long userId = UserContext.getUserIdBySessionId(session.getId());
            if (userId != null) {
                UserModel user = UserContext.getUser(userId);
                if (user != null) {
                    if (!user.isOnline()) {
                        return;
                    }
                }
            }
            ByteBuffer messageBuffer = sendMassageEvent.getMessageBuffer();
            try {
                session.sendMessage(new BinaryMessage(messageBuffer));
            } catch (IOException e) {
                e.printStackTrace();
                DebugUtil.logError("SendMassageListener error!!! messageBuffer : " + messageBuffer + ", userId: " + userId);
            }
        }
    }
}
