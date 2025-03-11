package top.mcfpp.mod.debugger.dap;

import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;
import org.eclipse.lsp4j.debug.services.IDebugProtocolClient;
import org.eclipse.lsp4j.debug.launch.DSPLauncher;
import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.glassfish.tyrus.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Optional;
import java.util.concurrent.LinkedBlockingQueue;

@ServerEndpoint(value="/dap")
public class WebSocketServer {

    private static final Logger logger = LoggerFactory.getLogger("datapack-debugger");

    private DapServer dapServer;
    private Launcher<IDebugProtocolClient> launcher;
    private final LinkedBlockingQueue<byte[]> messageQueue = new LinkedBlockingQueue<>();


    @OnOpen
    public void onOpen(Session session) {
//        logger.info("WebSocket connected: {}", session.getRequestURI());
        dapServer = new DapServer();
        InputStream in = new WebSocketInputStream(messageQueue);
        OutputStream out = new WebSocketOutputStream(session);
        launcher = DSPLauncher.createServerLauncher(dapServer, in, out);
        dapServer.setClient(launcher.getRemoteProxy());
        launcher.startListening();
    }

    @OnMessage
    public void onMessage(String message) {
//        logger.info("Received message: {}", message);
        messageQueue.offer(message.getBytes());
    }

    @OnMessage
    public void onMessage(byte[] message) {
//        logger.info("Received byte message: {}", new String(message));
        messageQueue.offer(message);
    }

    @OnClose
    public void onClose(Session session, CloseReason closeReason) {
        dapServer.exit();
        logger.info("WebSocket closed: {}", closeReason);
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        dapServer.exit();
        logger.error("Error in DAP server", throwable);
    }

    private static Server server;

    public static Optional<Server> launch(int port) {
        if(server == null) {
            server = new Server("localhost", 25599, "/", null, WebSocketServer.class);
            try {
                server.start();
                logger.info("Jakarta WebSocket DAP server is running on ws://localhost:{}/dap", port);
            } catch (Exception e) {
                logger.error("Error starting DAP server", e);
                server.stop();
                server = null;
            }
        }
        return Optional.ofNullable(server);
    }
}
