// ClientEventHandler.java - 修改后的事件处理器
package com.zzdzt.show_damage.client;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.WeakHashMap;

import com.zzdzt.show_damage.client.style.DamageStyle;
import com.zzdzt.show_damage.client.style.IndependentStyle;
import com.zzdzt.show_damage.client.style.MergeStyle;
import com.zzdzt.show_damage.client.style.StyleManager;
import com.zzdzt.show_damage.client.style.StyleRegistry;
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
        DamageStyle.Context context;
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
                    Vec3 currentPos = getMergeDisplayPos(entity, mc.gameRenderer.getMainCamera(), 
                        (MergeStyle) session.particle.getStyle());
                    session.particle.setPosition(currentPos.x, currentPos.y, currentPos.z);
                }
                
                it.remove();
                continue;
            }
            
            MergeStyle style = (MergeStyle) session.particle.getStyle();
            Vec3 pos = getMergeDisplayPos(entity, mc.gameRenderer.getMainCamera(), style);
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

        DamageStyle.Context context = new DamageStyle.Context(entity, damage, false,
            config.performance.getLifetime());

        if (config.physics.isMergeEnabled()) {
            handleMerge(mc, entity, damage, config, context);
        } else {
            spawnIndependentParticle(mc, entity, damage, config, context);
        }
    }

    private static void handleMerge(Minecraft mc, LivingEntity target, float damage, 
                                   ModConfigs config, DamageStyle.Context context) {
        long now = System.currentTimeMillis();
        MergeSession session = mergeSessions.get(target);
        
        ModConfigs.PhysicsConfig p = config.physics;
        ModConfigs.RenderingConfig r = config.rendering;
        ModConfigs.PerformanceConfig pf = config.performance;
        
        MergeStyle style = StyleManager.INSTANCE.getCurrentMergeStyle();
        if (style == null) style = StyleRegistry.getMergeStyle(StyleRegistry.DEFAULT_MERGE_STYLE);
        
        if (session != null && session.particle.isAlive()) {
            session.totalDamage += damage;
            session.lastHitTime = now;
            
            session.particle.updateContent(
                formatDamage(session.totalDamage),
                calculateColor(session.totalDamage, config),
                calculateScale(session.totalDamage, config),
                pf.getLifetime(),
                pf.getFadeStartRatio(),
                session.totalDamage
            );
            
        } else {
            Camera camera = mc.gameRenderer.getMainCamera();
            Vec3 pos = getMergeDisplayPos(target, camera, style);
            
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
                r.isShadowEnabled(),
                r.getShadowOffsetX(),
                r.getShadowOffsetY(),
                r.shadowColor,
                r.getTextAlpha(),
                r.getShadowAlpha(),
                style,
                context
            );
            
            particle.setFollowing(true);
            mc.particleEngine.add(particle);
            
            MergeSession newSession = new MergeSession();
            newSession.particle = particle;
            newSession.totalDamage = damage;
            newSession.lastHitTime = now;
            newSession.lastEntityPos = target.position();
            newSession.context = context;
            mergeSessions.put(target, newSession);
        }
    }

    private static Vec3 getMergeDisplayPos(LivingEntity entity, Camera camera, MergeStyle style) {
        double heightMultiplier = style != null ? style.getFollowHeightOffset() : 1.1;
        Vec3 entityPos = entity.position().add(0, entity.getBbHeight() * heightMultiplier, 0);
            
        if (camera == null) {
            return entityPos.add(0, 0.8, 0);
        }
        
        Vec3 cameraPos = camera.getPosition();
        double offsetDist = style != null ? style.getFollowDistanceOffset() : entity.getBbWidth() * 0.5;
        double horizontalOffset = style != null ? style.getHorizontalOffset() : 0.0;
        
        Vec3 toCamera = cameraPos.subtract(entityPos).normalize();
        Vec3 right = toCamera.cross(new Vec3(0, 1, 0)).normalize();
        Vec3 offset = toCamera.scale(offsetDist).add(right.scale(horizontalOffset));;
        
        return entityPos.add(offset).add(0, 0.3, 0);
    }

    private static void spawnIndependentParticle(Minecraft mc, LivingEntity target, float damage,
                                                ModConfigs config, DamageStyle.Context context) {
        ModConfigs.PhysicsConfig p = config.physics;
        ModConfigs.RenderingConfig r = config.rendering;
        ModConfigs.PerformanceConfig pf = config.performance;
        
        IndependentStyle style = StyleManager.INSTANCE.getCurrentIndependentStyle();
        if (style == null) style = StyleRegistry.getIndependentStyle(StyleRegistry.DEFAULT_INDEPENDENT_STYLE);
        
        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 pos = getDisplayPos(target, camera, config);
        
        // 应用风格的位置偏移
        if (style != null) {
            pos = pos.add(
                (RANDOM.nextDouble() - 0.5) * 0.1,
                (RANDOM.nextDouble() - 0.5) * 0.1,
                (RANDOM.nextDouble() - 0.5) * 0.1
            );
        }

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
            r.isShadowEnabled(),
            r.getShadowOffsetX(),
            r.getShadowOffsetY(),
            r.shadowColor,
            r.getTextAlpha(),
            r.getShadowAlpha(),
            style,
            context
        );
        
        mc.particleEngine.add(particle);
    }

    private static Vec3 getDisplayPos(LivingEntity entity, Camera camera, ModConfigs config) {
        double heightMultiplier = config.physics.getIndependentHeightMultiplier();
        Vec3 entityPos = entity.position().add(0, entity.getBbHeight() * heightMultiplier, 0);
        
        if (camera == null) {
            return entityPos.add(0, 0.5, 0);
        }
        
        Vec3 cameraPos = camera.getPosition();
        double offsetDist = entity.getBbWidth() * 1.0;
        
        Vec3 toCamera = cameraPos.subtract(entityPos).normalize();
        Vec3 offset = toCamera.scale(offsetDist);
        
        return entityPos.add(offset).add(0, 0.5, 0);
    }

    private static String formatDamage(float damage) {
        ModConfigs.DisplayConfig display = ModConfigs.get().display;
        return display.customPrefix + display.formatDamage(damage) + display.customSuffix;
    }

    private static int calculateColor(float damage, ModConfigs config) {
        Color cSm = new Color(config.colorSmall | 0xFF000000);
        Color cMd = new Color(config.colorMedium | 0xFF000000);
        Color cLg = new Color(config.colorLarge | 0xFF000000);

        if (damage < config.smallDamageThreshold) {
            return config.colorSmall;
        } else if (damage < config.mediumDamageThreshold) {
            float t = (damage - config.smallDamageThreshold) / 
                     (config.mediumDamageThreshold - config.smallDamageThreshold);
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