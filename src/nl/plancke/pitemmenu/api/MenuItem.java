package nl.plancke.pitemmenu.api;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;

public class MenuItem {
	private ItemStack item;
	private List<String> commands;
	private Map<String, String> info;
	
	public MenuItem(){
		this.commands = new ArrayList<String>();
		this.info = new HashMap<String, String>();
	}
	
	public void addInfo(String key, String value) {
		this.info.put(key, value);
	}
	
	public void removeInfo(String key){
		this.info.remove(key);
	}
	
	public Map<String, String> getInfo(){
		return this.info;
	}
	
	public void addCommand(String command){
		commands.add(command);
		Bukkit.getServer().broadcastMessage(command);
	}
	
	public List<String> getCommands(){
		return this.commands;
	}
	
	public void setitem(ItemStack item){
		this.item = item;
	}
	
	public ItemStack getItem(){
		return this.item;
	}
}
