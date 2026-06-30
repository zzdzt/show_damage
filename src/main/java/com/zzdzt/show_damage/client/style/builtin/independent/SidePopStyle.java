package com.zzdzt.show_damage.client.style.builtin.independent;

import com.zzdzt.show_damage.client.DamageNumberParticle;
import com.zzdzt.show_damage.client.style.IndependentStyle;
import com.zzdzt.show_damage.client.style.StyleProperties;
import com.zzdzt.show_damage.config.ModConfigs;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * SidePopStyle - 侧向弹出风格（无重力版）
 * 
 * 特性：
 * - 位置：实体左右两侧随机出现，距离受 horizontalSpread 配置影响
 * - 出现：侧向弹出，大伤害带缩放脉冲
 * - 物理：无重力，自然飘动
 * - 消失：渐隐淡出，不下落
 */
@OnlyIn(Dist.CLIENT)
public class SidePopStyle extends IndependentStyle {
    
    // ========== 动画阶段 ==========
    private enum AnimPhase {
        POP_OUT,     // 侧向弹出（0-6 ticks）
        SETTLE,      // 减速稳定（7-12 ticks）
        FLOAT,       // 自然飘动（13 ticks至结束）
    }
    
    // ========== 配置参数 ==========
    // 弹出动画
    private static final float POP_SCALE_LARGE = 1.6f;       // 大伤害最大缩放
    private static final float POP_SCALE_MEDIUM = 1.3f;      // 中伤害最大缩放
    private static final float POP_SCALE_SMALL = 1.15f;      // 小伤害最大缩放
    private static final float POP_DECAY = 0.25f;            // 缩放回弹速度
    private static final int POP_DURATION = 6;               // 弹出持续ticks
    
    // 侧向偏移基础倍数（相对于 horizontalSpread）
    private static final double SIDE_OFFSET_MIN_MULT = 3.0;  // 最小倍数
    private static final double SIDE_OFFSET_MAX_MULT = 6.0;  // 最大倍数
    
    // 高度偏移
    private static final double HEIGHT_OFFSET_BASE = 0.8;    // 基础高度
    
    // 飘动参数
    private static final float FLOAT_DAMPING = 0.94f;        // 速度阻尼
    private static final float BREATHE_FREQ = 0.12f;         // 飘动频率
    private static final float BREATHE_AMP = 0.003f;         // 飘动幅度

    public SidePopStyle() {
        super("side_pop", "show_damage.style.side_pop.name");
    }
    
    // ========== IndependentStyle 实现 ==========
    
    @Override
    public Velocity calculateInitialVelocity(float damage, boolean isCrit) {
        // 侧向速度在 onSpawn 中根据方向设置
        double upward = 0.04 + random.nextDouble() * 0.03;
        return Velocity.of(0, upward, 0);
    }
    
    @Override
    public float calculateInitialRotation(float damage, boolean isCrit) {
        return (random.nextFloat() - 0.5f) * 8f;
    }
    
    @Override
    public boolean hasPhysics() {
        return false;  // 无重力物理
    }
    
    @Override
    public double getGravityMultiplier() {
        return 0.0;  // 零重力
    }
    
    // ========== 生命周期回调 ==========
    
    @Override
    public void onSpawn(DamageNumberParticle particle, Context context) {
        StyleProperties props = particle.getProperties();
        ModConfigs config = getConfig();
        
        float damage = context.damage;
        boolean isLarge = damage >= config.mediumDamageThreshold;
        boolean isMedium = damage >= config.smallDamageThreshold && !isLarge;
        
        // 获取配置中的 spread 值（0-1范围）
        double spreadFactor = config.physics.getHorizontalSpreadFactor();
        
        // 计算侧向距离：基于 horizontalSpread 配置
        double sideMin = spreadFactor * SIDE_OFFSET_MIN_MULT;
        double sideMax = spreadFactor * SIDE_OFFSET_MAX_MULT;
        double sideDistance = sideMin + random.nextDouble() * (sideMax - sideMin);
        
        // 决定左右侧
        boolean isRightSide = random.nextBoolean();
        double sideDirection = isRightSide ? 1.0 : -1.0;
        
        props.set("anim_phase", AnimPhase.POP_OUT)
             .set("phase_timer", 0)
             .set("is_right_side", isRightSide)
             .set("side_distance", sideDistance * sideDirection)  // 带符号的距离
             .set("base_scale", calculateBaseScale(damage, config))
             .set("max_scale", isLarge ? POP_SCALE_LARGE : (isMedium ? POP_SCALE_MEDIUM : POP_SCALE_SMALL))
             .set("is_large_hit", isLarge)
             .set("pop_velocity", 0f)
             .set("rotation_velocity", calculateInitialRotation(damage, context.isCrit))
             .set("spread_factor", spreadFactor);  // 存储用于后续飘动幅度计算
        
        // 初始缩放动画
        float targetMaxScale = props.getFloat("max_scale", 1.0f);
        particle.setCurrentScale(0f);
        particle.setTargetScale(targetMaxScale);
        
        // 设置初始速度（侧向弹出）
        double sideVel = sideDistance * sideDirection * 0.15;  // 侧向速度
        double upVel = 0.05 + random.nextDouble() * 0.04;       // 向上初速
        
        particle.setVX(sideVel);
        particle.setVY(upVel);
        particle.setVZ((random.nextDouble() - 0.5) * 0.02);     // 轻微前后随机
    }
    
    @Override
    public void onUpdate(DamageNumberParticle particle, Context context) {
        StyleProperties props = particle.getProperties();
        AnimPhase phase = props.getEnum("anim_phase", AnimPhase.class, AnimPhase.POP_OUT);
        int timer = props.getInt("phase_timer", 0);
        float baseScale = props.getFloat("base_scale", 1.0f);

        applyVelocity(particle);
        
        switch (phase) {
            case POP_OUT -> updatePopOutPhase(particle, props, timer, baseScale);
            case SETTLE -> updateSettlePhase(particle, props, timer, baseScale);
            case FLOAT -> updateFloatPhase(particle, props);
        }
        
        props.set("phase_timer", timer + 1);

    }

    private void applyVelocity(DamageNumberParticle particle) {
        particle.setPosition(
            particle.getX() + particle.getVX(),
            particle.getY() + particle.getVY(),
            particle.getZ() + particle.getVZ()
        );
    }

    // ========== 阶段更新方法 ==========
    
    private void updatePopOutPhase(DamageNumberParticle particle, StyleProperties props, 
                                   int timer, float baseScale) {
        float maxScale = props.getFloat("max_scale", 1.0f);
        float currentScale = particle.getCurrentScale();
        float velocity = props.getFloat("pop_velocity", 0f);
        
        // 弹性动画向 maxScale 逼近
        float diff = maxScale - currentScale;
        velocity += diff * 0.5f;
        velocity *= (1 - POP_DECAY);
        currentScale += velocity;
        
        props.set("pop_velocity", velocity);
        particle.setCurrentScale(currentScale);
        
        // 速度阻尼（逐渐减速）
        particle.setVX(particle.getVX() * 0.9);
        particle.setVY(particle.getVY() * 0.85);
        particle.setVZ(particle.getVZ() * 0.9);
        
        // 阶段转换
        if (timer >= POP_DURATION && Math.abs(diff) < 0.05f) {
            props.set("anim_phase", AnimPhase.SETTLE);
            props.set("phase_timer", 0);
        }
    }
    
    private void updateSettlePhase(DamageNumberParticle particle, StyleProperties props, 
                                   int timer, float baseScale) {
        float currentScale = particle.getCurrentScale();
        
        // 平滑回到基础大小
        float diff = baseScale - currentScale;
        currentScale += diff * 0.12f;
        particle.setCurrentScale(currentScale);
        
        // 速度快速衰减至飘动速度
        particle.setVX(particle.getVX() * 0.8);
        particle.setVY(particle.getVY() * 0.75);
        particle.setVZ(particle.getVZ() * 0.8);
        
        // 旋转稳定
        float rotVel = props.getFloat("rotation_velocity", 0f);
        rotVel *= 0.85f;
        props.set("rotation_velocity", rotVel);
        particle.setRotation(particle.getRotation() + rotVel);
        
        // 进入飘动阶段
        if (timer >= 6) {
            props.set("anim_phase", AnimPhase.FLOAT);
            props.set("phase_timer", 0);
        }
    }
    
    private void updateFloatPhase(DamageNumberParticle particle, StyleProperties props) {
        int age = particle.getAge();
        float spreadFactor = props.getFloat("spread_factor", 0.15f);
        
        // 自然飘动：正弦波运动，幅度受 spread 配置影响
        float breatheAmp = BREATHE_AMP * (0.5f + spreadFactor * 2);
        float breatheY = (float) Math.sin(age * BREATHE_FREQ) * breatheAmp;
        float breatheX = (float) Math.cos(age * BREATHE_FREQ * 0.7) * breatheAmp * 0.5f;
        
        // 应用飘动（叠加到现有速度）
        particle.setVY(particle.getVY() * FLOAT_DAMPING + breatheY);
        particle.setVX(particle.getVX() * FLOAT_DAMPING + breatheX);
        particle.setVZ(particle.getVZ() * FLOAT_DAMPING);
        
        // 轻微向上漂移（模拟浮力）
        particle.setVY(particle.getVY() + 0.0005f);
    }
    
    // ========== 辅助方法 ==========
    
    @Override
    public void onRender(DamageNumberParticle particle, RenderContext renderContext) {
    }
}