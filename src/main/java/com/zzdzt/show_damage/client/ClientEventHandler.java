package com.zzdzt.show_damage.client;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;

import com.zzdzt.show_damage.config.ModConfigs;
import com.zzdzt.show_damage.util.Color;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = "show_damage", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ClientEventHandler {

    private static final Random RANDOM = new Random();
    // Key: 实体, Value: 当前累加的总伤害
    private static final Map<LivingEntity, Float> damageBuffer = new HashMap<>();
    // Key: 实体, Value: 上次受伤时间
    private static final Map<LivingEntity, Long> lastHitTime = new HashMap<>();

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
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
            spawnParticle(mc, target, amount, config);
        }
    }

    private static void handleRealTimeMerge(Minecraft mc, LivingEntity target, float amount, ModConfigs config) {
        long now = System.currentTimeMillis();
        long timeout = config.physics.getMergeTimeout();

        // 检查是否超时
        Long lastTime = lastHitTime.get(target);
        if (lastTime != null && (now - lastTime) >= timeout) {
            damageBuffer.remove(target);
            DamageNumberParticle.removeParticlesForEntity(target);
        }

        float currentTotal = damageBuffer.getOrDefault(target, 0.0f) + amount;
        damageBuffer.put(target, currentTotal);

        lastHitTime.put(target, now);

        // 在生成新数字前，移除该实体身上所有旧的伤害数字
        DamageNumberParticle.removeParticlesForEntity(target);

        //显示当前的累加总值
        spawnParticle(mc, target, currentTotal, config);
        
        //定期清理过期数据 
        cleanupExpired(mc, config, now);
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

            if (entity == null || !entity.isAlive() || (time != null && (now - time) >= timeout * 2)) {
                it.remove();
                lastHitTime.remove(entity);
                DamageNumberParticle.removeParticlesForEntity(entity);
            }
        }
    }

    private static void spawnParticle(Minecraft mc, LivingEntity target, float damage, ModConfigs config) {
        if (damage <= 0) return;

        String text = (damage == (int) damage) ? String.valueOf((int) damage) : String.format("%.1f", damage);
        
        Color cSm = new Color(config.colorSmall | 0xFF000000);
        Color cMd = new Color(config.colorMedium | 0xFF000000);
        Color cLg = new Color(config.colorLarge | 0xFF000000);

        int colorRgb;
        float scale;

        if (damage < config.smallDamageThreshold) {
            colorRgb = config.colorSmall;
            scale = config.getActualScaleSmall();
        } else if (damage < config.mediumDamageThreshold) {
            float t = (damage - config.smallDamageThreshold) / (config.mediumDamageThreshold - config.smallDamageThreshold);
            colorRgb = Color.lerp(cSm, cMd, t).getRGB();
            scale = config.getActualScaleMedium();
        } else {
            colorRgb = config.colorLarge;
            scale = config.getActualScaleLarge();
        }

        ModConfigs.PhysicsConfig p = config.physics;
        float spread = p.getHorizontalSpreadFactor() * 2.0f;
        double rx = (RANDOM.nextDouble() - 0.5) * spread;
        double rz = (RANDOM.nextDouble() - 0.5) * spread;


        DamageNumberParticle particle = new DamageNumberParticle(
            mc.level,
            target, 
            target.getX() + rx,
            target.getY() + target.getBbHeight() * 1.1,
            target.getZ() + rz,
            text, colorRgb, scale,
            p.getGravity(), p.getInitialUpwardVelocity(), spread,
            p.getLifetime(), p.getFadeStartRatio()
        );
        mc.particleEngine.add(particle);
    }
}