package com.zzdzt.show_damage.client.style.builtin.independent;

import com.zzdzt.show_damage.client.DamageNumberParticle;
import com.zzdzt.show_damage.client.style.IndependentStyle;
import com.zzdzt.show_damage.config.ModConfigs;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * 经典弹出风格 — 配置驱动的随机弹出 + 标准物理。
 */
@OnlyIn(Dist.CLIENT)
public class ClassicPopStyle extends IndependentStyle {
    ModConfigs config = getConfig();

    public ClassicPopStyle() {
        super("classic_pop", "show_damage.style.classic_pop.name");
    }

    @Override
    public Velocity calculateInitialVelocity(float damage, boolean isCrit) {
        double spread = config.physics.getHorizontalSpreadFactor() * 2.0;
        double baseUpward = config.physics.getInitialUpwardVelocity();
        double vx = (random.nextDouble() - 0.5) * spread;
        double vz = (random.nextDouble() - 0.5) * spread;
        double vy = baseUpward * (1.0 + random.nextDouble() * 0.4);
        return new Velocity(vx, vy, vz);
    }

    @Override
    public float calculateInitialRotation(float damage, boolean isCrit) {
        return (float) (random.nextGaussian() * (isCrit ? 5.0 : 2.0));
    }

    @Override
    public boolean hasPhysics() {
        return true;
    }

    @Override
    public double getGravityMultiplier() {
        return 1.0;
    }

    @Override
    public void onSpawn(DamageNumberParticle particle, Context context) {
    }

    @Override
    public void onUpdate(DamageNumberParticle particle, Context context) {
    }

    @Override
    public void onRender(DamageNumberParticle particle, RenderContext renderContext) {
    }
}
