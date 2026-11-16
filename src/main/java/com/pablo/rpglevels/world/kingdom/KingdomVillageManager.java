package com.pablo.rpglevels.world.kingdom;

import com.pablo.rpglevels.entities.GuildMasterNpcEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.List;

public class KingdomVillageManager {

    private static final String[] KINGDOM_PREFIXES = {
            "Reino de", "Ducado de", "Corona de", "Principado de",
            "Condado de", "Imperio de", "Marquesado de", "Tierras de"
    };

    private static final String[] KINGDOM_NAMES = {
            "Valoria", "Solsticio", "Eldoria", "Valle Cenizo",
            "Brumaverde", "Lunargenta", "Aurelia", "Viento Norte",
            "Oakhaven", "Piedrasol", "Altacresta", "Ríos Profundos",
            "Corona Solar", "Hierro Viejo", "Cielos Claros", "Torrebruma"
    };

    private static final String[] VILLAGE_PREFIXES = {
            "Aldea de", "Pueblo de", "Villa", "Poblado de",
            "Bastión de", "Fuerte", "Refugio de"
    };

    private static final String[] VILLAGE_NAMES = {
            "Robledal", "Aguasclaras", "Fuentedorada", "Montealto",
            "Colinaverde", "Puerto de Hielo", "Piedras Blancas", "Pinos Altos",
            "Canto del Río", "Valle Sereno", "Sombraverde", "Sol Naciente",
            "Brisa Cálida", "Llanura Alta", "Arenas Doradas", "Espino Blanco",
            "Arroyo Dulce", "Cruz de Hierro", "Luz de Luna", "Bosquepuro"
    };

    /**
     * Obtiene el nombre del Reino basado en una cuadrícula de 1200x1200 bloques y la semilla del mundo.
     */
    public static String getKingdomName(Level level, BlockPos pos) {
        long worldSeed = (level instanceof ServerLevel sl) ? sl.getSeed() : 42L;
        int regX = Math.floorDiv(pos.getX(), 1200);
        int regZ = Math.floorDiv(pos.getZ(), 1200);

        long hash = ((long) regX * 73856093L) ^ ((long) regZ * 19349663L) ^ worldSeed;
        int prefixIdx = (int) (Math.abs(hash) % KINGDOM_PREFIXES.length);
        int nameIdx = (int) (Math.abs(hash >> 8) % KINGDOM_NAMES.length);

        return KINGDOM_PREFIXES[prefixIdx] + " " + KINGDOM_NAMES[nameIdx];
    }

    /**
     * Obtiene el nombre de la Aldea si la posición está dentro de un asentamiento civil (aldeanos o gremio).
     * Si no hay asentamiento, retorna null.
     */
    public static String getVillageName(Level level, BlockPos pos) {
        BlockPos anchor = findSettlementAnchor(level, pos, 56.0);
        if (anchor == null) {
            return null;
        }

        long worldSeed = (level instanceof ServerLevel sl) ? sl.getSeed() : 42L;
        int cellX = Math.floorDiv(anchor.getX(), 96);
        int cellZ = Math.floorDiv(anchor.getZ(), 96);

        long hash = ((long) cellX * 83492791L) ^ ((long) cellZ * 29712157L) ^ worldSeed;
        int prefixIdx = (int) (Math.abs(hash) % VILLAGE_PREFIXES.length);
        int nameIdx = (int) (Math.abs(hash >> 8) % VILLAGE_NAMES.length);

        String prefix = VILLAGE_PREFIXES[prefixIdx];
        String name = VILLAGE_NAMES[nameIdx];
        if (prefix.equals("Villa") || prefix.equals("Fuerte")) {
            return prefix + " " + name;
        }
        return prefix + " " + name;
    }

    /**
     * Determina si la posición está cerca de un asentamiento civil (radio dado).
     */
    public static boolean isNearSettlement(Level level, BlockPos pos, double radius) {
        return findSettlementAnchor(level, pos, radius) != null;
    }

    private record SettlementCacheEntry(BlockPos anchor, long expireTick) {}
    private static final java.util.Map<net.minecraft.world.level.ChunkPos, SettlementCacheEntry> SETTLEMENT_CACHE = new java.util.concurrent.ConcurrentHashMap<>();

    private static BlockPos findSettlementAnchor(Level level, BlockPos pos, double radius) {
        long gameTime = level.getGameTime();
        net.minecraft.world.level.ChunkPos cp = new net.minecraft.world.level.ChunkPos(pos);

        SettlementCacheEntry cached = SETTLEMENT_CACHE.get(cp);
        if (cached != null && cached.expireTick() > gameTime) {
            return cached.anchor();
        }

        AABB box = new AABB(pos).inflate(radius, 16.0, radius);
        BlockPos found = null;

        // 1. Maestro del Gremio presente
        List<GuildMasterNpcEntity> masters = level.getEntitiesOfClass(GuildMasterNpcEntity.class, box);
        if (!masters.isEmpty()) {
            found = masters.get(0).blockPosition();
        } else {
            // 2. Aldeanos presentes
            List<Villager> villagers = level.getEntitiesOfClass(Villager.class, box);
            if (!villagers.isEmpty()) {
                found = villagers.get(0).blockPosition();
            }
        }

        // Limpieza periódica de caché
        if (SETTLEMENT_CACHE.size() > 500) {
            SETTLEMENT_CACHE.entrySet().removeIf(e -> e.getValue().expireTick() <= gameTime);
        }
        SETTLEMENT_CACHE.put(cp, new SettlementCacheEntry(found, gameTime + 200L));
        return found;
    }

    /**
     * Actualiza y sincroniza la ubicación del jugador, enviando notificación si entra a una nueva aldea.
     */
    public static void checkPlayerLocation(ServerPlayer player) {
        if (player.level().dimension() != Level.OVERWORLD || !player.isAlive()) {
            return;
        }

        BlockPos pos = player.blockPosition();
        Level level = player.level();

        String kingdom = getKingdomName(level, pos);
        String village = getVillageName(level, pos);

        String fullLocation = (village != null)
                ? village + " (" + kingdom + ")"
                : "Tierras Salvajes (" + kingdom + ")";

        com.pablo.rpglevels.data.PlayerRpgData rpgData = player.getData(com.pablo.rpglevels.data.ModAttachments.PLAYER_RPG_DATA);
        if (rpgData != null) {
            String prevLoc = rpgData.getCurrentLocationName();
            if (!fullLocation.equals(prevLoc)) {
                rpgData.setCurrentLocationName(fullLocation);

                // Si entró a una aldea desde fuera u otra aldea, avisar en la Action Bar
                if (village != null && (prevLoc == null || !prevLoc.startsWith(village))) {
                    Component announcement = Component.literal("§6🏰 Has entrado en: §e" + village + " §7(" + kingdom + ")");
                    player.displayClientMessage(announcement, true);
                    player.playNotifySound(SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.PLAYERS, 0.7f, 1.0f);
                }

                com.pablo.rpglevels.network.ModNetworking.syncToPlayer(player);
            }
        }
    }
}
