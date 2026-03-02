package com.zzdzt.show_damage.config;

import com.zzdzt.show_damage.client.FontManager;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

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

        public float getGravity() { return gravityValue / 1000.0f; }
        public float getInitialUpwardVelocity() { return initialUpwardVelocity / 1000.0f; }
        public float getHorizontalSpreadFactor() { return horizontalSpread / 1000.0f; }
        public boolean isMergeEnabled() { return enableDamageMerge; }
        public long getMergeTimeout() { return mergeTimeoutMs; }
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
        
        public ResourceLocation getFontLocation() {
            if (!useCustomFont) {
                return new ResourceLocation(forcedFontNamespace, forcedFontPath);
            }
            return FontManager.INSTANCE.getCurrentFont();
        }
        
        public boolean isUsingCustomFont() {
            return useCustomFont && FontManager.INSTANCE.isUsingCustomFont();
        }
    }

    public static void register() { 
        AutoConfig.register(ModConfigs.class, GsonConfigSerializer::new); 
    }
    
    public static ModConfigs get() { 
        return AutoConfig.getConfigHolder(ModConfigs.class).getConfig(); 
    }
    
    public static void save() { 
        AutoConfig.getConfigHolder(ModConfigs.class).save(); 
    }

    @OnlyIn(Dist.CLIENT)
    public static Screen createConfigScreen(Screen parent) {
        ModConfigs defaults = new ModConfigs();
        ModConfigs current = get();

        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.translatable("show_damage.config.title"));
        
        builder.setSavingRunnable(() -> save());
        ConfigEntryBuilder entryBuilder = builder.entryBuilder();

        // --- 通用设置 ---
        ConfigCategory general = builder.getOrCreateCategory(
            Component.translatable("show_damage.category.general")
        );

        general.addEntry(entryBuilder
            .startBooleanToggle(Component.translatable("show_damage.option.enabled"), current.isEnabled)
            .setDefaultValue(defaults.isEnabled)
            .setSaveConsumer(value -> current.isEnabled = value)
            .setTooltip(Component.translatable("show_damage.tooltip.enabled"))
            .build());

        general.addEntry(entryBuilder
            .startFloatField(Component.translatable("show_damage.option.smallThreshold"), current.smallDamageThreshold)
            .setDefaultValue(defaults.smallDamageThreshold)
            .setSaveConsumer(value -> current.smallDamageThreshold = value)
            .setTooltip(Component.translatable("show_damage.tooltip.smallThreshold"))
            .build());

        general.addEntry(entryBuilder
            .startFloatField(Component.translatable("show_damage.option.mediumThreshold"), current.mediumDamageThreshold)
            .setDefaultValue(defaults.mediumDamageThreshold)
            .setSaveConsumer(value -> current.mediumDamageThreshold = value)
            .setTooltip(Component.translatable("show_damage.tooltip.mediumThreshold"))
            .build());

        general.addEntry(entryBuilder
            .startColorField(Component.translatable("show_damage.option.colorSmall"), current.colorSmall) 
            .setDefaultValue(defaults.colorSmall)
            .setSaveConsumer(colorInt -> current.colorSmall = colorInt) 
            .setTooltip(Component.translatable("show_damage.tooltip.colorFormat"))
            .build());

        general.addEntry(entryBuilder
            .startColorField(Component.translatable("show_damage.option.colorMedium"), current.colorMedium)
            .setDefaultValue(defaults.colorMedium)
            .setSaveConsumer(colorInt -> current.colorMedium = colorInt)
            .setTooltip(Component.translatable("show_damage.tooltip.colorFormat"))
            .build());

        general.addEntry(entryBuilder
            .startColorField(Component.translatable("show_damage.option.colorLarge"), current.colorLarge)
            .setDefaultValue(defaults.colorLarge)
            .setSaveConsumer(colorInt -> current.colorLarge = colorInt)
            .setTooltip(Component.translatable("show_damage.tooltip.colorFormat"))
            .build());

        general.addEntry(entryBuilder
            .startIntSlider(Component.translatable("show_damage.option.scaleSmall"), current.scaleSmall, 5, 100)
            .setDefaultValue(defaults.scaleSmall)
            .setSaveConsumer(value -> current.scaleSmall = value)
            .setTooltip(Component.translatable("show_damage.tooltip.scaleSmall"))
            .build());

        general.addEntry(entryBuilder
            .startIntSlider(Component.translatable("show_damage.option.scaleMedium"), current.scaleMedium, 5, 100)
            .setDefaultValue(defaults.scaleMedium)
            .setSaveConsumer(value -> current.scaleMedium = value)
            .setTooltip(Component.translatable("show_damage.tooltip.scaleMedium"))
            .build());

        general.addEntry(entryBuilder
            .startIntSlider(Component.translatable("show_damage.option.scaleLarge"), current.scaleLarge, 5, 100)
            .setDefaultValue(defaults.scaleLarge)
            .setSaveConsumer(value -> current.scaleLarge = value)
            .setTooltip(Component.translatable("show_damage.tooltip.scaleLarge"))
            .build());

        // --- 物理效果 ---
        ConfigCategory physicsCat = builder.getOrCreateCategory(
            Component.translatable("show_damage.category.physics")
        );
        PhysicsConfig pCurr = current.physics;
        PhysicsConfig pDef = defaults.physics;

        physicsCat.addEntry(entryBuilder
            .startIntSlider(Component.translatable("show_damage.option.gravity"), pCurr.gravityValue, 0, 100)
            .setDefaultValue(pDef.gravityValue)
            .setSaveConsumer(value -> pCurr.gravityValue = value)
            .setTooltip(Component.translatable("show_damage.tooltip.gravity"))
            .build());

        physicsCat.addEntry(entryBuilder
            .startIntSlider(Component.translatable("show_damage.option.upwardVelocity"), pCurr.initialUpwardVelocity, 0, 500)
            .setDefaultValue(pDef.initialUpwardVelocity)
            .setSaveConsumer(value -> pCurr.initialUpwardVelocity = value)
            .setTooltip(Component.translatable("show_damage.tooltip.upwardVelocity"))
            .build());
                
        physicsCat.addEntry(entryBuilder
            .startIntSlider(Component.translatable("show_damage.option.spread"), pCurr.horizontalSpread, 0, 600)
            .setDefaultValue(pDef.horizontalSpread)
            .setSaveConsumer(value -> pCurr.horizontalSpread = value)
            .setTooltip(Component.translatable("show_damage.tooltip.spread"))
            .build());

        physicsCat.addEntry(entryBuilder
            .startBooleanToggle(Component.translatable("show_damage.option.enableMerge"), pCurr.enableDamageMerge)
            .setDefaultValue(pDef.enableDamageMerge)
            .setSaveConsumer(value -> pCurr.enableDamageMerge = value)
            .setTooltip(Component.translatable("show_damage.tooltip.enableMerge"))
            .build());

        physicsCat.addEntry(entryBuilder
            .startIntSlider(Component.translatable("show_damage.option.mergeTimeout"), pCurr.mergeTimeoutMs, 100, 5000)
            .setDefaultValue(pDef.mergeTimeoutMs)
            .setSaveConsumer(value -> pCurr.mergeTimeoutMs = value)
            .setTooltip(Component.translatable("show_damage.tooltip.mergeTimeout"))
            .build());

        // --- 性能设置 ---
        ConfigCategory performanceCat = builder.getOrCreateCategory(
            Component.translatable("show_damage.category.performance")
        );
        PerformanceConfig perfCurr = current.performance;
        PerformanceConfig perfDef = defaults.performance;

        performanceCat.addEntry(entryBuilder
            .startIntSlider(Component.translatable("show_damage.option.lifetime"), perfCurr.lifetimeTicks, 10, 300)
            .setDefaultValue(perfDef.lifetimeTicks)
            .setSaveConsumer(value -> perfCurr.lifetimeTicks = value)
            .setTooltip(Component.translatable("show_damage.tooltip.lifetime"))
            .build());

        performanceCat.addEntry(entryBuilder
            .startIntSlider(Component.translatable("show_damage.option.fadeStart"), perfCurr.fadeStartPercent, 50, 95)
            .setDefaultValue(perfDef.fadeStartPercent)
            .setSaveConsumer(value -> perfCurr.fadeStartPercent = value)
            .setTooltip(Component.translatable("show_damage.tooltip.fadeStart"))
            .build());
        
        performanceCat.addEntry(entryBuilder
            .startIntSlider(Component.translatable("show_damage.option.maxDistance"), perfCurr.maxDisplayDistance, 4, 128) 
            .setDefaultValue(perfDef.maxDisplayDistance)
            .setSaveConsumer(value -> perfCurr.maxDisplayDistance = value)
            .setTooltip(Component.translatable("show_damage.tooltip.maxDistance"))
            .build());

        // --- 渲染设置 ---
        ConfigCategory renderingCat = builder.getOrCreateCategory(
            Component.translatable("show_damage.category.rendering")
        );
        RenderingConfig rCurr = current.rendering;
        RenderingConfig rDef = defaults.rendering;

        renderingCat.addEntry(entryBuilder
            .startIntSlider(Component.translatable("show_damage.option.textAlpha"), rCurr.textAlpha, 0, 100)
            .setDefaultValue(rDef.textAlpha)
            .setSaveConsumer(value -> rCurr.textAlpha = value)
            .setTooltip(Component.translatable("show_damage.tooltip.textAlpha"))
            .build());

        renderingCat.addEntry(entryBuilder
            .startIntSlider(Component.translatable("show_damage.option.shadowAlpha"), rCurr.shadowAlpha, 0, 100)
            .setDefaultValue(rDef.shadowAlpha)
            .setSaveConsumer(value -> rCurr.shadowAlpha = value)
            .setTooltip(Component.translatable("show_damage.tooltip.shadowAlpha"))
            .build());

        renderingCat.addEntry(entryBuilder
            .startBooleanToggle(Component.translatable("show_damage.option.enableShadow"), rCurr.enableShadow)
            .setDefaultValue(rDef.enableShadow)
            .setSaveConsumer(value -> rCurr.enableShadow = value)
            .setTooltip(Component.translatable("show_damage.tooltip.enableShadow"))
            .build());

        renderingCat.addEntry(entryBuilder
            .startIntSlider(Component.translatable("show_damage.option.shadowOffsetX"), rCurr.shadowOffsetX, -50, 50)
            .setDefaultValue(rDef.shadowOffsetX)
            .setSaveConsumer(value -> rCurr.shadowOffsetX = value)
            .setTooltip(Component.translatable("show_damage.tooltip.shadowOffsetX"))
            .build());

        renderingCat.addEntry(entryBuilder
            .startIntSlider(Component.translatable("show_damage.option.shadowOffsetY"), rCurr.shadowOffsetY, -50, 50)
            .setDefaultValue(rDef.shadowOffsetY)
            .setSaveConsumer(value -> rCurr.shadowOffsetY = value)
            .setTooltip(Component.translatable("show_damage.tooltip.shadowOffsetY"))
            .build());

        renderingCat.addEntry(entryBuilder
            .startColorField(Component.translatable("show_damage.option.shadowColor"), rCurr.shadowColor)
            .setDefaultValue(rDef.shadowColor)
            .setSaveConsumer(colorInt -> rCurr.shadowColor = colorInt)
            .setTooltip(Component.translatable("show_damage.tooltip.shadowColor"))
            .build());

        renderingCat.addEntry(entryBuilder
            .startBooleanToggle(
                Component.translatable("show_damage.option.useCustomFont"), 
                rCurr.useCustomFont)
            .setDefaultValue(rDef.useCustomFont)
            .setSaveConsumer(value -> rCurr.useCustomFont = value)
            .setTooltip(Component.translatable("show_damage.tooltip.useCustomFont"))
            .build());

        renderingCat.addEntry(entryBuilder
            .startStrField(
                Component.translatable("show_damage.option.forcedFont"), 
                rCurr.forcedFontNamespace + ":" + rCurr.forcedFontPath)
            .setDefaultValue("minecraft:default")
            .setSaveConsumer(value -> {
                String[] parts = value.split(":", 2);
                if (parts.length == 2) {
                    rCurr.forcedFontNamespace = parts[0];
                    rCurr.forcedFontPath = parts[1];
                }
            })
            .setTooltip(
                Component.translatable("show_damage.tooltip.forcedFont1"),
                Component.translatable("show_damage.tooltip.forcedFont2")
            )
            .build());

        return builder.build();
    }

    public float getActualScaleSmall() { return scaleSmall / 10.0f; }
    public float getActualScaleMedium() { return scaleMedium / 10.0f; }
    public float getActualScaleLarge() { return scaleLarge / 10.0f; }
}