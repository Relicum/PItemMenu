package nl.plancke.pitemmenu;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;

import static nl.plancke.pitemmenu.PItemMenu.*;

public class Functions extends JavaPlugin{    

	public static void tagMessage(String msg) {
		tagMessage(msg, "console");
	}
	
	public static void tagMessage(String msg, Object receiver) {
		if(msg == null) { return; }
		if(!config.getBoolean("log", true)) { return; }
		for(String line : msg.split("\n")) {
			if(receiver instanceof Player){
				((Player) receiver).sendMessage(prefix + line);
			} else {
				console.sendMessage(prefix + line);
			}
		}
	}

	public static String colorize(String string) { 
		return string.replaceAll("(&([a-fk-or0-9]))", "\u00A7$2"); 
	}
	public static ArrayList<String> colorizeArray (ArrayList<String> arrayList) {
		ArrayList<String> newArrayList = new ArrayList<String>();
		for (String element: arrayList) {
			newArrayList.add(colorize(element));
		}
		return newArrayList;
	}
	
	public static String getLocale(String path) {
		return colorize(locale.getString(path, "Missing locale: " + path));
	}

	public static void debugMessage(String message) {
		if(!config.getBoolean("debug", false)) { return; }
		String newmsg = "";
		for(String line : message.split("\n")) {
			newmsg += "\n[DEBUG] " + line;
		}
		tagMessage(newmsg.replaceFirst("\n", ""), "Console");
	}
	
	public static void logCommand(Player player, String command) {
		if(!config.getBoolean("log", true)) { return; }
		String message;
		String pName = player.getName();
		message = getLocale("logMessage");
		if(message == null) { return; }
		
		message = message.replace("%player%", pName);
		message = message.replace("%command%", command);

		tagMessage(message);
	}

	public static void setTempOp(Player player, Boolean state) {
		try {
			File file = new File(dataFolder, "temp-opped.yml");
			if (!file.exists()) { file.createNewFile(); }
			FileConfiguration tempOps = YamlConfiguration.loadConfiguration(file);
			List<String> ops = tempOps.getStringList("players");

			if(state) {
				// add to templist
				ops.add(player.getName());
			} else {
				// remove from templist
				ops.remove(player.getName());
			}

			tempOps.set("players", ops);
			tempOps.save(file);
		} catch (Exception e) { e.printStackTrace(); }
	}

	public static String replaceVars(Player player, String string) {
		string = string.replaceAll("%player%", player.getName());
		string = string.replaceAll("%world%", player.getWorld().getName());
		string = string.replaceAll("%wtime%", Long.toString(player.getWorld().getTime()));
		string = string.replaceAll("%time%", new SimpleDateFormat("HH:mm:ss").format(Calendar.getInstance().getTime()));
		string = string.replaceAll("%date%", new SimpleDateFormat("yyyy/MMM/dd").format(Calendar.getInstance().getTime()));
		string = string.replaceAll("%maxplayers%", Long.toString(server.getMaxPlayers()));
		string = string.replaceAll("%curplayers%", Integer.toString(server.getOnlinePlayers().length));
		string = string.replaceAll("%prefix%", prefix);
		string = string.replaceAll("%exp%", player.getExp() + ""); //Temp fix for tostring on float
		string = string.replaceAll("%level%", Integer.toString(player.getLevel()));
		
		if(econ != null) {
			string = string.replaceAll("%balance%", Double.toString(econ.getBalance(player.getName())));
			string = string.replaceAll("%bankbalance%", Double.toString(econ.bankBalance(player.getName()).balance));
			string = string.replaceAll("%balanceformat%", econ.format(econ.getBalance(player.getName())));
			string = string.replaceAll("%bankbalanceformat%", econ.format(econ.bankBalance(player.getName()).balance));
			string = string.replaceAll("%currencyplural%", econ.currencyNamePlural());
			string = string.replaceAll("%currencysingural%", econ.currencyNameSingular());
		}
		return colorize(string);
	}
	
	public static boolean showUsage(CommandSender sender) {
		if(!(sender instanceof Player)){ // If Console fails send admin usage.
			tagMessage(getLocale("usage.admin")); 
		}  else {
			Player player = (Player) sender;
			if(sender.hasPermission("menu.admin")) {		
				tagMessage(getLocale("usage.admin"), player); // If Player has the admin permission show admin usage.
			} else {
				tagMessage(getLocale("usage.normal"), player); // If Player fails send usage.
			}
		}
		return true;
	}
}