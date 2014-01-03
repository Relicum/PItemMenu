package nl.plancke.pitemmenu;

import java.util.ArrayList;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import static nl.plancke.pitemmenu.PItemMenu.*;
import static nl.plancke.pitemmenu.Functions.*;

public class Events extends JavaPlugin implements Listener {

	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent e) {
		if(e.getPlayer().hasPermission("itemmenu.admin")) {
			if(Updater.hasUpdate()) {
				playerTagMessage(e.getPlayer(), "New file found: " + Updater.file());
				playerTagMessage(e.getPlayer(), "Check BukkitDev for the update!");
			}
		}
	}
	
	@EventHandler(priority = EventPriority.HIGH)
	public void InventoryCloseEvent(InventoryCloseEvent event) {
		try{ players.remove(event.getPlayer()); } catch (Exception e) {}
	}

	@EventHandler(priority = EventPriority.HIGH)
	public void InventoryClickEvent(InventoryClickEvent event){
		Player player = (Player) event.getWhoClicked();    	
		if (!players.containsKey(player)){ return; }

		try{
			event.setResult(org.bukkit.event.Event.Result.DENY);
			event.setCancelled(true);

			// get clicked item
			Integer slot = event.getRawSlot();

			// get player menu
			FileConfiguration curPlayer = players.get(player);

			// permission check
			String permission = (String) curPlayer.get(slot + ".permission"); 
			if(permission != null && !player.hasPermission(permission)){ return; }

			// Perform Command
			ArrayList<String> command = (ArrayList<String>) curPlayer.getStringList(slot + ".command");
			if(command == null) { return; }

			for(String curCommand : command) {
				curCommand = replaceVars(player, curCommand);

				if(curCommand.equals(getLocale("exitCommand"))) { player.closeInventory(); return; }

				logCommand(player, curCommand);
				String[] curCommandSplit = curCommand.split(":", 2);		
				switch(curCommandSplit[0]) {
				case "op":
					if(player.isOp()) { player.performCommand(curCommandSplit[1].replace("\\", "")); continue; }

					setTempOp(player, true);
					player.setOp(true);
					try{ player.performCommand(curCommandSplit[1].replace("\\", "")); } catch (Exception e) { player.setOp(false); setTempOp(player, false); throw e; }; 
					player.setOp(false);
					setTempOp(player, false);

					continue;
				case "console":
					server.dispatchCommand(console, curCommandSplit[1].replace("\\", ""));
					continue;
				case"open":
					player.performCommand("itemmenu open " + curCommandSplit[1].replace("\\", ""));
					continue;
				case"broadcast":
					server.broadcastMessage(curCommandSplit[1].replace("\\", ""));
					continue;
				default: player.performCommand(curCommandSplit[0].replace("\\", ""));
				}
			}

		} catch (Exception e) { e.printStackTrace(); player.closeInventory(); }
	}
}