package com.zzdzt.show_damage.client.style.builtin.independent;

import com.zzdzt.show_damage.client.DamageNumberParticle;
import com.zzdzt.show_damage.client.style.IndependentStyle;
import com.zzdzt.show_damage.client.style.StyleProperties;
import com.zzdzt.show_damage.config.ModConfigs;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * 终末地工业风格 — 纯纵向上升、无旋转、无重力，克制精确的弹出。
 *
 * 参考《明日方舟：终末地》视觉语言设计：
 * - 纯直线上升，无水平偏移、无旋转
 * - 轻微弹入缩放，无弹簧过冲
 * - 平滑淡出，数字始终清晰可辨
 */
@OnlyIn(Dist.CLIENT)
public class EndfieldPopStyle extends IndependentStyle {

    // ========== 运动参数 ==========

    /**
     * 基准上升速度（每 tick）。
     * 终末地风格因 vx=0/vz=0 无法靠水平漂移脱离实体遮挡，
     * 所以比 Classic Pop 需要更快的上升速度来尽快清出实体身体区域。
     */
    private static final double UPWARD_VELOCITY = 0.10;

    /** 粒子间的微小速度差异，避免完全一致产生机械感 */
    private static final double UPWARD_VARIANCE = 0.008;

    // ========== 生成位移 ==========

    /**
     * 生成时 Y 轴额外抬高。
     * 因为终末地风格无水平漂移（vx=0, vz=0），数字始终与实体同 XZ 坐标，
     * 需要在生成时向上提一些，避免数字与实体头部纹理重叠而不可读。
     * 单位：方块（block）。
     */
    private static final double SPAWN_Y_LIFT = 0.35;

    /**
     * 生成时的微观水平偏移量。
     * 因为 vx=0/vz=0 导致所有数字在同一条垂直线上重叠，
     * 引入 ±0.04 方块的微偏移让每个数字从略微不同的起点出发，
     * 肉眼几乎看不出偏移，但足以防止数字叠在一起无法辨认。
     */
    private static final double HORIZONTAL_MICRO_OFFSET = 0.04;

    // ========== 缩放参数 ==========

    /** 生成时的初始缩放比例（越小弹入效果越明显），终末地约 15% */
    private static final float SPAWN_SCALE_RATIO = 0.18f;

    /** 过冲保护倍数 — 缩放一旦超过此阈值立即钳制回目标值 */
    private static final float OVERSCALE_GUARD = 1.04f;

    // ========== 颜色 ==========

    /**
     * 终末地风格的伤害颜色偏移。
     * 基础色来自配置（白/黄/红三级），本风格仅做微调：
     * - 略微降低大伤害的红色饱和度（#d50000 → #e04040 方向），
     *   使颜色在深色背景下更内敛，符合终末地工业审美。
     */
    private static final int ENDFIELD_RED   = 0xE04040;   // 克制红
    private static final int ENDFIELD_YELLOW = 0xFFDD55;  // 柔和金

    public EndfieldPopStyle() {
        super("endfield_pop", "show_damage.style.endfield_pop.name");
    }

    // ========== 核心物理 ==========

    @Override
    public Velocity calculateInitialVelocity(float damage, boolean isCrit) {
        // 终末地核心：vx = 0, vz = 0，纯纵向上升
        double vy = UPWARD_VELOCITY + random.nextDouble() * UPWARD_VARIANCE;
        return new Velocity(0, vy, 0);
    }

    @Override
    public float calculateInitialRotation(float damage, boolean isCrit) {
        return 0;  // 无旋转 — Billboard 纯正面
    }

    @Override
    public boolean hasPhysics() {
        return false;  // 完全关闭重力、阻力、加速度
    }

    @Override
    public double getGravityMultiplier() {
        return 0.0;
    }

    // ========== 生命周期钩子 ==========

    @Override
    public void onSpawn(DamageNumberParticle particle, Context context) {
        StyleProperties props = particle.getProperties();
        ModConfigs config = getConfig();

        // 缩放：从极小开始，自然弹入到伤害对应尺寸
        float baseScale = calculateBaseScale(context.damage, config);
        props.set("base_scale", baseScale);
        particle.setCurrentScale(baseScale * SPAWN_SCALE_RATIO);
        particle.setTargetScale(baseScale);
        particle.setScaleVelocity(0);

        // 终末地风格 — 着色微调
        applyEndfieldColor(particle, context.damage, config);

        // 确保初始状态纯净
        enforceStraightUp(particle);

        // 生成时向上提 SPAWN_Y_LIFT，避免数字与实体纹理重叠
        // （终末地 vx=vz=0，无法靠侧移脱离遮挡，只能靠抬高 Y 和加速上升）
        // 同时施加微观水平偏移，让多个数字沿略微不同的垂直线上升，防止重叠
        double microX = (random.nextDouble() - 0.5) * HORIZONTAL_MICRO_OFFSET * 2.0;
        double microZ = (random.nextDouble() - 0.5) * HORIZONTAL_MICRO_OFFSET * 2.0;
        particle.setPosition(particle.getX() + microX,
                             particle.getY() + SPAWN_Y_LIFT,
                             particle.getZ() + microZ);
    }

    @Override
    public void onUpdate(DamageNumberParticle particle, Context context) {
        // 每帧强制保持 "纯净直线" 约束
        enforceStraightUp(particle);

        // 防缩放过冲（Minecraft 默认弹簧会在接近目标时过冲 → 钳制）
        guardScaleOvershoot(particle);
    }

    @Override
    public void onRender(DamageNumberParticle particle, RenderContext renderContext) {
    }

    @Override
    public void onMerge(DamageNumberParticle particle, float newDamage, Context context) {
    }

    // ========== 内部方法 ==========

    /**
     * 每次 tick 强制将水平速度和旋转归零。
     * 因为 Minecraft 粒子系统可能在碰撞等情况下引入侧向速度，
     * 终末地风格要求始终保持纯净直线上升。
     */
    private void enforceStraightUp(DamageNumberParticle particle) {
        particle.setVX(0);
        particle.setVZ(0);
        particle.setRotation(0);
    }

    /**
     * 防止缩放弹簧过冲。
     * Minecraft 默认 tick() 使用二阶弹簧驱动缩放，
     * 在接近目标时会过冲 10%~30%。终末地风格通过每帧钳制来消除此行为。
     */
    private void guardScaleOvershoot(DamageNumberParticle particle) {
        float current = particle.getCurrentScale();
        float target = particle.getTargetScale();

        if (current > target * OVERSCALE_GUARD) {
            particle.setCurrentScale(target);
            particle.setScaleVelocity(0);
        }
    }

    /**
     * 应用终末地风格的伤害颜色。
     * <p>
     * 相比默认颜色，终末地版本：
     * <ul>
     *   <li>大伤害 — 克制深红（0xE04040），降低视觉侵略性</li>
     *   <li>中伤害 — 柔和金色（0xFFDD55），更接近终末地的亮白/金</li>
     *   <li>小伤害 — 保留配置白色，符合"干净留白"美学</li>
     * </ul>
     */
    private void applyEndfieldColor(DamageNumberParticle particle, float damage, ModConfigs config) {
        int color;
        if (damage >= config.mediumDamageThreshold) {
            color = ENDFIELD_RED;
        } else if (damage >= config.smallDamageThreshold) {
            color = ENDFIELD_YELLOW;
        } else {
            // 小伤害保留纯白 — 终末地的"留白"哲学
            color = config.colorSmall;
        }
        particle.setColor(color);
    }
}
