package com.zzdzt.show_damage.config;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ModConfigScreenFactory {

    /**
     * 创建配置界面入口
     */
    public static Screen create(Screen parent) {
        ModConfigs defaults = new ModConfigs();
        ModConfigs current = ModConfigs.get();

        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.translatable("show_damage.config.title"))
                .setSavingRunnable(ModConfigs::save);

        ConfigEntryBuilder entryBuilder = builder.entryBuilder();

        // 添加各个分类
        addGeneralCategory(builder, entryBuilder, current, defaults);
        addPhysicsCategory(builder, entryBuilder, current, defaults);
        addPerformanceCategory(builder, entryBuilder, current, defaults);
        addRenderingCategory(builder, entryBuilder, current, defaults);

        return builder.build();
    }

    // ========== 通用设置 ==========

    private static void addGeneralCategory(ConfigBuilder builder, 
                                          ConfigEntryBuilder entryBuilder,
                                          ModConfigs current, 
                                          ModConfigs defaults) {
        ConfigCategory category = builder.getOrCreateCategory(
            Component.translatable("show_damage.category.general")
        );

        category.addEntry(entryBuilder
            .startBooleanToggle(
                Component.translatable("show_damage.option.enabled"), 
                current.isEnabled)
            .setDefaultValue(defaults.isEnabled)
            .setSaveConsumer(value -> current.isEnabled = value)
            .setTooltip(Component.translatable("show_damage.tooltip.enabled"))
            .build());

        category.addEntry(entryBuilder
            .startBooleanToggle(
                Component.translatable("show_damage.option.enableMerge"), 
                current.physics.enableDamageMerge)
            .setDefaultValue(defaults.physics.enableDamageMerge)
            .setSaveConsumer(value -> current.physics.enableDamageMerge = value)
            .setTooltip(Component.translatable("show_damage.tooltip.enableMerge"))
            .build());

        category.addEntry(entryBuilder
            .startFloatField(
                Component.translatable("show_damage.option.smallThreshold"), 
                current.smallDamageThreshold)
            .setDefaultValue(defaults.smallDamageThreshold)
            .setSaveConsumer(value -> current.smallDamageThreshold = value)
            .setTooltip(Component.translatable("show_damage.tooltip.smallThreshold"))
            .build());

        category.addEntry(entryBuilder
            .startFloatField(
                Component.translatable("show_damage.option.mediumThreshold"), 
                current.mediumDamageThreshold)
            .setDefaultValue(defaults.mediumDamageThreshold)
            .setSaveConsumer(value -> current.mediumDamageThreshold = value)
            .setTooltip(Component.translatable("show_damage.tooltip.mediumThreshold"))
            .build());

        // 颜色设置
        addColorEntries(category, entryBuilder, current, defaults);
        
        // 缩放设置
        addScaleEntries(category, entryBuilder, current, defaults);
    }

    private static void addColorEntries(ConfigCategory category,
                                       ConfigEntryBuilder entryBuilder,
                                       ModConfigs current,
                                       ModConfigs defaults) {
        category.addEntry(entryBuilder
            .startColorField(
                Component.translatable("show_damage.option.colorSmall"), 
                current.colorSmall)
            .setDefaultValue(defaults.colorSmall)
            .setSaveConsumer(color -> current.colorSmall = color)
            .setTooltip(Component.translatable("show_damage.tooltip.colorFormat"))
            .build());

        category.addEntry(entryBuilder
            .startColorField(
                Component.translatable("show_damage.option.colorMedium"), 
                current.colorMedium)
            .setDefaultValue(defaults.colorMedium)
            .setSaveConsumer(color -> current.colorMedium = color)
            .setTooltip(Component.translatable("show_damage.tooltip.colorFormat"))
            .build());

        category.addEntry(entryBuilder
            .startColorField(
                Component.translatable("show_damage.option.colorLarge"), 
                current.colorLarge)
            .setDefaultValue(defaults.colorLarge)
            .setSaveConsumer(color -> current.colorLarge = color)
            .setTooltip(Component.translatable("show_damage.tooltip.colorFormat"))
            .build());
    }

    private static void addScaleEntries(ConfigCategory category,
                                       ConfigEntryBuilder entryBuilder,
                                       ModConfigs current,
                                       ModConfigs defaults) {
        category.addEntry(entryBuilder
            .startIntSlider(
                Component.translatable("show_damage.option.scaleSmall"), 
                current.scaleSmall, 5, 100)
            .setDefaultValue(defaults.scaleSmall)
            .setSaveConsumer(value -> current.scaleSmall = value)
            .setTooltip(Component.translatable("show_damage.tooltip.scaleSmall"))
            .build());

        category.addEntry(entryBuilder
            .startIntSlider(
                Component.translatable("show_damage.option.scaleMedium"), 
                current.scaleMedium, 5, 100)
            .setDefaultValue(defaults.scaleMedium)
            .setSaveConsumer(value -> current.scaleMedium = value)
            .setTooltip(Component.translatable("show_damage.tooltip.scaleMedium"))
            .build());

        category.addEntry(entryBuilder
            .startIntSlider(
                Component.translatable("show_damage.option.scaleLarge"), 
                current.scaleLarge, 5, 100)
            .setDefaultValue(defaults.scaleLarge)
            .setSaveConsumer(value -> current.scaleLarge = value)
            .setTooltip(Component.translatable("show_damage.tooltip.scaleLarge"))
            .build());
    }

    // ========== 物理效果 ==========

    private static void addPhysicsCategory(ConfigBuilder builder,
                                          ConfigEntryBuilder entryBuilder,
                                          ModConfigs current,
                                          ModConfigs defaults) {
        ConfigCategory category = builder.getOrCreateCategory(
            Component.translatable("show_damage.category.physics")
        );

        ModConfigs.PhysicsConfig pCurr = current.physics;
        ModConfigs.PhysicsConfig pDef = defaults.physics;

        category.addEntry(entryBuilder
            .startIntSlider(
                Component.translatable("show_damage.option.gravity"), 
                pCurr.gravityValue, 0, 100)
            .setDefaultValue(pDef.gravityValue)
            .setSaveConsumer(value -> pCurr.gravityValue = value)
            .setTooltip(Component.translatable("show_damage.tooltip.gravity"))
            .build());

        category.addEntry(entryBuilder
            .startIntSlider(
                Component.translatable("show_damage.option.upwardVelocity"), 
                pCurr.initialUpwardVelocity, 0, 500)
            .setDefaultValue(pDef.initialUpwardVelocity)
            .setSaveConsumer(value -> pCurr.initialUpwardVelocity = value)
            .setTooltip(Component.translatable("show_damage.tooltip.upwardVelocity"))
            .build());

        category.addEntry(entryBuilder
            .startIntSlider(
                Component.translatable("show_damage.option.spread"), 
                pCurr.horizontalSpread, 0, 600)
            .setDefaultValue(pDef.horizontalSpread)
            .setSaveConsumer(value -> pCurr.horizontalSpread = value)
            .setTooltip(Component.translatable("show_damage.tooltip.spread"))
            .build());

        category.addEntry(entryBuilder
            .startIntSlider(
                Component.translatable("show_damage.option.mergeModeHeight"), 
                pCurr.mergeModeHeightOffset, 0, 300)
            .setDefaultValue(pDef.mergeModeHeightOffset)
            .setSaveConsumer(value -> pCurr.mergeModeHeightOffset = value)
            .setTooltip(
                Component.translatable("show_damage.tooltip.mergeModeHeight1"),
                Component.translatable("show_damage.tooltip.mergeModeHeight2")
            )
            .build());

        category.addEntry(entryBuilder
            .startIntSlider(
                Component.translatable("show_damage.option.independentModeHeight"), 
                pCurr.independentModeHeightOffset, 0, 300)
            .setDefaultValue(pDef.independentModeHeightOffset)
            .setSaveConsumer(value -> pCurr.independentModeHeightOffset = value)
            .setTooltip(
                Component.translatable("show_damage.tooltip.independentModeHeight1"),
                Component.translatable("show_damage.tooltip.independentModeHeight2")
            )
            .build());

        category.addEntry(entryBuilder
            .startIntSlider(
                Component.translatable("show_damage.option.mergeTimeout"), 
                pCurr.mergeTimeoutMs, 100, 5000)
            .setDefaultValue(pDef.mergeTimeoutMs)
            .setSaveConsumer(value -> pCurr.mergeTimeoutMs = value)
            .setTooltip(Component.translatable("show_damage.tooltip.mergeTimeout"))
            .build());
    }

    // ========== 性能设置 ==========

    private static void addPerformanceCategory(ConfigBuilder builder,
                                              ConfigEntryBuilder entryBuilder,
                                              ModConfigs current,
                                              ModConfigs defaults) {
        ConfigCategory category = builder.getOrCreateCategory(
            Component.translatable("show_damage.category.performance")
        );

        ModConfigs.PerformanceConfig perfCurr = current.performance;
        ModConfigs.PerformanceConfig perfDef = defaults.performance;

        category.addEntry(entryBuilder
            .startIntSlider(
                Component.translatable("show_damage.option.lifetime"), 
                perfCurr.lifetimeTicks, 10, 300)
            .setDefaultValue(perfDef.lifetimeTicks)
            .setSaveConsumer(value -> perfCurr.lifetimeTicks = value)
            .setTooltip(Component.translatable("show_damage.tooltip.lifetime"))
            .build());

        category.addEntry(entryBuilder
            .startIntSlider(
                Component.translatable("show_damage.option.fadeStart"), 
                perfCurr.fadeStartPercent, 50, 95)
            .setDefaultValue(perfDef.fadeStartPercent)
            .setSaveConsumer(value -> perfCurr.fadeStartPercent = value)
            .setTooltip(Component.translatable("show_damage.tooltip.fadeStart"))
            .build());

        category.addEntry(entryBuilder
            .startIntSlider(
                Component.translatable("show_damage.option.maxDistance"), 
                perfCurr.maxDisplayDistance, 4, 128)
            .setDefaultValue(perfDef.maxDisplayDistance)
            .setSaveConsumer(value -> perfCurr.maxDisplayDistance = value)
            .setTooltip(Component.translatable("show_damage.tooltip.maxDistance"))
            .build());
    }

    // ========== 渲染设置 ==========

    private static void addRenderingCategory(ConfigBuilder builder,
                                            ConfigEntryBuilder entryBuilder,
                                            ModConfigs current,
                                            ModConfigs defaults) {
        ConfigCategory category = builder.getOrCreateCategory(
            Component.translatable("show_damage.category.rendering")
        );

        ModConfigs.RenderingConfig rCurr = current.rendering;
        ModConfigs.RenderingConfig rDef = defaults.rendering;

        // 透明度
        category.addEntry(entryBuilder
            .startIntSlider(
                Component.translatable("show_damage.option.textAlpha"), 
                rCurr.textAlpha, 0, 100)
            .setDefaultValue(rDef.textAlpha)
            .setSaveConsumer(value -> rCurr.textAlpha = value)
            .setTooltip(Component.translatable("show_damage.tooltip.textAlpha"))
            .build());

        category.addEntry(entryBuilder
            .startIntSlider(
                Component.translatable("show_damage.option.shadowAlpha"), 
                rCurr.shadowAlpha, 0, 100)
            .setDefaultValue(rDef.shadowAlpha)
            .setSaveConsumer(value -> rCurr.shadowAlpha = value)
            .setTooltip(Component.translatable("show_damage.tooltip.shadowAlpha"))
            .build());

        // 阴影开关和偏移
        category.addEntry(entryBuilder
            .startBooleanToggle(
                Component.translatable("show_damage.option.enableShadow"), 
                rCurr.enableShadow)
            .setDefaultValue(rDef.enableShadow)
            .setSaveConsumer(value -> rCurr.enableShadow = value)
            .setTooltip(Component.translatable("show_damage.tooltip.enableShadow"))
            .build());

        category.addEntry(entryBuilder
            .startIntSlider(
                Component.translatable("show_damage.option.shadowOffsetX"), 
                rCurr.shadowOffsetX, -50, 50)
            .setDefaultValue(rDef.shadowOffsetX)
            .setSaveConsumer(value -> rCurr.shadowOffsetX = value)
            .setTooltip(Component.translatable("show_damage.tooltip.shadowOffsetX"))
            .build());

        category.addEntry(entryBuilder
            .startIntSlider(
                Component.translatable("show_damage.option.shadowOffsetY"), 
                rCurr.shadowOffsetY, -50, 50)
            .setDefaultValue(rDef.shadowOffsetY)
            .setSaveConsumer(value -> rCurr.shadowOffsetY = value)
            .setTooltip(Component.translatable("show_damage.tooltip.shadowOffsetY"))
            .build());

        category.addEntry(entryBuilder
            .startColorField(
                Component.translatable("show_damage.option.shadowColor"), 
                rCurr.shadowColor)
            .setDefaultValue(rDef.shadowColor)
            .setSaveConsumer(color -> rCurr.shadowColor = color)
            .setTooltip(Component.translatable("show_damage.tooltip.shadowColor"))
            .build());

        // 字体设置
        addFontEntries(category, entryBuilder, rCurr, rDef);
    }

    private static void addFontEntries(ConfigCategory category,
                                      ConfigEntryBuilder entryBuilder,
                                      ModConfigs.RenderingConfig current,
                                      ModConfigs.RenderingConfig defaults) {
        category.addEntry(entryBuilder
            .startBooleanToggle(
                Component.translatable("show_damage.option.useCustomFont"), 
                current.useCustomFont)
            .setDefaultValue(defaults.useCustomFont)
            .setSaveConsumer(value -> current.useCustomFont = value)
            .setTooltip(Component.translatable("show_damage.tooltip.useCustomFont"))
            .build());

        category.addEntry(entryBuilder
            .startStrField(
                Component.translatable("show_damage.option.forcedFont"), 
                current.forcedFontNamespace + ":" + current.forcedFontPath)
            .setDefaultValue("minecraft:default")
            .setSaveConsumer(value -> {
                String[] parts = value.split(":", 2);
                if (parts.length == 2) {
                    current.forcedFontNamespace = parts[0];
                    current.forcedFontPath = parts[1];
                }
            })
            .setTooltip(
                Component.translatable("show_damage.tooltip.forcedFont1"),
                Component.translatable("show_damage.tooltip.forcedFont2")
            )
            .build());
    }
}