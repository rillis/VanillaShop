package com.rillis;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;

public class EventListener implements Listener {
	Database db;
	Balance b;
	Inventory shop;
	private String pre = ChatColor.DARK_GREEN+""+ChatColor.BOLD+"[$] "+ChatColor.GREEN;
	private String pre_e = ChatColor.DARK_RED+""+ChatColor.BOLD+"[$] "+ChatColor.RED;
	public EventListener(Database db, Balance b) {
		this.db = db;
		this.b = b;
	}
	
	@EventHandler
    public void onPlayerJoin(PlayerJoinEvent event){
		
		Player p = event.getPlayer();
		if(!playerExistsDB(db, p.getUniqueId().toString(),"bank")) {
			db.statement("INSERT INTO bank (\"user\",\"display\") values (\""+p.getUniqueId().toString()+"\",\""+p.getName()+"\")");
		}
		
		p.sendMessage(pre+"Saldo: "+b.getBalance(p.getUniqueId())+" esmeraldas.");
		p.sendMessage(pre+"Para sacar use: /sacar");
		p.sendMessage(pre+"Para depositar use: /depositar");
		
	}
	
	@EventHandler
	public void onInventoryClick(InventoryClickEvent event) {
		Player p = (Player) event.getWhoClicked(); // The player that clicked the item
		ItemStack clicked = event.getCurrentItem(); // The item that was clicked
		InventoryView inventory = event.getView(); // The inventory that was clicked in
		if (inventory.getTitle().equals("Shop menu: Players.")) {
			event.setCancelled(true);
			if (clicked.getType() == Material.PLAYER_HEAD) {
				String display = clicked.getItemMeta().getDisplayName().toString();
				p.closeInventory();
				
				ItemFromSell[] is = getItemsFromSeller(display);
				
				shop = Bukkit.createInventory(null, 54, "Shop from "+display);
				
				startInv(shop, is, p);
				
				p.openInventory(shop);
			}
			
		}else if(inventory.getTitle().contains("Edit from ")) {
			event.setCancelled(true);
			String seller = inventory.getTitle().split("Edit from ")[1];
			ItemFromSell[] items = getItemsFromSeller(seller);
			ItemFromSell item_c = items[event.getSlot()];
			p.closeInventory();
			PlayerInventory inv = p.getInventory();
			if (inv.firstEmpty() == -1) {
                //cheio
				p.sendMessage(pre_e+"Seu inventário está cheio.");
			}else {
				if(b.removeItem(item_c.item, item_c.value, seller)){
					p.getInventory().addItem(SerialItem.stringToItemStack(item_c.item));
					p.sendMessage(pre+"Item removido.");
				}else {
					p.sendMessage(pre_e+"Error.");
				}
			}
			
		}else if(inventory.getTitle().contains("Shop from ")) {
			event.setCancelled(true);
			String seller = inventory.getTitle().split("Shop from ")[1];
			String buyer = p.getDisplayName();
			ItemFromSell[] items = getItemsFromSeller(seller);
			ItemFromSell item_c = items[event.getSlot()];
			
			
			UUID uuid_buyer = p.getUniqueId();
			ItemStack item = SerialItem.stringToItemStack(item_c.item);
			int price = item_c.value;
			
			if(seller.equals(buyer)) {
				p.closeInventory();
				p.sendMessage(pre_e+"Você não pode comprar de você mesmo.");
			}else {
				
				p.closeInventory();
				if(b.getBalance(uuid_buyer)<price) {
					p.sendMessage(pre_e+"Você não tem essa quantia.");
				}else {
					
					PlayerInventory inv = p.getInventory();
					if (inv.firstEmpty() == -1) {
                        //cheio
						p.sendMessage(pre_e+"Seu inventário está cheio.");
					}else {
						if(b.decrease(uuid_buyer, price)) {
							b.increase(seller, price);
							p.getInventory().addItem(item);
							p.sendMessage(pre+"Item comprado por "+price+" esmeraldas de "+seller+".");
							sendMessageIfOnline(seller,pre+"Item comprado por "+price+" esmeraldas para "+buyer+".");
							b.removeItem(item_c.item, item_c.value, seller);
						}else {
							p.sendMessage(pre_e+"Error.");
						}
					}
				}
				
				
			}
			
			
		}
	}
	
	private void sendMessageIfOnline(String name, String msg) {
		for(Player p : Bukkit.getOnlinePlayers()){
			if(p.getDisplayName().equals(name)) {
				p.sendMessage(msg);
			}
		}
		
	}

	private void startInv(Inventory i, ItemFromSell[] is, Player p) {
		for (int j = 0; j < is.length; j++) {
			ItemStack item = SerialItem.stringToItemStack(is[j].item);
			
			ItemMeta im = item.getItemMeta();
			ArrayList<String> arr = new ArrayList<String>();
			arr.add("Preço: "+is[j].value);
			im.setLore(arr);
			item.setItemMeta(im);
			
			i.setItem(j, item);
			
			i.setItem(53, createItem(Material.EMERALD, 1, ChatColor.DARK_GREEN+"Saldo:", new String[] {""+b.getBalance(p.getUniqueId())+" esmeraldas."}));
		}
	}

	private ItemStack createItem(Material m, int quantity, String name, String[] lore) {
		ItemStack is = new ItemStack(m, quantity);
		ItemMeta im = is.getItemMeta();
		im.setDisplayName(name);
		if(lore==null) {
			
		}else if(lore.length>0) {
			ArrayList<String> lores = new ArrayList<String>();
			for (int i = 0; i < lore.length; i++) {
				lores.add(lore[i]);
			}
			im.setLore(lores);
		}
		
		is.setItemMeta(im);
		
		return is;
	}

	public ItemFromSell[] getItemsFromSeller(String user) {
		ArrayList<ItemFromSell> items = new ArrayList<ItemFromSell>();
		try {
			ResultSet rs = db.returnStatement("SELECT * FROM items where display=\""+user+"\"");
			while (rs.next()) {
				  items.add(new ItemFromSell(rs.getString("item"), rs.getInt("price")));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return items.toArray(new ItemFromSell[0]);
	}
	
	public boolean playerExistsDB(Database db,String uuid, String table) {
		try {
			ResultSet rs = db.returnStatement("select count(*) from "+table+" where user = \""+uuid+"\"");
			int n = 0;
			if ( rs.next() ) {
				n = rs.getInt(1);
			}
			if ( n > 0 ) {
			   return true;
			}
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
	}
}
