package com.zzdzt.show_damage.client.style;

import com.zzdzt.show_damage.config.ModConfigs;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;

@OnlyIn(Dist.CLIENT)
public class StyleManager {
    
    public static final StyleManager INSTANCE = new StyleManager();
    
    private String currentMergeStyleId = StyleRegistry.DEFAULT_MERGE_STYLE;
    private String currentIndependentStyleId = StyleRegistry.DEFAULT_INDEPENDENT_STYLE;
    
    private StyleManager() {}
    
    public void init() {
        ModConfigs config = ModConfigs.get();
        if (StyleRegistry.hasStyle(config.mergeStyleId)) {
            this.currentMergeStyleId = config.mergeStyleId;
        }
        if (StyleRegistry.hasStyle(config.independentStyleId)) {
            this.currentIndependentStyleId = config.independentStyleId;
        }
    }
    
    public void setMergeStyle(String styleId) {
        if (StyleRegistry.hasStyle(styleId)) {
            DamageStyle style = StyleRegistry.get(styleId);
            if (style.getType() == DamageStyle.StyleType.MERGE) {
                this.currentMergeStyleId = styleId;
                ModConfigs.get().mergeStyleId = styleId;
                ModConfigs.get().save();
            }
        }
    }
    
    public void setIndependentStyle(String styleId) {
        if (StyleRegistry.hasStyle(styleId)) {
            DamageStyle style = StyleRegistry.get(styleId);
            if (style.getType() == DamageStyle.StyleType.INDEPENDENT) {
                this.currentIndependentStyleId = styleId;
                ModConfigs.get().independentStyleId = styleId;
                ModConfigs.get().save();
            }
        }
    }
    
    @Nullable
    public MergeStyle getCurrentMergeStyle() {
        String styleId = ModConfigs.get().mergeStyleId;
        MergeStyle style = StyleRegistry.getMergeStyle(styleId);
        
        if (style == null) {
            System.out.println("[ShowDamage] 警告：找不到风格 " + styleId + "，使用默认");
            return StyleRegistry.getMergeStyle(StyleRegistry.DEFAULT_MERGE_STYLE);
        }
        
        return style;
    }
    
    @Nullable
    public IndependentStyle getCurrentIndependentStyle() {
        String styleId = ModConfigs.get().independentStyleId;
        IndependentStyle style = StyleRegistry.getIndependentStyle(styleId);
        
        if (style == null) {
            return StyleRegistry.getIndependentStyle(StyleRegistry.DEFAULT_INDEPENDENT_STYLE);
        }
        
        return style;
    }
    
    public DamageStyle getStyleForMode(boolean mergeMode) {
        return mergeMode ? getCurrentMergeStyle() : getCurrentIndependentStyle();
    }
    
    public String getCurrentMergeStyleId() {
        return currentMergeStyleId;
    }
    
    public String getCurrentIndependentStyleId() {
        return currentIndependentStyleId;
    }
}