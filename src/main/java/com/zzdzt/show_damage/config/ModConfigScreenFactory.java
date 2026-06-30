package com.zzdzt.show_damage.config;

import com.zzdzt.show_damage.client.style.IndependentStyle;
import com.zzdzt.show_damage.client.style.MergeStyle;
import com.zzdzt.show_damage.client.style.StyleRegistry;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@OnlyIn(Dist.CLIENT)
public class ModConfigScreenFactory {

    public static Screen create(Screen parent) {
        ModConfigs defaults = new ModConfigs();
        ModConfigs current = ModConfigs.get();

        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.translatable("show_damage.config.title"))
                .setSavingRunnable(ModConfigs::save);

        ConfigEntryBuilder eb = builder.entryBuilder();

        addGeneral(builder, eb, current, defaults);
        addPhysics(builder, eb, current, defaults);
        addPerformance(builder, eb, current, defaults);
        addRendering(builder, eb, current, defaults);
        addDisplay(builder, eb, current, defaults);
        addAnimation(builder, eb, current, defaults);
        addStyle(builder, eb, current, defaults);

        return builder.build();
    }

    // ========== 通用设置 ==========

    private static void addGeneral(ConfigBuilder builder, ConfigEntryBuilder eb,
                                   ModConfigs c, ModConfigs d) {
        ConfigCategory cat = builder.getOrCreateCategory(t("show_damage.category.general"));

        addBool(cat, eb, "show_damage.option.enabled",
                c.isEnabled, d.isEnabled, v -> c.isEnabled = v,
                "show_damage.tooltip.enabled");

        addBool(cat, eb, "show_damage.option.enableMerge",
                c.physics.enableDamageMerge, d.physics.enableDamageMerge, v -> c.physics.enableDamageMerge = v,
                "show_damage.tooltip.enableMerge");

        addFloat(cat, eb, "show_damage.option.smallThreshold",
                c.smallDamageThreshold, d.smallDamageThreshold, v -> c.smallDamageThreshold = v,
                "show_damage.tooltip.smallThreshold");

        addFloat(cat, eb, "show_damage.option.mediumThreshold",
                c.mediumDamageThreshold, d.mediumDamageThreshold, v -> c.mediumDamageThreshold = v,
                "show_damage.tooltip.mediumThreshold");

        addColor(cat, eb, "show_damage.option.colorSmall",
                c.colorSmall, d.colorSmall, v -> c.colorSmall = v,
                "show_damage.tooltip.colorFormat");

        addColor(cat, eb, "show_damage.option.colorMedium",
                c.colorMedium, d.colorMedium, v -> c.colorMedium = v,
                "show_damage.tooltip.colorFormat");

        addColor(cat, eb, "show_damage.option.colorLarge",
                c.colorLarge, d.colorLarge, v -> c.colorLarge = v,
                "show_damage.tooltip.colorFormat");

        addIntSlider(cat, eb, "show_damage.option.scaleSmall",
                c.scaleSmall, d.scaleSmall, 5, 100, v -> c.scaleSmall = v,
                "show_damage.tooltip.scaleSmall");

        addIntSlider(cat, eb, "show_damage.option.scaleMedium",
                c.scaleMedium, d.scaleMedium, 5, 100, v -> c.scaleMedium = v,
                "show_damage.tooltip.scaleMedium");

        addIntSlider(cat, eb, "show_damage.option.scaleLarge",
                c.scaleLarge, d.scaleLarge, 5, 100, v -> c.scaleLarge = v,
                "show_damage.tooltip.scaleLarge");
    }

    // ========== 物理效果 ==========

    private static void addPhysics(ConfigBuilder builder, ConfigEntryBuilder eb,
                                   ModConfigs c, ModConfigs d) {
        ConfigCategory cat = builder.getOrCreateCategory(t("show_damage.category.physics"));
        ModConfigs.PhysicsConfig p = c.physics, dp = d.physics;

        addIntSlider(cat, eb, "show_damage.option.gravity",
                p.gravityValue, dp.gravityValue, 0, 100, v -> p.gravityValue = v,
                "show_damage.tooltip.gravity");

        addIntSlider(cat, eb, "show_damage.option.upwardVelocity",
                p.initialUpwardVelocity, dp.initialUpwardVelocity, 0, 500, v -> p.initialUpwardVelocity = v,
                "show_damage.tooltip.upwardVelocity");

        addIntSlider(cat, eb, "show_damage.option.spread",
                p.horizontalSpread, dp.horizontalSpread, 0, 600, v -> p.horizontalSpread = v,
                "show_damage.tooltip.spread");

        addIntSlider(cat, eb, "show_damage.option.mergeModeHeight",
                p.mergeModeHeightOffset, dp.mergeModeHeightOffset, 0, 300, v -> p.mergeModeHeightOffset = v,
                "show_damage.tooltip.mergeModeHeight");

        addIntSlider(cat, eb, "show_damage.option.independentModeHeight",
                p.independentModeHeightOffset, dp.independentModeHeightOffset, 0, 300, v -> p.independentModeHeightOffset = v,
                "show_damage.tooltip.independentModeHeight");

        addIntSlider(cat, eb, "show_damage.option.mergeTimeout",
                p.mergeTimeoutMs, dp.mergeTimeoutMs, 100, 5000, v -> p.mergeTimeoutMs = v,
                "show_damage.tooltip.mergeTimeout");
    }

    // ========== 性能设置 ==========

    private static void addPerformance(ConfigBuilder builder, ConfigEntryBuilder eb,
                                       ModConfigs c, ModConfigs d) {
        ConfigCategory cat = builder.getOrCreateCategory(t("show_damage.category.performance"));
        ModConfigs.PerformanceConfig pf = c.performance, dpf = d.performance;

        addIntSlider(cat, eb, "show_damage.option.lifetime",
                pf.lifetimeTicks, dpf.lifetimeTicks, 10, 300, v -> pf.lifetimeTicks = v,
                "show_damage.tooltip.lifetime");

        addIntSlider(cat, eb, "show_damage.option.fadeStart",
                pf.fadeStartPercent, dpf.fadeStartPercent, 50, 95, v -> pf.fadeStartPercent = v,
                "show_damage.tooltip.fadeStart");

        addIntSlider(cat, eb, "show_damage.option.maxDistance",
                pf.maxDisplayDistance, dpf.maxDisplayDistance, 4, 128, v -> pf.maxDisplayDistance = v,
                "show_damage.tooltip.maxDistance");
    }

    // ========== 渲染设置 ==========

    private static void addRendering(ConfigBuilder builder, ConfigEntryBuilder eb,
                                      ModConfigs c, ModConfigs d) {
        ConfigCategory cat = builder.getOrCreateCategory(t("show_damage.category.rendering"));
        ModConfigs.RenderingConfig r = c.rendering, dr = d.rendering;

        addIntSlider(cat, eb, "show_damage.option.textAlpha",
                r.textAlpha, dr.textAlpha, 0, 100, v -> r.textAlpha = v,
                "show_damage.tooltip.textAlpha");

        addIntSlider(cat, eb, "show_damage.option.shadowAlpha",
                r.shadowAlpha, dr.shadowAlpha, 0, 100, v -> r.shadowAlpha = v,
                "show_damage.tooltip.shadowAlpha");

        addBool(cat, eb, "show_damage.option.enableShadow",
                r.enableShadow, dr.enableShadow, v -> r.enableShadow = v,
                "show_damage.tooltip.enableShadow");

        addIntSlider(cat, eb, "show_damage.option.shadowOffsetX",
                r.shadowOffsetX, dr.shadowOffsetX, -50, 50, v -> r.shadowOffsetX = v,
                "show_damage.tooltip.shadowOffsetX");

        addIntSlider(cat, eb, "show_damage.option.shadowOffsetY",
                r.shadowOffsetY, dr.shadowOffsetY, -50, 50, v -> r.shadowOffsetY = v,
                "show_damage.tooltip.shadowOffsetY");

        addColor(cat, eb, "show_damage.option.shadowColor",
                r.shadowColor, dr.shadowColor, v -> r.shadowColor = v,
                "show_damage.tooltip.shadowColor");

        addBool(cat, eb, "show_damage.option.useCustomFont",
                r.useCustomFont, dr.useCustomFont, v -> r.useCustomFont = v,
                "show_damage.tooltip.useCustomFont");

        // 字体路径解析需要特殊处理
        cat.addEntry(eb
                .startStrField(t("show_damage.option.forcedFont"),
                        r.forcedFontNamespace + ":" + r.forcedFontPath)
                .setDefaultValue("minecraft:default")
                .setSaveConsumer(value -> {
                    String[] parts = value.split(":", 2);
                    if (parts.length == 2) {
                        r.forcedFontNamespace = parts[0];
                        r.forcedFontPath = parts[1];
                    }
                })
                .setTooltip(
                        t("show_damage.tooltip.forcedFont1"),
                        t("show_damage.tooltip.forcedFont2"))
                .build());
    }

    // ========== 数值显示 ==========

    private static void addDisplay(ConfigBuilder builder, ConfigEntryBuilder eb,
                                   ModConfigs c, ModConfigs d) {
        ConfigCategory cat = builder.getOrCreateCategory(t("show_damage.category.display"));
        ModConfigs.DisplayConfig dc = c.display, dd = d.display;

        addBool(cat, eb, "show_damage.option.showDecimal",
                dc.showDecimal, dd.showDecimal, v -> dc.showDecimal = v,
                "show_damage.tooltip.showDecimal.1", "show_damage.tooltip.showDecimal.2");

        addIntSlider(cat, eb, "show_damage.option.decimalPlaces",
                dc.decimalPlaces, dd.decimalPlaces, 0, 2, v -> dc.decimalPlaces = v,
                "show_damage.tooltip.decimalPlaces.1", "show_damage.tooltip.decimalPlaces.2");

        addBool(cat, eb, "show_damage.option.useThousandsSeparator",
                dc.useThousandsSeparator, dd.useThousandsSeparator, v -> dc.useThousandsSeparator = v,
                "show_damage.tooltip.useThousandsSeparator");

        addBool(cat, eb, "show_damage.option.enableAbbreviation",
                dc.enableAbbreviation, dd.enableAbbreviation, v -> dc.enableAbbreviation = v,
                "show_damage.tooltip.enableAbbreviation.1", "show_damage.tooltip.enableAbbreviation.2");

        addIntSlider(cat, eb, "show_damage.option.abbreviationThreshold",
                dc.abbreviationThreshold, dd.abbreviationThreshold, 1000, 100000, v -> dc.abbreviationThreshold = v,
                "show_damage.tooltip.abbreviationThreshold.1", "show_damage.tooltip.abbreviationThreshold.2");

        addStr(cat, eb, "show_damage.option.customPrefix",
                dc.customPrefix, dd.customPrefix, v -> dc.customPrefix = v,
                "show_damage.tooltip.customPrefix");

        addStr(cat, eb, "show_damage.option.customSuffix",
                dc.customSuffix, dd.customSuffix, v -> dc.customSuffix = v,
                "show_damage.tooltip.customSuffix");
    }

    // ========== 动画调整 ==========

    private static void addAnimation(ConfigBuilder builder, ConfigEntryBuilder eb,
                                     ModConfigs c, ModConfigs d) {
        ConfigCategory cat = builder.getOrCreateCategory(t("show_damage.category.animation"));
        ModConfigs.AnimationConfig a = c.animation, da = d.animation;

        addIntSlider(cat, eb, "show_damage.option.scaleSmoothness",
                a.scaleSmoothness, da.scaleSmoothness, 10, 90, v -> a.scaleSmoothness = v,
                "show_damage.tooltip.scaleSmoothness.1", "show_damage.tooltip.scaleSmoothness.2");

        addIntSlider(cat, eb, "show_damage.option.scaleDamping",
                a.scaleDamping, da.scaleDamping, 50, 99, v -> a.scaleDamping = v,
                "show_damage.tooltip.scaleDamping.1", "show_damage.tooltip.scaleDamping.2");

        addIntSlider(cat, eb, "show_damage.option.maxOvershoot",
                a.maxOvershoot, da.maxOvershoot, 100, 300, v -> a.maxOvershoot = v,
                "show_damage.tooltip.maxOvershoot");

        addIntSlider(cat, eb, "show_damage.option.drag",
                a.drag, da.drag, 80, 99, v -> a.drag = v,
                "show_damage.tooltip.drag.1", "show_damage.tooltip.drag.2");

        addIntSlider(cat, eb, "show_damage.option.maxSpeed",
                a.maxSpeed, da.maxSpeed, 50, 200, v -> a.maxSpeed = v,
                "show_damage.tooltip.maxSpeed.1", "show_damage.tooltip.maxSpeed.2");

        addIntSlider(cat, eb, "show_damage.option.rotationDamping",
                a.rotationDamping, da.rotationDamping, 80, 99, v -> a.rotationDamping = v,
                "show_damage.tooltip.rotationDamping.1", "show_damage.tooltip.rotationDamping.2");
    }

    // ========== 风格设置 ==========

    private static void addStyle(ConfigBuilder builder, ConfigEntryBuilder eb,
                                 ModConfigs c, ModConfigs d) {
        ConfigCategory cat = builder.getOrCreateCategory(t("show_damage.category.style"));

        // 合并风格下拉框
        List<String> mergeIds = new ArrayList<>();
        List<String> mergeNames = new ArrayList<>();
        for (MergeStyle style : StyleRegistry.getMergeStyles()) {
            mergeIds.add(style.getId());
            mergeNames.add(style.getDisplayName());
        }
        cat.addEntry(eb.startSelector(
                        t("show_damage.option.mergeStyle"),
                        mergeNames.toArray(new String[0]),
                        styleName(c.mergeStyleId, mergeIds, mergeNames, d.mergeStyleId))
                .setDefaultValue(styleName(d.mergeStyleId, mergeIds, mergeNames, d.mergeStyleId))
                .setSaveConsumer(name -> {
                    int i = mergeNames.indexOf(name);
                    if (i >= 0) c.mergeStyleId = mergeIds.get(i);
                })
                .setTooltip(t("show_damage.tooltip.mergeStyle"))
                .build());

        // 独立风格下拉框
        List<String> indIds = new ArrayList<>();
        List<String> indNames = new ArrayList<>();
        for (IndependentStyle style : StyleRegistry.getIndependentStyles()) {
            indIds.add(style.getId());
            indNames.add(style.getDisplayName());
        }
        cat.addEntry(eb.startSelector(
                        t("show_damage.option.independentStyle"),
                        indNames.toArray(new String[0]),
                        styleName(c.independentStyleId, indIds, indNames, d.independentStyleId))
                .setDefaultValue(styleName(d.independentStyleId, indIds, indNames, d.independentStyleId))
                .setSaveConsumer(name -> {
                    int i = indNames.indexOf(name);
                    if (i >= 0) c.independentStyleId = indIds.get(i);
                })
                .setTooltip(t("show_damage.tooltip.independentStyle"))
                .build());
    }

    // ========== 通用辅助方法 ==========

    private static Component t(String key) {
        return Component.translatable(key);
    }

    private static Component[] ts(String... keys) {
        Component[] arr = new Component[keys.length];
        for (int i = 0; i < keys.length; i++) arr[i] = t(keys[i]);
        return arr;
    }

    private static void addBool(ConfigCategory cat, ConfigEntryBuilder eb,
                                String key, boolean cur, boolean def,
                                Consumer<Boolean> save, String... tips) {
        cat.addEntry(eb.startBooleanToggle(t(key), cur)
                .setDefaultValue(def)
                .setSaveConsumer(save)
                .setTooltip(ts(tips))
                .build());
    }

    private static void addIntSlider(ConfigCategory cat, ConfigEntryBuilder eb,
                                     String key, int cur, int def, int min, int max,
                                     Consumer<Integer> save, String... tips) {
        cat.addEntry(eb.startIntSlider(t(key), cur, min, max)
                .setDefaultValue(def)
                .setSaveConsumer(save)
                .setTooltip(ts(tips))
                .build());
    }

    private static void addFloat(ConfigCategory cat, ConfigEntryBuilder eb,
                                 String key, float cur, float def,
                                 Consumer<Float> save, String... tips) {
        cat.addEntry(eb.startFloatField(t(key), cur)
                .setDefaultValue(def)
                .setSaveConsumer(save)
                .setTooltip(ts(tips))
                .build());
    }

    private static void addColor(ConfigCategory cat, ConfigEntryBuilder eb,
                                 String key, int cur, int def,
                                 Consumer<Integer> save, String... tips) {
        cat.addEntry(eb.startColorField(t(key), cur)
                .setDefaultValue(def)
                .setSaveConsumer(save)
                .setTooltip(ts(tips))
                .build());
    }

    private static void addStr(ConfigCategory cat, ConfigEntryBuilder eb,
                               String key, String cur, String def,
                               Consumer<String> save, String... tips) {
        cat.addEntry(eb.startStrField(t(key), cur)
                .setDefaultValue(def)
                .setSaveConsumer(save)
                .setTooltip(ts(tips))
                .build());
    }

    private static String styleName(String id, List<String> ids, List<String> names, String defaultId) {
        int i = ids.indexOf(id);
        if (i >= 0) return names.get(i);
        i = ids.indexOf(defaultId);
        return i >= 0 ? names.get(i) : names.get(0);
    }
}
