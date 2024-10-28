package game.dispatcher.event;

import jsa.aio.message.IMessage;
import jsa.aio.session.IoSession;
import jsa.event.Event;

public class JsaEvent extends Event<IoSession> {
    private IMessage cmd = null;

    public JsaEvent(String type, IoSession source) {
        super(type, source);
    }

    public IMessage getMessage() {
        return cmd;
    }

    public void setMessage(IMessage cmd) {
        this.cmd = cmd;
    }
}
