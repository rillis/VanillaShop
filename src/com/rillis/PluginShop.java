package com.rillis;

import java.io.File;

import org.bukkit.plugin.java.JavaPlugin;

public class PluginShop extends JavaPlugin{
	Database db = null;
	
	@Override
    public void onEnable() {
		String name = "Shop.db";
		db = new Database(name);
		Balance b = new Balance(db);
		
		CommandList c = new CommandList(db,b);
		this.getCommand("money").setExecutor(c);
		this.getCommand("sacar").setExecutor(c);
		this.getCommand("depositar").setExecutor(c);
		this.getCommand("shop").setExecutor(c);
		this.getCommand("vender").setExecutor(c);
		this.getCommand("remover").setExecutor(c);
		
		getServer().getPluginManager().registerEvents(new EventListener(db,b), this);
	}
	@Override
    public void onDisable() {
		db.close();
    }
}
