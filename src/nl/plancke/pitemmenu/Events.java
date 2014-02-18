package nl.plancke.pitemmenu;

import java.util.ArrayList;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;

import static nl.plancke.pitemmenu.PItemMenu.*;
import static nl.plancke.pitemmenu.Functions.*;

public class Events implements Listener {

	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent e) {
		if(e.getPlayer().hasPermission("menu.admin")) {
			if(Updater.hasUpdate()) {
				tagMessage("PItemMenu is outdated!", e.getPlayer());
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
			
			event.setCursor(new ItemStack(Material.AIR));
			player.updateInventory();

			// get clicked item
			Integer slot = event.getRawSlot();

			// get player menu
			FileConfiguration playerMenu = players.get(player);
			
			//Check if slot is set
			if(!playerMenu.getConfigurationSection("items").getKeys(false).contains(slot + "")) { return; }

			// permission check
			String permission = playerMenu.getString("items." + slot + ".permission"); 
			if(permission != null) { 
				debugMessage("Checking if ["+ player.getName() + "] has permission [" + permission + "]"); 
				if(!player.hasPermission(permission)) { return; } 
			}

			// Perform Command
			ArrayList<String> commands = (ArrayList<String>) playerMenu.getStringList("items." + slot + ".command");
			if(commands == null) { return; }
			debugMessage("Slot Number: [" + slot + "] - Commands: " + commands + " - Permission: [" + playerMenu.getString(slot + ".permission") + "]");

			for(String curCommand : commands) {
				debugMessage("Running " + curCommand +" as [" + player.getName() + "]");
				curCommand = replaceVars(player, curCommand);

				if(curCommand.equals(getLocale("exitCommand"))) { player.closeInventory(); return; }

				logCommand(player, curCommand);
				String[] curCommandSplit = curCommand.split(":", 2);		
				switch(curCommandSplit[0]) {
				case "op":
					if(player.isOp()) { player.performCommand(curCommandSplit[1]); break; } // Execute when player is alreaady OP

					setTempOp(player, true);
					player.setOp(true);
					try{ player.performCommand(curCommandSplit[1]); } catch (Exception e) { player.setOp(false); setTempOp(player, false); throw e; }; 
					player.setOp(false);
					setTempOp(player, false);
					break;
				case "console":
					server.dispatchCommand(console, curCommandSplit[1]); 
					break;
				case"open":
					player.performCommand("menu open " + curCommandSplit[1]); 
					break;
				case"broadcast":
					server.broadcastMessage(curCommandSplit[1]); 
					break;
				default: player.performCommand(curCommandSplit[0]);
				}
			}
			
			if(playerMenu.getBoolean("closeonclick", false)) { player.closeInventory(); }
			if(playerMenu.getBoolean("reopenonclick", false)) { PItemMenu.OpenMenu(player, playerMenu.getString("name")); }

		} catch (Exception e) { e.printStackTrace(); player.closeInventory(); }
	}
}