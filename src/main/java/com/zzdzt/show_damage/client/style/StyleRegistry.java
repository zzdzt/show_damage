package com.zzdzt.show_damage.client.style;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
public class StyleRegistry {
    
    private static final Map<String, DamageStyle> STYLES = new LinkedHashMap<>();
    private static final Map<String, MergeStyle> MERGE_STYLES = new LinkedHashMap<>();
    private static final Map<String, IndependentStyle> INDEPENDENT_STYLES = new LinkedHashMap<>();
    
    public static final String DEFAULT_MERGE_STYLE = "classic_merge";
    public static final String DEFAULT_INDEPENDENT_STYLE = "classic_pop";
    
    static {
        // 注册默认风格将在 StyleInit 中进行
    }
    
    public static void register(DamageStyle style) {
        STYLES.put(style.getId(), style);
        
        if (style instanceof MergeStyle mergeStyle) {
            MERGE_STYLES.put(style.getId(), mergeStyle);
        } else if (style instanceof IndependentStyle independentStyle) {
            INDEPENDENT_STYLES.put(style.getId(), independentStyle);
        }
    }
    
    @Nullable
    public static DamageStyle get(String id) {
        return STYLES.get(id);
    }
    
    @Nullable
    public static MergeStyle getMergeStyle(String id) {
        return MERGE_STYLES.getOrDefault(id, MERGE_STYLES.get(DEFAULT_MERGE_STYLE));
    }
    
    @Nullable
    public static IndependentStyle getIndependentStyle(String id) {
        return INDEPENDENT_STYLES.getOrDefault(id, INDEPENDENT_STYLES.get(DEFAULT_INDEPENDENT_STYLE));
    }
    
    public static Collection<DamageStyle> getAllStyles() {
        return Collections.unmodifiableCollection(STYLES.values());
    }
    
    public static Collection<MergeStyle> getMergeStyles() {
        return Collections.unmodifiableCollection(MERGE_STYLES.values());
    }
    
    public static Collection<IndependentStyle> getIndependentStyles() {
        return Collections.unmodifiableCollection(INDEPENDENT_STYLES.values());
    }
    
    public static boolean hasStyle(String id) {
        return STYLES.containsKey(id);
    }
    
    public static void clear() {
        STYLES.clear();
        MERGE_STYLES.clear();
        INDEPENDENT_STYLES.clear();
    }
}