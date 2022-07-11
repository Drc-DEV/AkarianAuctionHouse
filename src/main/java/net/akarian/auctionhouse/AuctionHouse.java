package net.akarian.auctionhouse;

import lombok.Getter;
import lombok.Setter;
import net.akarian.auctionhouse.commands.AuctionHouseCommand;
import net.akarian.auctionhouse.commands.CommandManager;
import net.akarian.auctionhouse.cooldowns.CooldownManager;
import net.akarian.auctionhouse.events.AuctionHouseGUIEvents;
import net.akarian.auctionhouse.events.ExpireJoinEvent;
import net.akarian.auctionhouse.events.UpdateJoinEvent;
import net.akarian.auctionhouse.listings.ListingManager;
import net.akarian.auctionhouse.updater.UpdateManager;
import net.akarian.auctionhouse.utils.*;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.logging.Level;

public final class AuctionHouse extends JavaPlugin {

    @Getter
    private Economy econ;
    @Getter
    private static AuctionHouse instance;
    @Getter
    private ListingManager listingManager;
    @Getter
    private Chat chat;
    @Getter
    @Setter
    private DatabaseType databaseType;
    @Getter
    private NameManager nameManager;
    @Getter
    private MySQL mySQL;
    @Getter
    private FileManager fileManager;
    @Getter
    private GUIManager guiManager;
    @Getter
    private UpdateManager updateManager;
    @Getter
    private boolean update;
    @Getter
    private Messages messages;
    @Getter
    private Configuration configFile;
    @Getter
    private CooldownManager cooldownManager;

    @Override
    public void onEnable() {
        getLogger().log(Level.INFO, "========== Akarian Auction House ==========");
        getLogger().log(Level.INFO, " ");
        getLogger().log(Level.INFO, "Loading Akarian Auction House v" + getDescription().getVersion() + "...");
        instance = this;
        this.fileManager = new FileManager(this);
        this.configFile = new Configuration();
        chat = new Chat(this, getConfigFile().getPrefix());
        nameManager = new NameManager();
        this.messages = new Messages();
        mySQL = new MySQL();
        update = getConfigFile().isUpdates();
        updateManager = new UpdateManager(this);
        guiManager = new GUIManager();
        cooldownManager = new CooldownManager();
        getLogger().log(Level.INFO, "Setting up Economy...");
        if (!setupEconomy()) {
            chat.alert("&cAuctionHouse has Failed to detect an economy.");
            chat.log("AuctionHouse disabled due to no found economy.");
            setEnabled(false);
            return;
        }
        getLogger().log(Level.INFO, "Successfully hooked into " + econ.getName() + ".");
        getLogger().log(Level.INFO, "Loading database...");
        // Set the database storage type
        switch (Objects.requireNonNull(getConfigFile().getDatabaseType()).toUpperCase(Locale.ROOT)) {
            case "FILE":
                databaseType = DatabaseType.FILE;
                if (!fileManager.getFile("/database/listings").exists()) {
                    fileManager.createFile("/database/listings");
                }
                if (!fileManager.getFile("/database/expired").exists()) {
                    fileManager.createFile("/database/expired");
                }
                if (!fileManager.getFile("/database/completed").exists()) {
                    fileManager.createFile("/database/completed");
                }
                break;
            case "FILE2MYSQL":
                databaseType = DatabaseType.FILE2MYSQL;
                mySQL.setup();
                break;
            case "MYSQL":
                databaseType = DatabaseType.MYSQL;
                mySQL.setup();
                break;
            case "MYSQL2FILE":
                databaseType = DatabaseType.MYSQL2FILE;
                mySQL.setup();
                break;

        }
        getLogger().log(Level.INFO, "Successfully connected to " + databaseType.getStr() + " database.");
        if (databaseType != DatabaseType.FILE) {
            if (mySQL.isConnected()) getLogger().log(Level.INFO, "Successfully loaded MySQL.");
            else getLogger().log(Level.SEVERE, "Failed loaded MySQL.");
        }
        getLogger().log(Level.INFO, "Loading listings...");
        this.listingManager = new ListingManager();
        getLogger().log(Level.INFO, "Listing loaded successfully.");
        registerCommands();
        registerEvents();

        int pluginId = 15488;
        Metrics metrics = new Metrics(this, pluginId);

        metrics.addCustomChart(new Metrics.SingleLineChart("active_listings", new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                return listingManager.getListings().size();
            }
        }));
        getLogger().log(Level.INFO, "Finished loading Akarian Auction House. Enjoy!");
        getLogger().log(Level.INFO, "=================================================");
    }

    private void registerCommands() {
        new CommandManager();
        this.getCommand("auctionhouse").setExecutor(new AuctionHouseCommand());
    }

    private void registerEvents() {
        PluginManager pm = Bukkit.getPluginManager();
        pm.registerEvents(new AuctionHouseGUIEvents(), this);
        pm.registerEvents(new ExpireJoinEvent(), this);
        pm.registerEvents(new UpdateJoinEvent(), this);
        pm.registerEvents(guiManager, this);
    }

    @Override
    public void onDisable() {
        if (databaseType != DatabaseType.FILE) {
            mySQL.shutdown();
        }
        guiManager.closeAllInventories();
        cooldownManager.saveCooldowns();
    }

    private boolean setupEconomy() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            return false;
        }

        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        econ = rsp.getProvider();
        return econ != null;
    }

    public String encode(ItemStack itemStack, boolean asOne) {
        ItemStack local = itemStack;
        if (asOne) {
            local.setAmount(1);
        }
        String encodedObj;

        try {
            ByteArrayOutputStream io = new ByteArrayOutputStream();
            BukkitObjectOutputStream os = new BukkitObjectOutputStream(io);
            os.writeObject(local);
            os.flush();

            byte[] serializedObj = io.toByteArray();

            encodedObj = Base64.getEncoder().encodeToString(serializedObj);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return encodedObj;
    }

    /**
     * Decode a String to an ItemStack with Base64
     *
     * @param string - String of ItemStack encoded with Base64
     * @return - Decoded ItemStack
     */
    public ItemStack decode(String string) {
        try {
            byte[] serializedObj = Base64.getDecoder().decode(string);
            ByteArrayInputStream in = new ByteArrayInputStream(serializedObj);
            BukkitObjectInputStream is = new BukkitObjectInputStream(in);

            return (ItemStack) is.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

}
