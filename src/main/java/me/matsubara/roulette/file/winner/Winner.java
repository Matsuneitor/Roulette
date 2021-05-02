package me.matsubara.roulette.file.winner;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class Winner {

    private final UUID uuid;
    private final List<WinnerData> winnerData;

    public Winner(UUID uuid) {
        this.uuid = uuid;
        this.winnerData = new ArrayList<>();
    }

    public void add(Integer mapId, double money) {
        winnerData.add(new WinnerData(mapId, money));
    }

    public UUID getUUID() {
        return uuid;
    }

    public List<WinnerData> getWinnerData() {
        return winnerData;
    }

    public final static class WinnerData {

        private final Integer mapId;
        private final double money;

        public WinnerData(@Nullable Integer mapId, double money) {
            this.mapId = mapId;
            this.money = money;
        }

        public Integer getMapId() {
            return mapId;
        }

        public double getMoney() {
            return money;
        }
    }
}