package net.gunivers.sniffer.dap;


import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Adapts WebSocket messages into an InputStream.
 * This class provides a bridge between the WebSocket message-based API and 
 * the Java stream-based API by converting incoming WebSocket messages 
 * into a continuous input stream that can be consumed by stream-based APIs.
 * <p>
 * The class blocks when no data is available and waits for messages
 * to arrive on the provided message queue.
 *
 * @author theogiraudet
 */
public class WebSocketInputStream extends InputStream {
    /** Queue containing incoming WebSocket messages */
    private final LinkedBlockingQueue<byte[]> queue;
    /** Current stream being read from */
    private ByteArrayInputStream currentStream;

    /**
     * Creates a new WebSocketInputStream.
     * 
     * @param queue The queue that will be filled with WebSocket messages
     */
    public WebSocketInputStream(LinkedBlockingQueue<byte[]> queue) {
        this.queue = queue;
    }

    /**
     * Reads a single byte from the WebSocket message stream.
     * If the current message has been fully read, this method will block until
     * a new message is available in the queue.
     * 
     * @return The byte read, or -1 if the end of the stream is reached
     * @throws IOException If an I/O error occurs or the thread is interrupted
     */
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
