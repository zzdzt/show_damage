package com.zzdzt.show_damage.client.style;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.HashMap;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
public class StyleProperties {
    
    private final Map<String, Object> properties = new HashMap<>();
    
    public StyleProperties set(String key, Object value) {
        properties.put(key, value);
        return this;
    }
    
    @SuppressWarnings("unchecked")
    public <T> T get(String key, T defaultValue) {
        return (T) properties.getOrDefault(key, defaultValue);
    }

    @SuppressWarnings("unchecked")
    public <T extends Enum<T>> T getEnum(String key, Class<T> enumClass, T defaultValue) {
        Object value = properties.get(key);
        if (value == null) {
            return defaultValue;
        }
        
        if (enumClass.isInstance(value)) {
            return (T) value;
        }
        
        if (value instanceof String) {
            try {
                return Enum.valueOf(enumClass, (String) value);
            } catch (IllegalArgumentException e) {
                return defaultValue;
            }
        }
        
        if (value instanceof Enum) {
            try {
                return Enum.valueOf(enumClass, ((Enum<?>) value).name());
            } catch (IllegalArgumentException e) {
                return defaultValue;
            }
        }
        
        return defaultValue;
    }
    
    public boolean getBoolean(String key, boolean defaultValue) {
        Object value = properties.get(key);
        return value instanceof Boolean ? (Boolean) value : defaultValue;
    }
    
    public int getInt(String key, int defaultValue) {
        Object value = properties.get(key);
        return value instanceof Number ? ((Number) value).intValue() : defaultValue;
    }
    
    public float getFloat(String key, float defaultValue) {
        Object value = properties.get(key);
        return value instanceof Number ? ((Number) value).floatValue() : defaultValue;
    }
    
    public double getDouble(String key, double defaultValue) {
        Object value = properties.get(key);
        return value instanceof Number ? ((Number) value).doubleValue() : defaultValue;
    }
    
    public String getString(String key, String defaultValue) {
        Object value = properties.get(key);
        return value instanceof String ? (String) value : defaultValue;
    }
    
    public boolean has(String key) {
        return properties.containsKey(key);
    }
    
    public void clear() {
        properties.clear();
    }
    
    public static class Keys {
        public static final String SCALE = "scale";
        public static final String COLOR = "color";
        public static final String ALPHA = "alpha";
        public static final String ROTATION = "rotation";
        public static final String OFFSET_X = "offset_x";
        public static final String OFFSET_Y = "offset_y";
        public static final String OFFSET_Z = "offset_z";
        public static final String VELOCITY_X = "velocity_x";
        public static final String VELOCITY_Y = "velocity_y";
        public static final String VELOCITY_Z = "velocity_z";
        public static final String IS_CRIT = "is_crit";
        public static final String DAMAGE = "damage";
        public static final String MERGE_COUNT = "merge_count";
        public static final String LAST_MERGE_TIME = "last_merge_time";
    }
}