package top.mcfpp.mod.debugger;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.resource.ResourceType;
import org.glassfish.tyrus.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.mcfpp.mod.debugger.command.BreakPointCommand;
import top.mcfpp.mod.debugger.command.FunctionPathGetter;
import top.mcfpp.mod.debugger.config.DebuggerConfig;
import top.mcfpp.mod.debugger.dap.DebuggerState;
import top.mcfpp.mod.debugger.dap.ScopeManager;
import top.mcfpp.mod.debugger.dap.WebSocketServer;

import java.io.InputStream;
import java.util.logging.LogManager;

/**
 * Main class of the Datapack Debugger mod.
 * This mod provides debugging capabilities for Minecraft datapacks by adding breakpoints
 * and debugging features to help developers debug their datapack functions.
 */
public class DatapackDebugger implements ModInitializer {
	/** Main logger for the mod's logging system */
	private static final Logger logger = LoggerFactory.getLogger("datapack-debugger");
	private static Server webSocketServer;

	/**
	 * Mod initialization method called on startup.
	 * Configures server events and initializes debugging commands.
	 */
	@Override
	public void onInitialize() {
		// Configure Java logging to reduce Tyrus logs
		try {
			final InputStream inputStream = DatapackDebugger.class.getResourceAsStream("/logging.properties");
			if (inputStream != null) {
				LogManager.getLogManager().readConfiguration(inputStream);
				logger.info("Successfully configured Java logging from properties file");
			} else {
				logger.warn("Could not find logging.properties file");
			}
		} catch (Exception e) {
			logger.error("Failed to configure Java logging", e);
		}
		
		// Load configuration
		DebuggerConfig.getInstance();
		logger.info("Datapack Debugger configured to run on localhost:{}/{}",
			DebuggerConfig.getInstance().getPath(),
			DebuggerConfig.getInstance().getPort());
		
		// Clear breakpoints when server starts
		ServerLifecycleEvents.SERVER_STARTED.register(server -> BreakPointCommand.clear());
		
		// Reset and initialize debugger state
		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			// Reset the debugger state
			DebuggerState.get().reset();
			// Set the server reference
			DebuggerState.get().setServer(server);
		});

		ServerLifecycleEvents.START_DATA_PACK_RELOAD.register((server, a) -> ScopeManager.get().clearFunctionPaths());
		
		// Start WebSocket server for DAP communication using configured settings
		ServerLifecycleEvents.SERVER_STARTED.register(server -> WebSocketServer.launch().ifPresent(wss -> webSocketServer = wss));
		
		// Handle server shutdown to clean up resources
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			// First, shut down the debugger state to send termination events
			try {
				DebuggerState.get().shutdown();
				
				// Wait 200ms to ensure events have time to be sent
				try {
					Thread.sleep(200);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					logger.warn("Interrupted while waiting for shutdown events to be sent", e);
				}
			} catch (Exception e) {
				logger.error("Error shutting down debugger state", e);
			}
			
			// Use the new clean WebSocket server shutdown method
			try {
				WebSocketServer.stopServer();
			} catch (Exception e) {
				logger.error("Error stopping WebSocket server", e);
			}
		});
		
		ResourceManagerHelper.get(ResourceType.SERVER_DATA).registerReloadListener(new FunctionPathGetter());

		// Initialize breakpoint command system
		BreakPointCommand.onInitialize();
	}

	/**
	 * Retrieves the main logger of the mod.
	 * @return The logger used for mod event logging
	 */
	public static Logger getLogger(){
		return logger;
	}
}
