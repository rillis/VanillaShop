package com.rillis;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public class Balance {
	Database db;
	public Balance(Database db) {
		this.db = db;
	}
	
	public int getBalance(UUID uuid) {
    	ResultSet rs = null;
		try {
			 rs = db.returnStatement("select * from bank where user = \""+uuid+"\"");
			 if(rs.isClosed()) {
				 return 0;
			 }
			 
			 return rs.getInt("balance");
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return 0;
    }
	
	public boolean decrease(UUID uuid, int qtd) {
		int newValue = getBalance(uuid)-qtd;
		
		return db.statement("update bank set balance="+newValue+" where user=\""+uuid+"\"");
		
	}
	
	
	public boolean increase(UUID uuid, int qtd) {
		int newValue = getBalance(uuid)+qtd;
		
		return db.statement("update bank set balance="+newValue+" where user=\""+uuid+"\"");
		
	}
	
	public boolean increase(String display, int qtd) {
		int newValue = getBalance(display)+qtd;
		
		return db.statement("update bank set balance="+newValue+" where display=\""+display+"\"");
		
	}
	
	private int getBalance(String display) {
		ResultSet rs = null;
		try {
			 rs = db.returnStatement("select * from bank where display = \""+display+"\"");
			 if(rs.isClosed()) {
				 return 0;
			 }
			 
			 return rs.getInt("balance");
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return 0;
	}

	public boolean putItem(UUID uuid, String name, String item, int price) {
		//System.out.println(item);
		return db.statement("insert into items(user,display,item,price) values(\""+uuid+"\",\""+name+"\",\""+item+"\","+price+")");
	}

	public boolean removeItem(String item, int price, String seller) {
		return db.statement("delete from items where item=\""+item+"\" and price="+price+" and display=\""+seller+"\"");
	}
}
