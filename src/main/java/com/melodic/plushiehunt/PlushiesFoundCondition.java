package com.melodic.plushiehunt;

import com.hypixel.hytale.builtin.triggervolumes.effect.TriggerCondition;
import com.hypixel.hytale.builtin.triggervolumes.effect.TriggerContext;
import com.hypixel.hytale.builtin.triggervolumes.effect.builtin.conditions.PlayerCountCondition.Comparison;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.EnumCodec;

import javax.annotation.Nonnull;

/** Passes when the triggering player's found-plushie count satisfies the comparison. */
public class PlushiesFoundCondition extends TriggerCondition {
    public static final String TYPE = "PlushiesFound";

    public static final BuilderCodec<PlushiesFoundCondition> CODEC = BuilderCodec.builder(PlushiesFoundCondition.class, PlushiesFoundCondition::new, BASE_CODEC)
        .append(new KeyedCodec<>("Comparison", new EnumCodec<>(Comparison.class), false), (c, v) -> c.comparison = v, c -> c.comparison)
        .add()
        .append(new KeyedCodec<>("Count", Codec.INTEGER, false), (c, v) -> c.count = v, c -> c.count)
        .add()
        .build();

    private Comparison comparison = Comparison.AT_LEAST;
    private int count = 1;

    @Override
    public boolean test(@Nonnull TriggerContext context) {
        var ref = context.getEntityRef();
        if (ref == null || !ref.isValid()) return false;
        var progress = context.getStore().getComponent(ref, PlushiesFound.getComponentType());
        return matches(comparison, progress == null ? 0 : progress.count(), count);
    }

    static boolean matches(Comparison comparison, int found, int count) {
        if (comparison == null) comparison = Comparison.AT_LEAST;
        return switch (comparison) {
            case AT_LEAST -> found >= count;
            case AT_MOST -> found <= count;
            case EXACTLY -> found == count;
            case NOT_EQUALS -> found != count;
            case MORE_THAN -> found > count;
            case LESS_THAN -> found < count;
        };
    }
}
