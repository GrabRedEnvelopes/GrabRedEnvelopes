package game.dispatcher.event;

import jsa.aio.session.IoSession;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PlayEvent extends JsaEvent {

    public final static String Shuffle = "Shuffle";
    public final static String Flip = "Flip";

    private Long channelId;
    private int bindingId;
    private Long roomId;
    private Long userId;
    private String cardIndex;

    public PlayEvent(String type, IoSession source) {
        super(type, source);
    }
}
