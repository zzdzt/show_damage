package com.zzdzt.show_damage.client;

import java.util.Collections;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.WeakHashMap;

import org.jetbrains.annotations.NotNull;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public class DamageNumberParticle extends Particle {
    // 静态注册表：记录每个实体对应的所有活跃粒子
    private static final Map<LivingEntity, Set<DamageNumberParticle>> ACTIVE_PARTICLES = new WeakHashMap<>();
    private static final Random RANDOM = new Random();

    private final String text;
    private final int baseColorRgb;
    private final float scale;
    
    // 物理属性
    private final float gravity;
    private final float drag = 0.96f; // 空气阻力
    
    private final int maxAge;
    private final int startFadeAge;
    private final LivingEntity targetEntity;

    public DamageNumberParticle(ClientLevel level, LivingEntity target, double x, double y, double z, 
                                String text, int colorRgb, float scale, 
                                float gravity, float initialUpwardVel, float horizontalSpread,
                                int lifetime, float fadeRatio) {
        super(level, x, y, z);
        
        this.targetEntity = target;
        this.text = text;
        this.baseColorRgb = colorRgb & 0x00FFFFFF;
        this.scale = scale;
        this.gravity = gravity;
        this.maxAge = lifetime;
        this.lifetime = this.maxAge;
        this.hasPhysics = false;
        this.startFadeAge = (int)(this.maxAge * fadeRatio);

        // 确保数字生成在实体面向玩家的一侧，避免被模型遮挡
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            // 1. 实体中心点
            Vec3 entityLoc = target.position().add(0, target.getBbHeight() * 0.8, 0);
            // 2. 相机位置
            Vec3 cameraLoc = mc.gameRenderer.getMainCamera().getPosition();
            // 3. 计算偏移量
            double offsetDist = Math.max(0.5, target.getBbWidth() * 0.8); 
            Vec3 direction = cameraLoc.subtract(entityLoc).normalize().scale(offsetDist);
            // 4. 最终位置 = 实体中心 + 向外偏移 + 稍微抬高
            this.x = entityLoc.x + direction.x;
            this.y = entityLoc.y + direction.y + 0.5; 
            this.z = entityLoc.z + direction.z;
        } else {
            // Fallback: 如果没有玩家 (极端情况)，使用传入坐标
            this.x = x;
            this.y = y;
            this.z = z;
        }
        
        // 同步旧坐标 (用于渲染插值)
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;

        // 水平方向：高斯分布 * 扩散系数
        this.xd = (float)(RANDOM.nextGaussian() * 0.05 * horizontalSpread);
        this.zd = (float)(RANDOM.nextGaussian() * 0.05 * horizontalSpread);
        
        // 垂直方向：基础初速 + 高斯微调
        this.yd = initialUpwardVel + (float)(RANDOM.nextGaussian() * 0.05);

        // 注册到静态表
        ACTIVE_PARTICLES.computeIfAbsent(target, k -> Collections.newSetFromMap(new WeakHashMap<>())).add(this);
    }

    public LivingEntity getTargetEntity() {
        return this.targetEntity;
    }

    @Override
    public @NotNull ParticleRenderType getRenderType() {
        return ParticleRenderType.CUSTOM;
    }

    @Override
    public void render(@NotNull VertexConsumer buffer, @NotNull Camera camera, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.font == null) return;

        // 使用插值位置，使运动更平滑
        double renderX = Mth.lerp(partialTick, this.xo, this.x) - camera.getPosition().x();
        double renderY = Mth.lerp(partialTick, this.yo, this.y) - camera.getPosition().y();
        double renderZ = Mth.lerp(partialTick, this.zo, this.z) - camera.getPosition().z();

        PoseStack poseStack = new PoseStack();
        poseStack.pushPose();
        
        poseStack.translate(renderX, renderY, renderZ);
        poseStack.mulPose(camera.rotation());
        
        // 缩放 (负值是为了让文字始终面向相机)
        float s = -0.025f * scale;
        poseStack.scale(s, s, s);

        // 计算透明度 淡出效果
        float alpha;
        if (this.age < this.startFadeAge) {
            alpha = 1.0f;
        } else {
            alpha = 1.0f - ((float)this.age - this.startFadeAge) / (float)(this.lifetime - this.startFadeAge);
        }
        alpha = Mth.clamp(alpha, 0.0f, 1.0f);

        int alphaInt = (int)(alpha * 255.0f) << 24;
        int colorWithAlpha = alphaInt | this.baseColorRgb;

        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
        float width = mc.font.width(text);
        
        // 居中绘制
        mc.font.drawInBatch(
            text,
            -width / 2.0f,
            0.0f,
            colorWithAlpha,
            false,
            poseStack.last().pose(),
            bufferSource,
            Font.DisplayMode.NORMAL,
            0,
            LightTexture.FULL_BRIGHT
        );
        
        bufferSource.endBatch();
        poseStack.popPose();
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        
        this.yd -= this.gravity; 
        
        this.xd *= this.drag;
        this.zd *= this.drag;

        this.move(this.xd, this.yd, this.zd);
        
        this.age++;
        if (this.age >= this.lifetime) {
            this.remove();
        }
    }

    @Override
    public void remove() {
        super.remove();
        if (this.targetEntity != null && ACTIVE_PARTICLES.containsKey(this.targetEntity)) {
            ACTIVE_PARTICLES.get(this.targetEntity).remove(this);
        }
    }

    public static void removeParticlesForEntity(LivingEntity target) {
        Set<DamageNumberParticle> particles = ACTIVE_PARTICLES.get(target);
        if (particles != null) {
            for (DamageNumberParticle p : particles.toArray(new DamageNumberParticle[0])) {
                p.remove();
            }
            particles.clear();
        }
    }
}