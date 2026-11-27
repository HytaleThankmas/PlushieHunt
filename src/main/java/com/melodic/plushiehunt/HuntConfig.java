package com.melodic.plushiehunt;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

public class HuntConfig {
    public static final BuilderCodec<HuntConfig> CODEC = BuilderCodec.builder(HuntConfig.class, HuntConfig::new)
        .append(new KeyedCodec<>("TotalPlushies", Codec.INTEGER), (c, v) -> c.totalPlushies = v, c -> c.totalPlushies)
        .documentation("Number of hidden plushies, shown as {total} in messages").add()
            .append(new KeyedCodec<>("Coins", Codec.INTEGER), (c, v) -> c.coinReward = v, c -> c.coinReward)
            .documentation("How many coins to give").add()
            .append(new KeyedCodec<>("PercentNeeded", Codec.INTEGER), (c, v) -> c.percentNeeded = v, c -> c.percentNeeded)
            .documentation("What % is required to give the reward, 100 = 100%").add()
            .append(new KeyedCodec<>("CosmeticRewardId", Codec.STRING), (c, v) -> c.reward = v, c -> c.reward)
            .documentation("The wardrobe ID for the item to give").add()
            .build();

    private int totalPlushies = 2;
    private int coinReward = 10;

    private int percentNeeded = 100;
    private String reward = "Stage_HeadSet";

    int totalPlushies() {
        return totalPlushies;
    }

    public int getCoinReward() {
        return coinReward;
    }

    public String getReward() {
        return reward;
    }

    public int getPercentNeeded() {
        return percentNeeded;
    }
}
