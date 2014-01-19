/*
 PItemMenu v1.1.4 by Plancke
 Feel free to use all of the code in here for your
 own projects but please give me credit when you do.
 */

package nl.plancke.pitemmenu;

import java.io.*;
import java.util.*;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.command.*;
import org.bukkit.configuration.file.*;

import static nl.plancke.pitemmenu.Functions.*;

public final class PItemMenu extends JavaPlugin implements Listener {
	public static String prefix;
	public static Server server;
	public static ConsoleCommandSender console;
	public static FileConfiguration config, locale, specialItem;
	public static Map<Player, FileConfiguration> players = new HashMap<Player, FileConfiguration>();
	public static File dataFolder;
	public static String version;

	@Override
	public void onEnable() {
		dataFolder = this.getDataFolder();
		server = getServer();
		console = server.getConsoleSender();
		version = getDescription().getVersion();

		reloadConfigs();
		checkOps();

		Bukkit.getServer().getPluginManager().registerEvents(new Events(), this);

		if(Updater.hasUpdate()) {
			ArrayList<String> info = Updater.getInfo();
			for(String line : info) {
				consoleTagMessage(line);
			}
		}

		consoleTagMessage("Enabled v" + version + "!");
	}
	@Override
	public void onDisable() {
		consoleTagMessage("Disabled v" + version + "!");
	}  

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args){
		if(cmd.getName().equalsIgnoreCase("itemmenu")){

			if(args.length == 0) { return showUsage(sender); }

			if(args[0].equalsIgnoreCase("admin")) {
				if(args.length == 1) { return showUsage(sender); } 

				if(args[1].equalsIgnoreCase("reload") && (sender.hasPermission("itemmenu.admin.reload") || sender.isOp())) {
					reloadConfigs();
					consoleTagMessage(getLocale("reload")); 
					if(sender instanceof Player) {
						Player Player = (Player) sender;
						playerTagMessage(Player, getLocale("reload")); 
					}
				}
				return true;
			}

			if(args[0].equalsIgnoreCase("open")) {
				if(!(sender instanceof Player)){ // Console Check
					consoleTagMessage(getLocale("onlyPlayers")); 
					return true;
				}

				if(args.length == 1){ return showUsage(sender); }

				Player player = (Player) sender;

				String name = args[1];
				if(sender.isOp() || sender.hasPermission("itemmenu.open." + name)) { // Permission Check					
					File menuFile = new File(dataFolder + File.separator + "menus" + File.separator + name + ".yml");
					if(!menuFile.exists()) { // Check if the menu exists
						playerTagMessage(player, getLocale("menu.notExist").replace("%menu%", name));
						return true;
					} 
					OpenMenu(player, menuFile);

				} else {
					playerTagMessage(player, getLocale("menu.notPerms").replace("%menu%", name));
				}
				return true;
			}
			return showUsage(sender);
		} 
		return false; 
	}

	private ItemStack setName(ItemStack itemStack, String name, ArrayList<String> lore) {
		ItemMeta itemMeta = itemStack.getItemMeta();
		if (name != null)
			itemMeta.setDisplayName(colorize(name));
		if (lore != null)
			itemMeta.setLore(colorizeArray(lore));
		itemStack.setItemMeta(itemMeta);
		return itemStack;
	}

	public void OpenMenu(Player player, File menuFile) { 
		FileConfiguration specialItems = YamlConfiguration.loadConfiguration(new File(dataFolder + "//special-items.yml"));
		FileConfiguration curMenu, menu = YamlConfiguration.loadConfiguration(menuFile); // Load Menu from Path
		FileConfiguration commandMenu = new YamlConfiguration();
		Set<String> items = menu.getConfigurationSection("items").getKeys(false);
		
		Integer maxSlot = 0;
		for(String slot : items ){ 
			if(Integer.parseInt(slot) > maxSlot) {
				maxSlot = Integer.parseInt(slot);
			}
		}
		Integer size = (int) ((maxSlot - 1) / 9 + 1) * 9;
		if(size > 54) { playerTagMessage(player, getLocale("menu.tooBig").replace("%size%", size.toString())); return; }
		
		String title = replaceVars(player, menu.getString("title"));
		
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
			ItemStack item = new ItemStack(Material.getMaterial(itemID), amount, durability );
			inventory.setItem(pos, setName(item, display, lore));

			
			debugMessage("Slot Number: [" + pos + "] - Commands: " + command + " - Permission: [" + permission + "]");
			commandMenu.set(pos + ".command", command);
			commandMenu.set(pos + ".permission", permission);
		}

		player.closeInventory();
		players.put(player , commandMenu); // Save commands linked to player object and position
		player.openInventory(inventory); // Show Menu to player
		
		debugMessage("Menu built with no Errors. Woo!");
	}

	public void reloadConfigs(){ 
		Boolean newlyCreated = false;

		if (!new File(dataFolder, "locale.yml").exists()){ saveResource("locale.yml", false); newlyCreated = true; }        
		if (!new File(dataFolder, "config.yml").exists()){ saveResource("config.yml", false); newlyCreated = true; }
		if (!new File(dataFolder, "special-items.yml").exists()){ saveResource("special-items.yml", false); newlyCreated = true; }

		locale = YamlConfiguration.loadConfiguration(new File(dataFolder, "locale.yml"));
		config = YamlConfiguration.loadConfiguration(new File(dataFolder, "config.yml"));
		prefix = colorize(getConfig().getString("prefix", "&5[PItemMenu] &7"));
		specialItem = YamlConfiguration.loadConfiguration(new File(dataFolder, "special-item.yml"));

		if (newlyCreated) {
			if (!new File(dataFolder, File.pathSeparator + "menus" + File.pathSeparator + "itemmenu.yml").exists()){ 
				saveResource("menus/itemmenu.yml", false); 
				consoleTagMessage(getLocale("defaultGen"));
			}
		}

		reloadConfig();
	}

	public void checkOps() {
		FileConfiguration tempOps = YamlConfiguration.loadConfiguration(new File(dataFolder, "temp-opped.yml"));
		for (String playerName : tempOps.getStringList("players")){
			Player player = (Player) server.getOfflinePlayer(playerName);
			consoleTagMessage(getLocale("opFix").replace("%player%", playerName));
			player.setOp(false);
			setTempOp(player, false);
		}
	}
}