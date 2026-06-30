package com.zzdzt.show_damage.client.style;

import com.zzdzt.show_damage.client.DamageNumberParticle;
import com.zzdzt.show_damage.config.ModConfigs;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class MergeStyle extends AbstractDamageStyle {
    
    protected MergeStyle(String id, String displayName) {
        super(id, displayName, StyleType.MERGE, true);
    }
    
    // 合并模式特有：获取跟随高度偏移
    public abstract double getFollowHeightOffset();
    
    // 合并模式特有：获取跟随距离偏移
    public abstract double getFollowDistanceOffset();
    
    // 合并模式特有：更新时是否继续跟随
    public abstract boolean shouldContinueFollowing(DamageNumberParticle particle, Context context);

    // 合并模式特有：水平偏移（相对于相机方向）
    public abstract double getHorizontalOffset();

    /**
     * 根据累计伤害计算基础缩放值（供子类复用）。
     */
    protected float calculateBaseScale(float totalDamage, ModConfigs config) {
        if (totalDamage < config.smallDamageThreshold) {
            return config.getActualScaleSmall();
        } else if (totalDamage < config.mediumDamageThreshold) {
            return config.getActualScaleMedium();
        } else {
            return config.getActualScaleLarge();
        }
    }
}