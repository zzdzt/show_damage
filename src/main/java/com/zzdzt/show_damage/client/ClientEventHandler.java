package com.zzdzt.show_damage.client;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.WeakHashMap;

import com.zzdzt.show_damage.config.ModConfigs;
import com.zzdzt.show_damage.util.Color;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = "show_damage", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ClientEventHandler {

    private static final Random RANDOM = new Random();
    private static final Map<LivingEntity, Float> lastHealthMap = new WeakHashMap<>();
    
    private static class MergeSession {
        DamageNumberParticle particle;
        float totalDamage;
        long lastHitTime;
        Vec3 lastEntityPos; 
    }
    
    private static final Map<LivingEntity, MergeSession> mergeSessions = new HashMap<>();

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        if (!event.getEntity().level().isClientSide()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        LivingEntity entity = event.getEntity();
        
        if (entity instanceof Player player && mc.player != null 
                && player.getUUID().equals(mc.player.getUUID())) {
            return;
        }

        float currentHealth = entity.getHealth();
        Float lastHealth = lastHealthMap.get(entity);
        
        if (lastHealth == null) {
            lastHealthMap.put(entity, currentHealth);
            return;
        }
        
        if (currentHealth < lastHealth) {
            float damage = lastHealth - currentHealth;
            lastHealthMap.put(entity, currentHealth);
            processDamage(mc, entity, damage);
        } else if (currentHealth > lastHealth) {
            lastHealthMap.put(entity, currentHealth);
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        
        ModConfigs config = ModConfigs.get();
        long now = System.currentTimeMillis();
        
        Iterator<Map.Entry<LivingEntity, MergeSession>> it = mergeSessions.entrySet().iterator();
        
        while (it.hasNext()) {
            Map.Entry<LivingEntity, MergeSession> entry = it.next();
            LivingEntity entity = entry.getKey();
            MergeSession session = entry.getValue();
            
            boolean timeout = (now - session.lastHitTime) >= config.physics.getMergeTimeout();
            boolean dead = !entity.isAlive();
            boolean particleDead = !session.particle.isAlive();
            
            if (timeout || dead || particleDead) {
                session.particle.setFollowing(false);
                
                if (timeout && !dead && entity.isAlive()) {
                    Vec3 currentPos = getMergeDisplayPos(entity, mc.gameRenderer.getMainCamera());
                    session.particle.setPosition(currentPos.x, currentPos.y, currentPos.z);
                }
                
                it.remove();
                continue;
            }
            
            Vec3 pos = getMergeDisplayPos(entity, mc.gameRenderer.getMainCamera());
            session.particle.setPosition(pos.x, pos.y, pos.z);
            session.lastEntityPos = entity.position();
        }
    }

    @SubscribeEvent
    public static void onEntityLeave(EntityLeaveLevelEvent event) {
        if (event.getEntity() instanceof LivingEntity entity) {
            cleanupEntity(entity);
        }
    }
    
    private static void processDamage(Minecraft mc, LivingEntity entity, float damage) {
        ModConfigs config = ModConfigs.get();
        if (!config.isEnabled) return;
        
        double maxDistSqr = config.performance.getMaxDisplayDistanceSqr();
        if (mc.player != null && mc.player.distanceToSqr(entity) > maxDistSqr) {
            return;
        }

        if (config.physics.isMergeEnabled()) {
            handleMerge(mc, entity, damage, config);
        } else {
            spawnIndependentParticle(mc, entity, damage, config);
        }
    }

    private static void handleMerge(Minecraft mc, LivingEntity target, float damage, ModConfigs config) {
        long now = System.currentTimeMillis();
        MergeSession session = mergeSessions.get(target);
        ModConfigs.PhysicsConfig p = config.physics;
        ModConfigs.RenderingConfig r = config.rendering;
        ModConfigs.PerformanceConfig pf = config.performance;
        
        if (session != null && session.particle.isAlive()) {
            session.totalDamage += damage;
            session.lastHitTime = now;
            
            session.particle.updateContent(
                formatDamage(session.totalDamage),
                calculateColor(session.totalDamage, config),
                calculateScale(session.totalDamage, config),
                pf.getLifetime(),
                pf.getFadeStartRatio()
            );
            
        } else {
            Camera camera = mc.gameRenderer.getMainCamera();
            Vec3 pos = getMergeDisplayPos(target, camera);
            
            DamageNumberParticle particle = new DamageNumberParticle(
                mc.level,
                pos.x, pos.y, pos.z,
                formatDamage(damage),
                calculateColor(damage, config),
                calculateScale(damage, config),
                pf.getLifetime(),
                pf.getFadeStartRatio(),
                p.getGravity(),
                p.getInitialUpwardVelocity(),
                p.getHorizontalSpreadFactor() * 2.0f,
                false,
                r.isShadowEnabled(),
                r.getShadowOffsetX(),
                r.getShadowOffsetY(),
                r.shadowColor,
                r.getTextAlpha(),      
                r.getShadowAlpha()  
            );
            
            particle.setFollowing(true);
            mc.particleEngine.add(particle);
            
            MergeSession newSession = new MergeSession();
            newSession.particle = particle;
            newSession.totalDamage = damage;
            newSession.lastHitTime = now;
            newSession.lastEntityPos = target.position();
            mergeSessions.put(target, newSession);
        }
    }

    private static Vec3 getMergeDisplayPos(LivingEntity entity, Camera camera) {
        Vec3 entityPos = entity.position().add(0, entity.getBbHeight() * 1.1, 0);
            
        if (camera == null) {
            return entityPos.add(0, 0.8, 0);
        }
        
        Vec3 cameraPos = camera.getPosition();
        double offsetDist = entity.getBbWidth() * 0.5;
        
        Vec3 toCamera = cameraPos.subtract(entityPos).normalize();
        Vec3 offset = toCamera.scale(offsetDist);
        
        return entityPos.add(offset).add(0, 0.3, 0);
    }

    private static void spawnIndependentParticle(Minecraft mc, LivingEntity target, float damage, ModConfigs config) {
        ModConfigs.PhysicsConfig p = config.physics;
        ModConfigs.RenderingConfig r = config.rendering;
        ModConfigs.PerformanceConfig pf = config.performance;
        
        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 pos = getDisplayPos(target, camera);
        
        double spread = p.getHorizontalSpreadFactor();
        pos = pos.add(
            (RANDOM.nextDouble() - 0.5) * spread * 0.5,
            (RANDOM.nextDouble() - 0.5) * 0.1,
            (RANDOM.nextDouble() - 0.5) * spread * 0.5
        );
        
        DamageNumberParticle particle = new DamageNumberParticle(
            mc.level,
            pos.x, pos.y, pos.z,
            formatDamage(damage),
            calculateColor(damage, config),
            calculateScale(damage, config),
            pf.getLifetime(),
            pf.getFadeStartRatio(),
            p.getGravity(),
            p.getInitialUpwardVelocity(),
            p.getHorizontalSpreadFactor() * 2.0f,
            true,
            r.isShadowEnabled(),
            r.getShadowOffsetX(),
            r.getShadowOffsetY(),
            r.shadowColor,
            r.getTextAlpha(),      
            r.getShadowAlpha()
        );
        
        mc.particleEngine.add(particle);
    }

    private static Vec3 getDisplayPos(LivingEntity entity, Camera camera) {
        Vec3 entityPos = entity.position().add(0, entity.getBbHeight() * 0.8, 0);
        
        if (camera == null) {
            return entityPos.add(0, 0.5, 0);
        }
        
        Vec3 cameraPos = camera.getPosition();
        double offsetDist = entity.getBbWidth() * 0.6;
        
        Vec3 toCamera = cameraPos.subtract(entityPos).normalize();
        Vec3 offset = toCamera.scale(offsetDist);
        
        return entityPos.add(offset).add(0, 0.2, 0);
    }

    private static String formatDamage(float damage) {
        return (damage == (int) damage) ? String.valueOf((int) damage) : String.format("%.1f", damage);
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

    private static void cleanupEntity(LivingEntity entity) {
        lastHealthMap.remove(entity);
        MergeSession session = mergeSessions.remove(entity);
        if (session != null && session.particle.isAlive()) {
            session.particle.setFollowing(false);
        }
    }
}