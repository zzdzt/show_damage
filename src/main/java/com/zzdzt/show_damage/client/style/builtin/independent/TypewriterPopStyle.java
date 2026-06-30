package com.zzdzt.show_damage.client.style.builtin.independent;

import com.zzdzt.show_damage.client.DamageNumberParticle;
import com.zzdzt.show_damage.client.style.IndependentStyle;
import com.zzdzt.show_damage.client.style.StyleProperties;
import com.zzdzt.show_damage.config.ModConfigs;
import com.zzdzt.show_damage.util.EasingUtils;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * TypewriterPopStyle — 打字机独立风格
 *
 * 独立模式的打字机效果：数字像从电报机逐字打出来。
 *
 * 动画分为三阶段：
 *
 * ① 打字阶段（typewriter phase）
 *    - 数字从左到右逐位出现：先是"?"，然后最左位落定，
 *      接着次位落定，直到全部落定。每位落定时有"咔嗒"缩放弹入。
 *    - 整体从初始位置向右偏移渐入，再弹回正位（模拟打字机的进纸感）
 *
 * ② 悬停阶段（hold phase）
 *    - 数字稳定显示，伴随轻微的上下浮动（光标闪烁感）
 *    - 持续约 8 tick
 *
 * ③ 消散阶段（fade phase）
 *    - 数字向上加速淡出（使用粒子的自然寿命淡出 + 向上速度）
 */
@OnlyIn(Dist.CLIENT)
public class TypewriterPopStyle extends IndependentStyle {

    // ============ 打字阶段参数 ============

    /** 每位数字打出所需的 tick 数 */
    private static final int TICKS_PER_DIGIT = 3;

    /** 打字完成后弹入落位的 easeOutBack 过冲系数 */
    private static final float SETTLE_OVERSHOOT = 1.18f;

    /** 落位完成后弹性回落耗时 tick */
    private static final int SETTLE_TICKS = 4;

    /** 打字过程中数字的初始缩放（从小弹出） */
    private static final float TYPE_SCALE_START = 0.4f;

    /** 出现前的占位字符（模拟等待打出） */
    private static final char PLACEHOLDER = '?';

    // ============ 悬停阶段参数 ============

    /** 悬停持续 tick 数 */
    private static final int HOLD_TICKS = 8;

    /** 悬停期间上下浮动幅度（单位：方块） */
    private static final double HOLD_FLOAT_AMP = 0.004;

    /** 悬停期间浮动频率 (rad/tick) */
    private static final double HOLD_FLOAT_FREQ = 0.25;

    // ============ 速度参数 ============

    /** 打字阶段基本无垂直速度，保持悬浮感 */
    private static final double VY_TYPING = 0.012;

    /** 消散阶段上浮速度 */
    private static final double VY_FADE = 0.055;

    // ============ 阶段标识 ============

    private static final int PHASE_TYPING  = 0;
    private static final int PHASE_HOLDING = 1;
    private static final int PHASE_FADING  = 2;

    public TypewriterPopStyle() {
        super("typewriter_pop", "show_damage.style.typewriter_pop.name");
    }

    // ============ IndependentStyle 必要实现 ============

    @Override
    public Velocity calculateInitialVelocity(float damage, boolean isCrit) {
        // 打字阶段近乎悬浮：极低上升速度，无水平漂移
        return new Velocity(0, VY_TYPING, 0);
    }

    @Override
    public float calculateInitialRotation(float damage, boolean isCrit) {
        // 打字机风格：完全正面，无旋转
        return 0.0f;
    }

    @Override
    public boolean hasPhysics() {
        // 自己管理运动，关闭内置物理
        return false;
    }

    @Override
    public double getGravityMultiplier() {
        return 0.0;
    }

    // ============ 生命周期 ============

    @Override
    public void onSpawn(DamageNumberParticle particle, Context context) {
        StyleProperties props = particle.getProperties();
        ModConfigs config = getConfig();

        float damage = context.damage;
        float baseScale = calculateBaseScale(damage, config);
        String fullText = particle.getText(); // 完整伤害数字字符串

        props.set("base_scale",       baseScale)
             .set("full_text",         fullText)
             .set("phase",             PHASE_TYPING)
             .set("typed_count",       0)                // 已落定位数
             .set("digit_timer",       TICKS_PER_DIGIT)  // 当前位剩余 tick
             .set("settle_timer",      0)
             .set("hold_timer",        HOLD_TICKS)
             .set("hold_phase",        0.0)
             .set("fade_vy",           VY_FADE);

        // 打字起始：所有数字替换为占位符
        particle.setText(buildTypingDisplay(fullText, 0));
        // 从小弹出
        particle.setCurrentScale(TYPE_SCALE_START * baseScale);
        particle.setTargetScale(baseScale);
    }

    @Override
    public void onUpdate(DamageNumberParticle particle, Context context) {
        StyleProperties props = particle.getProperties();
        int phase = props.getInt("phase", PHASE_TYPING);

        switch (phase) {
            case PHASE_TYPING  -> updateTypingPhase(particle, props);
            case PHASE_HOLDING -> updateHoldingPhase(particle, props);
            case PHASE_FADING  -> updateFadingPhase(particle, props);
        }
    }

    @Override
    public void onRender(DamageNumberParticle particle, RenderContext renderContext) {
    }

    // ============ 打字阶段 ============

    private void updateTypingPhase(DamageNumberParticle particle, StyleProperties props) {
        String fullText = props.getString("full_text", particle.getText());
        int totalDigits = fullText.length();
        int typedCount  = props.getInt("typed_count", 0);
        float baseScale = props.getFloat("base_scale", 1.0f);

        // 所有位都打完了 → 进入悬停
        if (typedCount >= totalDigits) {
            particle.setText(fullText);
            enterHoldPhase(particle, props, baseScale);
            return;
        }

        // 当前正在打的位的计时器
        int digitTimer = props.getInt("digit_timer", TICKS_PER_DIGIT);
        digitTimer--;

        if (digitTimer <= 0) {
            // 当前位落定
            typedCount++;
            props.set("typed_count", typedCount)
                 .set("digit_timer", TICKS_PER_DIGIT);

            // 落定弹性：小 → 超调 → 正常
            particle.setCurrentScale(baseScale * TYPE_SCALE_START);
            particle.setTargetScale(baseScale * SETTLE_OVERSHOOT);

            // 下一位更新显示
            particle.setText(buildTypingDisplay(fullText, typedCount));

            // 启动落位弹回计时
            props.set("settle_timer", SETTLE_TICKS);
        } else {
            props.set("digit_timer", digitTimer);

            // 落定弹回动画
            int settleTimer = props.getInt("settle_timer", 0);
            if (settleTimer > 0) {
                settleTimer--;
                props.set("settle_timer", settleTimer);

                float t = 1.0f - settleTimer / (float) SETTLE_TICKS;
                float eased = EasingUtils.easeOutBack(t);
                float scale = baseScale * TYPE_SCALE_START + (baseScale - baseScale * TYPE_SCALE_START) * eased;
                particle.setCurrentScale(scale);
                particle.setTargetScale(baseScale);
            }
        }

        // 打字阶段：缓慢上升（用 moveBy，因为 hasPhysics=false 内置物理不执行）
        particle.moveBy(0, VY_TYPING, 0);
        particle.setVY(0);
        particle.setVX(0);
        particle.setVZ(0);
        particle.setRotation(0);
    }

    /**
     * 构建打字显示字符串：
     * typedCount 位已落定（显示真实数字），其余用占位符。
     *
     * 从左到右逐位打出。
     */
    private static String buildTypingDisplay(String fullText, int typedCount) {
        StringBuilder sb = new StringBuilder();
        int len = fullText.length();
        for (int i = 0; i < len; i++) {
            if (i < typedCount) {
                sb.append(fullText.charAt(i));
            } else {
                sb.append(PLACEHOLDER);
            }
        }
        return sb.toString();
    }

    // ============ 悬停阶段 ============

    private void enterHoldPhase(DamageNumberParticle particle, StyleProperties props, float baseScale) {
        props.set("phase", PHASE_HOLDING)
             .set("hold_timer", HOLD_TICKS)
             .set("hold_phase", 0.0);

        particle.setCurrentScale(baseScale);
        particle.setTargetScale(baseScale);
        particle.setVY(0);
        particle.setVX(0);
        particle.setVZ(0);
    }

    private void updateHoldingPhase(DamageNumberParticle particle, StyleProperties props) {
        int holdTimer = props.getInt("hold_timer", HOLD_TICKS);
        holdTimer--;
        props.set("hold_timer", holdTimer);

        // 上下浮动（用 moveBy，让 lerp 能平滑插值）
        double holdPhase = props.getDouble("hold_phase", 0.0);
        double prevPhase = holdPhase;
        holdPhase += HOLD_FLOAT_FREQ;
        props.set("hold_phase", holdPhase);

        // 计算本 tick 位移差（而不是设置绝对位置）
        double floatDY = (Math.sin(holdPhase) - Math.sin(prevPhase)) * HOLD_FLOAT_AMP;
        particle.moveBy(0, floatDY, 0);
        particle.setVY(0);
        particle.setVX(0);
        particle.setVZ(0);

        if (holdTimer <= 0) {
            props.set("phase", PHASE_FADING);
        }
    }

    // ============ 消散阶段 ============

    private void updateFadingPhase(DamageNumberParticle particle, StyleProperties props) {
        // 通过设置 vy 让内置物理引擎负责上移（TypewriterPopStyle 的 hasPhysics 返回 false，
        // 所以必须自己用 moveBy 移动）
        double selfVy = props.getDouble("fade_vy", VY_FADE);
        selfVy = Math.min(selfVy + 0.003, VY_FADE * 1.6);
        props.set("fade_vy", selfVy);

        particle.moveBy(0, selfVy, 0);
        particle.setVY(0); // 确保内置物理不再叠加

        // 缩放轻微收缩（即将消失感）
        float baseScale = props.getFloat("base_scale", 1.0f);
        particle.setCurrentScale(EasingUtils.smoothDamp(particle.getCurrentScale(), baseScale * 0.6f, 0.04f));
    }
}
