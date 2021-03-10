package me.matsubara.roulette.util;

import com.gmail.filoghost.holographicdisplays.api.placeholder.PlaceholderReplacer;

public final class CyclicPlaceholderReplacer implements PlaceholderReplacer {

    private final String[] frames;
    private int index;

    public CyclicPlaceholderReplacer(String[] frames) {
        this.frames = frames;
        index = 0;
    }

    @Override
    public String update() {
        String result = frames[index];

        index++;
        if (index >= frames.length) index = 0;

        return result;
    }
}