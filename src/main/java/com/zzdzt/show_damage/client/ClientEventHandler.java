package com.zzdzt.show_damage.client;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.WeakHashMap;

import com.zzdzt.show_damage.config.ModConfigs;
import com.zzdzt.show_damage.util.Color;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = "show_damage", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ClientEventHandler {

    private static final Random RANDOM = new Random();
    
    private static final Map<LivingEntity, Float> lastHealthMap = new WeakHashMap<>();
    
    private static final Map<LivingEntity, Float> damageBuffer = new HashMap<>();
    // Key: 实体, Value: 上次受伤时间
    private static final Map<LivingEntity, Long> lastHitTime = new HashMap<>();
    // Key: 实体, Value: 当前显示的合并模式粒子
    private static final Map<LivingEntity, DamageNumberParticle> activeMergeParticles = new HashMap<>();

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        if (!event.getEntity().level().isClientSide()) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }

        LivingEntity entity = event.getEntity();
        
        // 跳过玩家自己
        if (entity instanceof Player player && mc.player != null 
                && player.getUUID().equals(mc.player.getUUID())) {
            return;
        }

        float currentHealth = entity.getHealth();
        //立即获取lastHealth，确保实体还被引用
        Float lastHealth = lastHealthMap.get(entity);
        
        if (lastHealth == null) {
            lastHealthMap.put(entity, currentHealth);
            return;
        }
        
        boolean healthChanged = false;
        float damage = 0;
        
        if (currentHealth < lastHealth) {

            damage = lastHealth - currentHealth;
            healthChanged = true;      
            lastHealthMap.put(entity, currentHealth);
            processDamage(mc, entity, damage);
            
        } else if (currentHealth > lastHealth) {
            lastHealthMap.put(entity, currentHealth);
            healthChanged = true;
        }
        
        if (!entity.isAlive()) {
            handleEntityDeath(entity);
        }
    }

    @SubscribeEvent
    public static void onEntityLeave(EntityLeaveLevelEvent event) {
        if (event.getEntity() instanceof LivingEntity entity) {
            cleanupAllEntityData(entity, false);
        }
    }
    
    private static void processDamage(Minecraft mc, LivingEntity entity, float damage) {
        ModConfigs config = ModConfigs.get();
        if (!config.isEnabled) {
            return;
        }
        
        // 距离检查
        double maxDistSqr = config.physics.getMaxDisplayDistanceSqr();
        if (mc.player != null && mc.player.distanceToSqr(entity) > maxDistSqr) {
            return;
        }

        // 处理伤害显示
        if (config.physics.isMergeEnabled()) {
            handleRealTimeMerge(mc, entity, damage, config);
        } else {
            spawnParticle(mc, entity, damage, config, false);
        }
    }

    private static void handleRealTimeMerge(Minecraft mc, LivingEntity target, float amount, ModConfigs config) {

        long now = System.currentTimeMillis();
        long timeout = config.physics.getMergeTimeout();
        Long lastTime = lastHitTime.get(target);
        
        if (lastTime != null && (now - lastTime) >= timeout) {
            DamageNumberParticle oldParticle = activeMergeParticles.remove(target);
            if (oldParticle != null && oldParticle.isAlive()) {
                oldParticle.unfreeze();
            }
            
            damageBuffer.remove(target);
        }

        float currentTotal = damageBuffer.getOrDefault(target, 0.0f) + amount;
        damageBuffer.put(target, currentTotal);
        lastHitTime.put(target, now);

        int colorRgb = calculateColor(currentTotal, config);
        float scale = calculateScale(currentTotal, config);
        ModConfigs.PhysicsConfig p = config.physics;

        DamageNumberParticle existing = activeMergeParticles.get(target);

        if (existing != null && existing.isAlive()) {
            existing.updateDamage(
                currentTotal, 
                colorRgb, 
                scale,
                p.getGravity(),
                p.getInitialUpwardVelocity(),
                p.getLifetime(),
                p.getFadeStartRatio()
            );
            if (!existing.isFrozen()) {
                existing.freeze();
            }
        } else {
            DamageNumberParticle particle = spawnParticle(mc, target, currentTotal, config, true);
            if (particle != null) {
                activeMergeParticles.put(target, particle);
            }
        }
        
        cleanupExpired(config, now);
    }

    private static int calculateColor(float damage, ModConfigs config) {
        Color cSm = new Color(config.colorSmall | 0xFF000000);
        Color cMd = new Color(config.colorMedium | 0xFF000000);
        Color cLg = new Color(config.colorLarge | 0xFF000000);

        if (damage < config.smallDamageThreshold) {
            return config.colorSmall;
        } else if (damage < config.mediumDamageThreshold) {
            float t = (damage - config.smallDamageThreshold) / (config.mediumDamageThreshold - config.smallDamageThreshold);
            return Color.lerp(cSm, cMd, t).getRGB();
        } else {
            return config.colorLarge;
        }
    }

    private static float calculateScale(float damage, ModConfigs config) {
        if (damage < config.smallDamageThreshold) {
            return config.getActualScaleSmall();
        } else if (damage < config.mediumDamageThreshold) {
            return config.getActualScaleMedium();
        } else {
            return config.getActualScaleLarge();
        }
    }

    private static DamageNumberParticle spawnParticle(Minecraft mc, LivingEntity target, float damage, 
                                                      ModConfigs config, boolean isMerged) {
        if (damage <= 0) return null;

        String text = (damage == (int) damage) ? String.valueOf((int) damage) : String.format("%.1f", damage);
        int colorRgb = calculateColor(damage, config);
        float scale = calculateScale(damage, config);

        ModConfigs.PhysicsConfig p = config.physics;
        
        double rx = 0, rz = 0;
        if (!isMerged) {
            float spread = p.getHorizontalSpreadFactor() * 2.0f;
            rx = (RANDOM.nextDouble() - 0.5) * spread;
            rz = (RANDOM.nextDouble() - 0.5) * spread;
        }

        DamageNumberParticle particle = new DamageNumberParticle(
            mc.level,
            target, 
            target.getX() + rx,
            target.getY() + target.getBbHeight() * 1.1,
            target.getZ() + rz,
            text, colorRgb, scale,
            p.getGravity(), p.getInitialUpwardVelocity(), 
            isMerged ? 0 : p.getHorizontalSpreadFactor() * 2.0f,
            p.getLifetime(), p.getFadeStartRatio(),
            isMerged
        );
        
        mc.particleEngine.add(particle);
        return particle;
    }

    /**
     * 清理过期数据 - 只清理真正超时的，不清理死亡的（死亡由handleEntityDeath处理）
     */
    private static void cleanupExpired(ModConfigs config, long now) {
        long timeout = config.physics.getMergeTimeout();
        
        // 清理damageBuffer中的过期条目
        Iterator<Map.Entry<LivingEntity, Float>> bufferIt = damageBuffer.entrySet().iterator();
        while (bufferIt.hasNext()) {
            Map.Entry<LivingEntity, Float> entry = bufferIt.next();
            LivingEntity entity = entry.getKey();
            
            // 跳过null或已死亡的实体（由handleEntityDeath处理）
            if (entity == null || !entity.isAlive()) {
                continue;
            }
            
            Long time = lastHitTime.get(entity);
            if (time != null && (now - time) >= timeout * 3) {
                bufferIt.remove();
                lastHitTime.remove(entity);
                
                DamageNumberParticle particle = activeMergeParticles.remove(entity);
                if (particle != null && particle.isAlive()) {
                    particle.unfreeze();
                }
            }
        }
    }
    
    /**
     * 处理实体死亡 - 解冻粒子让玩家看到最终伤害
     */
    private static void handleEntityDeath(LivingEntity entity) {
        // 实体已死亡，不需要再追踪生命值
        lastHealthMap.remove(entity);
        
        damageBuffer.remove(entity);
        lastHitTime.remove(entity);
        
        // 解冻合并粒子，让玩家看到最终总伤害数字飘落
        DamageNumberParticle particle = activeMergeParticles.remove(entity);
        if (particle != null && particle.isAlive()) {
            particle.unfreeze();
        }
    }
    
    /**
     * 完全清理实体所有数据（用于实体离开世界）
     */
    private static void cleanupAllEntityData(LivingEntity entity, boolean unfreezeParticle) {
        lastHealthMap.remove(entity);
        damageBuffer.remove(entity);
        lastHitTime.remove(entity);
        
        DamageNumberParticle particle = activeMergeParticles.remove(entity);
        if (particle != null && particle.isAlive() && unfreezeParticle) {
            particle.unfreeze();
        }
    }
}