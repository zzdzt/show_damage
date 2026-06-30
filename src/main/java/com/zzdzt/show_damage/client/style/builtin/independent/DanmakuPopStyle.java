package com.zzdzt.show_damage.client.style.builtin.independent;

import com.zzdzt.show_damage.client.DamageNumberParticle;
import com.zzdzt.show_damage.client.style.IndependentStyle;
import com.zzdzt.show_damage.client.style.StyleProperties;
import com.zzdzt.show_damage.config.ModConfigs;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * 弹幕飘字风格 — 模仿视频网站弹幕的从右往左横穿效果。
 *
 * 效果：
 * - 数字从目标实体右侧（玩家视角）生成
 * - 以恒定速度向左横穿，从右到左滑过实体
 * - 穿过实体后逐步淡出消失
 * - 每条弹幕有微小 Y 轴抖动，避免快速攻击时多条弹幕完全重叠
 * - 无重力、无旋转，纯直线运动
 */
@OnlyIn(Dist.CLIENT)
public class DanmakuPopStyle extends IndependentStyle {

    // ========== 弹幕几何参数 ==========

    /** 生成时向右偏移距离（方块）。数字从实体右侧这么远的地方出现。 */
    private static final double SPAWN_OFFSET = 1.8;

    /** 向左滚动速度（方块/tick）。值越大弹幕飞得越快。 */
    private static final double SCROLL_SPEED = 0.08;

    /** 弹幕在实体上方的基准高度偏移（方块）。 */
    private static final double BASE_Y_OFFSET = 1.1;

    /** Y 轴随机抖动范围（方块）。每条弹幕在基准高度 ±(Y_JITTER/2) 内浮动。 */
    private static final double Y_JITTER = 0.35;

    /** 额外生命周期（tick）。默认 30 tick 不够弹幕从右到左完整横穿，延长它。 */
    private static final int LIFETIME_EXTEND = 15;

    /** 缩放系数。弹幕风格稍小更精致，避免大字压迫感。 */
    private static final float SCALE_MODIFIER = 0.82f;


    public DanmakuPopStyle() {
        super("danmaku_pop", "show_damage.style.danmaku_pop.name");
    }

    // ========== IndependentStyle 抽象方法实现 ==========

    @Override
    public Velocity calculateInitialVelocity(float damage, boolean isCrit) {
        return Velocity.ZERO;  // 弹幕自己管移动，不需要初始速度
    }

    @Override
    public float calculateInitialRotation(float damage, boolean isCrit) {
        return 0;
    }

    @Override
    public boolean hasPhysics() {
        return false;  // 关闭物理，完全自管理
    }

    @Override
    public double getGravityMultiplier() {
        return 0;
    }

    // ========== 生命周期钩子 ==========

    @Override
    public void onSpawn(DamageNumberParticle particle, Context context) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        ModConfigs config = getConfig();
        StyleProperties props = particle.getProperties();
        LivingEntity target = context.target;

        // 1. 计算玩家视角的「右侧」在世界坐标中的方向
        //    Minecraft yaw=0→南(+Z), 90→西(-X), 180→北(-Z), 270→东(+X)
        //    右侧向量 = forward × up = (-cos(yaw), 0, -sin(yaw))
        float yaw = mc.player.getYRot();
        double yawRad = Math.toRadians(yaw);
        double rightX = -Math.cos(yawRad);
        double rightZ = -Math.sin(yawRad);

        // 2. 实体上半身参考坐标
        double entityX = target.getX();
        double entityY = target.getY() + target.getBbHeight() * 0.7;
        double entityZ = target.getZ();

        // 3. 每条弹幕的 Y 轴微型偏移（防止多条弹幕完全排成一条线）
        double yJitter = (random.nextDouble() - 0.5) * Y_JITTER;

        // 4. 计算最终生成位置：实体右侧 SPAWN_OFFSET 方块处
        double spawnX = entityX + rightX * SPAWN_OFFSET;
        double spawnY = entityY + BASE_Y_OFFSET + yJitter;
        double spawnZ = entityZ + rightZ * SPAWN_OFFSET;

        particle.setPosition(spawnX, spawnY, spawnZ);

        // 5. 存方向向量到属性，供 onUpdate 每 tick 使用
        props.set("dan_right_x", rightX)
             .set("dan_right_z", rightZ);

        // 6. 缩放：弹幕风格精致小巧
        float baseScale = calculateBaseScale(context.damage, config) * SCALE_MODIFIER;
        particle.setCurrentScale(baseScale * 0.5f); // 从一半弹入
        particle.setTargetScale(baseScale);

        // 7. 延长生命周期，确保能滚完全程
        particle.setMaxAge(particle.getMaxAge() + LIFETIME_EXTEND);

        // 8. 归零一切速度和旋转
        particle.setVX(0);
        particle.setVY(0);
        particle.setVZ(0);
        particle.setRotation(0);
    }

    @Override
    public void onUpdate(DamageNumberParticle particle, Context context) {
        StyleProperties props = particle.getProperties();

        double rightX = props.getDouble("dan_right_x", 1.0);
        double rightZ = props.getDouble("dan_right_z", 0.0);

        // 向左匀速滚动（left = -right）
        particle.moveBy(-rightX * SCROLL_SPEED, 0, -rightZ * SCROLL_SPEED);

        // 保持正面朝向，无旋转
        particle.setRotation(0);
    }

    @Override
    public void onRender(DamageNumberParticle particle, RenderContext renderContext) {
    }
}
