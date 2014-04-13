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

			//Check for fees
			String itemfee = playerMenu.getString("items." + slot + ".itemfee", null);
			if(itemfee != null) {
				Material feemat = Material.getMaterial(Integer.parseInt(itemfee.split(",")[0].split(":")[0]));
				Short feedur = 0; try{ feedur = Short.parseShort(itemfee.split(",")[0].split(":")[1]);}catch(Exception e){ feedur = 0; }
				Integer feeamount= Integer.parseInt(itemfee.split(",")[1]);

				ItemStack item = new ItemStack(feemat, feeamount, feedur);
				debugMessage("Itemfee found: " + item.toString());

				if(!player.hasPermission("menu.itemfee.bypass." + playerMenu.getString("name") + ".slot." + slot)) {
					if(player.getInventory().containsAtLeast(item, feeamount)) {
						player.getInventory().removeItem(item);
						debugMessage("Item access granted!");
					} else {
						tagMessage(getLocale("fee.item").replaceAll("%item%", item.getType().name()).replaceAll("%amount%", feeamount + ""), player);
						return;
					}
				}
			}

			Double moneyfee = playerMenu.getDouble("items." + slot + ".moneyfee", 0);
			if(!player.hasPermission("menu.moneyfee.bypass." + playerMenu.getString("name") + ".slot." + slot)) {
				if(moneyfee != 0) {
					debugMessage("Moneyfee found: " + moneyfee);
					if(econ == null) {
						debugMessage("Economy is not enabled!");
					} else {
						if(econ.getBalance(player.getName()) >= moneyfee) {
							if(econ.withdrawPlayer(player.getName(), moneyfee).transactionSuccess()) {
								debugMessage("Player was granted access to the menu!");
							} else {
								debugMessage("Something went wrong while removong funds for " + player.getName());
								return;
							}
						} else {
							tagMessage(getLocale("fee.money").replaceAll("%amount%", econ.format(moneyfee)), player);
							return;
						}
					}
				}
			}

			// permission check
			String permission = playerMenu.getString("items." + slot + ".permission"); 
			if(permission != null) { 
				debugMessage("Checking if ["+ player.getName() + "] has permission [" + permission + "]"); 
				if(!player.hasPermission(permission)) { return; } 
			}

			// Perform Commands
			ArrayList<String> commands = (ArrayList<String>) playerMenu.getStringList("items." + slot + ".command");
			if(commands == null) { return; }

			debugMessage(
					"Slot Number: [" + slot + "]" +
					"\nCommands: " + commands +
					"\nPermission: [" + playerMenu.getString(slot + ".permission") + "]" +
					"\nFees: [Itemfee: " + itemfee + " - Moneyfee: " + moneyfee + "]"
					);

			for(String curCommand : commands) {
				debugMessage("Running " + curCommand +" as [" + player.getName() + "]");
				curCommand = replaceVars(player, curCommand);

				if(curCommand.equals(getLocale("exitCommand"))) { player.closeInventory(); return; }
				
				logCommand(player, curCommand);
				String[] curCommandSplit = curCommand.split(":", 2);
				
				/*
				 * Adding all command prefixes here
				 * Not using case structure because of Java6
				 * Is possible with workaround but not easy
				 * to use
				 */
				if(curCommandSplit[0].equalsIgnoreCase("op")) {
						if(player.isOp()) { player.performCommand(curCommandSplit[1]); continue; } // Execute when player is already OP
	
						setTempOp(player, true);
						player.setOp(true);
						try{ player.performCommand(curCommandSplit[1]); } catch (Exception e) { player.setOp(false); setTempOp(player, false); throw e; }; 
						player.setOp(false);
						setTempOp(player, false);
						continue;
				}

				if(curCommandSplit[0].equalsIgnoreCase("console")) {
					server.dispatchCommand(console, curCommandSplit[1]); 
					continue;
				}
				
				if(curCommandSplit[0].equalsIgnoreCase("open")) {
					player.performCommand("menu open " + curCommandSplit[1]); 
					continue;
				}
				
				if(curCommandSplit[0].equalsIgnoreCase("broadcast")) {
					server.broadcastMessage(curCommandSplit[1]); 
					continue;
				}
				
				if(curCommandSplit[0].equalsIgnoreCase("chat")) {
					player.chat(curCommandSplit[1]); 
					continue;
				}
				
				if(curCommandSplit[0].equalsIgnoreCase("econ")) {
					if(econ == null) { debugMessage("Economy not enabled"); continue; }
					String[] econSplit = curCommandSplit[1].split(":");
					
					if(econSplit[0].equalsIgnoreCase("withdraw")) {
						econ.withdrawPlayer(player.getName(), Double.parseDouble(econSplit[1]));
						debugMessage("Withdrawn " + econ.format(Double.parseDouble(econSplit[1])) + " from " + player.getName());
						continue;
					}
					
					if(econSplit[0].equalsIgnoreCase("deposit")) {
						econ.depositPlayer(player.getName(), Double.parseDouble(econSplit[1]));
						debugMessage("Added " + econ.format(Double.parseDouble(econSplit[1])) + " to " + player.getName());
						continue;
					}
					
					continue;
				}
				
				player.performCommand(curCommandSplit[0]);
			}

			// Extra actions after all commands
			if(playerMenu.getBoolean("closeonclick", false)) { player.closeInventory(); }
			if(playerMenu.getBoolean("reopenonclick", false)) { PItemMenu.OpenMenu(player, playerMenu.getString("name")); }

		} catch (Exception e) { e.printStackTrace(); player.closeInventory(); }
	}
}