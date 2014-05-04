package nl.plancke.pitemmenu.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;

public class MenuEventHandler implements Listener {
	private Map<String, Menu> players;
	private List<String> clist;
	
	public MenuEventHandler(Map<String, Menu> players){
		this.players = players;
		clist = new ArrayList<String>();
	}
	
	@EventHandler
	public void inventoryClose(InventoryCloseEvent event){

		String playerName = event.getPlayer().getName();

		//if the player was viewing a MenuInstance that's being provided for
		if (players.containsKey(playerName)){
			players.remove(playerName);
		}

	}

	@EventHandler
	public void inventoryClick(InventoryClickEvent event){	
		String playerName = event.getWhoClicked().getName();
		if (!players.containsKey(playerName)){
			return;
		}

		//cancel the clicking of the item
		event.setResult(org.bukkit.event.Event.Result.DENY);
        event.setCancelled(true);
        
		// get Menu
		Menu menu = players.get(playerName);
		if(!menu.getItems().containsKey(event.getRawSlot())) {
			return;
		}
		MenuItem mItem = menu.getItem(event.getRawSlot());
        clist = mItem.getCommands();
        
        MenuEvent e = new MenuEvent(clist);
        
        Bukkit.getServer().getPluginManager().callEvent(e);
	}
	
	@EventHandler
    public void onMenuClick(MenuEvent event) {
    	Bukkit.getServer().broadcastMessage(event.getCommands().toString());
    }
}
