package me.matsubara.roulette.data;

import javax.annotation.Nullable;
import java.util.List;

public final class Chip {

    private final String name;
    private final String url;
    private final double price;

    private final String displayName;
    private final List<String> lore;

    public Chip(String name, @Nullable String displayName, @Nullable List<String> lore, String url, double price) {
        this.name = name;
        this.displayName = displayName;
        this.lore = lore;
        this.url = url;
        this.price = price;
    }

    public String getName() {
        return name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public List<String> getLore() {
        return lore;
    }

    public String getUrl() {
        return url;
    }

    public double getPrice() {
        return price;
    }
}