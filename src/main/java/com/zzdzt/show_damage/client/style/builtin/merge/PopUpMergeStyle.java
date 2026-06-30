package com.zzdzt.show_damage.client.style.builtin.merge;

import com.zzdzt.show_damage.client.DamageNumberParticle;
import com.zzdzt.show_damage.client.style.MergeStyle;
import com.zzdzt.show_damage.client.style.StyleProperties;
import com.zzdzt.show_damage.config.ModConfigs;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * PopUpMergeStyle - 弹出式合并风格
 * 
 * 特性：
 * - 位置：实体上方偏右，不遮挡目标
 * - 出现：从实体位置快速弹入（缩放+位移）
 * - 停留：短暂固定在目标上方
 * - 消失：向上浮动并逐渐淡出
 */
@OnlyIn(Dist.CLIENT)
public class PopUpMergeStyle extends MergeStyle {
    
    // ========== 动画阶段定义 ==========
    private enum AnimationPhase {
        POP_IN,      // 弹出进入（0-4 ticks）
        HOLD,        // 停留（5-15 ticks）
        FLOAT_UP,    // 上浮淡出（16 ticks至结束）
        DONE
    }
    
    // ========== 配置参数 ==========
    private static final float POP_SCALE_MULTIPLIER = 1.8f;      // 弹出时最大缩放
    private static final float POP_DECAY = 0.35f;                 // 弹出回弹速度
    private static final int POP_DURATION = 4;                      // 弹出动画持续ticks
    private static final int HOLD_DURATION = 11;                    // 停留持续ticks（5-15）
    
    // 位置偏移
    private static final double HEIGHT_OFFSET = 1.25;               // 基础高度
    private static final double RIGHT_OFFSET = 0.45;                // 向右偏移（不遮挡）
    private static final double FORWARD_OFFSET = 0.3;               // 向前偏移（朝向相机）
    
    // 上浮参数
    private static final float FLOAT_UP_SPEED = 0.035f;              // 上浮速度
    private static final float FLOAT_FADE_START = 0.6f;             // 上浮阶段开始淡出比例

    private static final float DECAY_RATE = 0.4f;                   //弹出衰减系数
    private static final float MIN_REPOP_MULTIPLIER = 1.25f;        

    public PopUpMergeStyle() {
        super("popup_merge", "show_damage.style.popup_merge.name");
    }
    
    @Override
    public double getFollowHeightOffset() {
        return HEIGHT_OFFSET;
    }
    
    @Override
    public double getFollowDistanceOffset() {
        return RIGHT_OFFSET;
    }

    @Override
    public double getHorizontalOffset() {
        return 0.0;  
    }

    @Override
    public boolean shouldContinueFollowing(DamageNumberParticle particle, Context context) {
        StyleProperties props = particle.getProperties();
        AnimationPhase phase = props.getEnum("anim_phase", AnimationPhase.class, AnimationPhase.POP_IN);
        
        // 只有在停留阶段才继续跟随，上浮阶段脱离
        return phase == AnimationPhase.POP_IN || phase == AnimationPhase.HOLD;
    }

    
    @Override
    public void onSpawn(DamageNumberParticle particle, Context context) {
        StyleProperties props = particle.getProperties();
        ModConfigs config = getConfig();
        
        float damage = context.damage;
        float baseScale = calculateBaseScale(damage, config);
        
        props.set("base_scale", baseScale)
             .set("anim_phase", AnimationPhase.POP_IN)
             .set("phase_timer", 0)
             .set("hold_position_x", particle.getX())    // 记录停留位置
             .set("hold_position_y", particle.getY())
             .set("hold_position_z", particle.getZ())
             .set("pop_velocity", 0f)              // 弹出动画速度
             .set("merge_count", 0);
        
        // 初始状态：缩放为0（不可见），位置在实体中心
        particle.setCurrentScale(0f);
        particle.setTargetScale(baseScale * POP_SCALE_MULTIPLIER);
        
        // 初始速度：轻微向上弹出
        particle.setVY(0.08);
        particle.setVX((Math.random() - 0.5) * 0.02);  // 随机水平微动
        particle.setVZ((Math.random() - 0.5) * 0.02);
    }
    
    @Override
    public void onMerge(DamageNumberParticle particle, float newDamage, Context context) {
        StyleProperties props = particle.getProperties();
        ModConfigs config = getConfig();
        
        int mergeCount = props.getInt("merge_count", 0) + 1;
        props.set("merge_count", mergeCount);
        
        float baseScale = calculateBaseScale(newDamage, config);
        props.set("base_scale", baseScale);
        
        // 合并时重新触发弹出效果（但幅度递减）
        
        float rePopMultiplier = Math.max(
            MIN_REPOP_MULTIPLIER,
            1.5f + (POP_SCALE_MULTIPLIER - 1.0f) / (1 + mergeCount * DECAY_RATE));
        
        // 重置到弹出阶段
        props.set("anim_phase", AnimationPhase.POP_IN)
             .set("phase_timer", 0)
             .set("pop_velocity", 0f);
        
        particle.setTargetScale(baseScale * rePopMultiplier);
        
        // 合并时给予微小弹跳
        double currentVY = particle.getVY();
        particle.setVY(currentVY + 0.03);
    }
    
    @Override
    public void onUpdate(DamageNumberParticle particle, Context context) {
        StyleProperties props = particle.getProperties();
        AnimationPhase phase = props.getEnum("anim_phase", AnimationPhase.class, AnimationPhase.POP_IN);
        int timer = props.getInt("phase_timer", 0);
        float baseScale = props.getFloat("base_scale", 1.0f);
        
        switch (phase) {
            case POP_IN -> updatePopInPhase(particle, props, timer, baseScale);
            case HOLD -> updateHoldPhase(particle, props, timer, baseScale);
            case FLOAT_UP -> updateFloatUpPhase(particle, props, baseScale);
            case DONE -> particle.remove();
        }
        
        props.set("phase_timer", timer + 1);
    }
    
    
    /**
     * 弹出阶段：快速缩放动画 + 轻微位移
     */
    private void updatePopInPhase(DamageNumberParticle particle, StyleProperties props, 
                                  int timer, float baseScale) {
        float currentScale = particle.getCurrentScale();
        float targetScale = particle.getTargetScale();
        
        // 弹性回弹效果
        float diff = targetScale - currentScale;
        float velocity = props.getFloat("pop_velocity", 0f);
        
        velocity += diff * 0.4f;           // 向目标缩放加速
        velocity *= (1 - POP_DECAY);        // 阻尼
        currentScale += velocity;
        
        props.set("pop_velocity", velocity);
        particle.setCurrentScale(currentScale);
        
        // 速度衰减，准备进入停留
        particle.setVY(particle.getVY() * 0.85);
        particle.setVX(particle.getVX() * 0.9);
        particle.setVZ(particle.getVZ() * 0.9);
        
        // 阶段转换
        if (timer >= POP_DURATION && Math.abs(diff) < 0.1f) {
            props.set("anim_phase", AnimationPhase.HOLD);
            props.set("phase_timer", 0);
            
            props.set("hold_position_x", particle.getX())
                 .set("hold_position_y", particle.getY())
                 .set("hold_position_z", particle.getZ());
            
            particle.setVX(0);
            particle.setVY(0);
            particle.setVZ(0);
            particle.setTargetScale(baseScale);  // 回到基础大小
        }
    }
    
    /**
     * 停留阶段：固定在位置，轻微呼吸效果
     */
    private void updateHoldPhase(DamageNumberParticle particle, StyleProperties props, 
                                  int timer, float baseScale) {

        float currentScale = particle.getCurrentScale();
        float diff = baseScale - currentScale;
        particle.setCurrentScale(currentScale + diff * 0.15f);
        
        float breathe = (float) (Math.sin(timer * 0.3) * 0.003);
        particle.setPosition(particle.getX(), particle.getY() + breathe, particle.getZ());
        
        if (timer >= HOLD_DURATION) {
            props.set("anim_phase", AnimationPhase.FLOAT_UP);
            props.set("phase_timer", 0);
            
            particle.setFollowing(false);
            
            particle.setVY(FLOAT_UP_SPEED);
            particle.setVX((Math.random() - 0.5) * 0.01);
        }
    }
    
    /**
     * 上浮阶段：向上浮动 + 逐渐淡出
     */
    private void updateFloatUpPhase(DamageNumberParticle particle, StyleProperties props, 
                                     float baseScale) {

        particle.setVY(FLOAT_UP_SPEED * (1 - particle.getAge() / (float) particle.getMaxAge()));
        
        int age = particle.getAge();
        float driftX = (float) Math.sin(age * 0.1) * 0.002f;
        particle.setPosition(particle.getX() + driftX, particle.getY(), particle.getZ());
        
        float lifeRatio = 1 - (particle.getAge() / (float) particle.getMaxAge());
        float targetScale = baseScale * (0.7f + lifeRatio * 0.3f);
        float currentScale = particle.getCurrentScale();
        particle.setCurrentScale(currentScale + (targetScale - currentScale) * 0.1f);
        

        if (particle.getAge() >= particle.getMaxAge() - 1) {
            props.set("anim_phase", AnimationPhase.DONE);
        }
    }
    
    @Override
    public void onRender(DamageNumberParticle particle, RenderContext renderContext) {
    }
}