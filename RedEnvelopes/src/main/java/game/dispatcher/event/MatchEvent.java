package game.dispatcher.event;

import jsa.aio.session.IoSession;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class MatchEvent extends JsaEvent {
    public final static String Join_Matching = "Join_Match";
    public final static String Cancel_Matching = "Cancel_Match";
    public final static String Matching = "Matching";

    private Long channelId;
    private Long userId;
    private int bindingId;
    private Map.Entry<Integer, ConcurrentLinkedDeque<UserModel>> entry;
    private int rank;

    public MatchEvent(String type, IoSession source) {
        super(type, source);
    }
}
