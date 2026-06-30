package com.zzdzt.show_damage.client.style.builtin.merge;

import com.zzdzt.show_damage.client.DamageNumberParticle;
import com.zzdzt.show_damage.client.style.MergeStyle;
import com.zzdzt.show_damage.client.style.StyleProperties;
import com.zzdzt.show_damage.config.ModConfigs;
import com.zzdzt.show_damage.util.EasingUtils;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.Random;

/**
 * EnergyChargeMergeStyle — 聚能蓄力式合并风格（增强版）
 *
 * 三大充电阶段，每阶段有独立视觉特征：
 *
 * ① 蓄力中 [charge 0~0.5]
 *    - 颜色向亮白偏移
 *    - 数字低频脉动（像心跳）
 *    - 轻微随机抖动
 *
 * ② 超载 [charge 0.5~1.0]
 *    - 颜色闪烁：原色 <-> 纯白 交替（告警效果）
 *    - 抖动幅度加大，形成"电流颤抖"
 *    - 脉动频率加速，逼近临界
 *    - 前缀出现 "!" 警示（充能 >= 0.85 时）
 *
 * ③ 爆发脱离 [detached]
 *    - 闪光帧：纯白 + 1.4x 缩放超调
 *    - 3 tick 后颜色恢复，缩放平稳回落
 *    - 带弧形轨迹（upward burst + 微弱横向）
 *    - 淡出时保留原色，干净消散
 */
@OnlyIn(Dist.CLIENT)
public class EnergyChargeMergeStyle extends MergeStyle {

    // ============ 充能参数 ============

    /** 每次合并增加的充能值 */
    private static final float CHARGE_PER_HIT = 0.20f;

    /** 充能上限 */
    private static final float CHARGE_CAP = 1.0f;

    /** 超载阈值（进入闪烁告警） */
    private static final float OVERLOAD_THRESHOLD = 0.5f;

    /** 警示感叹号阈值 */
    private static final float ALERT_THRESHOLD = 0.85f;

    /** 充能每 tick 缓慢衰减量 */
    private static final float CHARGE_DECAY = 0.0015f;

    // ============ 颜色参数 ============

    /** 蓄力阶段向白色偏移最大比例（charge=0.5时的偏移量） */
    private static final float WHITEN_MAX = 0.45f;

    /** 超载阶段闪烁频率 (rad/tick)，越大越快 */
    private static final double FLASH_FREQ_MIN = 0.22;
    private static final double FLASH_FREQ_MAX = 0.60;

    /** 超载阶段闪烁深度（0=不闪，1=全白全原交替） */
    private static final float FLASH_DEPTH = 0.85f;

    // ============ 抖动参数 ============

    /** 蓄力阶段基础抖动（方块） */
    private static final double JITTER_BASE = 0.012;
    private static final double JITTER_MIN  = 0.002;

    /** 超载阶段额外抖动倍数 */
    private static final double JITTER_OVERLOAD_MULT = 3.0;

    // ============ 脉动参数 ============

    /** 脉动频率基数/峰值 (rad/tick) */
    private static final double PULSE_FREQ_BASE = 0.20;
    private static final double PULSE_FREQ_MAX  = 0.55;

    /** 脉动幅度基数/峰值（缩放波动） */
    private static final float  PULSE_AMP_BASE  = 0.04f;
    private static final float  PULSE_AMP_MAX   = 0.10f;

    // ============ 脱离爆发参数 ============

    /** 脱离时超调缩放 */
    private static final float DETACH_BURST_SCALE  = 1.40f;

    /** 爆发白光持续 tick */
    private static final int DETACH_FLASH_TICKS = 4;

    /** 脱离后轨迹衰减 */
    private static final float DETACH_DECAY = 0.07f;

    // ============ 位置 ============

    private static final double HEIGHT_OFFSET     = 1.15;
    private static final double FORWARD_OFFSET    = 0.5;
    private static final double HORIZONTAL_OFFSET = 0.0;

    private static final Random RAND = new Random();

    public EnergyChargeMergeStyle() {
        super("energy_charge_merge", "show_damage.style.energy_charge_merge.name");
    }

    // ============ MergeStyle 抽象方法 ============

    @Override public double getFollowHeightOffset()   { return HEIGHT_OFFSET; }
    @Override public double getFollowDistanceOffset() { return FORWARD_OFFSET; }
    @Override public double getHorizontalOffset()     { return HORIZONTAL_OFFSET; }

    @Override
    public boolean shouldContinueFollowing(DamageNumberParticle particle, Context context) {
        return particle.isAlive();
    }

    // ============ 生命周期 ============

    @Override
    public void onSpawn(DamageNumberParticle particle, Context context) {
        StyleProperties props = particle.getProperties();
        ModConfigs config = getConfig();

        float damage = context.damage;
        float baseScale = calculateBaseScale(damage, config);

        // 存储原始颜色供后续插值使用
        int origColor = particle.getColor();

        props.set("base_scale",       baseScale)
             .set("charge_level",     CHARGE_PER_HIT)
             .set("pulse_phase",      0.0)
             .set("flash_phase",      0.0)
             .set("jitter_x",         0.0)
             .set("jitter_y",         0.0)
             .set("jitter_z",         0.0)
             .set("detached",         false)
             .set("detach_flash",     0)
             .set("original_color",   origColor)
             .set("original_text",    particle.getText())
             .set("alert_active",     false)
             .set("base_vy",          0.0);

        applyChargeVisual(particle, CHARGE_PER_HIT, origColor, 0.0);
    }

    @Override
    public void onMerge(DamageNumberParticle particle, float newDamage, Context context) {
        StyleProperties props = particle.getProperties();
        ModConfigs config = getConfig();

        float baseScale = calculateBaseScale(newDamage, config);
        props.set("base_scale", baseScale);
        props.set("original_text", particle.getText()); // 更新基础文字

        // 如果当前处于警示状态，重新挂接 "!" 前缀
        // （因为合并时底层已经把粒子文字改成了裸数字，需要恢复前缀）
        if (props.getBoolean("alert_active", false)) {
            particle.setText("!" + particle.getText());
        }

        // 每次命中充能增加
        float curCharge = props.getFloat("charge_level", 0.1f);
        float newCharge = Math.min(curCharge + CHARGE_PER_HIT, CHARGE_CAP);
        props.set("charge_level", newCharge);

        // 命中时微脉冲（缩放瞬时跳大）
        float hitPulse = 1.0f + newCharge * 0.10f;
        particle.setCurrentScale(baseScale * hitPulse);
        particle.setTargetScale(baseScale);

        // 颜色立刻更新
        int origColor = props.getInt("original_color", particle.getColor());
        double flashPhase = props.getDouble("flash_phase", 0.0);
        applyChargeVisual(particle, newCharge, origColor, flashPhase);
    }

    @Override
    public void onUpdate(DamageNumberParticle particle, Context context) {
        StyleProperties props = particle.getProperties();
        boolean detached = props.getBoolean("detached", false);

        if (detached) {
            updateDetachedPhase(particle, props);
        } else {
            updateChargingPhase(particle, props);
        }
    }

    @Override
    public void onDetach(DamageNumberParticle particle, Context context) {
        StyleProperties props = particle.getProperties();

        // 解除警示感叹号（如果有）
        String origText = props.getString("original_text", particle.getText());
        particle.setText(origText);

        props.set("detached",     true)
             .set("detach_flash", DETACH_FLASH_TICKS);

        float baseScale = props.getFloat("base_scale", 1.0f);
        // 爆发：瞬间放大
        particle.setCurrentScale(baseScale * DETACH_BURST_SCALE);
        particle.setTargetScale(baseScale);
        // 纯白闪光
        particle.setColor(0xFFFFFF);

        // 向上冲速度
        particle.setVY(0.09);
        particle.setVX((RAND.nextDouble() - 0.5) * 0.014);
        particle.setVZ((RAND.nextDouble() - 0.5) * 0.014);

        props.set("base_vy", particle.getVY());
    }

    @Override
    public void onRender(DamageNumberParticle particle, RenderContext renderContext) {
        // 视觉完全靠 onUpdate 驱动，此处留空
    }

    // ============ 充能阶段更新 ============

    private void updateChargingPhase(DamageNumberParticle particle, StyleProperties props) {
        float charge = props.getFloat("charge_level", 0.1f);
        float baseScale = props.getFloat("base_scale", 1.0f);
        int origColor = props.getInt("original_color", particle.getColor());

        // 1. 充能缓慢衰减（防止永远保持红色）
        float decayed = Math.max(charge - CHARGE_DECAY, 0.0f);
        props.set("charge_level", decayed);

        // 2. 脉动缩放
        double phase = props.getDouble("pulse_phase", 0.0);
        double freq = lerp(PULSE_FREQ_BASE, PULSE_FREQ_MAX, charge);
        float amp = lerpF(PULSE_AMP_BASE, PULSE_AMP_MAX, charge);
        phase += freq;
        props.set("pulse_phase", phase);

        float pulse = (float) Math.sin(phase) * amp * charge;
        float targetScale = baseScale * (1.0f + pulse);
        particle.setTargetScale(targetScale);
        particle.setCurrentScale(EasingUtils.smoothDamp(particle.getCurrentScale(), targetScale, 0.28f));

        // 3. 颜色（含闪烁相位更新）
        double flashPhase = props.getDouble("flash_phase", 0.0);
        if (charge >= OVERLOAD_THRESHOLD) {
            double flashFreq = lerp(FLASH_FREQ_MIN, FLASH_FREQ_MAX, (charge - OVERLOAD_THRESHOLD) / (1.0f - OVERLOAD_THRESHOLD));
            flashPhase += flashFreq;
        }
        props.set("flash_phase", flashPhase);

        applyChargeVisual(particle, charge, origColor, flashPhase);

        // 4. 随机抖动
        double jitterMult = (charge >= OVERLOAD_THRESHOLD) ? JITTER_OVERLOAD_MULT : 1.0;
        double jitterAmt = lerp(JITTER_MIN, JITTER_BASE, charge) * jitterMult;

        double prevJX = props.getDouble("jitter_x", 0.0);
        double prevJY = props.getDouble("jitter_y", 0.0);
        double prevJZ = props.getDouble("jitter_z", 0.0);

        double newJX = (RAND.nextDouble() - 0.5) * jitterAmt * 2.0;
        double newJY = (RAND.nextDouble() - 0.5) * jitterAmt * 0.8;
        double newJZ = (RAND.nextDouble() - 0.5) * jitterAmt * 2.0;

        // 平滑抖动（避免机械跳跃）
        props.set("jitter_x", EasingUtils.smoothDamp((float) prevJX, (float) newJX, 0.55f))
             .set("jitter_y", EasingUtils.smoothDamp((float) prevJY, (float) newJY, 0.55f))
             .set("jitter_z", EasingUtils.smoothDamp((float) prevJZ, (float) newJZ, 0.55f));

        // 5. 超载警示感叹号
        boolean alertActive = props.getBoolean("alert_active", false);
        String origText = props.getString("original_text", particle.getText());
        if (charge >= ALERT_THRESHOLD && !alertActive) {
            particle.setText("!" + origText);
            props.set("alert_active", true);
        } else if (charge < ALERT_THRESHOLD && alertActive) {
            particle.setText(origText);
            props.set("alert_active", false);
        }
    }

    // ============ 脱离阶段更新 ============

    /**
     * 脱离后完全自管理运动：在 onUpdate 中直接更新 x/y/z，
     * 同时将速度归零，防止 DamageNumberParticle.updatePhysics() 重复叠加。
     *
     * tick() 的执行顺序：① onUpdate → ② updatePhysics（判断 !isFollowing 时执行）
     * 我们在 onUpdate 里移动了粒子，再把 vx/vy/vz 归零，
     * updatePhysics 就不会再动它（vx=vy=vz=0 时位移为 0）。
     */
    private void updateDetachedPhase(DamageNumberParticle particle, StyleProperties props) {
        int flashTimer = props.getInt("detach_flash", 0);
        float baseScale = props.getFloat("base_scale", 1.0f);

        // ——— 颜色 & 缩放 ———
        if (flashTimer > 0) {
            flashTimer--;
            props.set("detach_flash", flashTimer);

            float flashRatio = flashTimer / (float) DETACH_FLASH_TICKS; // 1→0
            int origColor = props.getInt("original_color", 0xFFFFFF);
            int r = lerpChannel((origColor >> 16) & 0xFF, 255, flashRatio);
            int g = lerpChannel((origColor >> 8)  & 0xFF, 255, flashRatio);
            int b = lerpChannel(origColor & 0xFF,          255, flashRatio);
            particle.setColor((r << 16) | (g << 8) | b);

            float burstScale = baseScale * (1.0f + (DETACH_BURST_SCALE - 1.0f) * flashRatio);
            particle.setCurrentScale(EasingUtils.smoothDamp(particle.getCurrentScale(), burstScale, 0.35f));
        } else {
            int origColor = props.getInt("original_color", 0xFFFFFF);
            particle.setColor(origColor);
            particle.setTargetScale(baseScale * 0.88f);
            particle.setCurrentScale(EasingUtils.smoothDamp(particle.getCurrentScale(), baseScale, 0.12f));
        }

        // ——— 自管理位置更新 ———
        // 从 props 中维护当前速度（不依赖 vx/vy/vz，避免物理引擎干扰）
        double selfVy = props.getDouble("self_vy", 0.09);
        double selfVx = props.getDouble("self_vx", 0.0);
        double selfVz = props.getDouble("self_vz", 0.0);

        // vy 随时间衰减（模拟阻力）
        selfVy = selfVy * (1.0 - DETACH_DECAY * 0.8);

        // 正弦漂移叠加到 x/z 速度分量
        int age = particle.getAge();
        selfVx += Math.sin(age * 0.10) * 0.00015;
        selfVz += Math.cos(age * 0.10) * 0.00010;
        // 阻尼防止水平速度积累
        selfVx *= 0.92;
        selfVz *= 0.92;

        props.set("self_vy", selfVy)
             .set("self_vx", selfVx)
             .set("self_vz", selfVz);

        // 直接移动粒子位置（用 moveBy 只更新 x/y/z，保留 xo/yo/zo 给 lerp 插值）
        particle.moveBy(selfVx, selfVy, selfVz);

        // 将粒子内置速度归零，让 updatePhysics 不再重复移动
        particle.setVX(0);
        particle.setVY(0);
        particle.setVZ(0);
    }

    // ============ 颜色工具 ============

    /**
     * 根据充能值 + 闪烁相位计算当前颜色：
     * - 蓄力阶段：原色 → 亮白偏移（程度随 charge 增加）
     * - 超载阶段：原色 <-> 纯白 正弦闪烁
     */
    private void applyChargeVisual(DamageNumberParticle particle, float charge, int origColor, double flashPhase) {
        int r = (origColor >> 16) & 0xFF;
        int g = (origColor >> 8)  & 0xFF;
        int b =  origColor        & 0xFF;

        if (charge < OVERLOAD_THRESHOLD) {
            // 蓄力：线性亮白偏移
            float whitenT = (charge / OVERLOAD_THRESHOLD) * WHITEN_MAX;
            r = lerpChannel(r, 255, whitenT);
            g = lerpChannel(g, 255, whitenT);
            b = lerpChannel(b, 255, whitenT);
        } else {
            // 超载：先做亮白偏移，再叠加闪烁
            r = lerpChannel(r, 255, WHITEN_MAX);
            g = lerpChannel(g, 255, WHITEN_MAX);
            b = lerpChannel(b, 255, WHITEN_MAX);

            // sin 振荡：[-1, 1] → [0, 1]，用 FLASH_DEPTH 控制深度
            float flashT = ((float) Math.sin(flashPhase) * 0.5f + 0.5f) * FLASH_DEPTH;
            r = lerpChannel(r, 255, flashT);
            g = lerpChannel(g, 255, flashT);
            b = lerpChannel(b, 255, flashT);
        }

        particle.setColor((r << 16) | (g << 8) | b);
    }

    private static int lerpChannel(int a, int b, float t) {
        return Math.min(255, Math.max(0, (int)(a + (b - a) * t)));
    }

    private static double lerp(double a, double b, float t) {
        return a + (b - a) * Math.min(1.0f, Math.max(0.0f, t));
    }

    private static float lerpF(float a, float b, float t) {
        return a + (b - a) * Math.min(1.0f, Math.max(0.0f, t));
    }
}
