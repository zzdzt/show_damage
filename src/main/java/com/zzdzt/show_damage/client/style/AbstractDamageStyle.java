package com.zzdzt.show_damage.client.style;

import com.zzdzt.show_damage.client.DamageNumberParticle;
import com.zzdzt.show_damage.config.ModConfigs;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class AbstractDamageStyle implements DamageStyle {

    protected final String id;
    protected final String displayNameKey;
    protected final StyleType type;
    protected final boolean supportsMerge;

    /**
     * @param displayNameKey 翻译键，如 "show_damage.style.classic_merge.name"
     */
    protected AbstractDamageStyle(String id, String displayNameKey, StyleType type, boolean supportsMerge) {
        this.id = id;
        this.displayNameKey = displayNameKey;
        this.type = type;
        this.supportsMerge = supportsMerge;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getDisplayName() {
        return Component.translatable(displayNameKey).getString();
    }
    
    @Override
    public StyleType getType() {
        return type;
    }
    
    @Override
    public boolean supportsMerge() {
        return supportsMerge;
    }
    
    @Override
    public void onSpawn(DamageNumberParticle particle, Context context) {
        // 子类重写
    }
    
    @Override
    public void onUpdate(DamageNumberParticle particle, Context context) {
        // 子类重写
    }
    
    @Override
    public void onMerge(DamageNumberParticle particle, float newDamage, Context context) {
        if (!supportsMerge) return;
        // 子类重写
    }
    
    @Override
    public void onRender(DamageNumberParticle particle, RenderContext renderContext) {
        // 默认渲染，子类可重写
    }
    
    protected ModConfigs getConfig() {
        return ModConfigs.get();
    }
}