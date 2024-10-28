package game.dispatcher.event;

import com.alibaba.fastjson2.JSONObject;
import jsa.aio.session.IoSession;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.socket.WebSocketSession;

@Getter
@Setter
public class HeartBeatEvent extends JsaEvent{
    public final static String Heart_Beat = "Heart_Beat";

    private int bindingId;
    private WebSocketSession session;
    private JSONObject body;

    public HeartBeatEvent(String type, IoSession source) {
        super(type, source);
    }
}
