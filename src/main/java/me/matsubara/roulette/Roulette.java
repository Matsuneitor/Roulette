package me.matsubara.roulette;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.gmail.filoghost.holographicdisplays.api.Hologram;
import com.gmail.filoghost.holographicdisplays.api.HologramsAPI;
import me.matsubara.roulette.command.Main;
import me.matsubara.roulette.event.PlayerJumpEvent;
import me.matsubara.roulette.file.Chips;
import me.matsubara.roulette.file.Configuration;
import me.matsubara.roulette.file.Games;
import me.matsubara.roulette.file.Messages;
import me.matsubara.roulette.file.winner.Winners;
import me.matsubara.roulette.game.Game;
import me.matsubara.roulette.listener.*;
import me.matsubara.roulette.listener.clearlag.EntityRemove;
import me.matsubara.roulette.listener.npc.citizens.NPCRightClick;
import me.matsubara.roulette.listener.npc.npclib.ChunkLoad;
import me.matsubara.roulette.listener.npc.npclib.NPCInteract;
import me.matsubara.roulette.listener.PlayerJoin;
import me.matsubara.roulette.listener.npc.npclib.PlayerRespawn;
import me.matsubara.roulette.listener.npc.npclib.PlayerSpawnLocation;
import me.matsubara.roulette.listener.protocol.SteerVehicle;
import me.matsubara.roulette.papi.RoulettePlaceholder;
import me.matsubara.roulette.trait.LookCloseModified;
import me.matsubara.roulette.trait.SneakingTrait;
import me.matsubara.roulette.util.RUtils;
import me.matsubara.roulette.util.UpdateChecker;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.trait.TraitFactory;
import net.citizensnpcs.api.trait.TraitInfo;
import net.jitse.npclib.NPCLib;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Entity;
import org.bukkit.event.Listener;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.reflections.Reflections;

import java.io.File;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

public final class Roulette extends JavaPlugin {

    private Economy economy;
    private NPCLib npcLib;

    // Files.
    private Chips chips;
    private Configuration configuration;
    private Games games;
    private Messages messages;
    private Winners winners;

    private final String[] DEPENDENCIES = {"ProtocolLib", "Vault"};

    public static boolean USE_HOLOGRAPHIC;
    public static boolean USE_CITIZENS;

    @Override
    public void onEnable() {
        if (!versionAllowed()) {
            getLogger().severe("This plugin doesn't support your server version, disabling...");
            setEnabled(false);
            return;
        }

        if (!hasDependencies(DEPENDENCIES)) {
            getLogger().severe("You need to install all the dependencies to be able to use this plugin, disabling...");
            setEnabled(false);
            return;
        }

        if (hasNPCAPI() == -1) {
            getLogger().severe("You need to install an NPC API (Citizens or NPCLib) to use this plugin, disabling...");
            setEnabled(false);
            return;
        }

        if (hasHologramAPI() == -1) {
            getLogger().severe("You need to install a hologram API (HolographicDisplays or CMI) to use this plugin, disabling...");
            setEnabled(false);
            return;
        }

        if (!setupEconomy()) {
            getLogger().severe("You need to install an economy provider (like EssentialsX, CMI, etc...) to be able to use this plugin, disabling...");
            setEnabled(false);
            return;
        }

        // If the server has both, Citizens & NPCLib, will use Citizens by default.
        switch (hasNPCAPI()) {
            case 0:
            case 2:
                TraitFactory factory = CitizensAPI.getTraitFactory();
                factory.registerTrait(TraitInfo.create(LookCloseModified.class).withName("lookclosemodified"));
                factory.registerTrait(TraitInfo.create(SneakingTrait.class).withName("sneaking"));
                USE_CITIZENS = true;
                break;
            case 1:
                this.npcLib = new NPCLib(this);
                this.npcLib.setAutoHideDistance(Double.MAX_VALUE);
                USE_CITIZENS = false;
                break;
        }

        // If the server has both, HD & CMI, will use HD by default.
        switch (hasHologramAPI()) {
            case 0:
            case 2:
                // Delete every hologram and placeholder to prevent duplicates.
                HologramsAPI.getHolograms(this).forEach(Hologram::delete);
                HologramsAPI.unregisterPlaceholders(this);
                USE_HOLOGRAPHIC = true;
                break;
            case 1:
                USE_HOLOGRAPHIC = false;
                break;
        }

        // If using CMI intead of HD, we need PAPI for placeholders.
        if (!USE_HOLOGRAPHIC && !hasDependency("PlaceholderAPI")) {
            getLogger().severe("Since you're using CMI intead of HD, you need to install PlaceholderAPI to use this plugin, disabling...");
            setEnabled(false);
            return;
        }

        // Remove entities from bukkit worlds.
        removeEntities(getServer().getWorlds());

        ProtocolLibrary.getProtocolManager().addPacketListener(new SteerVehicle(this, ListenerPriority.HIGHEST, PacketType.Play.Client.STEER_VEHICLE));

        // Register our placeholder manager if PAPI is present.
        if (hasDependency("PlaceholderAPI")) new RoulettePlaceholder(this).register();

        PluginManager manager = getServer().getPluginManager();
        if (USE_CITIZENS) {
            manager.registerEvents(new NPCRightClick(this), this);
        } else {
            // These events are only necessary if using NPCLib.
            manager.registerEvents(new ChunkLoad(this), this);
            manager.registerEvents(new NPCInteract(this), this);
            manager.registerEvents(new PlayerRespawn(this), this);
            manager.registerEvents(new PlayerSpawnLocation(this), this);
        }
        if (hasDependency("ClearLag")) {
            manager.registerEvents(new EntityRemove(this), this);
        }
        PlayerJumpEvent.register(this);
        manager.registerEvents(new EntityDamageByEntity(this), this);
        manager.registerEvents(new EntitySpawn(this), this);
        manager.registerEvents(new EntityDismount(this), this);
        manager.registerEvents(new InventoryClick(this), this);
        manager.registerEvents(new InventoryClose(this), this);
        manager.registerEvents(new PlayerArmorStandManipulate(this), this);
        manager.registerEvents(new PlayerChangedWorld(this), this);
        //manager.registerEvents(new PlayerItemHeld(this), this);
        manager.registerEvents(new PlayerJoin(this), this);
        manager.registerEvents(new PlayerJump(this), this);
        manager.registerEvents(new PlayerQuit(this), this);

        /*
        // Save image if doens't exist.
        File image = new File(getDataFolder(), "image.png");
        if (!image.exists()) saveResource("image.png", false);*/

        // registerListeners();

        chips = new Chips(this);
        configuration = new Configuration(this);
        messages = new Messages(this);
        winners = new Winners(this);

        // Start games task after 1 second.
        new BukkitRunnable() {
            @Override
            public void run() {
                games = new Games(Roulette.this);
            }
        }.runTaskLater(this, 20L);

        PluginCommand roulette = getCommand("roulette");
        if (roulette != null) {
            Main main = new Main(this);
            roulette.setExecutor(main);
            roulette.setTabCompleter(main);
        }

        saveDefaultConfig();

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!Configuration.Config.UPDATE_CHECKER.asBoolean()) return;

                UpdateChecker.init(Roulette.this, 82197).requestUpdateCheck().whenComplete((result, throwable) -> {
                    Logger logger = Roulette.this.getLogger();
                    logger.info("Checking for new updates...");

                    if (result.requiresUpdate()) {
                        logger.info(String.format("A new version is now available! (%s)", result.getNewestVersion()));
                        return;
                    }

                    UpdateChecker.UpdateReason reason = result.getReason();
                    if (reason == UpdateChecker.UpdateReason.UP_TO_DATE) {
                        logger.info(String.format("Roulette is up to date! (%s)", result.getNewestVersion()));
                    } else {
                        logger.warning(String.format("Could not check for new updates! (%s)", reason));
                    }
                });
            }
        }.runTaskLater(this, 1L);
    }

    @Override
    public void onDisable() {
        if (games == null) return;
        if (games.getGamesSet() == null) return;

        // Remove all games parts from the game except the NPC, for /reload.
        getLogger().info("Removing games from the server...");
        Iterator<Game> iterator = games.getGamesSet().iterator();
        while (iterator.hasNext()) {
            Game current = iterator.next();
            current.delete(false, true, true);
            iterator.remove();
        }
    }

    public void removeEntities(List<World> worlds) {
        NamespacedKey key = new NamespacedKey(this, "fromRoulette");
        worlds.forEach(world -> world.getEntities().stream()
                .filter(entity -> entity.getPersistentDataContainer().has(key, PersistentDataType.STRING))
                .forEach(Entity::remove));
    }

    public NPCLib getNPCLibrary() {
        return npcLib;
    }

    private boolean versionAllowed() {
        return RUtils.getMajorVersion() > 13;
    }

    public boolean hasDependencies(String... dependencies) {
        for (String plugin : dependencies) {
            if (!hasDependency(plugin)) return false;
        }
        return true;
    }

    private void registerListeners() {
        for (Class<?> listenerClass : new Reflections(getClass().getPackage().getName() + ".listener").getSubTypesOf(Listener.class)) {
            try {
                Listener listener = (Listener) listenerClass.getDeclaredConstructor(Roulette.class).newInstance(this);
                getServer().getPluginManager().registerEvents(listener, this);
            } catch (ReflectiveOperationException exception) {
                exception.printStackTrace();
            }
        }
    }

    /**
     * Check if the server has at least one hologram API.
     *
     * @return -1 of none, 0 if HD, 1 if CMI and 2 if both.
     */
    private int hasHologramAPI() {
        if (hasDependencies("HolographicDisplays", "CMI")) return 2;
        if (hasDependency("HolographicDisplays")) return 0;
        if (hasDependency("CMI")) return 1;
        return -1;
    }

    /**
     * Check if the server has at least one hologram API.
     *
     * @return -1 of none, 0 if Citizens, 1 if NPCLibPlugin and 2 if both.
     */
    private int hasNPCAPI() {
        if (hasDependencies("Citizens", "NPCLibPlugin")) return 2;
        if (hasDependency("Citizens")) return 0;
        if (hasDependency("NPCLibPlugin")) return 1;
        return -1;
    }

    public boolean hasDependency(String plugin) {
        return getServer().getPluginManager().isPluginEnabled(plugin);
    }

    private boolean setupEconomy() {
        RegisteredServiceProvider<Economy> provider = getServer().getServicesManager().getRegistration(Economy.class);
        if (provider == null) return false;
        economy = provider.getProvider();
        return true;
    }

    public Economy getEconomy() {
        return economy;
    }

    public Chips getChips() {
        return chips;
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public Games getGames() {
        return games;
    }

    public Messages getMessages() {
        return messages;
    }

    public Winners getWinners() {
        return winners;
    }
}