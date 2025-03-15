package net.gunivers.sniffer.config;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * Integration with ModMenu to provide a configuration screen for the Sniffer.
 *
 * @author theogiraudet
 */
@Environment(EnvType.CLIENT)
public class ModMenuIntegration implements ModMenuApi {
    
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return DebuggerClothConfig::createConfigScreen;
    }
} 