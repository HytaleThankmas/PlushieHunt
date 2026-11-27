package com.melodic.plushiehunt;

import com.hypixel.hytale.builtin.triggervolumes.TriggerVolumesPlugin;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.modules.entity.player.ChunkTracker;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.Config;

import java.nio.file.Files;

public class PlushieHuntPlugin extends JavaPlugin {
    private static PlushieHuntPlugin instance;

    private final Config<HuntConfig> config = withConfig(HuntConfig.CODEC);
    private ComponentType<EntityStore, PlushiesFound> plushiesFoundType;

    public PlushieHuntPlugin(JavaPluginInit init) {
        super(init);
        instance = this;
    }

    public static PlushieHuntPlugin get() {
        return instance;
    }

    @Override
    protected void setup() {
        plushiesFoundType = getEntityStoreRegistry().registerComponent(PlushiesFound.class, "Melodic_PlushiesFound", PlushiesFound.CODEC);
        var triggers = TriggerVolumesPlugin.get();
        triggers.registerEffectType(FindPlushieEffect.TYPE, FindPlushieEffect.class, FindPlushieEffect.CODEC);
        triggers.registerConditionType(PlushiesFoundCondition.TYPE, PlushiesFoundCondition.class, PlushiesFoundCondition.CODEC);

        getEntityStoreRegistry().registerSystem(new HideFoundPlushiesSystem());

        getEventRegistry().registerGlobal(PlayerReadyEvent.class, this::onPlayerReady);
        getEventRegistry().registerGlobal(PlayerDisconnectEvent.class, this::onPlayerDisconnect);

        getCommandRegistry().registerCommand(new PlushiesCommand());
        getCommandRegistry().registerCommand(new ResetPlushiesCommand());
    }

    @Override
    protected void start() {
        if (!Files.exists(getDataDirectory().resolve("config.json"))) config.save().join();
    }

    public void onPlayerReady(PlayerReadyEvent event) {
        Player player = event.getPlayer();
        if (player == null) return;
        World world = player.getWorld();
        if (world == null) return;
        Ref<EntityStore> ref = event.getPlayerRef();
        if (ref == null || !ref.isValid()) return;
        Store<EntityStore> store = ref.getStore();
        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        if (playerRef == null) return;
        PlushiesFound progress = store.getComponent(ref, plushiesFoundType);
        if (progress == null || progress.count() == 0) return;

        ChunkTracker tracker = store.getComponent(ref, ChunkTracker.getComponentType());
        var plushies = PlushieManager.getPlushies(world);
        for (var loc : plushies) {
            if (progress.has(loc.plushieId())) {
                if (tracker == null || tracker.isLoaded(loc.sectionX(), loc.sectionY(), loc.sectionZ())) {
                    FindPlushieEffect.hideBlockForPlayer(playerRef, loc.position());
                    long sectionKey = ChunkUtil.indexChunk(loc.sectionX(), loc.sectionZ()) ^ ((long) loc.sectionY() << 32);
                    HideFoundPlushiesSystem.markSectionHidden(playerRef.getUuid(), sectionKey);
                }
            }
        }
    }

    public void onPlayerDisconnect(PlayerDisconnectEvent event) {
        if (event.getPlayerRef() != null) {
            HideFoundPlushiesSystem.clearPlayer(event.getPlayerRef().getUuid());
        }
    }

    public void resetPlayerState(PlayerRef playerRef, World world) {
        if (playerRef == null) return;
        HideFoundPlushiesSystem.resetPlayer(playerRef.getUuid());
        if (world != null) {
            var plushies = PlushieManager.getPlushies(world);
            for (var loc : plushies) {
                PlushieManager.restoreBlockForPlayer(world, playerRef, loc.position());
            }
        }
    }

    ComponentType<EntityStore, PlushiesFound> getPlushiesFoundType() {
        return plushiesFoundType;
    }

    int totalPlushies() {
        return config.get().totalPlushies();
    }

    HuntConfig config() {
        return config.get();
    }
}
