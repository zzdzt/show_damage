package com.zzdzt.show_damage.client.style.builtin.merge;

import com.zzdzt.show_damage.client.DamageNumberParticle;
import com.zzdzt.show_damage.client.style.MergeStyle;
import com.zzdzt.show_damage.client.style.StyleProperties;
import com.zzdzt.show_damage.config.ModConfigs;
import com.zzdzt.show_damage.util.EasingUtils;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * BounceMergeStyle - 弹跳合并风格
 * 
 * 特性：
 * - 跟随实体时带有持续弹跳动画（像皮球一样上下跳动）
 * - 每次合并伤害触发更强的弹跳反弹
 * - 脱离跟随时利用剩余弹跳动量自然飞出
 * - 大伤害触发旋转弹跳特效
 */
@OnlyIn(Dist.CLIENT)
public class BounceMergeStyle extends MergeStyle {

    // 弹跳物理参数
    private static final float BOUNCE_HEIGHT = 0.12f;        // 基础弹跳高度
    private static final float BOUNCE_SPEED = 0.3f;           // 弹跳频率
    private static final float BOUNCE_DAMPING = 0.92f;         // 弹跳衰减
    private static final float GRAVITY = 0.015f;               // 模拟重力

    // 合并反弹参数
    private static final float MERGE_BOUNCE_BOOST = 1.5f;      // 合并时弹跳增强倍数
    private static final float MERGE_SCALE_PULSE = 1.25f;      // 合并时缩放脉冲
    private static final float LARGE_HIT_ROTATION = 8.0f;      // 大伤害旋转角度

    public BounceMergeStyle() {
        super("bounce_merge", "show_damage.style.bounce_merge.name");
    }

    @Override
    public double getFollowHeightOffset() {
        return 1.3;
    }

    @Override
    public double getFollowDistanceOffset() {
        return 0.55;
    }

    @Override
    public double getHorizontalOffset() {
        return 0.0;
    }

    @Override
    public boolean shouldContinueFollowing(DamageNumberParticle particle, Context context) {
        // 前 75% 生命周期跟随，之后脱离
        return particle.getAge() < particle.getMaxAge() * 0.75f;
    }

    @Override
    public void onSpawn(DamageNumberParticle particle, Context context) {
        StyleProperties props = particle.getProperties();
        ModConfigs config = getConfig();

        float baseScale = calculateBaseScale(context.damage, config);

        props.set("base_scale", baseScale)
             .set("bounce_phase", 0f)           // 弹跳相位
             .set("bounce_velocity", 1.0f)       // 弹跳速度因子
             .set("rotation_target", 0f)         // 目标旋转
             .set("is_large", context.damage >= config.mediumDamageThreshold);

        // 初始弹出
        particle.setCurrentScale(baseScale * 0.5f);
        particle.setTargetScale(baseScale);
    }

    @Override
    public void onMerge(DamageNumberParticle particle, float newDamage, Context context) {
        StyleProperties props = particle.getProperties();
        ModConfigs config = getConfig();

        float baseScale = calculateBaseScale(newDamage, config);
        boolean isLarge = newDamage >= config.mediumDamageThreshold;

        // 更新基础缩放
        props.set("base_scale", baseScale)
             .set("is_large", isLarge);

        // 合并时增强弹跳
        float currentBounceVel = props.getFloat("bounce_velocity", 1.0f);
        props.set("bounce_velocity", Math.min(currentBounceVel * MERGE_BOUNCE_BOOST, 3.0f));

        // 缩放脉冲
        particle.setTargetScale(baseScale * MERGE_SCALE_PULSE);

        // 大伤害触发旋转
        if (isLarge) {
            float rotTarget = (float) ((Math.random() - 0.5) * 2 * LARGE_HIT_ROTATION);
            props.set("rotation_target", rotTarget);
        }
    }

    @Override
    public void onUpdate(DamageNumberParticle particle, Context context) {
        StyleProperties props = particle.getProperties();
        updateBounceAnimation(particle, props);
        updateRotation(particle, props);
    }

    @Override
    public void onDetach(DamageNumberParticle particle, Context context) {
        // 脱离时利用当前弹跳动量作为初始速度
        StyleProperties props = particle.getProperties();
        float bounceVel = props.getFloat("bounce_velocity", 1.0f);

        particle.setVY(0.06 + bounceVel * 0.04);
        particle.setVX((Math.random() - 0.5) * 0.03 * bounceVel);
        particle.setVZ((Math.random() - 0.5) * 0.03 * bounceVel);
    }

    @Override
    public void onRender(DamageNumberParticle particle, RenderContext renderContext) {
    }

    // ========== 动画方法 ==========

    private void updateBounceAnimation(DamageNumberParticle particle, StyleProperties props) {
        float phase = props.getFloat("bounce_phase", 0f);
        float velocity = props.getFloat("bounce_velocity", 1.0f);
        float baseScale = props.getFloat("base_scale", 1.0f);
        int age = particle.getAge();

        // 弹跳相位推进
        phase += BOUNCE_SPEED * velocity;
        props.set("bounce_phase", phase);

        // 使用 sin 绝对值模拟弹跳（地面在 0，最高点在 1）
        float bounceY = Math.abs((float) Math.sin(phase)) * BOUNCE_HEIGHT * velocity;
        float bounceScale = 1.0f + bounceY * 0.3f;  // 缩放随弹跳变化

        // 弹跳速度衰减
        velocity *= BOUNCE_DAMPING;
        props.set("bounce_velocity", velocity);

        // 应用缩放
        float currentTarget = baseScale * bounceScale;
        float currentScale = particle.getCurrentScale();
        particle.setCurrentScale(EasingUtils.smoothDamp(currentScale, currentTarget, 0.25f));
        particle.setTargetScale(currentTarget);

        // 通过垂直偏移模拟弹跳位置变化（跟随模式下位移会被覆盖，这里存起来供 render 使用）
        props.set("bounce_offset_y", bounceY * 0.5f);
        props.set("bounce_offset_x", (float) Math.cos(phase * 0.7) * bounceY * 0.15f);
    }

    private void updateRotation(DamageNumberParticle particle, StyleProperties props) {
        float rotTarget = props.getFloat("rotation_target", 0f);
        float currentRot = particle.getRotation();

        // 平滑旋转到目标
        currentRot = EasingUtils.smoothDamp(currentRot, rotTarget, 0.15f);
        particle.setRotation(currentRot);

        // 旋转目标逐渐衰减
        props.set("rotation_target", rotTarget * 0.92f);
    }
}
