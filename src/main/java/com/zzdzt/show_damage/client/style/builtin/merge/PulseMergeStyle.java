package com.zzdzt.show_damage.client.style.builtin.merge;

import com.zzdzt.show_damage.client.DamageNumberParticle;
import com.zzdzt.show_damage.client.style.MergeStyle;
import com.zzdzt.show_damage.client.style.StyleProperties;
import com.zzdzt.show_damage.config.ModConfigs;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * 脉冲合并风格 — 弹性脉冲缩放 + 呼吸浮动，大伤害触发额外弹跳和旋转抖动。
 */
@OnlyIn(Dist.CLIENT)
public class PulseMergeStyle extends MergeStyle {
    
    // 脉冲配置参数
    private static final float PULSE_SMALL_MULTIPLIER = 1.3f;      // 小伤害脉冲倍数
    private static final float PULSE_MEDIUM_MULTIPLIER = 1.6f;      // 中伤害脉冲倍数
    private static final float PULSE_LARGE_MULTIPLIER = 2.2f;      // 大伤害脉冲倍数（更夸张）
    
    private static final float PULSE_DECAY_SPEED = 0.12f;           // 更慢的衰减，让跳动更明显
    private static final float ELASTICITY = 0.25f;                  // 稍低的弹性，更有重量感
    private static final int PULSE_DURATION_TICKS = 12;             // 更长的脉冲持续时间
    
    // 大伤害特殊效果
    private static final float LARGE_HIT_BOUNCE_VELOCITY = 0.08f;     // 大伤害弹跳速度
    private static final float LARGE_HIT_ROTATION_JITTER = 3.0f;      // 大伤害旋转抖动

    public PulseMergeStyle() {
        super("pulse_merge", "show_damage.style.pulse_merge.name");
    }
    
    @Override
    public double getFollowHeightOffset() {
        return 1.35;  // 更高一点，给跳动留出空间
    }
    
    @Override
    public double getFollowDistanceOffset() {
        return 0.65;
    }

    @Override
    public double getHorizontalOffset() {
        return 0.0;  
    }    
    
    @Override
    public boolean shouldContinueFollowing(DamageNumberParticle particle, Context context) {
        return particle.isAlive();
    }
    
    @Override
    public void onSpawn(DamageNumberParticle particle, Context context) {
        StyleProperties props = particle.getProperties();
        ModConfigs config = getConfig();
        
        float initialDamage = context.damage;
        
        props.set("pulse_active", false)
             .set("pulse_timer", 0)
             .set("pulse_scale", 1.0f)
             .set("base_scale", calculateBaseScale(initialDamage, config))
             .set("velocity_scale", 0f)
             .set("total_hits", 1)
             .set("last_damage", initialDamage)
             .set("consecutive_large_hits", 0);

        // 初始脉冲
        float pulseMultiplier = getPulseMultiplierForDamage(initialDamage, config);
        triggerPulse(particle, pulseMultiplier, initialDamage >= config.mediumDamageThreshold);
    }
    
    @Override
    public void onMerge(DamageNumberParticle particle, float newDamage, Context context) {
        StyleProperties props = particle.getProperties();
        ModConfigs config = getConfig();
        
        float lastDamage = props.getFloat("last_damage", newDamage);
        float damageDelta = newDamage - lastDamage;
        
        // 更新统计
        int totalHits = props.getInt("total_hits", 1) + 1;
        props.set("total_hits", totalHits);
        
        // 判断本次伤害大小
        boolean isLargeHit = damageDelta >= config.mediumDamageThreshold;
        boolean isMediumHit = damageDelta >= config.smallDamageThreshold && !isLargeHit;
        
        // 连续大伤害递增效果
        int consecutiveLarge = props.getInt("consecutive_large_hits", 0);
        if (isLargeHit) {
            consecutiveLarge++;
            props.set("consecutive_large_hits", consecutiveLarge);
        } else {
            consecutiveLarge = 0;
            props.set("consecutive_large_hits", 0);
        }
        
        // 根据伤害大小决定脉冲强度
        float pulseMultiplier = getPulseMultiplierForDamage(damageDelta, config);
        
        // 连续大伤害增强效果
        if (consecutiveLarge > 1) {
            pulseMultiplier += (consecutiveLarge - 1) * 0.15f;  // 每次连续大伤害增加15%脉冲
            pulseMultiplier = Math.min(pulseMultiplier, 3.0f);  // 封顶3倍
        }
        
        // 触发脉冲（大伤害有特殊效果）
        triggerPulse(particle, pulseMultiplier, isLargeHit);
        
        float baseScale = calculateBaseScale(newDamage, config);
        props.set("base_scale", baseScale)
             .set("last_damage", newDamage);
    }
    
    @Override
    public void onUpdate(DamageNumberParticle particle, Context context) {
        StyleProperties props = particle.getProperties();
        updateElasticPulse(particle, props);
        updateBreathingEffect(particle, props);
    }
    
    @Override
    public void onRender(DamageNumberParticle particle, RenderContext renderContext) {
    }
    
    // ========== 私有辅助方法 ==========
    
    /**
     * 根据伤害值获取对应的脉冲倍数
     */
    private float getPulseMultiplierForDamage(float damage, ModConfigs config) {
        if (damage >= config.mediumDamageThreshold) {
            return PULSE_LARGE_MULTIPLIER;
        } else if (damage >= config.smallDamageThreshold) {
            return PULSE_MEDIUM_MULTIPLIER;
        } else {
            return PULSE_SMALL_MULTIPLIER;
        }
    }
    
    /**
     * 触发脉冲效果
     * 
     * @param particle 粒子
     * @param multiplier 脉冲倍数
     * @param isLargeHit 是否是大伤害（触发额外效果）
     */
    private void triggerPulse(DamageNumberParticle particle, float multiplier, boolean isLargeHit) {
        StyleProperties props = particle.getProperties();
        
        float baseScale = props.getFloat("base_scale", 1.0f);
        float targetPulseScale = baseScale * multiplier;
        
        // 重置脉冲状态（确保每次都能触发）
        props.set("pulse_active", true)
             .set("pulse_timer", PULSE_DURATION_TICKS)
             .set("pulse_scale", targetPulseScale)
             .set("pulse_start_scale", targetPulseScale)
             .set("is_large_pulse", isLargeHit);
        
        // 立即设置缩放，产生瞬间放大效果
        particle.setCurrentScale(targetPulseScale);
        particle.setTargetScale(targetPulseScale);
        
        // 大伤害特殊效果
        if (isLargeHit) {
            // 向上的弹跳速度
            double currentVY = particle.getVY();
            particle.setVY(currentVY + LARGE_HIT_BOUNCE_VELOCITY);
            
            // 随机旋转抖动
            float rotationJitter = (float) (Math.random() * 2 - 1) * LARGE_HIT_ROTATION_JITTER;
            particle.setRotation(particle.getRotation() + rotationJitter);
            
            // 水平微动（模拟震动）
            double currentVX = particle.getVX();
            double currentVZ = particle.getVZ();
            particle.setVX(currentVX + (Math.random() - 0.5) * 0.02);
            particle.setVZ(currentVZ + (Math.random() - 0.5) * 0.02);
        }
    }
    
    /**
     * 弹性脉冲物理更新
     */
    private void updateElasticPulse(DamageNumberParticle particle, StyleProperties props) {
        boolean pulseActive = props.getBoolean("pulse_active", false);
        int pulseTimer = props.getInt("pulse_timer", 0);
        float baseScale = props.getFloat("base_scale", 1.0f);
        
        if (!pulseActive && pulseTimer <= 0) {
            // 确保回到基础大小
            particle.setTargetScale(baseScale);
            return;
        }
        
        float currentPulseScale = props.getFloat("pulse_scale", baseScale);
        float velocity = props.getFloat("velocity_scale", 0f);
        
        if (pulseTimer > 0) {
            pulseTimer--;
            props.set("pulse_timer", pulseTimer);
            
            // 弹簧物理：向基础缩放值回弹
            float displacement = currentPulseScale - baseScale;
            float acceleration = -ELASTICITY * displacement;
            velocity += acceleration;
            velocity *= (1 - PULSE_DECAY_SPEED);
            
            currentPulseScale += velocity;
            
            // 过冲保护（防止缩得太小）
            if (currentPulseScale < baseScale * 0.85f) {
                currentPulseScale = baseScale * 0.85f;
                velocity = Math.abs(velocity) * 0.3f;  // 反弹
            }
            
            props.set("pulse_scale", currentPulseScale)
                 .set("velocity_scale", velocity);
            
            particle.setTargetScale(currentPulseScale);
            particle.setCurrentScale(currentPulseScale);
            
            // 结束判定：时间到了且速度足够小
            if (pulseTimer <= 0 && Math.abs(displacement) < 0.03f && Math.abs(velocity) < 0.005f) {
                props.set("pulse_active", false);
                particle.setTargetScale(baseScale);
                particle.setCurrentScale(baseScale);
            }
        }
    }
    
    /**
     * 呼吸浮动效果（让数字更有生命力）
     */
    private void updateBreathingEffect(DamageNumberParticle particle, StyleProperties props) {
        int age = particle.getAge();
        
        // 主呼吸波
        float breatheOffset = (float) Math.sin(age * 0.15) * 0.015f;
        
        // 叠加快速微颤（模拟紧张感）
        float jitter = (float) Math.sin(age * 0.8) * 0.005f;
        
        props.set("breathe_offset", breatheOffset + jitter);
    }
}