package game.dispatcher.event;

import jsa.aio.session.IoSession;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class RoomEvent extends JsaEvent {

    public final static String Create_Room = "Create_Room";

    private Long channelId;
    private int bindingId;
    private long creationTime = System.currentTimeMillis();
    private List<UserModel> users;
    private int gameType;

    public RoomEvent(String type, IoSession source) {
        super(type, source);
    }
}


