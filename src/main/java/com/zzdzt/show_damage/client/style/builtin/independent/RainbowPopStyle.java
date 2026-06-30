package com.zzdzt.show_damage.client.style.builtin.independent;

import com.zzdzt.show_damage.client.DamageNumberParticle;
import com.zzdzt.show_damage.client.style.IndependentStyle;
import com.zzdzt.show_damage.client.style.StyleProperties;
import com.zzdzt.show_damage.config.ModConfigs;
import com.zzdzt.show_damage.util.EasingUtils;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * RainbowPopStyle - 彩虹弹出风格
 * 
 * 特性：
 * - 数字颜色随时间在色相环上循环变化
 * - 弹出动画带有缩放回弹
 * - 轻微旋转飘动，无重力下落
 * - 大伤害颜色变化更快、缩放更大
 */
@OnlyIn(Dist.CLIENT)
public class RainbowPopStyle extends IndependentStyle {

    // 动画参数
    private static final float HUE_SPEED_BASE = 1.2f;       // 基础色相变化速度
    private static final float HUE_SPEED_LARGE = 2.5f;      // 大伤害色相速度
    private static final float POP_SCALE_MULT = 2.0f;        // 弹出最大缩放倍数
    private static final float POP_DECAY = 0.18f;            // 缩放回弹衰减

    // 运动参数
    private static final float DRIFT_AMPLITUDE = 0.004f;     // 飘动幅度
    private static final float DRIFT_FREQUENCY = 0.08f;      // 飘动频率

    public RainbowPopStyle() {
        super("rainbow_pop", "show_damage.style.rainbow_pop.name");
    }

    @Override
    public Velocity calculateInitialVelocity(float damage, boolean isCrit) {
        double vy = 0.06 + random.nextDouble() * 0.04;
        double vx = (random.nextDouble() - 0.5) * 0.03;
        double vz = (random.nextDouble() - 0.5) * 0.03;
        return new Velocity(vx, vy, vz);
    }

    @Override
    public float calculateInitialRotation(float damage, boolean isCrit) {
        return (random.nextFloat() - 0.5f) * 15f;
    }

    @Override
    public boolean hasPhysics() {
        return false;  // 无重力，飘动由风格控制
    }

    @Override
    public double getGravityMultiplier() {
        return 0.0;
    }

    @Override
    public void onSpawn(DamageNumberParticle particle, Context context) {
        StyleProperties props = particle.getProperties();
        ModConfigs config = getConfig();

        float baseScale = calculateBaseScale(context.damage, config);
        boolean isLarge = context.damage >= config.mediumDamageThreshold;

        props.set("base_scale", baseScale)
             .set("hue", random.nextFloat() * 360f)       // 随机初始色相
             .set("hue_speed", isLarge ? HUE_SPEED_LARGE : HUE_SPEED_BASE)
             .set("pop_velocity", 0f)
             .set("is_large", isLarge)
             .set("drift_phase_x", random.nextFloat() * 360f)
             .set("drift_phase_y", random.nextFloat() * 360f);

        // 弹出动画：从 0 缩放到 overshoot
        particle.setCurrentScale(0f);
        particle.setTargetScale(baseScale * POP_SCALE_MULT);
    }

    @Override
    public void onUpdate(DamageNumberParticle particle, Context context) {
        StyleProperties props = particle.getProperties();

        updateColorCycle(particle, props);
        updateScaleAnimation(particle, props);
        updateDriftMotion(particle, props);
    }

    @Override
    public void onRender(DamageNumberParticle particle, RenderContext renderContext) {
    }

    // ========== 动画方法 ==========

    /**
     * 色相循环：颜色随时间在 HSL 色相环上移动。
     */
    private void updateColorCycle(DamageNumberParticle particle, StyleProperties props) {
        float hue = props.getFloat("hue", 0f);
        float hueSpeed = props.getFloat("hue_speed", HUE_SPEED_BASE);

        hue = (hue + hueSpeed) % 360f;
        props.set("hue", hue);

        // HSL -> RGB 转换
        int rgb = hslToRgb(hue, 1.0f, 0.55f);
        particle.setColor(rgb);
    }

    /**
     * 缩放弹性动画：overshoot -> 回弹到基础大小。
     */
    private void updateScaleAnimation(DamageNumberParticle particle, StyleProperties props) {
        float baseScale = props.getFloat("base_scale", 1.0f);
        float currentScale = particle.getCurrentScale();
        float targetScale = particle.getTargetScale();

        float diff = targetScale - currentScale;
        float velocity = props.getFloat("pop_velocity", 0f);

        velocity += diff * 0.45f;
        velocity *= (1 - POP_DECAY);
        currentScale += velocity;

        props.set("pop_velocity", velocity);
        particle.setCurrentScale(currentScale);

        // 当接近 overshoot 峰值后开始回弹到 baseScale
        if (currentScale >= targetScale * 0.95f || currentScale > baseScale * 1.3f) {
            particle.setTargetScale(baseScale);
        }
    }

    /**
     * 飘动运动：正弦波水平 + 轻微上下浮动。
     */
    private void updateDriftMotion(DamageNumberParticle particle, StyleProperties props) {
        int age = particle.getAge();
        float phaseX = props.getFloat("drift_phase_x", 0f);
        float phaseY = props.getFloat("drift_phase_y", 0f);

        float driftX = (float) Math.sin(age * DRIFT_FREQUENCY + phaseX) * DRIFT_AMPLITUDE;
        float driftY = (float) Math.cos(age * DRIFT_FREQUENCY * 0.7 + phaseY) * DRIFT_AMPLITUDE * 0.6f;

        particle.setVX(particle.getVX() * 0.95f + driftX);
        particle.setVY(particle.getVY() * 0.95f + driftY + 0.0003f);  // 轻微上浮
        particle.setVZ(particle.getVZ() * 0.95f);
    }

    // ========== 工具方法 ==========

    /**
     * HSL 转 RGB（整数格式，不含 alpha）。
     */
    private static int hslToRgb(float h, float s, float l) {
        float c = (1 - Math.abs(2 * l - 1)) * s;
        float x = c * (1 - Math.abs((h / 60) % 2 - 1));
        float m = l - c / 2;

        float r, g, b;
        if (h < 60) {
            r = c; g = x; b = 0;
        } else if (h < 120) {
            r = x; g = c; b = 0;
        } else if (h < 180) {
            r = 0; g = c; b = x;
        } else if (h < 240) {
            r = 0; g = x; b = c;
        } else if (h < 300) {
            r = x; g = 0; b = c;
        } else {
            r = c; g = 0; b = x;
        }

        int ri = Math.round((r + m) * 255);
        int gi = Math.round((g + m) * 255);
        int bi = Math.round((b + m) * 255);

        return (ri << 16) | (gi << 8) | bi;
    }
}
