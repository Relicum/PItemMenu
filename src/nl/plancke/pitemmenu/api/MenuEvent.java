package nl.plancke.pitemmenu.api;

import java.util.List;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class MenuEvent extends Event implements Cancellable {
    private boolean cancelled;
    private List<String> commands;
    private static final HandlerList handlers = new HandlerList();
 
    public MenuEvent(List<String> commands) {
        this.commands = commands;
    }
 
    public List<String> getCommands() {
        return this.commands;
    }
 
    public void setText(List<String> commands) {
        this.commands = commands;
    }
 
    public boolean isCancelled() {
        return cancelled;
    }
 
    public void setCancelled(boolean bool) {
        this.cancelled = bool;
    }

	@Override
	public HandlerList getHandlers() {
	    return handlers;
	}
	 
	public static HandlerList getHandlerList() {
	    return handlers;
	}
}
