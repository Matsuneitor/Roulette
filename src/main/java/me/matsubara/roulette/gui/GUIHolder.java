package me.matsubara.roulette.gui;

import me.matsubara.roulette.game.Game;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import javax.annotation.Nullable;

public final class GUIHolder implements InventoryHolder {

    private final GUI gui;
    private final GUIType type;
    private final Game game;

    public GUIHolder(@Nullable GUI gui, GUIType type, Game game) {
        this.gui = gui;
        this.type = type;
        this.game = game;
    }

    public GUI getGUI() {
        return gui;
    }

    public GUIType getType() {
        return type;
    }

    public Game getGame() {
        return game;
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public Inventory getInventory() {
        return (gui == null) ? null : gui.getInventory();
    }
}