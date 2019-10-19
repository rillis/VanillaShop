package com.rillis;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MainHand;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;

public class CommandList implements CommandExecutor {
	Database db;
	Balance b;
	Inventory invPlayers = null;
	Inventory invRemove = null;
	private String pre = ChatColor.DARK_GREEN+""+ChatColor.BOLD+"[$] "+ChatColor.GREEN;
	private String pre_e = ChatColor.DARK_RED+""+ChatColor.BOLD+"[$] "+ChatColor.RED;
	public CommandList(Database db, Balance b) {
		this.db = db;
		this.b = b;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		boolean isPlayer = false;
		boolean isConsole = true;
		Player p = null;
		UUID uuid = null;
		if(sender instanceof Player) {
			isPlayer=true;
			isConsole=false;
			p = (Player) sender;
			uuid = p.getUniqueId();
		}
		
		if(command.getName().equalsIgnoreCase("money") && isPlayer) {
			p.sendMessage(pre+"Seu saldo é de "+b.getBalance(uuid)+" esmeraldas.");
			p.sendMessage(pre+"Para sacar use: /sacar");
			p.sendMessage(pre+"Para depositar use: /depositar");
			return true;
		}
		
		if(command.getName().equalsIgnoreCase("remover") && isPlayer) {
			invRemove = Bukkit.createInventory(null, 54, "Edit from "+p.getDisplayName());
			ItemFromSell[] is = getItemsFromSeller(p.getDisplayName());
			startInvFromPlayer(invRemove, is, p);
			p.openInventory(invRemove);
			return true;
		}
		
		if(command.getName().equalsIgnoreCase("vender") && isPlayer) {
			if(args.length!=1) {
				p.sendMessage(pre_e+"Uso incorreto! Use: /vender <preço>.");
				return true;
			}
			int valor = 0;
			try {
				valor = Integer.parseInt(args[0]);
			}catch(Exception e) {
				p.sendMessage(pre_e+"Uso incorreto, use: /vender <preço>");
			}
			
			ItemStack is = p.getInventory().getItemInMainHand();
			if(is.getType().equals(Material.AIR)) {
				p.sendMessage(pre_e+"Você precisa ter um item na mão para fazer isso.");
			}else {
				if(b.putItem(p.getUniqueId(), p.getName(), SerialItem.itemStackToString(is), valor)) {
					p.getInventory().removeItem(is);
					p.sendMessage(pre+"Item foi colocado a venda por "+valor+" esmeraldas.");
					p.sendMessage(pre+"Para remover um item a venda use /remover.");
				}else {
					p.sendMessage(pre_e+"Error.");
				}
				
			}
			return true;
		}
		
		if(command.getName().equalsIgnoreCase("sacar") && isPlayer) {
			if(args.length!=1) {
				p.sendMessage(pre_e+"Uso incorreto, use: /sacar <valor>");
			}else {
				int saldo = b.getBalance(uuid);
				int valor = 0;
				try {
					valor = Integer.parseInt(args[0]);
				}catch(Exception e) {
					p.sendMessage(pre_e+"Uso incorreto, use: /sacar <valor>");
				}
				
				if(saldo<valor) {
					p.sendMessage(pre_e+"Você não tem essa quantia.");
				}else {
					PlayerInventory inv = p.getInventory();
					if (inv.firstEmpty() == -1) {
                        //cheio
						p.sendMessage(pre_e+"Seu inventário está cheio.");
                    } else {
                    	int espacos = (valor-1)/64;
                    	espacos++;
                    	int freeSlots = getFreeSlots(p);
                    	if(freeSlots<espacos) {
                    		p.sendMessage(pre_e+"Para sacar essa quantia você deve esvaziar um pouco seu inventário.");
                    	}
                    	
	                    if(b.decrease(uuid, valor)) {
	                    	p.getInventory().addItem(new ItemStack(Material.EMERALD, valor));
	                    	p.sendMessage(pre+"Você sacou "+valor+" esmeraldas!");
	                    	p.sendMessage(pre+"Novo saldo: "+b.getBalance(uuid)+" esmeraldas.");
	                    }else {
							p.sendMessage(pre_e+"Error.");
						}
                    }
				}
			}
			return true;
		}
		
		if(command.getName().equalsIgnoreCase("depositar") && isPlayer) {
			if(args.length!=1) {
				p.sendMessage(pre_e+"Uso incorreto, use: /depositar <valor>");
			}else {
				int saldo = b.getBalance(uuid);
				int valor = 0;
				try {
					valor = Integer.parseInt(args[0]);
				}catch(Exception e) {
					p.sendMessage(pre_e+"Uso incorreto, use: /depositar <valor>");
				}
				
				int emeralds = getQuantity(p, Material.EMERALD);
				
				if(emeralds<valor) {
					p.sendMessage(pre_e+"Você não tem essa quantia.");
				}else {
					if(b.increase(uuid, valor)) {
						removeItem(p, new ItemStack(Material.EMERALD,valor));
						p.sendMessage(pre+"Você depositou "+valor+" esmeraldas!");
	                	p.sendMessage(pre+"Novo saldo: "+b.getBalance(uuid)+" esmeraldas.");
					}else {
						p.sendMessage(pre_e+"Error.");
					}
				}
			}
			return true;
		}
		if(command.getName().equalsIgnoreCase("shop") && isPlayer) {
			invPlayers = Bukkit.createInventory(null, 54, "Shop menu: Players.");
			startInv(invPlayers, p);
			p.openInventory(invPlayers);
			return true;
		}
		return false;
	}
	

	private void startInv(Inventory invPlayers, Player p) {
		//invPlayers.setItem(0, createItem(Material.PLAYER_HEAD, 1, ChatColor.AQUA+"rillis", new String[]{"Shop de rillis"}));
		String[] sellers = getSellers();
		for (int i = 0; i < sellers.length; i++) {
			invPlayers.setItem(i, createItem(Material.PLAYER_HEAD, 1, sellers[i], new String[]{"Shop de "+sellers[i]}));
		}
		
		invPlayers.setItem(53, createItem(Material.BARRIER, 1, ChatColor.RED+"Next Page", null));
		invPlayers.setItem(52, createItem(Material.GLASS_PANE, 1, ChatColor.RED+"-", null));
		invPlayers.setItem(51, createItem(Material.GLASS_PANE, 1, ChatColor.RED+"-", null));
		invPlayers.setItem(50, createItem(Material.GLASS_PANE, 1, ChatColor.RED+"-", null));
		invPlayers.setItem(49, createItem(Material.EMERALD, 1, ChatColor.DARK_GREEN+"Saldo:", new String[] {""+b.getBalance(p.getUniqueId())+" esmeraldas."}));
		invPlayers.setItem(48, createItem(Material.GLASS_PANE, 1, ChatColor.RED+"-", null));
		invPlayers.setItem(47, createItem(Material.GLASS_PANE, 1, ChatColor.RED+"-", null));
		invPlayers.setItem(46, createItem(Material.GLASS_PANE, 1, ChatColor.RED+"-", null));
		invPlayers.setItem(45, createItem(Material.BARRIER, 1, ChatColor.RED+"Previous Page", null));
	}
	
	public String[] getSellers() {
		ArrayList<String> arr = new ArrayList<String>();
		try {
			ResultSet rs = db.returnStatement("SELECT * FROM items");
			while (rs.next()) {
				  arr.add(rs.getString("display"));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return removeDuplicates(arr.toArray(new String[0]));
	}
	
	private String[] removeDuplicates(String[] i2) {
		ArrayList<String> antiga = new ArrayList<String>();
		ArrayList<String> unicos = new ArrayList<String>();
		
		for (int i = 0; i < i2.length; i++) {
			antiga.add(i2[i]);
		}
		
		for (int i = 0; i < antiga.size(); i++) {
			String compare = i2[i];
			boolean unico = true;
			for (int j = 0; j < unicos.size(); j++) {
				if(compare.equals(unicos.get(j))) {
					unico = false;
				}
			}
			if(unico) {
				unicos.add(compare);
			}
			
			
		}
		return unicos.toArray(new String[0]);
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
	
	private void startInvFromPlayer(Inventory i, ItemFromSell[] is, Player p) {
		for (int j = 0; j < is.length; j++) {
			ItemStack item = SerialItem.stringToItemStack(is[j].item);
			
			ItemMeta im = item.getItemMeta();
			ArrayList<String> arr = new ArrayList<String>();
			arr.add("Preço: "+is[j].value);
			im.setLore(arr);
			item.setItemMeta(im);
			
			i.setItem(j, item);
		}
	}
	
	private void removeItem(Player p, ItemStack is) {
		int numberOld = getQuantity(p, Material.EMERALD);
		int remove = is.getAmount();
		p.getInventory().removeItem(is);
		
		int numberNew = getQuantity(p, Material.EMERALD);
		int dupeAmount = numberNew - (numberOld-remove);
		if(numberNew != (numberOld-remove) && p.getInventory().getItemInOffHand().getType().equals(Material.EMERALD)) {
			p.getInventory().getItemInOffHand().setAmount(p.getInventory().getItemInOffHand().getAmount()-dupeAmount);
		}
		
	}
	private int getQuantity(Player player, Material m) {
		int i = 0;
		for(ItemStack is : player.getInventory().getContents()) {
			if(is==null) {
				
			}else if(is.getType().equals(m)) {
				i+=is.getAmount();
			}
		}
		return i;
	}
	private int getFreeSlots(Player player) {
		int i = 0;
		for(ItemStack is : player.getInventory().getContents()) {
			if(is== null) {
				i++;
			}
		}
		ItemStack is = player.getInventory().getItemInOffHand();
		if(is.getType().equals(Material.AIR)) {
			i--;
		}
		return i;
	}

}
