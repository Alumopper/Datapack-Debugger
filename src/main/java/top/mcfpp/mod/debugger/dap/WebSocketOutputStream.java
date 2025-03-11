package top.mcfpp.mod.debugger.dap;

import jakarta.websocket.Session;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Adapts output to the WebSocket by buffering data until flush() is called.
 */
public class WebSocketOutputStream extends OutputStream {
    private final Session session;
    private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

    public WebSocketOutputStream(Session session) {
        this.session = session;
    }

    @Override
    public void write(int b) {
        buffer.write(b);
    }

    @Override
    public void flush() throws IOException {
        String message = buffer.toString(StandardCharsets.UTF_8);
        buffer.reset();
        if (session.isOpen()) {
            session.getAsyncRemote().sendText(message);
        }
    }

    @Override
    public void close() throws IOException {
        flush();
    }
}