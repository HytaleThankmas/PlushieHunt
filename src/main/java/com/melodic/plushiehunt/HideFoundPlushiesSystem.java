package com.melodic.plushiehunt;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.player.ChunkTracker;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.joml.Vector3d;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class HideFoundPlushiesSystem extends EntityTickingSystem<EntityStore> {
    private static final Map<UUID, PlayerTrackingState> playerStates = new ConcurrentHashMap<>();

    private final ComponentType<EntityStore, PlayerRef> playerRefType = PlayerRef.getComponentType();
    private final ComponentType<EntityStore, PlushiesFound> plushiesFoundType = PlushiesFound.getComponentType();
    private final ComponentType<EntityStore, ChunkTracker> chunkTrackerType = ChunkTracker.getComponentType();
    private final ComponentType<EntityStore, TransformComponent> transformType = TransformComponent.getComponentType();

    private final Query<EntityStore> query;

    public HideFoundPlushiesSystem() {
        this.query = Query.and(
            playerRefType,
            plushiesFoundType,
            chunkTrackerType,
            transformType
        );
    }

    public static void clearPlayer(UUID uuid) {
        playerStates.remove(uuid);
    }

    public static void markSectionHidden(UUID uuid, long sectionKey) {
        var state = playerStates.computeIfAbsent(uuid, k -> new PlayerTrackingState());
        state.hiddenSections.add(sectionKey);
    }

    public static void resetPlayer(UUID uuid) {
        playerStates.remove(uuid);
    }

    @Override
    public Query<EntityStore> getQuery() {
        return query;
    }

    @Override
    public boolean isParallel(int archetypeChunkSize, int taskCount) {
        return false;
    }

    @Override
    public void tick(float dt, int index, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        PlayerRef playerRef = archetypeChunk.getComponent(index, playerRefType);
        PlushiesFound found = archetypeChunk.getComponent(index, plushiesFoundType);
        ChunkTracker chunkTracker = archetypeChunk.getComponent(index, chunkTrackerType);
        TransformComponent transform = archetypeChunk.getComponent(index, transformType);

        if (playerRef == null || found == null || chunkTracker == null || transform == null) return;
        if (found.count() == 0) return;

        UUID uuid = playerRef.getUuid();
        PlayerTrackingState state = playerStates.computeIfAbsent(uuid, k -> new PlayerTrackingState());

        Vector3d playerPos = transform.getPosition();
        int currentChunkX = ChunkUtil.chunkCoordinate(playerPos.x);
        int currentChunkZ = ChunkUtil.chunkCoordinate(playerPos.z);

        boolean chunkChanged = false;
        if (state.hasLastChunk) {
            if (state.lastChunkX != currentChunkX || state.lastChunkZ != currentChunkZ) {
                chunkChanged = true;
            }
        } else {
            state.hasLastChunk = true;
            chunkChanged = true;
        }
        state.lastChunkX = currentChunkX;
        state.lastChunkZ = currentChunkZ;

        World world = store.getExternalData().getWorld();
        List<PlushieManager.PlushieLocation> plushies = PlushieManager.getPlushies(world);
        if (plushies.isEmpty()) return;

        for (PlushieManager.PlushieLocation loc : plushies) {
            if (!found.has(loc.plushieId())) continue;

            int sx = loc.sectionX();
            int sy = loc.sectionY();
            int sz = loc.sectionZ();

            boolean isLoaded = chunkTracker.isLoaded(sx, sy, sz);
            long sectionKey = ChunkUtil.indexChunk(sx, sz) ^ ((long) sy << 32);

            if (isLoaded) {
                boolean notYetHidden = !state.hiddenSections.contains(sectionKey);
                boolean enteredPlushieChunk = chunkChanged && (currentChunkX == sx && currentChunkZ == sz);

                if (notYetHidden || enteredPlushieChunk) {
                    FindPlushieEffect.hideBlockForPlayer(playerRef, loc.position());
                    state.hiddenSections.add(sectionKey);
                }
            } else {
                state.hiddenSections.remove(sectionKey);
            }
        }
    }

    private static class PlayerTrackingState {
        int lastChunkX;
        int lastChunkZ;
        boolean hasLastChunk = false;
        final Set<Long> hiddenSections = new HashSet<>();
    }
}
