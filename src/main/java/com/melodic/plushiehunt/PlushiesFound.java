package com.melodic.plushiehunt;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Saved with the player, so progress survives logouts, restarts, and world changes. */
public class PlushiesFound implements Component<EntityStore> {
    public static final BuilderCodec<PlushiesFound> CODEC = BuilderCodec.builder(PlushiesFound.class, PlushiesFound::new)
        .append(new KeyedCodec<>("Found", Codec.STRING_ARRAY),
            (c, v) -> c.found = new LinkedHashSet<>(List.of(v)), c -> c.found.toArray(String[]::new))
        .add()
        .build();

    private Set<String> found = new LinkedHashSet<>();

    public static ComponentType<EntityStore, PlushiesFound> getComponentType() {
        return PlushieHuntPlugin.get().getPlushiesFoundType();
    }

    /** Returns true if the plushie had not been found before. */
    public boolean add(String plushieId) {
        return found.add(plushieId);
    }

    public boolean has(String plushieId) {
        return found.contains(plushieId);
    }

    public int count() {
        return found.size();
    }

    public void clear() {
        found.clear();
    }

    public Set<String> getFound() {
        return java.util.Collections.unmodifiableSet(found);
    }

    // Doesn't call super.clone(). For some reason it's causing issues? HT-8119
    @Override
    public Component<EntityStore> clone() {
        var copy = new PlushiesFound();
        copy.found = new LinkedHashSet<>(found);
        return copy;
    }
}
