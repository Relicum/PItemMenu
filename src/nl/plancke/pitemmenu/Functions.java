package nl.plancke.pitemmenu;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;

import static nl.plancke.pitemmenu.PItemMenu.*;

public class Functions extends JavaPlugin{    

	public static void consoleTagMessage(String msg) {
		if(msg == null) { return; }
		console.sendMessage(prefix + msg);
	}
	public static void playerTagMessage(Player player, String msg) {
		if(msg == null) { return; }
		player.sendMessage(prefix + msg);
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

	public static void logCommand(Player player, String command) {
		if(!PItemMenu.config.getBoolean("log")) { return; }
		String message;
		String pName = player.getName();
		message = getLocale("logMessage");
		if(message == "") { return; }
		
		message = message.replace("%player%", pName);
		message = message.replace("%command%", command);

		consoleTagMessage(message);
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
		string = string.replace("%player%", player.getName());
		string = string.replace("%world%", player.getWorld().getName());
		string = string.replace("%maxplayers%", Long.toString(server.getMaxPlayers()));
		string = string.replace("%curplayers%", Integer.toString(server.getOnlinePlayers().length));
		string = string.replace("%prefix%", prefix);
		return colorize(string);
	}
	
	public static boolean showUsage(CommandSender sender) {
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
}