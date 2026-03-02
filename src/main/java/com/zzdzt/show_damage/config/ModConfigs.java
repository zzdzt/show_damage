package com.zzdzt.show_damage.config;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;

@Config(name = "show_damage")
public class ModConfigs implements ConfigData {

    public boolean isEnabled = true;
    
    @ConfigEntry.Gui.Tooltip(count = 2) 
    public float smallDamageThreshold = 5.0f;
    
    @ConfigEntry.Gui.Tooltip(count = 2) 
    public float mediumDamageThreshold = 12.0f;

    public int colorSmall = 0xFFFFFF;
    public int colorMedium = 0xFFFF55;
    public int colorLarge = 0xd50000;

    @ConfigEntry.BoundedDiscrete(min = 5, max = 100) 
    public int scaleSmall = 10;
    
    @ConfigEntry.BoundedDiscrete(min = 5, max = 100) 
    public int scaleMedium = 15;
    
    @ConfigEntry.BoundedDiscrete(min = 5, max = 100) 
    public int scaleLarge = 25;

    @ConfigEntry.Category("physics")
    @ConfigEntry.Gui.TransitiveObject
    public PhysicsConfig physics = new PhysicsConfig();

    @ConfigEntry.Category("performance")
    @ConfigEntry.Gui.TransitiveObject
    public PerformanceConfig performance = new PerformanceConfig();

    @ConfigEntry.Category("rendering")
    @ConfigEntry.Gui.TransitiveObject
    public RenderingConfig rendering = new RenderingConfig();

    // ========== 嵌套配置类 ==========
    
    public static class PhysicsConfig {
        @ConfigEntry.BoundedDiscrete(min = 0, max = 100) 
        public int gravityValue = 15;
        
        @ConfigEntry.BoundedDiscrete(min = 0, max = 500) 
        public int initialUpwardVelocity = 150;
        
        @ConfigEntry.BoundedDiscrete(min = 0, max = 600) 
        public int horizontalSpread = 150;
        
        public boolean enableDamageMerge = false;
        
        @ConfigEntry.BoundedDiscrete(min = 100, max = 5000) 
        public int mergeTimeoutMs = 1000;

        @ConfigEntry.Gui.Tooltip(count = 2)
        @ConfigEntry.BoundedDiscrete(min = 0, max = 300) 
        public int mergeModeHeightOffset = 110; 
        
        @ConfigEntry.Gui.Tooltip(count = 2)
        @ConfigEntry.BoundedDiscrete(min = 0, max = 300) 
        public int independentModeHeightOffset = 80; 

        // 转换方法保留
        public float getGravity() { return gravityValue / 1000.0f; }
        public float getInitialUpwardVelocity() { return initialUpwardVelocity / 1000.0f; }
        public float getHorizontalSpreadFactor() { return horizontalSpread / 1000.0f; }
        public boolean isMergeEnabled() { return enableDamageMerge; }
        public long getMergeTimeout() { return mergeTimeoutMs; }
        public double getMergeHeightMultiplier() { return mergeModeHeightOffset / 100.0; }
        public double getIndependentHeightMultiplier() { return independentModeHeightOffset / 100.0; }

    }

    public static class PerformanceConfig {
        @ConfigEntry.BoundedDiscrete(min = 10, max = 300) 
        public int lifetimeTicks = 30;
        
        @ConfigEntry.BoundedDiscrete(min = 50, max = 95) 
        public int fadeStartPercent = 80;
        
        @ConfigEntry.Gui.Tooltip(count = 2)
        @ConfigEntry.BoundedDiscrete(min = 4, max = 128) 
        public int maxDisplayDistance = 32;

        public int getLifetime() { return lifetimeTicks; }
        public float getFadeStartRatio() { return fadeStartPercent / 100.0f; }
        public double getMaxDisplayDistanceSqr() {
            return (double) maxDisplayDistance * maxDisplayDistance;
        }
    }

    public static class RenderingConfig {
        @ConfigEntry.BoundedDiscrete(min = 0, max = 100) 
        public int textAlpha = 100;
        
        @ConfigEntry.BoundedDiscrete(min = 0, max = 100) 
        public int shadowAlpha = 60;
        
        public boolean enableShadow = true;
        
        @ConfigEntry.BoundedDiscrete(min = -50, max = 50) 
        public int shadowOffsetX = 6;
        
        @ConfigEntry.BoundedDiscrete(min = -50, max = 50) 
        public int shadowOffsetY = 6;
        
        public int shadowColor = 0x000000;
        
        public boolean useCustomFont = true;
        public String forcedFontNamespace = "minecraft";
        public String forcedFontPath = "default";

        public float getTextAlpha() { return textAlpha / 100.0f; }
        public float getShadowAlpha() { return shadowAlpha / 100.0f; }
        public boolean isShadowEnabled() { return enableShadow; }
        public float getShadowOffsetX() { return shadowOffsetX / 10.0f; }
        public float getShadowOffsetY() { return shadowOffsetY / 10.0f; }
        
        public int getShadowColorWithAlpha(float alpha) {
            int alphaInt = (int)(alpha * 255.0f) << 24;
            return alphaInt | (shadowColor & 0x00FFFFFF);
        }
    }

    // ========== 静态方法 ==========

    public static void register() { 
        AutoConfig.register(ModConfigs.class, GsonConfigSerializer::new); 
    }
    
    public static ModConfigs get() { 
        return AutoConfig.getConfigHolder(ModConfigs.class).getConfig(); 
    }
    
    public static void save() { 
        AutoConfig.getConfigHolder(ModConfigs.class).save(); 
    }

    public float getActualScaleSmall() { return scaleSmall / 10.0f; }
    public float getActualScaleMedium() { return scaleMedium / 10.0f; }
    public float getActualScaleLarge() { return scaleLarge / 10.0f; }
}