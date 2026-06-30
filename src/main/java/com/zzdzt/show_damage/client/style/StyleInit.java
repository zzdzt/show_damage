// StyleInit.java - 风格初始化
package com.zzdzt.show_damage.client.style;

import com.zzdzt.show_damage.client.style.builtin.independent.*;
import com.zzdzt.show_damage.client.style.builtin.merge.*;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = "show_damage", bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class StyleInit {
    
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            //----------合并模式----------
            StyleRegistry.register(new ClassicMergeStyle());
            StyleRegistry.register(new PopUpMergeStyle());
            StyleRegistry.register(new PulseMergeStyle());
            StyleRegistry.register(new BounceMergeStyle());
            StyleRegistry.register(new TypewriterMergeStyle());
            StyleRegistry.register(new EnergyChargeMergeStyle());
            // ---------独立模式----------
            StyleRegistry.register(new ClassicPopStyle());
            StyleRegistry.register(new DanmakuPopStyle());
            StyleRegistry.register(new SidePopStyle());
            StyleRegistry.register(new RainbowPopStyle());
            StyleRegistry.register(new EndfieldPopStyle());
            StyleRegistry.register(new TypewriterPopStyle());
            
            // 初始化风格管理器
            StyleManager.INSTANCE.init();
        });
    }
}