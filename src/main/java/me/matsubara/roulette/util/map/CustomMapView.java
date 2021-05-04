package me.matsubara.roulette.util.map;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
public final class CustomMapView implements MapView {

    private final int id;
    private final List<MapRenderer> renderers;

    public CustomMapView(int id) {
        this.id = id;
        this.renderers = new ArrayList<>();
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public boolean isVirtual() {
        return true;
    }

    @NotNull
    @Override
    public Scale getScale() {
        return Scale.NORMAL;
    }

    @Override
    public void setScale(@NotNull MapView.Scale scale) {

    }

    @Override
    public int getCenterX() {
        return 0;
    }

    @Override
    public int getCenterZ() {
        return 0;
    }

    @Override
    public void setCenterX(int x) {

    }

    @Override
    public void setCenterZ(int z) {

    }

    @Nullable
    @Override
    public World getWorld() {
        return Bukkit.getWorlds().get(0);
    }

    @Override
    public void setWorld(@NotNull World world) {

    }

    @NotNull
    @Override
    public List<MapRenderer> getRenderers() {
        return renderers;
    }

    @Override
    public void addRenderer(@NotNull MapRenderer renderer) {
        renderers.add(renderer);
    }

    @Override
    public boolean removeRenderer(@Nullable MapRenderer renderer) {
        return renderers.remove(renderer);
    }

    @Override
    public boolean isTrackingPosition() {
        return false;
    }

    @Override
    public void setTrackingPosition(boolean trackingPosition) {

    }

    @Override
    public boolean isUnlimitedTracking() {
        return true;
    }

    @Override
    public void setUnlimitedTracking(boolean unlimited) {

    }

    @Override
    public boolean isLocked() {
        return false;
    }

    @Override
    public void setLocked(boolean locked) {

    }
}