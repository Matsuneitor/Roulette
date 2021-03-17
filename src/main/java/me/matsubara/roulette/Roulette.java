package me.matsubara.roulette;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.gmail.filoghost.holographicdisplays.api.Hologram;
import com.gmail.filoghost.holographicdisplays.api.HologramsAPI;
import me.matsubara.roulette.command.Main;
import me.matsubara.roulette.file.Chips;
import me.matsubara.roulette.file.Configuration;
import me.matsubara.roulette.file.Games;
import me.matsubara.roulette.file.Messages;
import me.matsubara.roulette.listener.*;
import me.matsubara.roulette.listener.protocol.SteerVehicle;
import me.matsubara.roulette.trait.LookCloseModified;
import me.matsubara.roulette.util.CyclicPlaceholderReplacer;
import me.matsubara.roulette.util.RUtils;
import me.matsubara.roulette.util.UpdateChecker;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.trait.TraitInfo;
import net.md_5.bungee.api.ChatColor;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Logger;

public final class Roulette extends JavaPlugin {

    private Economy economy;

    // Files.
    private Chips chips;
    private Configuration configuration;
    private Games games;
    private Messages messages;

    private final String[] DEPENDENCIES = {"Citizens", "HolographicDisplays", "ProtocolLib", "Vault"};

    @Override
    public void onEnable() {
        if (!versionAllowed()) {
            getLogger().severe("This plugin doesn't support your server version, disabling...");
            setEnabled(false);
            return;
        }
        if (!hasDependencies()) {
            getLogger().severe("You need to install all the dependencies to be able to use this plugin, disabling...");
            setEnabled(false);
            return;
        }

        if (!setupEconomy()) {
            getLogger().severe("You need to install an economy provider (like EssentialsX, CMI, etc...) to be able to use this plugin, disabling...");
            setEnabled(false);
            return;
        }

        // Delete every hologram and placeholder to prevent duplicates.
        HologramsAPI.getHolograms(this).forEach(Hologram::delete);
        HologramsAPI.unregisterPlaceholders(this);

        ProtocolLibrary.getProtocolManager().addPacketListener(new SteerVehicle(this, ListenerPriority.HIGHEST, PacketType.Play.Client.STEER_VEHICLE));

        CitizensAPI.getTraitFactory().registerTrait(TraitInfo.create(LookCloseModified.class).withName("lookclosemodified"));

        // Animated rainbow color.
        HologramsAPI.registerPlaceholder(this, "&u", 0.2d, new CyclicPlaceholderReplacer(RUtils.arrayToStrings(
                ChatColor.RED,
                ChatColor.GOLD,
                ChatColor.YELLOW,
                ChatColor.GREEN,
                ChatColor.AQUA,
                ChatColor.LIGHT_PURPLE)));

        PluginManager manager = getServer().getPluginManager();
        manager.registerEvents(new EntityDamageByEntity(this), this);
        manager.registerEvents(new EntityDismount(this), this);
        manager.registerEvents(new InventoryClick(this), this);
        manager.registerEvents(new InventoryClose(this), this);
        manager.registerEvents(new PlayerArmorStandManipulate(this), this);
        manager.registerEvents(new PlayerChangedWorld(this), this);
        manager.registerEvents(new PlayerQuit(this), this);

        chips = new Chips(this);
        configuration = new Configuration(this);
        games = new Games(this);
        messages = new Messages(this);

        PluginCommand roulette = getCommand("roulette");
        if (roulette != null) {
            Main main = new Main(this);
            roulette.setExecutor(main);
            roulette.setTabCompleter(main);
        }

        saveDefaultConfig();

        if (!configuration.updateCheck()) return;

        UpdateChecker.init(this, 82197).requestUpdateCheck().whenComplete((result, throwable) -> {
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

    private boolean versionAllowed() {
        return RUtils.getMajorVersion() > 13;
    }

    private boolean hasDependencies() {
        for (String plugin : DEPENDENCIES) {
            if (!getServer().getPluginManager().isPluginEnabled(plugin)) return false;
        }
        return true;
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
}