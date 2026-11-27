package com.melodic.plushiehunt;

import com.hypixel.hytale.builtin.triggervolumes.effect.TriggerContext;
import com.hypixel.hytale.builtin.triggervolumes.effect.TriggerEffect;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.protocol.packets.world.ServerSetBlock;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import dev.hardaway.wardrobe.impl.player.PlayerWardrobeComponent;
import dev.rm20.thankmasvault.economy.EconomyAPI;
import org.joml.Vector3d;

import javax.annotation.Nonnull;
import java.util.logging.Level;

/**
 * Records a plushie as found by the triggering player. Messages support {found}, {total}, and
 * {plushie} placeholders; an empty message is not sent.
 */
public class FindPlushieEffect extends TriggerEffect {
    public static final String TYPE = "FindPlushie";

    public static final BuilderCodec<FindPlushieEffect> CODEC = BuilderCodec.builder(FindPlushieEffect.class, FindPlushieEffect::new, BASE_CODEC)
        .append(new KeyedCodec<>("Plushie", Codec.STRING, false), (e, v) -> e.plushie = v, e -> e.plushie)
        .documentation("Unique plushie ID. Required because volume names are not kept across restarts").add()
        .append(new KeyedCodec<>("FoundMessage", Codec.STRING, false), (e, v) -> e.foundMessage = v, e -> e.foundMessage)
        .add()
        .append(new KeyedCodec<>("AlreadyFoundMessage", Codec.STRING, false), (e, v) -> e.alreadyFoundMessage = v, e -> e.alreadyFoundMessage)
        .add()
        .build();

    private String plushie = "";
    private String foundMessage = "You found a hidden plushie! ({found}/{total}) and earned {coins} coins";
    private String alreadyFoundMessage = "You've already found this plushie. ({found}/{total})";

    public String getPlushie() {
        return plushie;
    }

    @Override
    public void execute(@Nonnull TriggerContext context) {
        var ref = context.getEntityRef();
        if (ref == null || !ref.isValid()) return;
        var store = context.getStore();
        var player = store.getComponent(ref, PlayerRef.getComponentType());
        if (player == null) return;

        if (plushie == null || plushie.isBlank()) {
            PlushieHuntPlugin.get().getLogger().at(Level.WARNING)
                .log("FindPlushie effect on trigger volume '%s' has no Plushie ID set; ignoring", context.getVolume().getName());
            return;
        }
        String id = plushie;
        var progress = store.ensureAndGetComponent(ref, PlushiesFound.getComponentType());
        boolean isNew = progress.add(id);

        String text = isNew ? foundMessage : alreadyFoundMessage;
        if (text == null || text.isEmpty()) return;
        player.sendMessage(Message.raw(format(text, id, progress.count(), PlushieHuntPlugin.get().totalPlushies(),PlushieHuntPlugin.get().config().getCoinReward())));
        hideBlockForPlayer(player, context.getBlockPosition());
        if (context.getBlockPosition() != null) {
            var world = store.getExternalData().getWorld();
            PlushieManager.registerLocation(world != null ? world.getName() : null, id, context.getBlockPosition());
            int sx = ChunkUtil.chunkCoordinate((int) Math.floor(context.getBlockPosition().x));
            int sy = ChunkUtil.chunkCoordinate((int) Math.floor(context.getBlockPosition().y));
            int sz = ChunkUtil.chunkCoordinate((int) Math.floor(context.getBlockPosition().z));
            long sectionKey = ChunkUtil.indexChunk(sx, sz) ^ ((long) sy << 32);
            HideFoundPlushiesSystem.markSectionHidden(player.getUuid(), sectionKey);
        }
        if(isNew) {
            EconomyAPI.deposit(ref,ref.getStore(),PlushieHuntPlugin.get().config().getCoinReward());
            int total = Math.max(1, PlushieHuntPlugin.get().totalPlushies());
            int percent = (progress.count() * 100) / total;

            if(percent >= PlushieHuntPlugin.get().config().getPercentNeeded())
            {
                PlayerWardrobeComponent wardrobe = store.ensureAndGetComponent(ref, PlayerWardrobeComponent.getComponentType());
                if(wardrobe.unlockCosmetic(PlushieHuntPlugin.get().config().getReward()))
                {
                    player.sendMessage(Message.raw("You unlocked a cosmetic, Open /wardrobe to equip it."));
                }
            }
        }
    }

    static String format(String text, String plushie, int found, int total, int coins) {
        return text.replace("{found}", Integer.toString(found))
            .replace("{total}", Integer.toString(total))
            .replace("{plushie}", plushie)
                .replace("{coins}", Integer.toString(coins));
    }


    public static void hideBlockForPlayer(PlayerRef playerRef, Vector3d pos) {
        if (playerRef == null || pos == null) return;
        ServerSetBlock fakeAirPacket = new ServerSetBlock(
                (int) Math.floor(pos.x),
                (int) Math.floor(pos.y),
                (int) Math.floor(pos.z),
                BlockType.EMPTY_ID,
                (short) 0,
                (byte) 0
        );

        playerRef.getPacketHandler().writeNoCache(fakeAirPacket);
    }
}
