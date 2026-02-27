package com.zzdzt.show_damage;

import com.zzdzt.show_damage.config.ModConfigs;

import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.ModLoadingContext; 
import net.minecraftforge.fml.common.Mod;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod("show_damage")
public class ShowDamageMod {
    public static final String MOD_ID = "show_damage";
    private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public ShowDamageMod() {
        ModConfigs.register();

        ModLoadingContext.get().registerExtensionPoint(
            ConfigScreenHandler.ConfigScreenFactory.class,
            () -> new ConfigScreenHandler.ConfigScreenFactory(
                (minecraft, parent) -> ModConfigs.createConfigScreen(parent)
            )
        );


        LOGGER.info("Show Damage loaded! Event listener registered MANUALLY.");
    }
}