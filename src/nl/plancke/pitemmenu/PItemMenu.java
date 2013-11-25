/*
 PItemMenu v1.07 by Plancke
 Feel free to use all of the code in here for your
 own projects but give me credit when you do.
 */

package nl.plancke.pitemmenu;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
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
		
		if(config.getBoolean("check-update")) { Updater.checkUpdate(); }
		
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
				if(args.length > 2 || args.length == 1) {
					return showUsage(sender);
				} else if(args[1].equalsIgnoreCase("reload") && (sender.hasPermission("itemmenu.admin.reload") || sender.isOp())) {
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
				if(!(sender instanceof Player)){ // Player Check
					consoleTagMessage(getLocale("onlyPlayers")); 
					return true;
				}

				if(args.length == 1){ // Empty arguments check
					return showUsage(sender);
				}

				Player player = (Player) sender;

				if(sender.isOp() || sender.hasPermission("itemmenu.open." + args[1])) { // Permission Check					
					String name = args[1];
					File menuFile = new File(dataFolder + File.separator + "menus" + File.separator + args[1] + ".yml");
					if(!menuFile.exists()) { // Check if the menu exists
						playerTagMessage(player, getLocale("menu.notExist").replace("%menu%", name));
						return true;
					} 
					OpenMenu(player, menuFile);

				} else {
					playerTagMessage(player, getLocale("menu.notPerms").replace("%menu%", args[1]));
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
		FileConfiguration menu = YamlConfiguration.loadConfiguration(menuFile); // Load Menu from Path
		FileConfiguration curMenu = menu; 
		FileConfiguration commandMenu = new YamlConfiguration();

		Set<String> items = menu.getConfigurationSection("items").getKeys(false);

		String title = replaceVars(player, menu.getString("title")); // Get Title
		Integer maxSlot = Integer.parseInt(Collections.max(items)) - 1;
		Integer size = (int) (maxSlot / 9 + 1) * 9; // Dynamically set size

		if(size > 54) { 
			playerTagMessage(player, getLocale("menu.tooBig").replace("%size%", size.toString()));
			return;
		}

		// Construct the menu

		Inventory inventory = Bukkit.getServer().createInventory(player,  size, title);
		String itemPath;

		for(String curItem : items ){
			itemPath = "items.";
			curMenu = menu;
			Integer pos = Integer.parseInt(curItem) -1;

			// Special Item check
			if(curMenu.get(itemPath + curItem) instanceof String) {
				String specialItem = (String) curMenu.get(itemPath + curItem);
				Set<String> specialItemNames = (Set<String>) specialItems.getKeys(false);
				if(specialItemNames.contains(specialItem)) {
					curItem = specialItem; // Set current item name
					curMenu = specialItems; // Set SpecialItem curMenu config to be used to get items
					itemPath = "";
				} else { continue; } // Go back to start of loop, Special item was not found
			}

			int amount = 1; // Default value
			short durability = 0; 
			String display = null; 
			String permission = null;
			
			ArrayList<String> lore = new ArrayList<String>();
			try {
				if(curMenu.get(itemPath + curItem + ".lore") instanceof String) { // Catch single lined Lore's
					lore.add(replaceVars(player, (String) curMenu.get(itemPath + curItem + ".lore")));
				} else {
					for(String loreString : curMenu.getStringList(itemPath + curItem + ".lore")) {
						lore.add(replaceVars(player, loreString));
					}
				}
			} catch (Exception e) { }

			// Durability check in Type
			String type = curMenu.get(itemPath + curItem + ".type").toString();
			Integer itemID = 0;
			if(type.contains(":")) {
				String[] typeData = type.split(":");
				itemID = Integer.parseInt(typeData[0]);
				durability = (short) Integer.parseInt(typeData[1]);
			} else {
				itemID = Integer.parseInt(type);
			}
			
			// General Item data gathering
			try { amount = (Integer) curMenu.get(itemPath + curItem + ".amount"); } catch (Exception e) { }
			try { display =  replaceVars(player, (String) curMenu.get(itemPath + curItem + ".display")); } catch (Exception e) { }
			try { permission =  (String) curMenu.get(itemPath + curItem + ".permission"); } catch (Exception e) { }
			try { 
				Boolean hide = curMenu.getBoolean(itemPath + curItem + ".hide"); 
				if(hide && !permission.equals(null) && !player.hasPermission(permission)) { continue; }
			} catch (Exception e) { }
			
			// Command gathering
			ArrayList<String> command = new ArrayList<String>();
			if(curMenu.get(itemPath + curItem + ".command") instanceof String) { // Catch single lined commands
				command.add(replaceVars(player, (String) curMenu.get(itemPath + curItem + ".command")));
			} else {
				for(String commandString : curMenu.getStringList(itemPath + curItem + ".command")) {
					command.add(replaceVars(player, commandString));
				}
			}
			
			ItemStack item = new ItemStack(Material.getMaterial(itemID), amount, durability ); // Build the ItemStack
			inventory.setItem(pos, setName(item, display, lore)); // Set Item Lore, Display and add it to the right spot
			
			commandMenu.set(pos + ".command", command); // Link Command of item to the right spot
			commandMenu.set(pos + ".permission", permission);
		}

		player.closeInventory();
		players.put(player , commandMenu); // Save commands linked to player object and position
		player.openInventory(inventory); // Show Menu to player
	}

	public boolean showUsage(CommandSender sender) {
		if(!(sender instanceof Player)){ // If Console fails send admin usage.
			consoleTagMessage(getLocale("usage.admin")); 
		}  else {
			Player player = (Player) sender;
			if(sender.hasPermission("itemmenu.admin")) {		
				playerTagMessage(player, getLocale("usage.admin")); // If Player has the admin permission show admin usage.
			} else {
				playerTagMessage(player, getLocale("usage.normal")); // If Player fails send usage.
			}
		}
		return true;
	}

	public void reloadConfigs(){ 
		Boolean newlyCreated = false;

		if (!new File(dataFolder, "locale.yml").exists()){ saveResource("locale.yml", false); newlyCreated = true; }        
		if (!new File(dataFolder, "config.yml").exists()){ saveResource("config.yml", false); newlyCreated = true; }
		if (!new File(dataFolder, "special-items.yml").exists()){ saveResource("special-items.yml", false); newlyCreated = true; }

		locale = YamlConfiguration.loadConfiguration(new File(dataFolder, "locale.yml"));
		config = YamlConfiguration.loadConfiguration(new File(dataFolder, "config.yml"));
		prefix = colorize(getConfig().getString("prefix"));
		specialItem = YamlConfiguration.loadConfiguration(new File(dataFolder, "special-item.yml"));

		if (newlyCreated) {
			if (!new File(dataFolder, File.pathSeparator + "menus" + File.pathSeparator + "itemmenu.yml").exists()){ saveResource("menus/itemmenu.yml", false); }
			// only create the default menu's if a config file is missing
			consoleTagMessage(getLocale("defaultGen"));
		}

		reloadConfig();
	}

	public void checkOps(){
		FileConfiguration tempOps = YamlConfiguration.loadConfiguration(new File(dataFolder, "temp-opped.yml"));
		for (String playerName : tempOps.getStringList("players")){
			Player player = (Player) server.getOfflinePlayer(playerName);
			consoleTagMessage(getLocale("opFix").replace("%player%", playerName));
			player.setOp(false);
			setTempOp(player, false);
		}
	}
}