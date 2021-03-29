package me.matsubara.roulette.game;

import org.bukkit.Location;

import javax.annotation.Nullable;
import java.util.UUID;

public final class GameData {

    private final String name;
    private final UUID creator;
    @Nullable
    private final UUID account;
    private final GameType type;
    private final Location location;
    @Nullable
    private final String npcName;
    @Nullable
    private final UUID npcUUID;
    private final int minPlayers, maxPlayers;

    private final boolean isUpdate;

    public GameData(String name, UUID creator, @Nullable UUID account, GameType type, Location location, @Nullable String npcName, @Nullable UUID npcUUID, int minPlayers, int maxPlayers, boolean isUpdate) {
        this.name = name;
        this.creator = creator;
        this.account = account;
        this.type = type;
        this.location = location;
        this.npcName = npcName;
        this.npcUUID = npcUUID;
        this.minPlayers = minPlayers;
        this.maxPlayers = maxPlayers;
        this.isUpdate = isUpdate;
    }

    public String getName() {
        return name;
    }

    public UUID getCreator() {
        return creator;
    }

    @Nullable
    public UUID getAccount() {
        return account;
    }

    public GameType getType() {
        return type;
    }

    public Location getLocation() {
        return location;
    }

    @Nullable
    public String getNPCName() {
        return npcName;
    }

    public UUID getNPCUUID() {
        return npcUUID;
    }

    public int getMinPlayers() {
        return minPlayers;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public boolean isUpdate() {
        return isUpdate;
    }
}