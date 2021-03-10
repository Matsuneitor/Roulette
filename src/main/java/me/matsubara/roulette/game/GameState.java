package me.matsubara.roulette.game;

public enum GameState {
    WAITING,
    COUNTDOWN,
    SELECTING,
    SPINNING,
    ENDING;

    public boolean isWaiting() {
        return this == WAITING;
    }

    public boolean isCountdown() {
        return this == COUNTDOWN;
    }

    public boolean isSelecting() {
        return this == SELECTING;
    }

    public boolean isSpinning() {
        return this == SPINNING;
    }

    public boolean isEnding() {
        return this == ENDING;
    }
}