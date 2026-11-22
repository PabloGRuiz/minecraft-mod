package com.pablo.tenseicraft.client.events;

import com.pablo.tenseicraft.TenseiCraftMod;
import com.pablo.tenseicraft.entities.ReimuBossEntity;
import com.pablo.tenseicraft.init.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.util.List;

/**
 * Controlador de música de combate en bucle para Reimu Hakurei en El Gensokyo.
 * Silencia la música ambiental vanilla durante el duelo y realiza fade-in/fade-out suaves.
 */
@EventBusSubscriber(modid = TenseiCraftMod.MOD_ID, value = Dist.CLIENT)
public class ReimuBossMusicClient {

    private static ReimuBossMusicInstance currentMusic = null;

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            if (currentMusic != null) {
                mc.getSoundManager().stop(currentMusic);
                currentMusic = null;
            }
            return;
        }

        List<ReimuBossEntity> bosses = mc.level.getEntitiesOfClass(
                ReimuBossEntity.class,
                mc.player.getBoundingBox().inflate(72.0),
                b -> b.isAlive() && !b.isRemoved()
        );

        if (!bosses.isEmpty() && mc.player.isAlive()) {
            // Silenciar la música ambiental vanilla mientras Reimu esté en combate o en intro
            mc.getMusicManager().stopPlaying();

            ReimuBossEntity boss = bosses.get(0);
            if (currentMusic == null || currentMusic.isStopped() || !mc.getSoundManager().isActive(currentMusic)) {
                currentMusic = new ReimuBossMusicInstance(boss);
                mc.getSoundManager().play(currentMusic);
            }
        } else if (currentMusic != null && (currentMusic.isStopped() || !mc.getSoundManager().isActive(currentMusic))) {
            currentMusic = null;
        }
    }

    private static class ReimuBossMusicInstance extends AbstractTickableSoundInstance {

        private final ReimuBossEntity boss;
        private boolean fadingOut = false;

        protected ReimuBossMusicInstance(ReimuBossEntity boss) {
            super(ModSounds.REIMU_BOSS_MUSIC.get(), SoundSource.MUSIC, SoundInstance.createUnseededRandom());
            this.boss = boss;
            this.looping = true;
            this.delay = 0;
            this.volume = 0.05F;
            this.attenuation = Attenuation.NONE;
            this.relative = true;
        }

        @Override
        public boolean canStartSilent() {
            return true;
        }

        @Override
        public void tick() {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.level == null || !mc.player.isAlive()
                    || this.boss == null || !this.boss.isAlive() || this.boss.isRemoved()
                    || this.boss.level() != mc.level
                    || this.boss.distanceToSqr(mc.player) > 80.0 * 80.0) {
                this.fadingOut = true;
            }

            if (this.fadingOut) {
                this.volume -= 0.07F;
                if (this.volume <= 0.0F) {
                    this.volume = 0.0F;
                    this.stop();
                }
                return;
            }

            if (this.volume < 1.0F) {
                this.volume = Math.min(1.0F, this.volume + 0.05F);
            }
        }
    }
}
