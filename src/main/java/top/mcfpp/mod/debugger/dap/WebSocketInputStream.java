package top.mcfpp.mod.debugger.dap;


import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Adapts WebSocket messages into an InputStream.
 */
public class WebSocketInputStream extends InputStream {
    private final LinkedBlockingQueue<byte[]> queue;
    private ByteArrayInputStream currentStream;

    public WebSocketInputStream(LinkedBlockingQueue<byte[]> queue) {
        this.queue = queue;
    }

    @Override
    public int read() throws IOException {
        if (currentStream == null || currentStream.available() == 0) {
            try {
                byte[] nextMessage = queue.take();
                currentStream = new ByteArrayInputStream(nextMessage);
            } catch (InterruptedException e) {
                throw new IOException("Interrupted while waiting for a WebSocket message", e);
            }
        }
        return currentStream.read();
    }
}
