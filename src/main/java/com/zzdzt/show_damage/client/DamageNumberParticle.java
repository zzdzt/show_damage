package com.zzdzt.show_damage.client;

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
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.Random;

public class DamageNumberParticle extends Particle {
    private static final Random RANDOM = new Random();
    
    // 显示内容
    private String text;
    private int colorRgb;
    private float targetScale;
    private float currentScale;
    private float scaleVelocity;  // 弹性缩放的速度
    
    // 生命周期
    private int maxAge;
    private int startFadeAge;
    
    // 物理系统 - 分离加速度、速度、位置
    private double ax, ay, az;      // 加速度
    private double vx, vy, vz;      // 速度
    private double gravity;         // 重力加速度
    private double drag;            // 空气阻力系数
    
    // 状态
    private boolean isFollowing = false;
    private boolean hasInitialBurst = false;
    
    // 渲染优化
    private float rotation;
    private float rotationVelocity;

    public DamageNumberParticle(ClientLevel level, double x, double y, double z,
                                String text, int colorRgb, float scale, 
                                int lifetime, float fadeRatio,
                                double gravity, double initialUpwardVel, 
                                double horizontalSpread, boolean shouldBurst) {
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
        this.drag = 0.92;  // 空气阻力
        this.hasPhysics = false;
        
        // 初始化速度 - 使用正态分布让运动更自然
        if (shouldBurst) {
            // 水平方向：正态分布随机，大部分集中在中间
            this.vx = RANDOM.nextGaussian() * horizontalSpread * 0.3;
            this.vz = RANDOM.nextGaussian() * horizontalSpread * 0.3;
            // 垂直方向：明确的向上初速度 + 小随机
            this.vy = initialUpwardVel + (RANDOM.nextGaussian() * 0.02);
            this.hasInitialBurst = true;
            
            // 微小的旋转
            this.rotationVelocity = (float)(RANDOM.nextGaussian() * 2.0);
        } else {
            this.vx = 0;
            this.vy = 0;
            this.vz = 0;
            this.rotationVelocity = 0;
        }
        this.rotation = 0;
        
        // 初始化加速度
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
        // 更新时给一个"弹跳"效果
        this.scaleVelocity = (newScale - this.currentScale) * 0.5f;
    }

    public void setFollowing(boolean following) {
        boolean wasFollowing = this.isFollowing;
        this.isFollowing = following;
        
        // 从跟随状态释放时，继承实体的运动
        if (wasFollowing && !following && !hasInitialBurst) {
            // 给一个小的向上弹跳，然后受重力下落
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
        // 跟随模式下不更新 xo/yo/zo，让渲染插值更平滑
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
        // 弹性缩放动画 - 弹簧物理
        float scaleDiff = targetScale - currentScale;
        scaleVelocity += scaleDiff * 0.4f;  // 弹簧力
        scaleVelocity *= 0.65f;              // 阻尼
        currentScale += scaleVelocity;
        
        // 限制缩放范围
        if (currentScale < 0) currentScale = 0;
        if (currentScale > targetScale * 1.5f) {
            currentScale = targetScale * 1.5f;
            scaleVelocity *= -0.5f;  // 撞墙反弹
        }

        // 保存上一帧位置（用于渲染插值）
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;

        if (this.isFollowing) {
            // 跟随模式：位置由外部设置，物理暂停
            // 但保留速度用于释放时的惯性
        } else {
            // 独立运动模式：完整的物理模拟
            
            // 计算加速度（阻力 + 重力）
            // 阻力与速度方向相反，大小与速度成正比
            ax = -vx * (1.0 - drag) * 2.0;  // 水平阻力
            az = -vz * (1.0 - drag) * 2.0;
            
            // 垂直方向：重力 + 阻力
            double verticalDrag = (vy > 0) ? drag * 0.98 : drag;  // 上升时阻力稍小
            ay = -gravity - vy * (1.0 - verticalDrag) * 1.5;
            
            // 更新速度（欧拉积分）
            vx += ax;
            vy += ay;
            vz += az;
            
            // 终端速度限制（避免过快）
            double maxSpeed = 2.0;
            vx = Mth.clamp(vx, -maxSpeed, maxSpeed);
            vy = Mth.clamp(vy, -maxSpeed, maxSpeed);
            vz = Mth.clamp(vz, -maxSpeed, maxSpeed);
            
            // 更新位置
            x += vx;
            y += vy;
            z += vz;
            
            // 旋转减速
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
        // 渲染时距离剔除（动态检查）
        double distSqr = camera.getPosition().distanceToSqr(this.x, this.y, this.z);
        ModConfigs  config = ModConfigs.get();
        if (distSqr > config.physics.getMaxDisplayDistanceSqr()) {
            return;
        }

        // 插值计算渲染位置
        double renderX = Mth.lerp(partialTick, this.xo, this.x) - camera.getPosition().x();
        double renderY = Mth.lerp(partialTick, this.yo, this.y) - camera.getPosition().y();
        double renderZ = Mth.lerp(partialTick, this.zo, this.z) - camera.getPosition().z();

        Minecraft mc = Minecraft.getInstance();
        if (mc.font == null) return;

        PoseStack poseStack = new PoseStack();
        poseStack.pushPose();
        
        // 移动到世界位置
        poseStack.translate(renderX, renderY, renderZ);
        
        //  billboard 效果：朝向相机
        poseStack.mulPose(Axis.YP.rotationDegrees(-camera.getYRot()));
        poseStack.mulPose(Axis.XP.rotationDegrees(camera.getXRot()));
        
        // 应用旋转
        if (Math.abs(rotation) > 0.1f) {
            poseStack.mulPose(Axis.ZP.rotationDegrees(rotation));
        }
        
        // 缩放
        float s = -0.025f * this.currentScale;
        poseStack.scale(s, s, s);

        // 计算透明度
        float alpha;
        if (this.age < this.startFadeAge) {
            alpha = 1.0f;
        } else {
            alpha = 1.0f - ((float)this.age - this.startFadeAge) / (float)(this.maxAge - this.startFadeAge);
        }
        alpha = Mth.clamp(alpha, 0.0f, 1.0f);

        // 添加描边效果增强可读性
        int alphaInt = (int)(alpha * 255.0f) << 24;
        int colorWithAlpha = alphaInt | this.colorRgb;
        
        // 阴影/描边颜色（黑色，稍大）
        int shadowColor = (alphaInt | 0x000000);

        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
        float width = mc.font.width(this.text);
        float xOffset = -width / 2.0f;
        
        // 先画阴影（偏移一点）
        mc.font.drawInBatch(
            this.text,
            xOffset + 1.0f,
            1.0f,
            shadowColor,
            false,
            poseStack.last().pose(),
            bufferSource,
            Font.DisplayMode.NORMAL,
            0,
            LightTexture.FULL_BRIGHT
        );
        
        // 再画主文字
        mc.font.drawInBatch(
            this.text,
            xOffset,
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

    public boolean isAlive() {
        return !this.removed && this.age < this.maxAge;
    }
}