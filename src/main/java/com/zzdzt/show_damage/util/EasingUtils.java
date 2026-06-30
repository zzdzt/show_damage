package com.zzdzt.show_damage.util;

/**
 * 缓动函数工具类，供所有动画风格复用。
 * 所有函数输入 t ∈ [0, 1]，输出 ∈ [0, 1]。
 */
public class EasingUtils {

    private EasingUtils() {}

    // ========== Quad ==========

    public static float easeInQuad(float t) {
        return t * t;
    }

    public static float easeOutQuad(float t) {
        return t * (2 - t);
    }

    public static float easeInOutQuad(float t) {
        return t < 0.5f ? 2 * t * t : -1 + (4 - 2 * t) * t;
    }

    // ========== Cubic ==========

    public static float easeInCubic(float t) {
        return t * t * t;
    }

    public static float easeOutCubic(float t) {
        float t1 = t - 1;
        return t1 * t1 * t1 + 1;
    }

    public static float easeInOutCubic(float t) {
        return t < 0.5f ? 4 * t * t * t : (t - 1) * (2 * t - 2) * (2 * t - 2) + 1;
    }

    // ========== Sine ==========

    public static float easeInSine(float t) {
        return 1 - (float) Math.cos(t * Math.PI / 2);
    }

    public static float easeOutSine(float t) {
        return (float) Math.sin(t * Math.PI / 2);
    }

    public static float easeInOutSine(float t) {
        return -(float) (Math.cos(Math.PI * t) - 1) / 2;
    }

    // ========== Elastic ==========

    public static float easeOutElastic(float t) {
        if (t == 0 || t == 1) return t;
        return (float) (Math.pow(2, -10 * t) * Math.sin((t - 0.075) * (2 * Math.PI) / 0.3) + 1);
    }

    public static float easeInElastic(float t) {
        if (t == 0 || t == 1) return t;
        return (float) (-Math.pow(2, 10 * (t - 1)) * Math.sin((t - 1.075) * (2 * Math.PI) / 0.3));
    }

    public static float easeInOutElastic(float t) {
        if (t == 0 || t == 1) return t;
        if (t < 0.5f) {
            return (float) (-0.5 * Math.pow(2, 20 * t - 10) * Math.sin((20 * t - 11.125) * (2 * Math.PI) / 4.5));
        }
        return (float) (0.5 * Math.pow(2, -20 * t + 10) * Math.sin((20 * t - 11.125) * (2 * Math.PI) / 4.5) + 1);
    }

    // ========== Bounce ==========

    public static float easeOutBounce(float t) {
        if (t < 1 / 2.75f) {
            return 7.5625f * t * t;
        } else if (t < 2 / 2.75f) {
            float t1 = t - 1.5f / 2.75f;
            return 7.5625f * t1 * t1 + 0.75f;
        } else if (t < 2.5 / 2.75f) {
            float t1 = t - 2.25f / 2.75f;
            return 7.5625f * t1 * t1 + 0.9375f;
        } else {
            float t1 = t - 2.625f / 2.75f;
            return 7.5625f * t1 * t1 + 0.984375f;
        }
    }

    public static float easeInBounce(float t) {
        return 1 - easeOutBounce(1 - t);
    }

    public static float easeInOutBounce(float t) {
        return t < 0.5f
            ? easeInBounce(t * 2) * 0.5f
            : easeOutBounce(t * 2 - 1) * 0.5f + 0.5f;
    }

    // ========== Back (overshoot) ==========

    private static final float BACK_OVERSHOOT = 1.70158f;

    public static float easeOutBack(float t) {
        float t1 = t - 1;
        return t1 * t1 * ((BACK_OVERSHOOT + 1) * t1 + BACK_OVERSHOOT) + 1;
    }

    public static float easeInBack(float t) {
        return t * t * ((BACK_OVERSHOOT + 1) * t - BACK_OVERSHOOT);
    }

    public static float easeInOutBack(float t) {
        float s = BACK_OVERSHOOT * 1.525f;
        if (t < 0.5f) {
            float t2 = t * 2;
            return 0.5f * (t2 * t2 * ((s + 1) * t2 - s));
        }
        float t2 = t * 2 - 2;
        return 0.5f * (t2 * t2 * ((s + 1) * t2 + s) + 2);
    }

    // ========== Utility ==========

    /**
     * 弹簧阻尼模型 — 返回新速度。
     * @param displacement 当前位移 = current - target
     * @param velocity 当前速度
     * @param stiffness 弹性系数（推荐 0.1~0.5）
     * @param damping 阻尼系数（推荐 0.05~0.3）
     * @return 新速度
     */
    public static float springDamp(float displacement, float velocity, float stiffness, float damping) {
        return velocity - stiffness * displacement - damping * velocity;
    }

    /**
     * 平滑阻尼插值（无过冲）。
     * @param current 当前值
     * @param target 目标值
     * @param smoothFactor 平滑因子（0~1，越大越快）
     * @return 新值
     */
    public static float smoothDamp(float current, float target, float smoothFactor) {
        return current + (target - current) * smoothFactor;
    }
}
