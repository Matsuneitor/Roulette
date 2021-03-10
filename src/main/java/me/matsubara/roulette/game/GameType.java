package me.matsubara.roulette.game;

public enum GameType {
    EUROPEAN,
    AMERICAN;

    public boolean isEuropean() {
        return this == EUROPEAN;
    }
}