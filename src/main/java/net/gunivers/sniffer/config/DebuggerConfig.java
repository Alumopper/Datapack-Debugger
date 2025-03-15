package net.gunivers.sniffer.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Configuration class for the Sniffer mod.
 * Handles loading and saving configuration settings.
 */
public class DebuggerConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger("sniffer");
    private static final String CONFIG_FILENAME = "sniffer.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    
    private static DebuggerConfig instance;
    
    // Default config values
    private int port = 25599;
    private String path = "dap";
    
    /**
     * Gets the singleton instance of the configuration
     * 
     * @return The configuration instance
     */
    public static DebuggerConfig getInstance() {
        if (instance == null) {
            instance = load();
        }
        return instance;
    }
    
    /**
     * Loads the configuration from disk or creates default settings if no file exists
     * 
     * @return The loaded configuration
     */
    private static DebuggerConfig load() {
        Path configDir = FabricLoader.getInstance().getConfigDir();
        File configFile = configDir.resolve(CONFIG_FILENAME).toFile();
        
        DebuggerConfig config = new DebuggerConfig();
        
        if (configFile.exists()) {
            try (FileReader reader = new FileReader(configFile)) {
                config = GSON.fromJson(reader, DebuggerConfig.class);
                LOGGER.info("Loaded debugger configuration from {}", configFile);
            } catch (IOException e) {
                LOGGER.error("Failed to load configuration, using defaults", e);
            }
        } else {
            config.save();
            LOGGER.info("Created default configuration at {}", configFile);
        }
        
        return config;
    }
    
    /**
     * Saves the current configuration to disk
     */
    public void save() {
        Path configDir = FabricLoader.getInstance().getConfigDir();
        File configFile = configDir.resolve(CONFIG_FILENAME).toFile();
        
        try {
            if (!configFile.exists()) {
                configFile.getParentFile().mkdirs();
                configFile.createNewFile();
            }
            
            try (FileWriter writer = new FileWriter(configFile)) {
                GSON.toJson(this, writer);
                LOGGER.info("Saved debugger configuration to {}", configFile);
            }
        } catch (IOException e) {
            LOGGER.error("Failed to save configuration", e);
        }
    }
    
    /**
     * Gets the WebSocket endpoint path
     * 
     * @return The endpoint path without leading slash
     */
    public String getPath() {
        return path;
    }
    
    /**
     * Sets the WebSocket endpoint path
     * 
     * @param path The endpoint path without leading slash
     */
    public void setPath(String path) {
        this.path = path;
        save();
    }
    
    /**
     * Gets the port for the WebSocket server
     * 
     * @return The port number
     */
    public int getPort() {
        return port;
    }
    
    /**
     * Sets the port for the WebSocket server
     * 
     * @param port The port number
     */
    public void setPort(int port) {
        this.port = port;
        save();
    }
} 