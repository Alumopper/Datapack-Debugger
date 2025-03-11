package top.mcfpp.mod.debugger.dap;

import jakarta.websocket.*;
import jakarta.websocket.server.ServerApplicationConfig;
import jakarta.websocket.server.ServerEndpointConfig;
import org.eclipse.lsp4j.debug.services.IDebugProtocolClient;
import org.eclipse.lsp4j.debug.launch.DSPLauncher;
import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.glassfish.tyrus.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.mcfpp.mod.debugger.config.DebuggerConfig;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.HashSet;

// We now define the endpoint programmatically, not with annotation
public class WebSocketServer extends Endpoint {

    private static final Logger logger = LoggerFactory.getLogger("datapack-debugger");

    private DapServer dapServer;
    private Launcher<IDebugProtocolClient> launcher;
    private final LinkedBlockingQueue<byte[]> messageQueue = new LinkedBlockingQueue<>();
    private Session currentSession;

    // This replaces the @OnOpen annotation
    @Override
    public void onOpen(Session session, EndpointConfig config) {
        logger.info("WebSocket connected: {}", session.getRequestURI());
        
        // Store the session for later clean closure
        this.currentSession = session;
        
        // Configure a longer timeout for the session
        session.setMaxIdleTimeout(0); // No inactivity timeout
        
        // Configure buffer sizes
        session.setMaxTextMessageBufferSize(65536);
        session.setMaxBinaryMessageBufferSize(65536);
        
        // Add message handlers for text and binary
        session.addMessageHandler(new MessageHandler.Whole<String>() {
            @Override
            public void onMessage(String message) {
                messageQueue.offer(message.getBytes());
            }
        });
        
        session.addMessageHandler(new MessageHandler.Whole<byte[]>() {
            @Override
            public void onMessage(byte[] message) {
                messageQueue.offer(message);
            }
        });
        
        // Initialize a new DAP server for this session
        dapServer = new DapServer();
        InputStream in = new WebSocketInputStream(messageQueue);
        OutputStream out = new WebSocketOutputStream(session);
        launcher = DSPLauncher.createServerLauncher(dapServer, in, out);
        dapServer.setClient(launcher.getRemoteProxy());
        launcher.startListening();
    }

    // This replaces the @OnClose annotation
    @Override
    public void onClose(Session session, CloseReason closeReason) {
        logger.info("WebSocket closed: {}", closeReason);
        cleanup();
    }

    // This replaces the @OnError annotation
    @Override
    public void onError(Session session, Throwable throwable) {
        logger.error("Error in DAP server", throwable);
        cleanup();
    }
    
    /**
     * Method to clean up resources for this instance
     */
    private void cleanup() {
        // Signal to the DAP server that it should terminate
        if (dapServer != null) {
            try {
                dapServer.exit();
            } catch (Exception e) {
                logger.error("Error shutting down DAP server", e);
            }
            dapServer = null;
        }
        
        // Clear the message queue
        messageQueue.clear();
        
        // Close the session if it is still open
        if (currentSession != null && currentSession.isOpen()) {
            try {
                currentSession.close();
            } catch (Exception e) {
                logger.error("Error closing WebSocket session", e);
            }
            currentSession = null;
        }
        
        // Stop the launcher listener if necessary
        if (launcher != null) {
            try {
                // No stopListening method, but we can help the GC
                launcher = null;
            } catch (Exception e) {
                logger.error("Error stopping launcher", e);
            }
        }
    }

    private static Server server;
    
    /**
     * Configuration class for the WebSocket endpoint
     */
    public static class WebSocketConfigurator implements ServerApplicationConfig {
        @Override
        public Set<ServerEndpointConfig> getEndpointConfigs(Set<Class<? extends Endpoint>> endpointClasses) {
            Set<ServerEndpointConfig> configs = new HashSet<>();
            
            // Create a programmatic endpoint with the configured path
            String path = "/" + DebuggerConfig.getInstance().getPath();
            ServerEndpointConfig config = ServerEndpointConfig.Builder
                .create(WebSocketServer.class, path)
                .build();
                
            configs.add(config);
            return configs;
        }

        @Override
        public Set<Class<?>> getAnnotatedEndpointClasses(Set<Class<?>> scanned) {
            // We don't use annotated endpoints
            return new HashSet<>();
        }
    }

    /**
     * Launches the WebSocket server for DAP communication
     * This method ensures any previous server is stopped before creating a new one
     * 
     * @return A reference to the started server, if startup was successful
     */
    public static Optional<Server> launch() {
        // Use configured port from settings
        return launch(DebuggerConfig.getInstance().getPort());
    }

    /**
     * Launches the WebSocket server for DAP communication
     * This method ensures any previous server is stopped before creating a new one
     * 
     * @param port The port on which to start the server
     * @return A reference to the started server, if startup was successful
     */
    public static Optional<Server> launch(int port) {
        // Properly stop any existing server
        if (server != null) {
            try {
                server.stop();
                // Wait a moment for the system to release resources
                Thread.sleep(100);
            } catch (Exception e) {
                logger.error("Error stopping existing WebSocket server", e);
            } finally {
                server = null;
            }
        }
        
        // Get configuration values
        DebuggerConfig config = DebuggerConfig.getInstance();
        String path = config.getPath();
        
        // Create and start a new server
        server = new Server("localhost", port, "/", null, WebSocketConfigurator.class);
        try {
            server.start();
            logger.info("Jakarta WebSocket DAP server is running on ws://localhost:{}/{}", port, path);
        } catch (Exception e) {
            logger.error("Error starting DAP server", e);
            try {
                server.stop();
            } catch (Exception stopEx) {
                logger.error("Error stopping failed server", stopEx);
            }
            server = null;
        }
        
        return Optional.ofNullable(server);
    }
    
    /**
     * Safely stops the WebSocket server
     */
    public static void stopServer() {
        if (server != null) {
            try {
                server.stop();
                logger.info("WebSocket server stopped");
            } catch (Exception e) {
                logger.error("Error stopping WebSocket server", e);
            } finally {
                server = null;
            }
        }
    }
}
