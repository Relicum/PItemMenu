package nl.plancke.pitemmenu;

import java.util.ArrayList;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.plugin.java.JavaPlugin;

import static nl.plancke.pitemmenu.PItemMenu.*;
import static nl.plancke.pitemmenu.Functions.*;

public class Events extends JavaPlugin implements Listener {
	
    @EventHandler(priority = EventPriority.HIGH)
    public void InventoryCloseEvent(InventoryCloseEvent event) {
    	players.remove(event.getPlayer());
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void InventoryClickEvent(InventoryClickEvent event){
		// check if player is viewing a menu
    	Player player = (Player) event.getWhoClicked();    	
		if (!players.containsKey(player)){ return; }
		
		try{
			// cancel the clicking of the item
			// this happens as soon as possible so the player doesn't pick up the item too long
			event.setResult(org.bukkit.event.Event.Result.DENY);
	        event.setCancelled(true);
	        
	        // get clicked item
	        Integer slot = event.getRawSlot();
	        
	        // get player menu
	        FileConfiguration curPlayer = players.get(player);
	        
	        // permission check
	        String permission = (String) curPlayer.get(slot + ".permission"); 
	        if(permission != null) {
	        	if(!player.hasPermission(permission)){ return; }
	        }
	        
	        // Perform Command
			ArrayList<String> command = (ArrayList<String>) curPlayer.getStringList(slot + ".command");
	        if(command == null) { return; }
       
	        // execute each command seperately
	        for(String curCommand : command) {
	        	curCommand = replaceVars(player, curCommand);
	        	
		        if(curCommand.equals(getLocale("exitCommand"))) { 
		        	player.closeInventory();
					return;
		        }
		        	 
	        	logCommand(player, curCommand);
	        	if(curCommand.split("(?<!\\\\):")[0].equals("op")) { 
		        	if(!player.isOp()) {
		        		setTempOp(player, true);
		        		player.setOp(true);
		        		try{ player.performCommand(curCommand); } catch (Exception e) { player.setOp(false); setTempOp(player, false); throw e; }; 
		        		player.setOp(false);
		        		setTempOp(player, false);
		        	} else {
		        		player.performCommand(curCommand);
		        	}
		        	continue;
		        } 
		        
		        if(curCommand.split("(?<!\\\\):")[0].equals("console")) { 
		        	server.dispatchCommand(console, curCommand.split(":")[1].replace("\\", ""));
		        	continue;
		        }
		        
		        if(curCommand.split("(?<!\\\\):")[0].equals("open")) { 
		        	player.performCommand("itemmenu open " + curCommand.split(":")[1].replace("\\", ""));
		        	continue;
		        }
		        
		        if(curCommand.split("(?<!\\\\):")[0].equals("broadcast")) { 
		        	server.broadcastMessage(curCommand.split("(?<!\\\\):")[1].replace("\\", ""));
		        	continue;
		        }
		        	
		        player.performCommand(curCommand);
	        }
	                
		} catch (Exception e) { 
			e.printStackTrace();
			player.closeInventory();
		};
    }
}