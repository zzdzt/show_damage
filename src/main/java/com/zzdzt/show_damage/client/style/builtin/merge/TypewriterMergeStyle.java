package com.zzdzt.show_damage.client.style.builtin.merge;

import com.zzdzt.show_damage.client.DamageNumberParticle;
import com.zzdzt.show_damage.client.style.MergeStyle;
import com.zzdzt.show_damage.client.style.StyleProperties;
import com.zzdzt.show_damage.config.ModConfigs;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.Random;

/**
 * TypewriterMergeStyle — 打字机计数器式合并风格
 *
 * 视觉特征：
 * 每次伤害累加时，数字不是直接替换，而是像机械计数器一样逐位翻转。
 * 个位先翻，十位其次，百位最后。翻转中显示随机数字模拟"拨动"效果。
 * 翻转完毕使用 easeOutBack 弹性缩放，产生"咔嗒"落位感。
 */
@OnlyIn(Dist.CLIENT)
public class TypewriterMergeStyle extends MergeStyle {

    // ============ 翻转参数 ============

    /** 每次翻转的总 tick 数。实际视觉长度 = FLIP_DURATION + 额外缓冲。 */
    private static final int FLIP_DURATION = 7;

    /** 每位数字独立翻转所需的 tick 数。 */
    private static final int TICKS_PER_DIGIT = 2;

    /** 翻转完成后弹性回弹的缩放倍数。 */
    private static final float SETTLE_OVERSHOOT = 1.15f;

    /** 翻转中数字缩小的最低比例。 */
    private static final float FLIP_SHRINK = 0.7f;

    // ============ 位置偏移 ============

    private static final double HEIGHT_OFFSET = 1.2;
    private static final double FORWARD_OFFSET = 0.5;
    private static final double HORIZONTAL_OFFSET = 0.0;

    private static final Random RAND = new Random();

    public TypewriterMergeStyle() {
        super("typewriter_merge", "show_damage.style.typewriter_merge.name");
    }

    // ============ MergeStyle 抽象方法 ============

    @Override
    public double getFollowHeightOffset() {
        return HEIGHT_OFFSET;
    }

    @Override
    public double getFollowDistanceOffset() {
        return FORWARD_OFFSET;
    }

    @Override
    public double getHorizontalOffset() {
        return HORIZONTAL_OFFSET;
    }

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

        // 初始状态：无翻转
        props.set("flip_timer", 0)
             .set("base_scale", baseScale)
             .set("last_text", String.valueOf((int) damage));
    }

    @Override
    public void onMerge(DamageNumberParticle particle, float newDamage, Context context) {
        StyleProperties props = particle.getProperties();
        ModConfigs config = getConfig();

        String prevText = props.getString("last_text", String.valueOf((int) newDamage));
        String targetText = String.valueOf((int) newDamage);

        float baseScale = calculateBaseScale(newDamage, config);
        props.set("base_scale", baseScale)
             .set("last_text", targetText)
             .set("flip_prev", prevText)
             .set("flip_target", targetText)
             .set("flip_timer", FLIP_DURATION)
             .set("settle_pending", false);

        // 翻转中轻微缩小
        particle.setTargetScale(baseScale * FLIP_SHRINK);
    }

    @Override
    public void onUpdate(DamageNumberParticle particle, Context context) {
        StyleProperties props = particle.getProperties();
        int timer = props.getInt("flip_timer", 0);

        if (timer > 0) {
            updateFlipAnimation(particle, props, timer);
            timer--;
            props.set("flip_timer", timer);

            if (timer == 0) {
                finishFlip(particle, props);
            }
        } else {
            // 翻转完成后的弹性回弹
            updateSettleAnimation(particle, props);
        }

        // 常驻呼吸微动
        float breathe = (float) Math.sin(particle.getAge() * 0.12) * 0.008f;
        props.set("breathe_offset", breathe);
    }

    @Override
    public void onRender(DamageNumberParticle particle, RenderContext renderContext) {
    }

    // ============ 翻转动画 ============

    /**
     * 逐位翻转动画 — 每一位数字独立翻转，从低位到高位级联。
     *
     * 翻转规则：
     * 位   timer 7→6→5→4→3→2→1
     * 个位 翻滚→翻滚→落位─────────
     * 十位 ──────翻滚→翻滚→落位──
     * 百位 ──────────────翻滚→落位
     */
    private void updateFlipAnimation(DamageNumberParticle particle, StyleProperties props, int timer) {
        String prev = props.getString("flip_prev", "0");
        String target = props.getString("flip_target", "0");

        // 使两字符串右对齐（数字右对齐更自然）
        String padded = alignRight(prev, target);
        String paddedPrev = padded.split("\\|")[0];
        String paddedTarget = padded.split("\\|")[1];
        int len = paddedPrev.length();

        // 每位独立翻转 tick 起点：最低位最先翻
        // timer 从 FLIP_DURATION 递减到 1
        // 第 i 位（从右边数，i=0 是个位）起始 tick = (i+1) * TICKS_PER_DIGIT
        StringBuilder display = new StringBuilder();

        for (int i = len - 1; i >= 0; i--) {
            int digitPos = len - 1 - i; // 0=个位, 1=十位, ...
            int digitStartTick = (digitPos + 1) * TICKS_PER_DIGIT;

            char targetCh = paddedTarget.charAt(i);

            if (timer > digitStartTick) {
                // 还没轮到这一位翻转 → 保持原值
                display.append(paddedPrev.charAt(i));
            } else if (timer > digitStartTick - TICKS_PER_DIGIT) {
                // 正在翻 → 随机字符
                display.append((char) ('0' + RAND.nextInt(10)));
            } else {
                // 已翻完 → 目标值
                display.append(targetCh);
            }
        }

        particle.setText(display.toString());

        // 翻转过程中的缩放抖动 — 每 tick 有轻微缩放波动
        float baseScale = props.getFloat("base_scale", 1.0f);
        float shake = 1.0f + (float) Math.sin(timer * 1.8) * 0.04f;
        particle.setTargetScale(baseScale * FLIP_SHRINK * shake);
    }

    /**
     * 翻转完成 — 切到目标数字、触发弹性落位。
     */
    private void finishFlip(DamageNumberParticle particle, StyleProperties props) {
        String target = props.getString("flip_target", "0");
        particle.setText(target);

        float baseScale = props.getFloat("base_scale", 1.0f);
        // 弹性回弹：超调到 SETTLE_OVERSHOOT，再回落到 baseScale
        particle.setTargetScale(baseScale * SETTLE_OVERSHOOT);
        particle.setCurrentScale(baseScale * FLIP_SHRINK);

        props.set("settle_pending", true)
             .set("settle_timer", 4);
    }

    /**
     * 弹性落位动画 — 翻转完成后用 easeOutBack 回弹到正常大小。
     */
    private void updateSettleAnimation(DamageNumberParticle particle, StyleProperties props) {
        if (!props.getBoolean("settle_pending", false)) return;

        int settleTimer = props.getInt("settle_timer", 0);
        if (settleTimer <= 0) {
            props.set("settle_pending", false);
            return;
        }

        float baseScale = props.getFloat("base_scale", 1.0f);
        settleTimer--;
        props.set("settle_timer", settleTimer);

        // 4 tick 弹性回弹: 超调 → 回落到目标
        float t = 1.0f - settleTimer / 4.0f; // 0 → 1
        float eased = easeOutBack(t, 1.7f);

        float currentScale = particle.getCurrentScale();
        float targetScale = baseScale;
        float scale = baseScale + (SETTLE_OVERSHOOT * baseScale - baseScale) * (1 - eased);

        particle.setCurrentScale(scale);

        if (settleTimer <= 0) {
            particle.setTargetScale(targetScale);
            particle.setCurrentScale(targetScale);
            props.set("settle_pending", false);
        }
    }

    // ============ 工具方法 ============

    /**
     * 将两数字字符串右对齐，用空格补位。
     * 返回 "左对齐prev|左对齐target"。
     */
    private static String alignRight(String a, String b) {
        int maxLen = Math.max(a.length(), b.length());
        String pa = padLeft(a, maxLen);
        String pb = padLeft(b, maxLen);
        return pa + "|" + pb;
    }

    private static String padLeft(String s, int len) {
        StringBuilder sb = new StringBuilder();
        while (sb.length() + s.length() < len) {
            sb.append(' ');  // 空格占位，渲染时不影响居中对齐
        }
        sb.append(s);
        return sb.toString();
    }

    /**
     * 自定义 easeOutBack：比标准版多一个强度参数。
     */
    private static float easeOutBack(float t, float strength) {
        float t1 = t - 1;
        return t1 * t1 * ((strength + 1) * t1 + strength) + 1;
    }
}
