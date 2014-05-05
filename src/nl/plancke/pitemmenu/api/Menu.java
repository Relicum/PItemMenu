package nl.plancke.pitemmenu.api;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.Plugin;

public class Menu implements Listener {
	private Map<String, Menu> players;
	private Map<Integer, MenuItem> items;
	private int size;
	private String title;
	private Inventory inventory;
	private Plugin plugin;
	
	public Menu(Plugin plugin){
		players = new HashMap<String, Menu>();
		items = new HashMap<Integer, MenuItem>();
		this.plugin = plugin;
	}

	public Map<Integer, MenuItem> getItems(){
		return this.items;
	}
	
	public MenuItem getItem(int slot){
		return this.items.get(slot);
	}
	
	public void setItem(int slot, MenuItem item){
		this.items.put(slot, item);
	}
	
	public int getSize(){
		return this.size;
	}
	
	public void setSize(int size){
		this.size = size;
	}

	public String getTitle(){
		return this.title;
	}
	
	public void setTitle(String title){
		this.title = title;
	}
	
	public void open(String playerName){
		Menu menu = this;
		Player player = Bukkit.getPlayerExact(playerName);
		
		Inventory inventory = Bukkit.createInventory(player, menu.getSize(), menu.getTitle());
		
		Iterator<Entry<Integer, MenuItem>> it = menu.getItems().entrySet().iterator();
		while(it.hasNext()){
			Map.Entry<Integer, MenuItem> pairs = it.next();
			int slot = (Integer) pairs.getKey();
			MenuItem mItem = pairs.getValue();
			inventory.setItem(slot, mItem.getItem());
		}
		
		this.inventory = inventory;
		
		player.closeInventory();
		players.put(playerName , menu);
		player.openInventory(menu.inventory);
		
		Bukkit.getPluginManager().registerEvents(new MenuEventHandler(players), plugin);
	}
}
