package com.melodic.plushiehunt;

import com.hypixel.hytale.builtin.triggervolumes.TriggerVolumesPlugin;
import com.hypixel.hytale.builtin.triggervolumes.effect.TriggerEffect;
import com.hypixel.hytale.builtin.triggervolumes.manager.TriggerVolumeManager;
import com.hypixel.hytale.builtin.triggervolumes.manager.VolumeEntry;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.protocol.packets.world.ServerSetBlock;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.section.BlockSection;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkGrid;
import org.joml.Vector3d;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PlushieManager {
    public record PlushieLocation(@Nonnull String plushieId, @Nonnull Vector3d position) {
        public int blockX() {
            return (int) Math.floor(position.x);
        }

        public int blockY() {
            return (int) Math.floor(position.y);
        }

        public int blockZ() {
            return (int) Math.floor(position.z);
        }

        public int sectionX() {
            return ChunkUtil.chunkCoordinate(blockX());
        }

        public int sectionY() {
            return ChunkUtil.chunkCoordinate(blockY());
        }

        public int sectionZ() {
            return ChunkUtil.chunkCoordinate(blockZ());
        }
    }

    private static final Map<String, List<PlushieLocation>> worldPlushies = new ConcurrentHashMap<>();
    private static final Map<String, Set<Vector3d>> dynamicLocations = new ConcurrentHashMap<>();

    @Nonnull
    public static List<PlushieLocation> getPlushies(@Nullable World world) {
        if (world == null) return List.of();
        var cached = worldPlushies.get(world.getName());
        if (cached != null && !cached.isEmpty()) {
            return cached;
        }
        return refreshWorldPlushies(world);
    }

    @Nonnull
    public static List<PlushieLocation> refreshWorldPlushies(@Nullable World world) {
        if (world == null) return List.of();
        List<PlushieLocation> locations = new ArrayList<>();
        var store = world.getEntityStore().getStore();
        var managerType = TriggerVolumesPlugin.get().getManagerResourceType();
        var manager = store.getResource(managerType);
        for (VolumeEntry entry : manager.getVolumes()) {
            for (TriggerEffect effect : entry.getEffects()) {
                if (effect instanceof FindPlushieEffect findPlushieEffect) {
                    String id = findPlushieEffect.getPlushie();
                    if (id != null && !id.isBlank()) {
                        locations.add(new PlushieLocation(id, entry.getPosition()));
                    }
                }
            }
        }

        for (var entry : dynamicLocations.entrySet()) {
            String id = entry.getKey();
            for (Vector3d pos : entry.getValue()) {
                int bx = (int) Math.floor(pos.x);
                int by = (int) Math.floor(pos.y);
                int bz = (int) Math.floor(pos.z);
                boolean exists = locations.stream().anyMatch(l ->
                    l.plushieId().equals(id) && l.blockX() == bx && l.blockY() == by && l.blockZ() == bz
                );
                if (!exists) {
                    locations.add(new PlushieLocation(id, pos));
                }
            }
        }

        worldPlushies.put(world.getName(), Collections.unmodifiableList(locations));
        return locations;
    }

    public static void registerLocation(@Nullable String worldName, @Nonnull String plushieId, @Nonnull Vector3d pos) {
        dynamicLocations.computeIfAbsent(plushieId, k -> ConcurrentHashMap.newKeySet()).add(new Vector3d(pos));
        if (worldName != null) {
            worldPlushies.remove(worldName);
        }
    }

    public static void restoreBlockForPlayer(@Nullable World world, @Nullable PlayerRef playerRef, @Nullable Vector3d pos) {
        if (world == null || playerRef == null || pos == null) return;
        int bx = (int) Math.floor(pos.x);
        int by = (int) Math.floor(pos.y);
        int bz = (int) Math.floor(pos.z);
        int sx = ChunkUtil.chunkCoordinate(bx);
        int sy = ChunkUtil.chunkCoordinate(by);
        int sz = ChunkUtil.chunkCoordinate(bz);

        var chunkStore = world.getChunkStore();
        var sectionRef = chunkStore.getChunkSectionReference(sx, sy, sz);
        if (sectionRef != null && sectionRef.isValid()) {
            var blockSection = chunkStore.getStore().getComponent(sectionRef, BlockSection.getComponentType());
            if (blockSection != null) {
                int blockIndex = ChunkUtil.indexBlock(bx, by, bz);
                int blockId = blockSection.get(blockIndex);
                short filler = (short) blockSection.getFiller(blockIndex);
                byte rotation = (byte) blockSection.getRotationIndex(blockIndex);
                ServerSetBlock packet = new ServerSetBlock(ChunkGrid.MAIN_CHUNK_GRID_NETWORK_ID, bx, by, bz, blockId, filler, rotation);
                playerRef.getPacketHandler().writeNoCache(packet);
            }
        }
    }
}
