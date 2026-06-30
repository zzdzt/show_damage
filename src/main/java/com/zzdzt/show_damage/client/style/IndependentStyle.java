package com.zzdzt.show_damage.client.style;

import com.zzdzt.show_damage.config.ModConfigs;
import net.minecraft.util.RandomSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class IndependentStyle extends AbstractDamageStyle {
    
    protected final RandomSource random = RandomSource.create();
    
    protected IndependentStyle(String id, String displayName) {
        super(id, displayName, StyleType.INDEPENDENT, false);
    }
    
    // 独立模式特有：计算初始速度
    public abstract Velocity calculateInitialVelocity(float damage, boolean isCrit);
    
    // 独立模式特有：计算初始旋转
    public abstract float calculateInitialRotation(float damage, boolean isCrit);
    
    // 独立模式特有：是否启用物理
    public abstract boolean hasPhysics();
    
    // 独立模式特有：获取重力系数
    public abstract double getGravityMultiplier();

    /**
     * 根据伤害值计算基础缩放（供子类复用）。
     */
    protected float calculateBaseScale(float damage, ModConfigs config) {
        if (damage < config.smallDamageThreshold) {
            return config.getActualScaleSmall();
        } else if (damage < config.mediumDamageThreshold) {
            return config.getActualScaleMedium();
        } else {
            return config.getActualScaleLarge();
        }
    }
    
    // 公共静态类，可被外部访问
    public static class Velocity {
        public final double vx, vy, vz;
        
        public Velocity(double vx, double vy, double vz) {
            this.vx = vx;
            this.vy = vy;
            this.vz = vz;
        }
        
        public static Velocity of(double vx, double vy, double vz) {
            return new Velocity(vx, vy, vz);
        }
        
        public static final Velocity ZERO = new Velocity(0, 0, 0);
    }
}