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
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = "show_damage", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ClientEventHandler {

    private static final Random RANDOM = new Random();
    // Key: 实体, Value: 当前累加的总伤害
    private static final Map<LivingEntity, Float> damageBuffer = new HashMap<>();
    // Key: 实体, Value: 上次受伤时间
    private static final Map<LivingEntity, Long> lastHitTime = new HashMap<>();
    // Key: 实体, Value: 当前显示的合并模式粒子
    private static final Map<LivingEntity, DamageNumberParticle> activeMergeParticles = new WeakHashMap<>();

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {

        if (FMLEnvironment.dist != Dist.CLIENT) return;
        Minecraft mc = Minecraft.getInstance();
        
        if (mc.level == null || !mc.level.isClientSide()) {
            return;
        }

        LivingEntity target = event.getEntity();
        float amount = event.getAmount();
        if (amount <= 0.0F) return;

        ModConfigs config = ModConfigs.get();
        if (!config.isEnabled) return;

        if (target instanceof Player player && mc.player != null && player.getUUID().equals(mc.player.getUUID())) {
            return;
        }
        
        double maxDistSqr = config.physics.getMaxDisplayDistanceSqr();
        if (mc.player != null && mc.player.distanceToSqr(target) > maxDistSqr) {
            return;
        }

        if (config.physics.isMergeEnabled()) {
            handleRealTimeMerge(mc, target, amount, config);
        } else {
            spawnParticle(mc, target, amount, config, false);
        }
    }

    @SubscribeEvent
    public static void onEntityLeave(EntityLeaveLevelEvent event) {
        if (event.getEntity() instanceof LivingEntity entity) {
            damageBuffer.remove(entity);
            lastHitTime.remove(entity);
            
            DamageNumberParticle particle = activeMergeParticles.remove(entity);
            if (particle != null && particle.isAlive()) {
                particle.unfreeze();
            }
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
            lastHitTime.remove(target);
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
        
        cleanupExpired(mc, config, now);
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
     * 清理超时太久的数据 ，防止内存泄漏
     */
    private static void cleanupExpired(Minecraft mc, ModConfigs config, long now) {
        long timeout = config.physics.getMergeTimeout();
        Iterator<Map.Entry<LivingEntity, Float>> it = damageBuffer.entrySet().iterator();
        
        while (it.hasNext()) {
            Map.Entry<LivingEntity, Float> entry = it.next();
            LivingEntity entity = entry.getKey();
            Long time = lastHitTime.get(entity);

            if (entity == null || !entity.isAlive() || (time != null && (now - time) >= timeout * 3)) {
                it.remove();
                lastHitTime.remove(entity);
                
                DamageNumberParticle particle = activeMergeParticles.remove(entity);
                if (particle != null && particle.isAlive()) {
                    particle.unfreeze();
                }
                
                DamageNumberParticle.removeParticlesForEntity(entity);
            }
        }
    }
}