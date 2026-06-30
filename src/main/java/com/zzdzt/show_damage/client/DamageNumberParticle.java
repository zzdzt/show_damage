// DamageNumberParticle.java - 修改后的粒子类
package com.zzdzt.show_damage.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.zzdzt.show_damage.client.style.DamageStyle;
import com.zzdzt.show_damage.client.style.IndependentStyle;
import com.zzdzt.show_damage.client.style.MergeStyle;
import com.zzdzt.show_damage.client.style.StyleManager;
import com.zzdzt.show_damage.client.style.StyleProperties;
import com.zzdzt.show_damage.config.ModConfigs;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Random;

@OnlyIn(Dist.CLIENT)
public class DamageNumberParticle extends Particle {
    private static final Random RANDOM = new Random();
    
    // 核心数据
    private String text;
    private int colorRgb;
    private float targetScale;
    private float currentScale;
    private float scaleVelocity;
    
    // 生命周期
    private int maxAge;
    private int startFadeAge;
    
    // 物理
    private double ax, ay, az;
    private double vx, vy, vz;
    private double gravity;
    private double drag;
    
    // 状态
    private boolean isFollowing = false;
    private boolean hasInitialBurst = false;
    private float rotation;
    private float rotationVelocity;
    
    // 渲染设置
    private final boolean shadowEnabled;
    private final float shadowOffsetX;
    private final float shadowOffsetY;
    private final int shadowColor;
    private final float textAlpha;
    private final float shadowAlpha;
    
    // 风格系统新增
    @Nullable
    private DamageStyle style;
    private final StyleProperties properties;
    private DamageStyle.Context spawnContext;
    private boolean isCrit;
    
    // 合并模式专用
    private int mergeCount = 0;
    private long lastMergeTime = 0;

    public DamageNumberParticle(ClientLevel level, double x, double y, double z,
                                String text, int colorRgb, float scale, 
                                int lifetime, float fadeRatio,
                                double gravity, double initialUpwardVel, 
                                double horizontalSpread, 
                                boolean shadowEnabled, float shadowOffsetX, 
                                float shadowOffsetY, int shadowColor,
                                float textAlpha, float shadowAlpha,
                                @Nullable DamageStyle style,
                                DamageStyle.Context context) {
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
        
        this.shadowEnabled = shadowEnabled;
        this.shadowOffsetX = shadowOffsetX;
        this.shadowOffsetY = shadowOffsetY;
        this.shadowColor = shadowColor & 0x00FFFFFF;
        this.textAlpha = Mth.clamp(textAlpha, 0.0f, 1.0f);
        this.shadowAlpha = Mth.clamp(shadowAlpha, 0.0f, 1.0f);
        
        // 风格系统初始化
        this.style = style;
        this.properties = new StyleProperties();
        this.spawnContext = context;
        this.isCrit = context != null && context.isCrit;
        
        // 初始化属性
        if (style != null) {
            properties.set(StyleProperties.Keys.DAMAGE, context != null ? context.damage : 0f)
                     .set(StyleProperties.Keys.SCALE, scale)
                     .set(StyleProperties.Keys.COLOR, colorRgb)
                     .set(StyleProperties.Keys.IS_CRIT, isCrit);
            
            style.onSpawn(this, context);
        }
        
        // 物理初始化
        initPhysics(initialUpwardVel, horizontalSpread);
    }
    

    private void initPhysics(double initialUpwardVel, double horizontalSpread) {
        if (style instanceof IndependentStyle independentStyle) {
            IndependentStyle.Velocity velocity = independentStyle.calculateInitialVelocity(
                properties.getFloat(StyleProperties.Keys.DAMAGE, 0), isCrit);
            this.vx = velocity.vx;
            this.vy = velocity.vy;
            this.vz = velocity.vz;
            this.rotationVelocity = independentStyle.calculateInitialRotation(
                properties.getFloat(StyleProperties.Keys.DAMAGE, 0), isCrit);
            this.hasInitialBurst = true;
            this.gravity *= independentStyle.getGravityMultiplier();
        } else if (style instanceof MergeStyle) {
            // 默认物理
            this.vx = 0;
            this.vy = 0;
            this.vz = 0;
            this.rotationVelocity = 0;
            this.rotationVelocity = 0;
            this.hasInitialBurst = false;

        } else {
            this.vx = RANDOM.nextGaussian() * horizontalSpread * 0.3;
            this.vz = RANDOM.nextGaussian() * horizontalSpread * 0.3;
            this.vy = (initialUpwardVel * 1.5) + (RANDOM.nextGaussian() * 0.02);
            this.hasInitialBurst = true;
            this.rotationVelocity = (float)(RANDOM.nextGaussian() * 2.0);
        }
        this.rotation = 0;
        this.ax = 0;
        this.ay = 0;
        this.az = 0;
    }

    public void updateContent(String newText, int newColor, float newScale, 
                                int newLifetime, float fadeRatio, float rawDamage) {
        this.text = newText;
        this.colorRgb = newColor & 0x00FFFFFF;
        this.targetScale = newScale;
        this.maxAge = newLifetime;
        this.startFadeAge = (int)(newLifetime * fadeRatio);
        this.age = 0;
        this.scaleVelocity = (newScale - this.currentScale) * 0.5f;
        this.mergeCount++;
        this.lastMergeTime = System.currentTimeMillis();
        
        properties.set(StyleProperties.Keys.DAMAGE, rawDamage)
                 .set(StyleProperties.Keys.COLOR, newColor)
                 .set(StyleProperties.Keys.SCALE, newScale)
                 .set(StyleProperties.Keys.MERGE_COUNT, mergeCount)
                 .set(StyleProperties.Keys.LAST_MERGE_TIME, lastMergeTime);
        
        if (style != null && spawnContext != null) {
            style.onMerge(this, properties.getFloat(StyleProperties.Keys.DAMAGE, 0), spawnContext);
        }
    }

    public void setFollowing(boolean following) {
        boolean wasFollowing = this.isFollowing;
        this.isFollowing = following;
        
        if (style instanceof MergeStyle mergeStyle) {
            if (!mergeStyle.shouldContinueFollowing(this, spawnContext)) {
                this.isFollowing = false;
            }
        }
        
        if (wasFollowing && !this.isFollowing) {
            // 通知风格粒子已脱离跟随
            if (style != null && spawnContext != null) {
                style.onDetach(this, spawnContext);
                // onDetach 可能已经设置了速度，标记为已有初速度，避免下方默认逻辑覆盖
                this.hasInitialBurst = true;
            }
            
            if (!hasInitialBurst) {
                // 脱离跟随时给予默认初始速度（无风格时的 fallback）
                this.vy = 0.05 + (RANDOM.nextDouble() * 0.03);
                this.vx = (RANDOM.nextDouble() - 0.5) * 0.02;
                this.vz = (RANDOM.nextDouble() - 0.5) * 0.02;
                this.hasInitialBurst = true;
            }
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

    /**
     * 仅更新当前帧坐标，不同步 xo/yo/zo。
     * 供风格在 onUpdate 中做自管理位移使用：tick() 开头已经做了 xo=x/yo=y/zo=z，
     * 所以 moveBy 对 x/y/z 的修改会在下一帧渲染时正确被 lerp(partialTick, xo, x) 插值。
     */
    public void moveBy(double dx, double dy, double dz) {
        this.x += dx;
        this.y += dy;
        this.z += dz;
    }
    
    // 风格系统 Getter/Setter
    public StyleProperties getProperties() {
        return properties;
    }
    
    public void setStyle(DamageStyle style) {
        this.style = style;
    }
    
    @Nullable
    public DamageStyle getStyle() {
        return style;
    }
    
    public void setCrit(boolean crit) {
        this.isCrit = crit;
    }
    
    public boolean isCrit() {
        return isCrit;
    }
    
    public int getMergeCount() {
        return mergeCount;
    }

    @Override
    public @NotNull ParticleRenderType getRenderType() {
        return ParticleRenderType.CUSTOM;
    }

    @Override
    public void tick() {
        // 缩放动画
        ModConfigs.AnimationConfig animConfig = ModConfigs.get().animation;
        float scaleAccel = animConfig.getScaleAcceleration();
        float scaleDecay = animConfig.getScaleVelocityDecay();
        
        float scaleDiff = targetScale - currentScale;
        scaleVelocity += scaleDiff * scaleAccel;
        scaleVelocity *= scaleDecay;
        currentScale += scaleVelocity;
        
        if (currentScale < 0) currentScale = 0;
        float maxOvershoot = animConfig.getMaxOvershootMultiplier();
        if (currentScale > targetScale * maxOvershoot) {
            currentScale = targetScale * maxOvershoot;
            scaleVelocity *= -0.5f;
        }

        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;

        // 风格更新
        if (style != null && spawnContext != null) {
            style.onUpdate(this, spawnContext);
        }

        // 物理更新
        if (!this.isFollowing) {
            updatePhysics();
        }

        this.age++;
        if (this.age >= this.maxAge) {
            this.remove();
        }
    }
    
    private void updatePhysics() {
        boolean hasPhysics = true;
        if (style instanceof IndependentStyle independentStyle) {
            hasPhysics = independentStyle.hasPhysics();
        }
        
        if (!hasPhysics) return;
        
        ModConfigs.AnimationConfig animConfig = ModConfigs.get().animation;
        float drag = animConfig.getDrag();
        double maxSpeed = animConfig.getMaxSpeed();
        float rotationDamping = animConfig.getRotationDamping();
        
        ax = -vx * (1.0 - drag) * 2.0;
        az = -vz * (1.0 - drag) * 2.0;
        
        double verticalDrag = (vy > 0) ? drag * 0.98 : drag;
        ay = -gravity - vy * (1.0 - verticalDrag) * 1.5;
        
        vx += ax;
        vy += ay;
        vz += az;
        
        vx = Mth.clamp(vx, -maxSpeed, maxSpeed);
        vy = Mth.clamp(vy, -maxSpeed, maxSpeed);
        vz = Mth.clamp(vz, -maxSpeed, maxSpeed);
        
        x += vx;
        y += vy;
        z += vz;
        
        rotation += rotationVelocity;
        rotationVelocity *= rotationDamping;
    }

    @Override
    public void render(@NotNull VertexConsumer buffer, @NotNull Camera camera, float partialTick) {
        double distSqr = camera.getPosition().distanceToSqr(this.x, this.y, this.z);
        ModConfigs config = ModConfigs.get();
        if (distSqr > config.performance.getMaxDisplayDistanceSqr()) {
            return;
        }

        double renderX = Mth.lerp(partialTick, this.xo, this.x) - camera.getPosition().x();
        double renderY = Mth.lerp(partialTick, this.yo, this.y) - camera.getPosition().y();
        double renderZ = Mth.lerp(partialTick, this.zo, this.z) - camera.getPosition().z();

        Minecraft mc = Minecraft.getInstance();
        if (mc.font == null) return;

        // 计算透明度
        float alpha;
        if (this.age < this.startFadeAge) {
            alpha = 1.0f;
        } else {
            alpha = 1.0f - ((float)this.age - this.startFadeAge) / (float)(this.maxAge - this.startFadeAge);
        }
        alpha = Mth.clamp(alpha, 0.0f, 1.0f);
        
        // 构建渲染上下文
        DamageStyle.RenderContext renderContext = new DamageStyle.RenderContext(
            partialTick, renderX, renderY, renderZ, alpha, this.age, this.maxAge
        );
        
        // 调用风格渲染
        if (style != null) {
            style.onRender(this, renderContext);
        }
        
        // 默认渲染
        renderDefault(mc, camera, partialTick, renderX, renderY, renderZ, alpha);
    }
    
    private void renderDefault(Minecraft mc, Camera camera, float partialTick,
                              double renderX, double renderY, double renderZ, float alpha) {
        ModConfigs.RenderingConfig renderConfig = ModConfigs.get().rendering;

        net.minecraft.network.chat.Style style = net.minecraft.network.chat.Style.EMPTY;
        
        if (renderConfig.useCustomFont && FontManager.INSTANCE.isUsingCustomFont()) {
            style = style.withFont(FontManager.CUSTOM_FONT);
        } else if (!renderConfig.useCustomFont) {
            ResourceLocation forcedFont = new ResourceLocation(
                renderConfig.forcedFontNamespace, 
                renderConfig.forcedFontPath
            );
            style = style.withFont(forcedFont);
        }

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

        float finalTextAlpha = alpha * this.textAlpha;
        float finalShadowAlpha = alpha * this.shadowAlpha;

        int textAlphaInt = (int)(finalTextAlpha * 255.0f) << 24;
        int shadowAlphaInt = (int)(finalShadowAlpha * 255.0f) << 24;
        int colorWithAlpha = textAlphaInt | this.colorRgb;
        
        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
        net.minecraft.network.chat.Component textComponent = 
            net.minecraft.network.chat.Component.literal(this.text).setStyle(style);

        float width = mc.font.width(textComponent);
        float xOffset = -width / 2.0f;
        
        if (this.shadowEnabled) {
            int shadowColorWithAlpha = shadowAlphaInt | this.shadowColor;
            
            poseStack.pushPose();
            poseStack.translate(0, 0, 0.001);
            
            mc.font.drawInBatch(
                textComponent,
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
        
        mc.font.drawInBatch(
            textComponent,
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
    
    // Getter 方法供风格使用
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public int getColor() { return colorRgb; }
    public void setColor(int color) { this.colorRgb = color; }
    public float getCurrentScale() { return currentScale; }
    public void setCurrentScale(float scale) { this.currentScale = scale; }
    public float getTargetScale() { return targetScale; }
    public void setTargetScale(float scale) { this.targetScale = scale; }
    public float getRotation() { return rotation; }
    public void setRotation(float rotation) { this.rotation = rotation; }
    public double getVX() { return vx; }
    public void setVX(double vx) { this.vx = vx; }
    public double getVY() { return vy; }
    public void setVY(double vy) { this.vy = vy; }
    public double getVZ() { return vz; }
    public void setVZ(double vz) { this.vz = vz; }
    public double getX() { return x; }
    public double getY() { return y; }
    public double getZ() { return z; }
    public double getPrevX() { return xo; }
    public double getPrevY() { return yo; }
    public double getPrevZ() { return zo; }
    public int getAge() { return age; }
    public int getMaxAge() { return maxAge; }
    public void setMaxAge(int maxAge) { this.maxAge = maxAge; }
    public float getScaleVelocity() { return scaleVelocity; }
    public void setScaleVelocity(float velocity) { this.scaleVelocity = velocity; }
}