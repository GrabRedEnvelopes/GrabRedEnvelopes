package game.dispatcher.event;

import jsa.aio.session.IoSession;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.socket.WebSocketSession;

import java.nio.ByteBuffer;

@Getter
@Setter
public class SendMassageEvent extends JsaEvent{

    public final static String Send_Massage = "Send_Massage";

    private int bindingId;
    private WebSocketSession session;
    private ByteBuffer messageBuffer;

    public SendMassageEvent(String type, IoSession source) {
        super(type, source);
    }
}
