package com.melodic.plushiehunt;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

final class ResetPlushiesCommand extends AbstractPlayerCommand {
    @Nonnull
    private final OptionalArg<PlayerRef> targetArg = withOptionalArg("player", "Player to reset", ArgTypes.PLAYER_REF);

    ResetPlushiesCommand() {
        super("resetplushies", "Reset found plushies progress");
        setPermissionGroups("Adventure");
        addAliases("plushiesreset");
    }

    @Override
    protected void execute(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> ref, PlayerRef player, World world) {
        PlayerRef target = targetArg.provided(ctx) ? targetArg.get(ctx) : player;
        Ref<EntityStore> targetRef = target.getReference();
        if (targetRef == null || !targetRef.isValid()) {
            ctx.sendMessage(Message.raw("Target player is not in a valid world."));
            return;
        }
        Store<EntityStore> targetStore = targetRef.getStore();
        PlushiesFound progress = targetStore.getComponent(targetRef, PlushiesFound.getComponentType());
        if (progress != null) {
            progress.clear();
        }
        World targetWorld = targetStore.getExternalData().getWorld();
        PlushieHuntPlugin.get().resetPlayerState(target, targetWorld);
        if (target.equals(player)) {
            ctx.sendMessage(Message.raw("Your found plushies have been reset."));
        } else {
            ctx.sendMessage(Message.raw("Reset found plushies for " + target.getUsername() + "."));
        }
    }
}
