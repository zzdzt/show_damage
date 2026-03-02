package com.zzdzt.show_damage.client;

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

import java.util.*;

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = "show_damage", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ClientEventHandler {

    private static final Random RANDOM = new Random();
    private static final Map<LivingEntity, Float> lastHealthMap = new WeakHashMap<>();
    
    private static class MergeSession {
        DamageNumberParticle particle;
        float totalDamage;
        long lastHitTime;
        Vec3 lastEntityPos;  // 记录上次位置用于计算速度
    }
    
    private static final Map<LivingEntity, MergeSession> mergeSessions = new HashMap<>();

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        if (!event.getEntity().level().isClientSide()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        LivingEntity entity = event.getEntity();
        
        // 跳过玩家自己（第一人称不显示自己的伤害）
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
            // 治疗或回血，更新记录但不生成粒子
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
                    // 释放时也使用合并位置
                    Vec3 currentPos = getMergeDisplayPos(entity, mc.gameRenderer.getMainCamera());
                    session.particle.setPosition(currentPos.x, currentPos.y, currentPos.z);
                }
                
                it.remove();
                continue;
            }
            
            // 更新跟随位置 - 使用合并专用的高位置
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
        
        // 生成时距离检查
        double maxDistSqr = config.physics.getMaxDisplayDistanceSqr();
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
        
        if (session != null && session.particle.isAlive()) {
            // 合并伤害
            session.totalDamage += damage;
            session.lastHitTime = now;
            
            // 更新显示内容，触发弹跳动画
            session.particle.updateContent(
                formatDamage(session.totalDamage),
                calculateColor(session.totalDamage, config),
                calculateScale(session.totalDamage, config),
                p.getLifetime(),
                p.getFadeStartRatio()
            );
            
        } else {
            // 创建新的合并粒子
            Camera camera = mc.gameRenderer.getMainCamera();
            Vec3 pos = getMergeDisplayPos(target, camera);
            
            DamageNumberParticle particle = new DamageNumberParticle(
                mc.level,
                pos.x, pos.y, pos.z,
                formatDamage(damage),
                calculateColor(damage, config),
                calculateScale(damage, config),
                p.getLifetime(),
                p.getFadeStartRatio(),
                p.getGravity(),
                p.getInitialUpwardVelocity(),
                p.getHorizontalSpreadFactor() * 2.0f,
                false  // 合并模式不需要初始爆发
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
            
            // 额外抬高 0.6 格，确保在头顶上方清晰可见
            return entityPos.add(offset).add(0, 0.3, 0);
    }

    private static void spawnIndependentParticle(Minecraft mc, LivingEntity target, float damage, ModConfigs config) {
        ModConfigs.PhysicsConfig p = config.physics;
        
        // 使用相机偏移计算生成位置
        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 pos = getDisplayPos(target, camera);
        
        // 添加一点随机偏移，避免多个数字完全重叠
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
            p.getLifetime(),
            p.getFadeStartRatio(),
            p.getGravity(),
            p.getInitialUpwardVelocity(),
            p.getHorizontalSpreadFactor() * 2.0f,
            true  // 独立模式需要初始爆发
        );
        
        mc.particleEngine.add(particle);
    }

    /**
     * 计算显示位置 - 采用 ToroHealth 的相机偏移策略
     */
    private static Vec3 getDisplayPos(LivingEntity entity, Camera camera) {
        // 实体中心偏上位置
        Vec3 entityPos = entity.position().add(0, entity.getBbHeight() * 0.8, 0);
        
        if (camera == null) {
            return entityPos.add(0, 0.5, 0);
        }
        
        // 计算朝向相机的偏移
        Vec3 cameraPos = camera.getPosition();
        double offsetDist = entity.getBbWidth() * 0.6;
        
        // 从实体指向相机的向量，归一化后乘以偏移距离
        Vec3 toCamera = cameraPos.subtract(entityPos).normalize();
        Vec3 offset = toCamera.scale(offsetDist);
        
        // 在朝向相机的一侧生成，稍微偏上一点
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