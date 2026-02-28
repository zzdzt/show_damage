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
    private static final Map<LivingEntity, Set<DamageNumberParticle>> ACTIVE_PARTICLES = new WeakHashMap<>();
    private static final Random RANDOM = new Random();

    private final boolean isMergedParticle;

    private String text;
    private int baseColorRgb;
    private float scale;
    
    private float gravity;
    private float initialUpwardVel;
    private int lifetime;
    private int startFadeAge;
    
    private final float drag = 0.96f;
    private final LivingEntity targetEntity;

    private boolean isFrozen = false;
    private double offsetX, offsetY, offsetZ;

    public DamageNumberParticle(ClientLevel level, LivingEntity target, double x, double y, double z, 
                                String text, int colorRgb, float scale, 
                                float gravity, float initialUpwardVel, float horizontalSpread,
                                int lifetime, float fadeRatio, boolean isMerged) {
        super(level, x, y, z);
        
        this.isMergedParticle = isMerged;
        this.targetEntity = target;
        this.text = text;
        this.baseColorRgb = colorRgb & 0x00FFFFFF;
        this.scale = scale;
        this.gravity = gravity;
        this.initialUpwardVel = initialUpwardVel;
        this.lifetime = lifetime;
        this.hasPhysics = false;
        this.startFadeAge = (int)(lifetime * fadeRatio);

        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.gameRenderer != null) {
            Vec3 entityCenter = target.position().add(0, target.getBbHeight() * 0.8, 0);
            Vec3 cameraPos = mc.gameRenderer.getMainCamera().getPosition();
            Vec3 toCamera = cameraPos.subtract(entityCenter).normalize();
            
            double offsetDist = Math.max(0.3, target.getBbWidth() * 0.6);
            Vec3 offset = toCamera.scale(offsetDist);
            
            this.x += offset.x;
            this.y += offset.y + 0.3;  
            this.z += offset.z;
        }
        
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;

        if (isMerged) {
            this.xd = 0;
            this.zd = 0;
            this.yd = initialUpwardVel;
        } else {
            this.xd = (float)(RANDOM.nextGaussian() * 0.02 * horizontalSpread);
            this.zd = (float)(RANDOM.nextGaussian() * 0.02 * horizontalSpread);
            this.yd = initialUpwardVel + (float)(RANDOM.nextGaussian() * 0.02);
        }

        ACTIVE_PARTICLES.computeIfAbsent(target, k -> Collections.newSetFromMap(new WeakHashMap<>())).add(this);
    }

    public LivingEntity getTargetEntity() {
        return this.targetEntity;
    }
    
    public void freeze() {
        if (this.targetEntity != null) {
            this.isFrozen = true;
            this.offsetX = this.x - this.targetEntity.getX();
            this.offsetY = this.y - this.targetEntity.getY();
            this.offsetZ = this.z - this.targetEntity.getZ();
            this.xd = 0;
            this.yd = 0;
            this.zd = 0;
        }
    }
    
    public void unfreeze() {
        if (this.isFrozen) {
            this.isFrozen = false;
            this.yd = -0.05f;
        }
    }
    
    public boolean isFrozen() {
        return this.isFrozen;
    }

    @Override
    public @NotNull ParticleRenderType getRenderType() {
        return ParticleRenderType.CUSTOM;
    }

    @Override
    public void render(@NotNull VertexConsumer buffer, @NotNull Camera camera, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.font == null) return;

        double renderX, renderY, renderZ;
        
        if (isFrozen && this.targetEntity != null && this.targetEntity.isAlive()) {
            double currentX = this.targetEntity.getX() + this.offsetX;
            double currentY = this.targetEntity.getY() + this.offsetY;
            double currentZ = this.targetEntity.getZ() + this.offsetZ;
            
            renderX = currentX - camera.getPosition().x();
            renderY = currentY - camera.getPosition().y();
            renderZ = currentZ - camera.getPosition().z();
        } else {
            renderX = Mth.lerp(partialTick, this.xo, this.x) - camera.getPosition().x();
            renderY = Mth.lerp(partialTick, this.yo, this.y) - camera.getPosition().y();
            renderZ = Mth.lerp(partialTick, this.zo, this.z) - camera.getPosition().z();
        }

        PoseStack poseStack = new PoseStack();
        poseStack.pushPose();
        
        poseStack.translate(renderX, renderY, renderZ);
        poseStack.mulPose(camera.rotation());
        
        float s = -0.025f * scale;
        poseStack.scale(s, s, s);

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
        if (isFrozen) {
            if (this.targetEntity == null || !this.targetEntity.isAlive()) {
                unfreeze();
                return;
            }
            
            this.x = this.targetEntity.getX() + this.offsetX;
            this.y = this.targetEntity.getY() + this.offsetY;
            this.z = this.targetEntity.getZ() + this.offsetZ;
            
            this.xo = this.x;
            this.yo = this.y;
            this.zo = this.z;
            
            this.age++;
            if (this.age >= this.lifetime) {
                this.remove();
            }
            return;
        }
        
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


    public void updateDamage(float newDamage, int newColorRgb, float newScale,
                            float newGravity, float newInitialUpwardVel, 
                            int newLifetime, float fadeRatio) {
        this.text = (newDamage == (int) newDamage) ? String.valueOf((int) newDamage) 
                                                   : String.format("%.1f", newDamage);
        this.baseColorRgb = newColorRgb & 0x00FFFFFF;
        this.scale = newScale;
        
        this.gravity = newGravity;
        this.initialUpwardVel = newInitialUpwardVel;
        this.lifetime = newLifetime;
        this.startFadeAge = (int)(newLifetime * fadeRatio);
        
        this.age = 0;
        
        if (this.isFrozen && this.targetEntity != null) {
            this.offsetX = this.x - this.targetEntity.getX();
            this.offsetY = this.y - this.targetEntity.getY();
            this.offsetZ = this.z - this.targetEntity.getZ();
        }
    }

    public boolean isAlive() {
        return !this.removed && this.age < this.lifetime;
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