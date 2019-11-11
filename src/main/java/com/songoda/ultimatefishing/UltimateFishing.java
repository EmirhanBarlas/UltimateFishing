package com.songoda.ultimatefishing;

import com.songoda.core.SongodaCore;
import com.songoda.core.SongodaPlugin;
import com.songoda.core.commands.CommandManager;
import com.songoda.core.compatibility.CompatibleMaterial;
import com.songoda.core.compatibility.ServerVersion;
import com.songoda.core.configuration.Config;
import com.songoda.core.configuration.ConfigSection;
import com.songoda.core.gui.GuiManager;
import com.songoda.core.hooks.EconomyManager;
import com.songoda.ultimatefishing.bait.Bait;
import com.songoda.ultimatefishing.bait.BaitManager;
import com.songoda.ultimatefishing.commands.*;
import com.songoda.ultimatefishing.listeners.*;
import com.songoda.ultimatefishing.lootables.LootablesManager;
import com.songoda.ultimatefishing.rarity.Rarity;
import com.songoda.ultimatefishing.rarity.RarityManager;
import com.songoda.ultimatefishing.settings.Settings;
import com.songoda.ultimatefishing.tasks.BaitParticleTask;
import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.PluginManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class UltimateFishing extends SongodaPlugin {

    private static UltimateFishing INSTANCE;

    private final Config rarityConfig = new Config(this, "rarity.yml");
    private final Config baitConfig = new Config(this, "bait.yml");

    private BaitParticleTask baitParticleTask;

    private final GuiManager guiManager = new GuiManager(this);
    private LootablesManager lootablesManager;
    private CommandManager commandManager;
    private RarityManager rarityManager;
    private BaitManager baitManager;

    public static UltimateFishing getInstance() {
        return INSTANCE;
    }

    @Override
    public void onPluginLoad() {
        INSTANCE = this;
    }

    @Override
    public void onPluginEnable() {
        // Run Songoda Updater
        SongodaCore.registerPlugin(this, 59, CompatibleMaterial.COD);

        // Load Economy
        EconomyManager.load();

        // Setup Config
        Settings.setupConfig();
        this.setLocale(Settings.LANGUGE_MODE.getString(), false);

        // Set economy preference
        EconomyManager.getManager().setPreferredHook(Settings.ECONOMY_PLUGIN.getString());

        // Register commands
        this.commandManager = new CommandManager(this);
        this.commandManager.addCommand(new CommandUltimateFishing(this))
                .addSubCommands(
                        new CommandSell(this, guiManager),
                        new CommandSellAll(this),
                        new CommandGive(this),
                        new CommandSettings(this, guiManager),
                        new CommandReload(this)
                );

        // Setup Lootables
        this.lootablesManager = new LootablesManager(this);
        this.lootablesManager.createDefaultLootables();
        this.getLootablesManager().getLootManager().loadLootables();

        // Setup Listeners
        guiManager.init();
        PluginManager pluginManager = Bukkit.getPluginManager();
        pluginManager.registerEvents(new FishingListeners(this), this);
        pluginManager.registerEvents(new FurnaceListeners(this), this);
        pluginManager.registerEvents(new EntityListeners(this), this);
        pluginManager.registerEvents(new InventoryListeners(this), this);
        pluginManager.registerEvents(new BlockListeners(this), this);

        // Start tasks
            this.baitParticleTask = BaitParticleTask.startTask(this);

        loadRarities();
        loadBaits();
    }

    @Override
    public void onPluginDisable() {
    }

    @Override
    public void onConfigReload() {
        this.setLocale(Settings.LANGUGE_MODE.getString(), true);
        this.getLootablesManager().getLootManager().loadLootables();
        this.loadRarities();
        this.loadBaits();
    }

    /*
     * Insert default fish sizes into config.
     */
    private void setupRarity() {
        if (!rarityConfig.contains("Rarity")) {
            rarityConfig.createDefaultSection("Rarity",
                    "The different levels of fish rarity.",
                    "You can rename, replace and add new rarities as you wish.")
                    .setDefault("Tiny.Chance", 15,
                            "The chance that a caught fish will be tiny.")
                    .setDefault("Tiny.Color", "9",
                            "The color used for the name tag.")
                    .setDefault("Tiny.Extra Health", -2,
                            "The amount of health on top of the initial health that the caught",
                            "fish grants.")
                    .setDefault("Tiny.Sell Price", 4.99,
                            "The price tiny fish will sell for.")
                    .setDefault("Tiny.Lure Chance Change", -5,
                            "The effect the lure fishing enchantment would have on the chance.",
                            "This is multiplied per enchantment level.")
                    .setDefault("Normal.Chance", 50)
                    .setDefault("Normal.Color", "7")
                    .setDefault("Normal.Extra Health", 0)
                    .setDefault("Normal.Sell Price", 19.99)
                    .setDefault("Normal.Lure Chance Change", -8)
                    .setDefault("Large.Chance", 25)
                    .setDefault("Large.Color", "c")
                    .setDefault("Large.Extra Health", 2)
                    .setDefault("Large.Sell Price", 49.99)
                    .setDefault("Large.Lure Chance Change", 5)
                    .setDefault("Huge.Chance", 10)
                    .setDefault("Huge.Color", "5")
                    .setDefault("Huge.Extra Health", 4)
                    .setDefault("Huge.Sell Price", 99.99)
                    .setDefault("Huge.Broadcast", true,
                            "Should we broadcast a message to all players when a huge fish",
                            "is caught?")
                    .setDefault("Huge.Lure Chance Change", 8);
        }
        rarityConfig.setRootNodeSpacing(1).setCommentSpacing(0);
    }

    /*
     * Insert default fish sizes into config.
     */
    private void setupBait() {
        if (!baitConfig.contains("Bait")) {
            baitConfig.createDefaultSection("Bait",
                    "The baits. You can rename, replace and add new baits as you wish.")
                    .setDefault("Worms.Bonus Chance", 100,
                            "The added chance (Weight) this bait will add towards the targets.")
                    .setDefault("Worms.Material", "SUNFLOWER",
                            "The material that represents this bait as an item.")
                    .setDefault("Worms.Uses", 3,
                            "The amount of uses this bait gets.")
                    .setDefault("Worms.Target", Arrays.asList("TINY", "NORMAL"),
                            "The added chance this bait will add towards the targets.")
                    .setDefault("Worms.Color", "9",
                            "The color used for the name tag.")
                    .setDefault("Worms.Sell Price", 4.99,
                            "The price worms will sell for.")

                    .setDefault("Super Worms.Bonus Chance", 25)
                    .setDefault("Super Worms.Material", "SUNFLOWER")
                    .setDefault("Super Worms.Uses", 3)
                    .setDefault("Super Worms.Target", Collections.singletonList("LARGE"))
                    .setDefault("Super Worms.Color", "c")
                    .setDefault("Super Worms.Sell Price", 19.99)

                    .setDefault("Ultra Worms.Bonus Chance", 25)
                    .setDefault("Ultra Worms.Material", "SUNFLOWER")
                    .setDefault("Ultra Worms.Uses", 3)
                    .setDefault("Ultra Worms.Target", Collections.singletonList("HUGE"))
                    .setDefault("Ultra Worms.Color", "5")
                    .setDefault("Ultra Worms.Sell Price", 49.99);
        }
        baitConfig.setRootNodeSpacing(1).setCommentSpacing(0);
    }

    private void loadRarities() {
        //Apply default fish rarity.
        rarityConfig.load();
        setupRarity();
        rarityConfig.saveChanges();
        this.rarityManager = new RarityManager();

        /*
         * Register rarities into RarityManager from Configuration.
         */
        if (rarityConfig.isConfigurationSection("Rarity")) {
            for (ConfigSection section : rarityConfig.getSections("Rarity")) {
                rarityManager.addRarity(new Rarity(
                        section.getNodeKey(),
                        section.getString("Color"),
                        section.getDouble("Chance"),
                        section.getInt("Extra Health"),
                        section.getDouble("Sell Price"),
                        section.getBoolean("Broadcast"),
                        section.getDouble("Lure Chance Change")));
            }
        }
    }

    private void loadBaits() {
        //Apply default baits.
        baitConfig.load();
        setupBait();
        baitConfig.saveChanges();
        this.baitManager = new BaitManager();

        /*
         * Register baits into BaitManager from Configuration.
         */
        if (baitConfig.isConfigurationSection("Bait")) {
            for (ConfigSection section : baitConfig.getSections("Bait")) {

                List<Rarity> targets = new ArrayList<>();

                if (section.contains("Target")) {
                    for (String target : section.getStringList("Target"))
                        if (rarityManager.isRarity(target))
                            targets.add(rarityManager.getRarity(target));
                } else
                    continue;

                baitManager.addBait(new Bait(
                        section.getNodeKey(),
                        section.getString("Color"),
                        section.getMaterial("Material").getMaterial(),
                        section.getDouble("Sell Price"),
                        section.getInt("Uses"),
                        targets,
                        section.getDouble("Bonus Chance")));
            }
        }
    }

    public BaitParticleTask getBaitParticleTask() {
        return baitParticleTask;
    }

    public LootablesManager getLootablesManager() {
        return lootablesManager;
    }

    public CommandManager getCommandManager() {
        return commandManager;
    }

    @Override
    public List<Config> getExtraConfig() {
        return Arrays.asList(rarityConfig);
    }

    public RarityManager getRarityManager() {
        return rarityManager;
    }

    public BaitManager getBaitManager() {
        return baitManager;
    }

    public static double calculateTotalValue(Inventory inventory) {
        double total = 0;
        for (ItemStack itemStack : inventory.getContents()) {
            Rarity rarity = INSTANCE.rarityManager.getRarity(itemStack);

            if (rarity == null) continue;
            total += rarity.getSellPrice() * itemStack.getAmount();
        }
        return total;
    }
}
