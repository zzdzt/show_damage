// DamageStyle.java - 移动到 client.style 包
package com.zzdzt.show_damage.client.style;

import com.zzdzt.show_damage.client.DamageNumberParticle;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;

@OnlyIn(Dist.CLIENT)
public interface DamageStyle {
    
    String getId();
    
    String getDisplayName();
    
    StyleType getType();
    
    void onSpawn(DamageNumberParticle particle, Context context);
    
    void onUpdate(DamageNumberParticle particle, Context context);
    
    void onMerge(DamageNumberParticle particle, float newDamage, Context context);
    
    void onRender(DamageNumberParticle particle, RenderContext renderContext);

    /**
     * 粒子脱离跟随实体时调用（仅合并模式）。
     * 风格可在此处设置脱离后的过渡动画（如初始速度、旋转等）。
     */
    default void onDetach(DamageNumberParticle particle, Context context) {}
    
    boolean supportsMerge();
    
    enum StyleType {
        MERGE,
        INDEPENDENT
    }
    
    class Context {
        public final LivingEntity target;
        public final float damage;
        public final boolean isCrit;
        public final long timestamp;
        public final int lifetime;
        
        public Context(LivingEntity target, float damage, boolean isCrit, int lifetime) {
            this.target = target;
            this.damage = damage;
            this.isCrit = isCrit;
            this.lifetime = lifetime;
            this.timestamp = System.currentTimeMillis();
        }
    }
    
    class RenderContext {
        public final float partialTick;
        public final double renderX, renderY, renderZ;
        public final float alpha;
        public final int age;
        public final int maxAge;
        
        public RenderContext(float partialTick, double renderX, double renderY, double renderZ, 
                           float alpha, int age, int maxAge) {
            this.partialTick = partialTick;
            this.renderX = renderX;
            this.renderY = renderY;
            this.renderZ = renderZ;
            this.alpha = alpha;
            this.age = age;
            this.maxAge = maxAge;
        }
    }
}