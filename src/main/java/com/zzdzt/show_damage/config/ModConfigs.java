package com.zzdzt.show_damage.config;

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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@Config(name = "show_damage")
public class ModConfigs implements ConfigData {

    public boolean isEnabled = true;
    @ConfigEntry.Gui.Tooltip(count = 2) public float smallDamageThreshold = 5.0f;
    @ConfigEntry.Gui.Tooltip(count = 2) public float mediumDamageThreshold = 12.0f;

    public int colorSmall = 0xFFFFFF;
    public int colorMedium = 0xFFFF55;
    public int colorLarge = 0xd50000;

    @ConfigEntry.BoundedDiscrete(min = 5, max = 100) public int scaleSmall = 10;
    @ConfigEntry.BoundedDiscrete(min = 5, max = 100) public int scaleMedium = 15;
    @ConfigEntry.BoundedDiscrete(min = 5, max = 100) public int scaleLarge = 25;

    @ConfigEntry.Category("physics")
    @ConfigEntry.Gui.TransitiveObject
    public PhysicsConfig physics = new PhysicsConfig();

    public static class PhysicsConfig {
        @ConfigEntry.BoundedDiscrete(min = 0, max = 100) public int gravityValue = 15;
        @ConfigEntry.BoundedDiscrete(min = 0, max = 500) public int initialUpwardVelocity = 200;
        @ConfigEntry.BoundedDiscrete(min = 0, max = 600) public int horizontalSpread = 150;
        @ConfigEntry.BoundedDiscrete(min = 10, max = 300) public int lifetimeTicks = 30;
        @ConfigEntry.BoundedDiscrete(min = 50, max = 95) public int fadeStartPercent = 80;
        
        public boolean enableDamageMerge = true;
        @ConfigEntry.BoundedDiscrete(min = 100, max = 5000) public int mergeTimeoutMs = 1000;

        @ConfigEntry.Gui.Tooltip(count = 2)
        @ConfigEntry.BoundedDiscrete(min = 4, max = 128) 
        public int maxDisplayDistance = 32;

        public float getGravity() { return gravityValue / 1000.0f; }
        public float getInitialUpwardVelocity() { return initialUpwardVelocity / 1000.0f; }
        public float getHorizontalSpreadFactor() { return horizontalSpread / 1000.0f; }
        public int getLifetime() { return lifetimeTicks; }
        public float getFadeStartRatio() { return fadeStartPercent / 100.0f; }
        public boolean isMergeEnabled() { return enableDamageMerge; }
        public long getMergeTimeout() { return mergeTimeoutMs; }

        public double getMaxDisplayDistanceSqr() {
            return (double) maxDisplayDistance * maxDisplayDistance;
        }
    }

    public static void register() { AutoConfig.register(ModConfigs.class, GsonConfigSerializer::new); }
    public static ModConfigs get() { return AutoConfig.getConfigHolder(ModConfigs.class).getConfig(); }
    public static void save() { AutoConfig.getConfigHolder(ModConfigs.class).save(); }

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
        ConfigCategory general = builder.getOrCreateCategory(Component.translatable("show_damage.category.general"));


        general.addEntry(entryBuilder.startBooleanToggle(Component.translatable("show_damage.option.enabled"), current.isEnabled)
                .setDefaultValue(defaults.isEnabled)
                .setSaveConsumer(value -> current.isEnabled = value)
                .setTooltip(Component.translatable("show_damage.tooltip.enabled"))
                .build());

        general.addEntry(entryBuilder.startFloatField(Component.translatable("show_damage.option.smallThreshold"), current.smallDamageThreshold)
                .setDefaultValue(defaults.smallDamageThreshold)
                .setSaveConsumer(value -> current.smallDamageThreshold = value)
                .setTooltip(Component.translatable("show_damage.tooltip.smallThreshold"))
                .build());

        general.addEntry(entryBuilder.startFloatField(Component.translatable("show_damage.option.mediumThreshold"), current.mediumDamageThreshold)
                .setDefaultValue(defaults.mediumDamageThreshold)
                .setSaveConsumer(value -> current.mediumDamageThreshold = value)
                .setTooltip(Component.translatable("show_damage.tooltip.mediumThreshold"))
                .build());

 
        general.addEntry(entryBuilder.startColorField(Component.translatable("show_damage.option.colorSmall"), current.colorSmall) 
                .setDefaultValue(defaults.colorSmall)
                .setSaveConsumer(colorInt -> current.colorSmall = colorInt) 
                .setTooltip(Component.translatable("show_damage.tooltip.colorFormat"))
                .build());

        general.addEntry(entryBuilder.startColorField(Component.translatable("show_damage.option.colorMedium"), current.colorMedium)
                .setDefaultValue(defaults.colorMedium)
                .setSaveConsumer(colorInt -> current.colorMedium = colorInt)
                .setTooltip(Component.translatable("show_damage.tooltip.colorFormat"))
                .build());

        general.addEntry(entryBuilder.startColorField(Component.translatable("show_damage.option.colorLarge"), current.colorLarge)
                .setDefaultValue(defaults.colorLarge)
                .setSaveConsumer(colorInt -> current.colorLarge = colorInt)
                .setTooltip(Component.translatable("show_damage.tooltip.colorFormat"))
                .build());

        general.addEntry(entryBuilder.startIntSlider(Component.translatable("show_damage.option.scaleSmall"),current.scaleSmall, 5, 100)
                .setDefaultValue(defaults.scaleSmall)
                .setSaveConsumer(value -> current.scaleSmall = value)
                .setTooltip(Component.translatable("show_damage.tooltip.scaleSmall"))
                .build());

        general.addEntry(entryBuilder.startIntSlider(Component.translatable("show_damage.option.scaleMedium"),current.scaleMedium, 5, 100)
                .setDefaultValue(defaults.scaleMedium)
                .setSaveConsumer(value -> current.scaleMedium = value)
                .setTooltip(Component.translatable("show_damage.tooltip.scaleMedium"))
                .build());

        general.addEntry(entryBuilder.startIntSlider(Component.translatable("show_damage.option.scaleLarge"), current.scaleLarge, 5, 100)
                .setDefaultValue(defaults.scaleLarge)
                .setSaveConsumer(value -> current.scaleLarge = value)
                .setTooltip(Component.translatable("show_damage.tooltip.scaleLarge"))
                .build());

        // --- 物理效果 ---
        ConfigCategory physicsCat = builder.getOrCreateCategory(Component.translatable("show_damage.category.physics"));
        ModConfigs.PhysicsConfig pCurr = current.physics;
        ModConfigs.PhysicsConfig pDef = defaults.physics;

        physicsCat.addEntry(entryBuilder.startBooleanToggle(Component.translatable("show_damage.option.enableMerge"), pCurr.enableDamageMerge)
                .setDefaultValue(pDef.enableDamageMerge)
                .setSaveConsumer(value -> pCurr.enableDamageMerge = value)
                .setTooltip(Component.translatable("show_damage.tooltip.enableMerge"))
                .build());

        physicsCat.addEntry(entryBuilder.startIntSlider(Component.translatable("show_damage.option.mergeTimeout"), pCurr.mergeTimeoutMs, 100, 5000)
                .setDefaultValue(pDef.mergeTimeoutMs)
                .setSaveConsumer(value -> pCurr.mergeTimeoutMs = value)
                .setTooltip(Component.translatable("show_damage.tooltip.mergeTimeout"))
                .build());

        physicsCat.addEntry(entryBuilder.startIntSlider(Component.translatable("show_damage.option.gravity"), pCurr.gravityValue, 0, 100)
                .setDefaultValue(pDef.gravityValue)
                .setSaveConsumer(value -> pCurr.gravityValue = value)
                .setTooltip(Component.translatable("show_damage.tooltip.gravity"))
                .build());

        physicsCat.addEntry(entryBuilder.startIntSlider(Component.translatable("show_damage.option.upwardVelocity"), pCurr.initialUpwardVelocity, 0, 500)
                .setDefaultValue(pDef.initialUpwardVelocity)
                .setSaveConsumer(value -> pCurr.initialUpwardVelocity = value)
                .setTooltip(Component.translatable("show_damage.tooltip.upwardVelocity"))
                .build());
                
        physicsCat.addEntry(entryBuilder.startIntSlider(Component.translatable("show_damage.option.spread"), pCurr.horizontalSpread, 0, 600)
                .setDefaultValue(pDef.horizontalSpread)
                .setSaveConsumer(value -> pCurr.horizontalSpread = value)
                .setTooltip(Component.translatable("show_damage.tooltip.spread"))
                .build());

        physicsCat.addEntry(entryBuilder.startIntSlider(Component.translatable("show_damage.option.lifetime"), pCurr.lifetimeTicks, 20, 300)
                .setDefaultValue(pDef.lifetimeTicks)
                .setSaveConsumer(value -> pCurr.lifetimeTicks = value)
                .setTooltip(Component.translatable("show_damage.tooltip.lifetime"))
                .build());

        physicsCat.addEntry(entryBuilder.startIntSlider(Component.translatable("show_damage.option.fadeStart"), pCurr.fadeStartPercent, 50, 95)
                .setDefaultValue(pDef.fadeStartPercent)
                .setSaveConsumer(value -> pCurr.fadeStartPercent = value)
                .setTooltip(Component.translatable("show_damage.tooltip.fadeStart"))
                .build());
        
        physicsCat.addEntry(entryBuilder.startIntSlider(Component.translatable("show_damage.option.maxDistance"), pCurr.maxDisplayDistance, 4, 128) 
                .setDefaultValue(pDef.maxDisplayDistance)
                .setSaveConsumer(value -> pCurr.maxDisplayDistance = value)
                .setTooltip(Component.translatable("show_damage.tooltip.maxDistance"))
                .build());

        return builder.build();
    }

    public float getActualScaleSmall() { return scaleSmall / 10.0f; }
    public float getActualScaleMedium() { return scaleMedium / 10.0f; }
    public float getActualScaleLarge() { return scaleLarge / 10.0f; }
}