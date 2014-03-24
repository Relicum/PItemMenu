/*
 PItemMenu v1.2.2 by Plancke
 Feel free to use all of the code in here for your
 own projects but please give me credit when you do.
 */

package nl.plancke.pitemmenu;

import java.io.*;
import java.util.*;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.command.*;
import org.bukkit.configuration.file.*;

import net.milkbowl.vault.economy.Economy;
import static nl.plancke.pitemmenu.Functions.*;

public final class PItemMenu extends JavaPlugin implements Listener {
	public static String prefix;
	public static Server server;
	public static ConsoleCommandSender console;
	public static FileConfiguration config, locale, specialItem;
	public static Map<Player, FileConfiguration> players = new HashMap<Player, FileConfiguration>();
	public static Map<String, Inventory> inventories = new HashMap<String, Inventory>();
	public static Map<String, FileConfiguration> menus = new HashMap<String, FileConfiguration>();
	public static File dataFolder;
	public static String version;

	public static Economy econ = null;

	@Override
	public void onEnable() {
		dataFolder = this.getDataFolder();
		server = getServer();
		console = server.getConsoleSender();
		version = getDescription().getVersion();

		reloadConfigs();
		checkOps();

		server.getPluginManager().registerEvents(new Events(), this);


		debugMessage("Checking for updates");
		if(Updater.hasUpdate()) {
			ArrayList<String> info = Updater.getInfo();
			for(String line : info) {
				tagMessage(line);
			}
		} else { debugMessage("No updates found"); }

		setupVault(getServer().getPluginManager());

		tagMessage("Enabled v" + version + "!");
	}
	@Override
	public void onDisable() {
		tagMessage("Disabled v" + version + "!");
	}  

	private void setupVault(PluginManager pm) {
		Plugin vault =  pm.getPlugin("Vault");
		if (vault != null && vault instanceof net.milkbowl.vault.Vault) {
			debugMessage("Loaded Vault v" + vault.getDescription().getVersion());
			setupEconomy();
		}
	}

	private boolean setupEconomy(){
		if (server.getPluginManager().getPlugin("Vault") == null) { return false; }
		debugMessage("Trying to enable economy...");
		RegisteredServiceProvider<Economy> economyProvider = getServer().getServicesManager().getRegistration(Economy.class);
		if (economyProvider != null) {
			econ = economyProvider.getProvider();
		} else {
			debugMessage("Economy Failed!");
			return false;
		}
		debugMessage("Economy enabled!");
		return (econ != null);
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args){
		if(cmd.getName().equalsIgnoreCase("menu")){

			if(args.length == 0) { return showUsage(sender); }

			if(args[0].equalsIgnoreCase("admin")) {
				if(args.length == 1) { return showUsage(sender); } 

				if(args[1].equalsIgnoreCase("reload") && (sender.hasPermission("menu.admin.reload") || sender.isOp())) {
					reloadConfigs();
					tagMessage(getLocale("reload")); 
					if(sender instanceof Player) {
						Player Player = (Player) sender;
						tagMessage(getLocale("reload"), Player); 
					}
				}
				return true;
			}

			if(args[0].equalsIgnoreCase("open")) {
				if(!(sender instanceof Player)){ // Console Check
					tagMessage(getLocale("onlyPlayers")); 
					return true;
				}

				if(args.length == 1){ return showUsage(sender); }

				Player player = (Player) sender;

				String name = args[1];
				if(sender.isOp() || sender.hasPermission("menu.open." + name)) { // Permission Check					
					OpenMenu(player, name);
				} else {
					tagMessage(getLocale("menu.notPerms").replace("%menu%", name), player);
				}
				return true;
			}
			return showUsage(sender);
		} 
		return false; 
	}

	private static ItemStack setName(ItemStack itemStack, String name, ArrayList<String> lore) {
		ItemMeta itemMeta = itemStack.getItemMeta();
		if (name != null)
			itemMeta.setDisplayName(colorize(name));
		if (lore != null)
			itemMeta.setLore(colorizeArray(lore));
		itemStack.setItemMeta(itemMeta);
		return itemStack;
	}

	public static void OpenMenu(Player player, String menuName) {
		File menuFile = new File(dataFolder + File.separator + "menus" + File.separator + menuName + ".yml");
		if(!menuFile.exists()) { // Check if the menu exists
			tagMessage(getLocale("menu.notExist").replace("%menu%", menuName), player);
			return;
		} 

		if(config.getBoolean("useCache", true)) {
			if(inventories.containsKey(menuName) && menus.containsKey(menuName)) {
				debugMessage("Using cached menu [" + menuName + "]");
				player.openInventory(inventories.get(menuName));
				players.put(player, menus.get(menuName));
				return;
			}
			debugMessage("No cache found, building menu [" + menuName + "]");
		}

		FileConfiguration specialItems = YamlConfiguration.loadConfiguration(new File(dataFolder + "//special-items.yml"));
		FileConfiguration curMenu, menu = YamlConfiguration.loadConfiguration(menuFile); // Load Menu from Path
		FileConfiguration playerMenu = new YamlConfiguration();
		Set<String> items = menu.getConfigurationSection("items").getKeys(false);

		Integer maxSlot = 0;
		for(String slot : items ){ if(Integer.parseInt(slot) > maxSlot) { maxSlot = Integer.parseInt(slot); } }
		Integer size = (int) ((maxSlot - 1) / 9 + 1) * 9;
		if(size > 54) { tagMessage(getLocale("menu.tooBig").replace("%size%", size.toString()), player); return; }

		String title = replaceVars(player, menu.getString("title"));

		ItemStack item = null;
		
		String itemfee = menu.getString("itemfee", null);
		if(!player.hasPermission("menu.itemfee.bypass." + menuName)) {
			if(itemfee != null) {
				Material feemat = Material.getMaterial(Integer.parseInt(itemfee.split(",")[0].split(":")[0]));
				Short feedur = 0; try{ feedur = Short.parseShort(itemfee.split(",")[0].split(":")[1]);}catch(Exception e){ feedur = 0; }
				Integer feeamount= Integer.parseInt(itemfee.split(",")[1]);

				item = new ItemStack(feemat, feeamount, feedur);
				debugMessage("Itemfee found: " + item.toString());

				if(player.getInventory().containsAtLeast(item, feeamount)) {
					player.getInventory().removeItem(item);
					debugMessage("Menu access granted!");
				} else {
					tagMessage(getLocale("fee.item").replaceAll("%item%", item.getType().name()).replaceAll("%amount%", Integer.toString(feeamount)), player);
					return;
				}
			}
		}

		Double moneyfee = menu.getDouble("moneyfee", 0);
		if(!player.hasPermission("menu.moneyfee.bypass." + menuName)) {
			if(moneyfee != 0) {
				debugMessage("Moneyfee found: " + moneyfee);
				if(econ.getBalance(player.getName()) >= moneyfee) {
					if(econ.withdrawPlayer(player.getName(), moneyfee).transactionSuccess()) {
						debugMessage("Player was granted access to the menu!");
					} else {
						debugMessage("Something went wrong while removong funds for " + player.getName());
						if(itemfee != null) { player.getInventory().addItem(item); }
						return;
					}
				} else {
					tagMessage(getLocale("fee.money").replaceAll("%amount%", econ.format(moneyfee)), player);
					if(itemfee != null) { player.getInventory().addItem(item); }
					return;
				}
			}
		}

		debugMessage("Player was granted access to the menu!");

		// Construct the menu
		debugMessage("Starting to build Menu.");
		debugMessage("Size: [" + size + "]");

		Inventory inventory = Bukkit.getServer().createInventory(player, size, title);
		for(String curItem : items ){
			String itemPath = "items.";
			curMenu = menu;
			Integer pos = Integer.parseInt(curItem) - 1;

			// Special Item check
			if(curMenu.get(itemPath + curItem) instanceof String) {
				String specialItem = (String) curMenu.get(itemPath + curItem);
				Set<String> specialItemNames = (Set<String>) specialItems.getKeys(false);
				if(specialItemNames.contains(specialItem)) {
					curItem = specialItem;
					curMenu = specialItems;
					itemPath = "";
				} else { continue; } // Go back to start of loop, Special item was not found
			}

			// Durability check in Type
			String type = curMenu.get(itemPath + curItem + ".type", "1").toString();
			Integer itemID = 0;
			short durability = 0; 
			if(type.contains(":")) {
				String[] typeData = type.split(":");
				itemID = Integer.parseInt(typeData[0]);
				durability = Short.parseShort(typeData[1]);
			} else {
				itemID = Integer.parseInt(type);
			}

			// General Item data gathering
			int amount = curMenu.getInt(itemPath + curItem + ".amount", 1);
			String permission = curMenu.getString(itemPath + curItem + ".permission", null);
			String display = replaceVars(player, curMenu.getString(itemPath + curItem + ".display", null));
			if(permission != null) { if(curMenu.getBoolean(itemPath + curItem + ".hide", false) && !player.hasPermission(permission)) { continue; } }
			itemfee = curMenu.getString(itemPath + curItem + ".itemfee", null);
			moneyfee = curMenu.getDouble(itemPath + curItem + ".moneyfee", 0);

			// Command gathering
			ArrayList<String> command = new ArrayList<String>(), lore = new ArrayList<String>();
			if(curMenu.get(itemPath + curItem + ".command") instanceof String) { // Catch single lined commands
				command.add(curMenu.getString(itemPath + curItem + ".command"));
			} else {
				for(String commandString : curMenu.getStringList(itemPath + curItem + ".command")) {
					command.add(commandString);
				}
			}

			try {
				if(curMenu.get(itemPath + curItem + ".lore") instanceof String) { 
					lore.add(replaceVars(player, curMenu.getString(itemPath + curItem + ".lore")));
				} else {
					for(String loreString : curMenu.getStringList(itemPath + curItem + ".lore")) {
						lore.add(replaceVars(player, loreString));
					}
				}
			} catch (Exception e) { }


			/* Building the item */
			item = new ItemStack(Material.getMaterial(itemID), amount, durability );
			inventory.setItem(pos, setName(item, display, lore));


			debugMessage("Slot Number: [" + pos + "] - Commands: " + command + " - Permission: [" + permission + "]" + " - Fees: [Itemfee: " + itemfee + " - Moneyfee: " + moneyfee + "]");
			playerMenu.set("items." + pos + ".command", command);
			playerMenu.set("items." + pos + ".permission", permission);
			playerMenu.set("items." + pos + ".itemfee", itemfee);
			playerMenu.set("items." + pos + ".moneyfee", moneyfee);
		}

		playerMenu.set("name", menuName);
		for(String key : menu.getKeys(false)) {
			if(key.equals("items")) { continue; }
			debugMessage("Adding value [" + key + ":" + menu.getString(key) + "] to the menu!");
			playerMenu.set(key, menu.get(key));
		}

		player.closeInventory();
		players.put(player , playerMenu); // Save commands linked to player object and position

		inventories.put(menuName, inventory); // Cache inventory
		menus.put(menuName, playerMenu);

		player.openInventory(inventory); // Show Menu to player

		debugMessage("Menu built with no Errors. Woo!");
	}

	public void reloadConfigs(){ 
		inventories.clear();
		menus.clear();

		Boolean newlyCreated = false;

		if (!new File(dataFolder, "locale.yml").exists()){ saveResource("locale.yml", false); newlyCreated = true; }        
		if (!new File(dataFolder, "config.yml").exists()){ saveResource("config.yml", false); newlyCreated = true; }
		if (!new File(dataFolder, "special-items.yml").exists()){ saveResource("special-items.yml", false); newlyCreated = true; }

		locale = YamlConfiguration.loadConfiguration(new File(dataFolder, "locale.yml"));
		config = YamlConfiguration.loadConfiguration(new File(dataFolder, "config.yml"));
		prefix = colorize(getConfig().getString("prefix", "&5[PItemMenu] &7"));
		specialItem = YamlConfiguration.loadConfiguration(new File(dataFolder, "special-item.yml"));

		if (newlyCreated) {
			if (!new File(dataFolder, File.pathSeparator + "menus" + File.pathSeparator + "menu.yml").exists()){ 
				saveResource("menus/menu.yml", false); 
			}
		}

		reloadConfig();
	}

	public void checkOps() {
		FileConfiguration tempOps = YamlConfiguration.loadConfiguration(new File(dataFolder, "temp-opped.yml"));
		for (String playerName : tempOps.getStringList("players")){
			Player player = (Player) server.getOfflinePlayer(playerName);
			tagMessage(getLocale("opFix").replace("%player%", playerName));
			player.setOp(false);
			setTempOp(player, false);
		}
	}
}