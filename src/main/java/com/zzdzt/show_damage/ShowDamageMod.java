package com.zzdzt.show_damage;

import com.zzdzt.show_damage.client.FontManager;
import com.zzdzt.show_damage.config.ModConfigScreenFactory;
import com.zzdzt.show_damage.config.ModConfigs;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.loading.FMLEnvironment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod("show_damage")
public class ShowDamageMod {
    public static final String MOD_ID = "show_damage";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public ShowDamageMod() {
        ModConfigs.register();

        if (FMLEnvironment.dist == Dist.CLIENT) {
            var modBus = net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext.get()
                .getModEventBus();
            
            modBus.register(this);
            
            modBus.addListener(this::onClientSetup);
        }

        ModLoadingContext.get().registerExtensionPoint(
            ConfigScreenHandler.ConfigScreenFactory.class,
            () -> new ConfigScreenHandler.ConfigScreenFactory(
                (minecraft, parent) -> ModConfigScreenFactory.create(parent)
            )
        );

        LOGGER.info("Show Damage loaded! Custom font support enabled.");
    }

    private void onClientSetup(FMLClientSetupEvent event) {
    }

    @SubscribeEvent
    public void registerReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(FontManager.INSTANCE);
        LOGGER.info("Font manager registered for resource reload events");
    }
}