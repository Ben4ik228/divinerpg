package com.lootforge.skillforge.client;

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
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public class DodgeTextParticle extends Particle {
    private static final String DEFAULT_DODGE_TEXT = "\u0423\u043a\u043b\u043e\u043d";

    private final Component textComponent;
    private final int rgbColor;

    protected DodgeTextParticle(
            ClientLevel level,
            double x,
            double y,
            double z,
            double vx,
            double vz,
            String text,
            int rgbColor
    ) {
        super(level, x, y, z, vx, 0.0D, vz);
        this.xd = vx;
        this.yd = 0.052D;
        this.zd = vz;
        this.hasPhysics = false;
        this.gravity = 0.0F;
        this.lifetime = 42;
        this.textComponent = Component.literal(text != null && !text.isEmpty() ? text : DEFAULT_DODGE_TEXT);
        this.rgbColor = rgbColor & 0xFFFFFF;
    }

    public static void spawn(double x, double y, double z) {
        spawnText(x, y, z, DEFAULT_DODGE_TEXT, 0xFFFFFF);
    }

    public static void spawnText(double x, double y, double z, String text, int rgbColor) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        double angle = mc.level.random.nextDouble() * Math.PI * 2.0D;
        double offsetX = Math.cos(angle) * 0.12D;
        double offsetZ = Math.sin(angle) * 0.12D;
        double driftX = Math.cos(angle) * 0.008D;
        double driftZ = Math.sin(angle) * 0.008D;
        mc.particleEngine.add(new DodgeTextParticle(
                mc.level,
                x + offsetX,
                y,
                z + offsetZ,
                driftX,
                driftZ,
                text,
                rgbColor
        ));
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        if (this.age++ >= this.lifetime) {
            this.remove();
            return;
        }
        this.x += this.xd;
        this.y += this.yd;
        this.z += this.zd;
        this.xd *= 0.94D;
        this.zd *= 0.94D;
        // Плавное вознесение вверх от шеи, как душа
        this.yd = Math.max(0.015D, this.yd * 0.955D);
    }

    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        Vec3 camPos = camera.getPosition();

        float ageF = this.age + partialTick;
        float progress = Mth.clamp(ageF / (float) this.lifetime, 0.0F, 1.0F);

        float px = (float) (Mth.lerp(partialTick, this.xo, this.x) - camPos.x);
        float py = (float) (Mth.lerp(partialTick, this.yo, this.y) - camPos.y);
        float pz = (float) (Mth.lerp(partialTick, this.zo, this.z) - camPos.z);

        // 1. Появление (упругий вылет) -> пульсация -> красивое сжатие при исчезновении
        float baseScale;
        if (ageF < 5.0F) {
            float t = ageF / 5.0F;
            baseScale = Mth.lerp(1.0F - (1.0F - t) * (1.0F - t), 0.45F, 1.14F);
        } else if (ageF < 9.0F) {
            baseScale = Mth.lerp((ageF - 5.0F) / 4.0F, 1.14F, 1.0F);
        } else {
            baseScale = 1.0F;
        }

        // Живая пульсация размера в полёте
        float pulseWave = Mth.sin(ageF * 0.42F);
        float pulseScale = 1.0F + 0.09F * pulseWave * (1.0F - progress * 0.4F);

        // Плавное растворение и мягкое сжатие в конце (исчезновение души)
        float alpha;
        float disappearScale = 1.0F;
        if (ageF < 4.0F) {
            alpha = ageF / 4.0F;
        } else if (progress > 0.55F) {
            float fadeT = (progress - 0.55F) / 0.45F;
            float easeFade = 1.0F - fadeT;
            alpha = easeFade * easeFade;
            disappearScale = Mth.lerp(fadeT * fadeT, 1.0F, 0.52F);
        } else {
            alpha = 0.90F + 0.10F * pulseWave;
        }

        int alphaInt = Mth.clamp((int) (alpha * 255.0F), 5, 255);
        int textColor = (alphaInt << 24) | this.rgbColor;

        // Волнообразное парение из стороны в сторону и лёгкий наклон
        float soulSway = Mth.sin(ageF * 0.20F) * 0.10F * Math.min(1.0F, progress * 2.2F);
        float verticalBreath = Mth.cos(ageF * 0.42F) * 0.025F;

        PoseStack poseStack = new PoseStack();
        poseStack.pushPose();
        poseStack.translate(px, py + verticalBreath, pz);
        poseStack.mulPose(camera.rotation());
        poseStack.translate(soulSway, 0.0F, 0.0F);

        float finalScale = 0.025F * baseScale * pulseScale * disappearScale;
        poseStack.scale(-finalScale, -finalScale, finalScale);

        float halfWidth = -font.width(this.textComponent) / 2.0F;
        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
        font.drawInBatch(
                this.textComponent,
                halfWidth,
                0.0F,
                textColor,
                true,
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
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.CUSTOM;
    }
}
