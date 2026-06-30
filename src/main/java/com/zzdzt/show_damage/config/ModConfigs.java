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

    @ConfigEntry.Gui.Tooltip(count = 3)
    public String mergeStyleId = "classic_merge";
    
    @ConfigEntry.Gui.Tooltip(count = 3)
    public String independentStyleId = "classic_pop";

    @ConfigEntry.Category("physics")
    @ConfigEntry.Gui.TransitiveObject
    public PhysicsConfig physics = new PhysicsConfig();

    @ConfigEntry.Category("performance")
    @ConfigEntry.Gui.TransitiveObject
    public PerformanceConfig performance = new PerformanceConfig();

    @ConfigEntry.Category("rendering")
    @ConfigEntry.Gui.TransitiveObject
    public RenderingConfig rendering = new RenderingConfig();

    @ConfigEntry.Category("display")
    public DisplayConfig display = new DisplayConfig();

    @ConfigEntry.Category("animation")
    public AnimationConfig animation = new AnimationConfig();

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

    public String getMergeStyleId() { return mergeStyleId; }
    public String getIndependentStyleId() { return independentStyleId; }

    // ========== 数值显示配置 ==========
    
    public static class DisplayConfig {
        @ConfigEntry.Gui.Tooltip(count = 2)
        public boolean showDecimal = false;
        
        @ConfigEntry.BoundedDiscrete(min = 0, max = 2)
        @ConfigEntry.Gui.Tooltip(count = 2)
        public int decimalPlaces = 1;
        
        @ConfigEntry.Gui.Tooltip
        public boolean useThousandsSeparator = false;
        
        @ConfigEntry.Gui.Tooltip(count = 2)
        public boolean enableAbbreviation = false;
        
        @ConfigEntry.BoundedDiscrete(min = 1000, max = 100000)
        @ConfigEntry.Gui.Tooltip(count = 2)
        public int abbreviationThreshold = 10000;
        
        @ConfigEntry.Gui.Tooltip
        public String customPrefix = "";
        
        @ConfigEntry.Gui.Tooltip
        public String customSuffix = "";
        
        public String formatDamage(float damage) {
            // 量级缩写
            if (enableAbbreviation && damage >= abbreviationThreshold) {
                return formatAbbreviated(damage);
            }
            
            String formatted;
            if (showDecimal) {
                formatted = String.format("%." + decimalPlaces + "f", damage);
            } else {
                formatted = String.format("%.0f", damage);
            }
            if (useThousandsSeparator) {
                formatted = addThousandsSeparator(formatted);
            }
            return formatted;
        }
        
        private String formatAbbreviated(float damage) {
            String suffix;
            double value;
            if (damage >= 1_000_000_000) {
                suffix = "B";
                value = damage / 1_000_000_000.0;
            } else if (damage >= 1_000_000) {
                suffix = "M";
                value = damage / 1_000_000.0;
            } else {
                suffix = "K";
                value = damage / 1_000.0;
            }
            
            String formatted;
            if (showDecimal && decimalPlaces > 0) {
                formatted = String.format("%." + decimalPlaces + "f", value);
            } else {
                // 缩写模式下至少保留1位小数，除非是整数
                if (value == (long) value) {
                    formatted = String.format("%.0f", value);
                } else {
                    formatted = String.format("%.1f", value);
                }
            }
            return formatted + suffix;
        }
        
        private String addThousandsSeparator(String numStr) {
            StringBuilder sb = new StringBuilder();
            boolean hasDecimal = numStr.contains(".");
            String intPart = hasDecimal ? numStr.split("\\.")[0] : numStr;
            String decPart = hasDecimal ? "." + numStr.split("\\.")[1] : "";
            
            int len = intPart.length();
            for (int i = 0; i < len; i++) {
                if (i > 0 && (len - i) % 3 == 0) {
                    sb.append(",");
                }
                sb.append(intPart.charAt(i));
            }
            sb.append(decPart);
            return sb.toString();
        }
    }

    // ========== 动画调整配置 ==========
    
    public static class AnimationConfig {
        @ConfigEntry.BoundedDiscrete(min = 10, max = 90)
        @ConfigEntry.Gui.Tooltip(count = 2)
        public int scaleSmoothness = 40;
        
        @ConfigEntry.BoundedDiscrete(min = 50, max = 99)
        @ConfigEntry.Gui.Tooltip(count = 2)
        public int scaleDamping = 65;
        
        @ConfigEntry.BoundedDiscrete(min = 100, max = 300)
        @ConfigEntry.Gui.Tooltip(count = 2)
        public int maxOvershoot = 150;
        
        @ConfigEntry.BoundedDiscrete(min = 80, max = 99)
        @ConfigEntry.Gui.Tooltip(count = 2)
        public int drag = 92;
        
        @ConfigEntry.BoundedDiscrete(min = 50, max = 200)
        @ConfigEntry.Gui.Tooltip(count = 2)
        public int maxSpeed = 200;
        
        @ConfigEntry.BoundedDiscrete(min = 80, max = 99)
        @ConfigEntry.Gui.Tooltip(count = 2)
        public int rotationDamping = 95;
        
        public float getScaleAcceleration() { return scaleSmoothness / 100.0f; }
        public float getScaleVelocityDecay() { return scaleDamping / 100.0f; }
        public float getMaxOvershootMultiplier() { return maxOvershoot / 100.0f; }
        public float getDrag() { return drag / 100.0f; }
        public double getMaxSpeed() { return maxSpeed / 100.0; }
        public float getRotationDamping() { return rotationDamping / 100.0f; }
    }
}