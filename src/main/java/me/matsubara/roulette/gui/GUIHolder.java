package me.matsubara.roulette.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public final class GUIHolder implements InventoryHolder {

    private final GUI gui;

    public GUIHolder(GUI gui) {
        this.gui = gui;
    }

    public GUI getGUI() {
        return gui;
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public Inventory getInventory() {
        return gui.getInventory();
    }
}