package me.matsubara.roulette.game;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import javax.annotation.Nullable;
import java.util.UUID;

public final class GameData {

    private final String name;
    private final GameType type;
    private final Location location;
    @Nullable
    private final String npcName;
    @Nullable
    private final UUID npcUUID;
    private final int minPlayers, maxPlayers;

    private final boolean isUpdate;

    @Nullable
    private final Player creator;

    public GameData(String name, GameType type, Location location, @Nullable String npcName, @Nullable UUID npcUUID, int minPlayers, int maxPlayers, boolean isUpdate, @Nullable Player creator) {
        this.name = name;
        this.type = type;
        this.location = location;
        this.npcName = npcName;
        this.npcUUID = npcUUID;
        this.minPlayers = minPlayers;
        this.maxPlayers = maxPlayers;
        this.isUpdate = isUpdate;
        this.creator = creator;
    }

    public String getName() {
        return name;
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

    @Nullable
    public Player getCreator() {
        return creator;
    }
}