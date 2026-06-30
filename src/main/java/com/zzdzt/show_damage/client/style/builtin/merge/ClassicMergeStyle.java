package com.zzdzt.show_damage.client.style.builtin.merge;

import com.zzdzt.show_damage.client.DamageNumberParticle;
import com.zzdzt.show_damage.client.style.MergeStyle;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * 经典合并风格 — 平滑跟随 + 合并缩放脉冲。
 */
@OnlyIn(Dist.CLIENT)
public class ClassicMergeStyle extends MergeStyle {

    public ClassicMergeStyle() {
        super("classic_merge", "show_damage.style.classic_merge.name");
    }

    @Override
    public double getFollowHeightOffset() {
        return 1.1;
    }

    @Override
    public double getFollowDistanceOffset() {
        return 0.5;
    }

    @Override
    public double getHorizontalOffset() {
        return 0.0;
    }

    @Override
    public boolean shouldContinueFollowing(DamageNumberParticle particle, Context context) {
        return particle.getAge() < particle.getMaxAge() * 0.8f;
    }

    @Override
    public void onSpawn(DamageNumberParticle particle, Context context) {
        particle.getProperties().set("spawn_time", System.currentTimeMillis());
    }

    @Override
    public void onMerge(DamageNumberParticle particle, float newDamage, Context context) {
        // 合并时缩放脉冲
        float currentScale = particle.getTargetScale();
        particle.setTargetScale(currentScale * 1.1f);
        particle.getProperties().set("merge_pulse", 1.0f);
    }

    @Override
    public void onUpdate(DamageNumberParticle particle, Context context) {
        // 合并脉冲衰减
        Float pulse = particle.getProperties().get("merge_pulse", 0f);
        if (pulse > 0) {
            pulse *= 0.9f;
            particle.getProperties().set("merge_pulse", pulse);
        }
    }

    @Override
    public void onRender(DamageNumberParticle particle, RenderContext renderContext) {
    }
}
