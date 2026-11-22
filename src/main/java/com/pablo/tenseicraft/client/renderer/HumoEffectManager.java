package com.pablo.tenseicraft.client.renderer;

import com.pablo.tenseicraft.data.ModAttachments;
import com.pablo.tenseicraft.data.PlayerRpgData;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gestor del efecto visual del Manto Celestial (Hagoromo divino / Transformación) en el cliente.
 * Permite que habilidades, estados o transformaciones futuras activen o desactiven el efecto
 * sin alterar la escala, el posicionamiento ni las partículas ya calibradas.
 */
public class HumoEffectManager {

    private static final Map<Integer, Long> ACTIVE_SMOKE_EXPIRE = new ConcurrentHashMap<>();
    private static final Map<Integer, Boolean> ACTIVE_SMOKE_TOGGLE = new ConcurrentHashMap<>();

    public static void setMantleActive(int entityId, boolean active) {
        setSmokeActive(entityId, active);
    }

    public static void setMantleDuration(int entityId, long expireTick) {
        setSmokeDuration(entityId, expireTick);
    }

    public static boolean isMantleActive(LivingEntity entity) {
        return isSmokeActive(entity);
    }

    public static void setSmokeActive(int entityId, boolean active) {
        if (active) {
            ACTIVE_SMOKE_TOGGLE.put(entityId, true);
        } else {
            ACTIVE_SMOKE_TOGGLE.remove(entityId);
            ACTIVE_SMOKE_EXPIRE.remove(entityId);
        }
    }

    public static void setSmokeDuration(int entityId, long expireTick) {
        ACTIVE_SMOKE_EXPIRE.put(entityId, expireTick);
    }

    public static boolean isSmokeActive(LivingEntity entity) {
        if (entity == null || !entity.isAlive()) return false;

        // 1. Verificar estado dinámico en cliente (sync por red)
        Boolean toggled = ACTIVE_SMOKE_TOGGLE.get(entity.getId());
        if (Boolean.TRUE.equals(toggled)) return true;

        Long expire = ACTIVE_SMOKE_EXPIRE.get(entity.getId());
        if (expire != null) {
            long currentTick = entity.level().getGameTime();
            if (currentTick < expire) {
                return true;
            } else {
                ACTIVE_SMOKE_EXPIRE.remove(entity.getId());
            }
        }

        // 2. Si es jugador, consultar datos de RPG persistentes
        if (entity instanceof Player player) {
            PlayerRpgData data = player.getData(ModAttachments.PLAYER_RPG_DATA);
            if (data != null) {
                long currentTick = entity.level().getGameTime();
                return data.isDivineSmokeActive(currentTick);
            }
        }

        return false;
    }
}
