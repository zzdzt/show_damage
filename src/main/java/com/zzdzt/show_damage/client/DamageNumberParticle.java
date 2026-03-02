package com.zzdzt.show_damage.client;

import java.util.Random;

import org.jetbrains.annotations.NotNull;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.zzdzt.show_damage.config.ModConfigs;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;

public class DamageNumberParticle extends Particle {
    private static final Random RANDOM = new Random();
    
    // 显示内容
    private String text;
    private int colorRgb;
    private float targetScale;
    private float currentScale;
    private float scaleVelocity;
    
    // 生命周期
    private int maxAge;
    private int startFadeAge;
    
    // 物理系统
    private double ax, ay, az;
    private double vx, vy, vz;
    private double gravity;
    private double drag;
    
    // 状态
    private boolean isFollowing = false;
    private boolean hasInitialBurst = false;
    
    // 渲染优化
    private float rotation;
    private float rotationVelocity;
    
    // 渲染配置
    private final boolean shadowEnabled;
    private final float shadowOffsetX;
    private final float shadowOffsetY;
    private final int shadowColor;
    private final float textAlpha;    
    private final float shadowAlpha;

    public DamageNumberParticle(ClientLevel level, double x, double y, double z,
                                String text, int colorRgb, float scale, 
                                int lifetime, float fadeRatio,
                                double gravity, double initialUpwardVel, 
                                double horizontalSpread, boolean shouldBurst,
                                boolean shadowEnabled, float shadowOffsetX, 
                                float shadowOffsetY, int shadowColor,
                                float textAlpha, float shadowAlpha) {
        super(level, x, y, z);
        this.xo = x;
        this.yo = y;
        this.zo = z;
        
        this.text = text;
        this.colorRgb = colorRgb & 0x00FFFFFF;
        this.targetScale = scale;
        this.currentScale = 0;
        this.scaleVelocity = 0;
        this.maxAge = lifetime;
        this.startFadeAge = (int)(lifetime * fadeRatio);
        this.gravity = gravity;
        this.drag = 0.92;
        this.hasPhysics = false;
        
        // 保存渲染配置（新增）
        this.shadowEnabled = shadowEnabled;
        this.shadowOffsetX = shadowOffsetX;
        this.shadowOffsetY = shadowOffsetY;
        this.shadowColor = shadowColor & 0x00FFFFFF;
        this.textAlpha = Mth.clamp(textAlpha, 0.0f, 1.0f);      
        this.shadowAlpha = Mth.clamp(shadowAlpha, 0.0f, 1.0f); 
        
        if (shouldBurst) {
            this.vx = RANDOM.nextGaussian() * horizontalSpread * 0.3;
            this.vz = RANDOM.nextGaussian() * horizontalSpread * 0.3;
            this.vy = initialUpwardVel + (RANDOM.nextGaussian() * 0.02);
            this.hasInitialBurst = true;
            this.rotationVelocity = (float)(RANDOM.nextGaussian() * 2.0);
        } else {
            this.vx = 0;
            this.vy = 0;
            this.vz = 0;
            this.rotationVelocity = 0;
        }
        this.rotation = 0;
        
        this.ax = 0;
        this.ay = 0;
        this.az = 0;
    }

    public void updateContent(String newText, int newColor, float newScale, 
                             int newLifetime, float fadeRatio) {
        this.text = newText;
        this.colorRgb = newColor & 0x00FFFFFF;
        this.targetScale = newScale;
        this.maxAge = newLifetime;
        this.startFadeAge = (int)(newLifetime * fadeRatio);
        this.age = 0;
        this.scaleVelocity = (newScale - this.currentScale) * 0.5f;
    }

    public void setFollowing(boolean following) {
        boolean wasFollowing = this.isFollowing;
        this.isFollowing = following;
        
        if (wasFollowing && !following && !hasInitialBurst) {
            this.vy = 0.05 + (RANDOM.nextDouble() * 0.03);
            this.vx = (RANDOM.nextDouble() - 0.5) * 0.02;
            this.vz = (RANDOM.nextDouble() - 0.5) * 0.02;
        }
    }

    public boolean isFollowing() {
        return this.isFollowing;
    }

    public void setPosition(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
        if (!isFollowing) {
            this.xo = x;
            this.yo = y;
            this.zo = z;
        }
    }

    @Override
    public @NotNull ParticleRenderType getRenderType() {
        return ParticleRenderType.CUSTOM;
    }

    @Override
    public void tick() {
        float scaleDiff = targetScale - currentScale;
        scaleVelocity += scaleDiff * 0.4f;
        scaleVelocity *= 0.65f;
        currentScale += scaleVelocity;
        
        if (currentScale < 0) currentScale = 0;
        if (currentScale > targetScale * 1.5f) {
            currentScale = targetScale * 1.5f;
            scaleVelocity *= -0.5f;
        }

        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;

        if (this.isFollowing) {
            // 跟随模式：位置由外部设置
        } else {
            ax = -vx * (1.0 - drag) * 2.0;
            az = -vz * (1.0 - drag) * 2.0;
            
            double verticalDrag = (vy > 0) ? drag * 0.98 : drag;
            ay = -gravity - vy * (1.0 - verticalDrag) * 1.5;
            
            vx += ax;
            vy += ay;
            vz += az;
            
            double maxSpeed = 2.0;
            vx = Mth.clamp(vx, -maxSpeed, maxSpeed);
            vy = Mth.clamp(vy, -maxSpeed, maxSpeed);
            vz = Mth.clamp(vz, -maxSpeed, maxSpeed);
            
            x += vx;
            y += vy;
            z += vz;
            
            rotation += rotationVelocity;
            rotationVelocity *= 0.95f;
        }

        this.age++;
        if (this.age >= this.maxAge) {
            this.remove();
        }
    }

    @Override
    public void render(@NotNull VertexConsumer buffer, @NotNull Camera camera, float partialTick) {
        double distSqr = camera.getPosition().distanceToSqr(this.x, this.y, this.z);
        ModConfigs config = ModConfigs.get();
        if (distSqr > config.physics.getMaxDisplayDistanceSqr()) {
            return;
        }

        double renderX = Mth.lerp(partialTick, this.xo, this.x) - camera.getPosition().x();
        double renderY = Mth.lerp(partialTick, this.yo, this.y) - camera.getPosition().y();
        double renderZ = Mth.lerp(partialTick, this.zo, this.z) - camera.getPosition().z();

        Minecraft mc = Minecraft.getInstance();
        if (mc.font == null) return;

        PoseStack poseStack = new PoseStack();
        poseStack.pushPose();
        
        poseStack.translate(renderX, renderY, renderZ);
        poseStack.mulPose(Axis.YP.rotationDegrees(-camera.getYRot()));
        poseStack.mulPose(Axis.XP.rotationDegrees(camera.getXRot()));
        
        if (Math.abs(rotation) > 0.1f) {
            poseStack.mulPose(Axis.ZP.rotationDegrees(rotation));
        }
        
        float s = -0.025f * this.currentScale;
        poseStack.scale(s, s, s);

        float alpha;
        if (this.age < this.startFadeAge) {
            alpha = 1.0f;
        } else {
            alpha = 1.0f - ((float)this.age - this.startFadeAge) / (float)(this.maxAge - this.startFadeAge);
        }
        alpha = Mth.clamp(alpha, 0.0f, 1.0f);

        float finalTextAlpha = alpha * this.textAlpha;
        float finalShadowAlpha = alpha * this.shadowAlpha;


        int textAlphaInt = (int)(finalTextAlpha * 255.0f) << 24;
        int shadowAlphaInt = (int)(finalShadowAlpha * 255.0f) << 24;
        int colorWithAlpha = textAlphaInt | this.colorRgb;

        
        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
        float width = mc.font.width(this.text);
        float xOffset = -width / 2.0f;
        
        if (this.shadowEnabled) {
            int shadowColorWithAlpha = shadowAlphaInt | this.shadowColor;
            
            poseStack.pushPose();
            poseStack.translate(0, 0, 0.001); // 阴影靠后
            
            mc.font.drawInBatch(
                this.text,
                xOffset + this.shadowOffsetX,
                this.shadowOffsetY,
                shadowColorWithAlpha,
                false,  
                poseStack.last().pose(),
                bufferSource,
                Font.DisplayMode.SEE_THROUGH,
                0,
                LightTexture.FULL_BRIGHT
            );
            
            poseStack.popPose();
        }
        
        // 主文字：Z 轴靠前（默认 0）
        mc.font.drawInBatch(
            this.text,
            xOffset,
            0.0f,
            colorWithAlpha,
            false,
            poseStack.last().pose(),
            bufferSource,
            Font.DisplayMode.SEE_THROUGH,
            0,
            LightTexture.FULL_BRIGHT
        );
        
        bufferSource.endBatch();
        poseStack.popPose();
    }

    public boolean isAlive() {
        return !this.removed && this.age < this.maxAge;
    }
}