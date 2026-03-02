package com.zzdzt.show_damage.client;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@OnlyIn(Dist.CLIENT)
public class FontManager implements PreparableReloadListener {
    public static final FontManager INSTANCE = new FontManager();
    
    // 默认字体（Minecraft 默认字体）
    public static final ResourceLocation DEFAULT_FONT = new ResourceLocation("minecraft", "default");
    // 模组备用字体
    public static final ResourceLocation FALLBACK_FONT = new ResourceLocation("minecraft", "uniform");
    
    // 资源包字体路径：assets/show_damage/font/damage_number.json
    public static final ResourceLocation CUSTOM_FONT = new ResourceLocation("show_damage", "damage_number");
    
    private ResourceLocation currentFont = DEFAULT_FONT;
    private boolean usingCustomFont = false;
    
    private FontManager() {}
    
    @Override
    public CompletableFuture<Void> reload(PreparationBarrier preparationBarrier, ResourceManager resourceManager,
            ProfilerFiller preparationsProfiler, ProfilerFiller reloadProfiler, 
            Executor backgroundExecutor, Executor gameExecutor) {
        
        return preparationBarrier.wait(null).thenRunAsync(() -> {
            reloadProfiler.startTick();
            reloadProfiler.push("show_damage_font_reload");
            
            Optional<?> fontResource = resourceManager.getResource(
                new ResourceLocation("show_damage", "font/damage_number.json")
            );
            
            if (fontResource.isPresent()) {
                this.currentFont = CUSTOM_FONT;
                this.usingCustomFont = true;
                System.out.println("[ShowDamage] 已加载自定义字体资源包");
            } else {
                this.currentFont = DEFAULT_FONT;
                this.usingCustomFont = false;
            }
            
            reloadProfiler.pop();
            reloadProfiler.endTick();
        }, gameExecutor);
    }
    
    @Override
    public String getName() {
        return "ShowDamageFontManager";
    }
    
    public ResourceLocation getCurrentFont() {
        return currentFont;
    }
    
    public boolean isUsingCustomFont() {
        return usingCustomFont;
    }
    
    public void resetToDefault() {
        this.currentFont = DEFAULT_FONT;
        this.usingCustomFont = false;
    }
}