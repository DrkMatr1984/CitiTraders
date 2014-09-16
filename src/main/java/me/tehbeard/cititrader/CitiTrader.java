package me.tehbeard.cititrader;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.jar.Attributes;
import java.util.logging.Level;
import java.util.logging.Logger;




//import me.tehbeard.cititrader.commands.CitiCommands;
import me.tehbeard.cititrader.commands.CitiCommands;
import me.tehbeard.cititrader.traits.LinkedChestTrait;
import me.tehbeard.cititrader.traits.ShopTrait;
import me.tehbeard.cititrader.traits.StockRoomTrait;
import me.tehbeard.cititrader.traits.TraderTrait;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.trait.TraitInfo;
import net.milkbowl.vault.economy.Economy;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import com.twillen.cititrader.utils.CheckForUpdates;

/**
 * Provides a trader for
 * 
 * @author James
 * 
 */
public class CitiTrader extends JavaPlugin {

	public static final String PERM_PREFIX = "traders";
	public static CitiTrader self;
	public static Economy economy;
	public static boolean outdated = false;
	public static boolean isTowny = false;
	public static Attributes atts;
	public static String strVersionCheck = "";
	private FileConfiguration profiles = null;
	private File profilesFile = null;
	private FileConfiguration languages = null;
	private File languageFile = null;
	private CheckForUpdates updateTask = null;

	@Override
	public void onEnable() {
		setupConfig();
		reloadProfiles();
		reloadLanguage();

		setupTowny();

		self = this;

		if (setupEconomy()) {
			CitizensAPI.getTraitFactory().registerTrait(
					TraitInfo.create(ShopTrait.class).withName("shop"));
			CitizensAPI.getTraitFactory().registerTrait(
					TraitInfo.create(WalletTrait.class).withName("wallet"));
		} else {
			getLogger().severe(getLang().getString("error.noecon"));
		}

		CitizensAPI.getTraitFactory().registerTrait(
				TraitInfo.create(LinkedChestTrait.class)
						.withName("linkedchest"));
		CitizensAPI.getTraitFactory().registerTrait(
				TraitInfo.create(StockRoomTrait.class).withName("stockroom"));
		CitizensAPI.getTraitFactory().registerTrait(
				TraitInfo.create(TraderTrait.class).withName("villagetrader"));

		//Set up checking of updates
		if (getConfig().getBoolean("Check-For-Updates", true)) {
			//Runs once an hour
			updateTask = new CheckForUpdates(this);
			this.getServer().getScheduler().runTaskTimerAsynchronously(this, updateTask, 0, 60 * 1200);
		}
		else{
			CitiTrader.strVersionCheck = "New release checking disabled.";
		}
		//registerCommands();
		getCommand("trader").setExecutor(new CitiCommands(this));
		Bukkit.getPluginManager().registerEvents(new Trader(), this);
		getLogger().log(Level.INFO, "v{0} loaded",
				getDescription().getVersion());
	}

	@Override
	public void onDisable(){
		saveConfig();
		saveProfiles();
	}
	private boolean setupEconomy() {
		RegisteredServiceProvider<Economy> economyProvider = getServer()
				.getServicesManager().getRegistration(
						net.milkbowl.vault.economy.Economy.class);
		if (economyProvider != null) {
			economy = economyProvider.getProvider();
		}

		return (economy != null);
	}

	public enum Style {

		TRADER("trader"), VILLAGER("villagertrader");
		private String charName;

		private Style(String charName) {
			this.charName = charName;
		}

		public String getStyle() {
			return charName;
		}
	}

	public boolean isValidNPCType(Player player, String type) {
		return getConfig().getStringList("trader-types").contains(type);
	}

	public boolean isValidTraderStyle(Player player) {
		return true;// TODO: Proper checks when 1.3 hits
	}

	public int getTraderLimit(Player player) {
		int limit = getProfiles().getInt("profiles.default.trader-limit", 1);
		for (String s : getProfiles().getConfigurationSection("profiles")
				.getKeys(false)) {
			if (s.equals("default")) {
				continue;
			}
			if (player.hasPermission(PERM_PREFIX + ".profile." + s)) {
				limit = Math
						.max(getProfiles().getInt(
								"profiles." + s + ".trader-limit"), limit);
			}

		}

		return limit;
	}

	public int getChestLimit(Player player) {
		int limit = getProfiles().getInt("profiles.default.chest-limit", 1);
		for (String s : getProfiles().getConfigurationSection("profiles")
				.getKeys(false)) {
			if (s.equals("default")) {
				continue;
			}
			if (player.hasPermission(PERM_PREFIX + ".profile." + s)) {
				limit = Math.max(
						getProfiles().getInt("profiles." + s + ".chest-limit"),
						limit);
			}
		}
		return limit;
	}

	public NPC isChestLinked(Location loc) {
		for (NPC npc : CitizensAPI.getNPCRegistry()) {
			if (npc.hasTrait(LinkedChestTrait.class)) {
				// if (npc.hasTrait(ShopTrait.class)) {
				if (npc.getTrait(LinkedChestTrait.class).hasLinkedChest()) {
					if (npc.getTrait(LinkedChestTrait.class).getLinkedChests()
							.containsKey(loc)) {
						return npc;
					}
				}
			}
		}

		return null;
	}

	public void setupTowny() {
		if (Bukkit.getPluginManager().getPlugin("Towny") != null) {
			if (getServer().getPluginManager().getPlugin("Towny").isEnabled() == true) {
				CitiTrader.isTowny = true;
			}
		}
	}

	public void setupConfig() {

		getConfig();
		getConfig().options().copyDefaults(true);
		this.saveConfig();
	}

	@SuppressWarnings("deprecation")
	public void reloadProfiles() {
		profilesFile = new File(this.getDataFolder(), "profiles.yml");
		profiles = YamlConfiguration.loadConfiguration(profilesFile);

		// Look for defaults in the jar
		InputStream defConfigStream = this.getResource("profiles.yml");

		if (defConfigStream != null && !profilesFile.exists()) {
			YamlConfiguration defConfig = YamlConfiguration
					.loadConfiguration(defConfigStream);
			profiles.setDefaults(defConfig);
			profiles.options().copyDefaults(true);
		}
		this.saveProfiles();
	}

	public FileConfiguration getProfiles() {
		if (profiles == null) {
			this.reloadProfiles();
		}
		return profiles;
	}

	public void saveProfiles() {
		if (profiles == null || profilesFile == null) {
			return;
		}
		try {
			getProfiles().save(profilesFile);
		} catch (IOException ex) {
			this.getLogger().log(Level.SEVERE,
					"Could not save config to " + profilesFile, ex);
		}
	}

	@SuppressWarnings("deprecation")
	public void reloadLanguage() {
		languageFile = new File(this.getDataFolder() + File.separator + "lang",
				getConfig().getString("language") + ".yml");
		languages = YamlConfiguration.loadConfiguration(languageFile);

		// Look for defaults in the jar
		InputStream defConfigStream = this.getResource("en.yml");

		if (defConfigStream != null) {
			YamlConfiguration defConfig = YamlConfiguration
					.loadConfiguration(defConfigStream);
			languages.setDefaults(defConfig);
			languages.options().copyDefaults(true);
		}
		this.saveLanguage();
	}

	@SuppressWarnings("deprecation")
	public void resetLanguage() {
		InputStream defConfigStream = this.getResource("en.yml");
		languages = YamlConfiguration.loadConfiguration(defConfigStream);
		this.saveLanguage();
	}

	public FileConfiguration getLang() {
		if (languages == null) {
			this.reloadLanguage();
		}
		return languages;
	}

	public void saveLanguage() {
		if (languages == null || languageFile == null) {
			return;
		}
		try {
			languages.save(languageFile);
		} catch (IOException ex) {
			this.getLogger().log(Level.SEVERE,
					"Could not save config to " + languageFile, ex);
		}
	}

	public boolean isMayorOrAssistant(Player player) {
		if (!CitiTrader.isTowny) {
			return false;
		}

		com.palmergames.bukkit.towny.object.Resident resident;

		try {
			resident = com.palmergames.bukkit.towny.object.TownyUniverse
					.getDataSource().getResident(player.getName());
		} catch (Exception ex) {
			Logger.getLogger(CitiTrader.class.getName()).log(Level.SEVERE,
					null, ex);

			return false;
		}

		if (resident.hasTown()) {
			try {
				com.palmergames.bukkit.towny.object.Town town = resident
						.getTown();
				if (resident.isMayor()
						|| town.getAssistants().contains(resident)) {
					return true;
				}
			} catch (Exception ex) {
				Logger.getLogger(WalletTrait.class.getName()).log(Level.SEVERE,
						null, ex);
				return false;
			}
		}
		return false;
	}

	public String getTownBank(Player player) {
		if (!CitiTrader.isTowny) {
			return null;
		}

		if (!isMayorOrAssistant(player)) {
			return null;
		}

		com.palmergames.bukkit.towny.object.Resident resident;

		try {
			resident = com.palmergames.bukkit.towny.object.TownyUniverse
					.getDataSource().getResident(player.getName());
		} catch (Exception ex) {
			Logger.getLogger(CitiTrader.class.getName()).log(Level.SEVERE,
					null, ex);

			return null;
		}
		try {
			return resident.getTown().getEconomyName();
		} catch (Exception ex) {
			Logger.getLogger(CitiTrader.class.getName()).log(Level.SEVERE,
					null, ex);
		}

		return null;
	}

	
	public void checkVersion() {
		if(updateTask == null)
			return;
		strVersionCheck = updateTask.strVersionCheck;
	}
	
	public void forceVersionCheck(){
		if(updateTask == null)
			return;
		updateTask.run();
	}
	
}
