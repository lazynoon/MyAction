package myaction.model;

import java.util.ArrayList;

public class ForeignKey {
	private String table = null;
	private String[] primaryKeys = null;
	private ArrayList<Condition> relation = new ArrayList<Condition>(); 
	private ArrayList<ForeignKey> children = new ArrayList<ForeignKey>();
	
	public ForeignKey(String table) {
		this.table = table;
	}
	public ForeignKey(String table, String primaryKey) {
		this.table = table;
		this.primaryKeys = new String[1];
		this.primaryKeys[0] = primaryKey;
	}
	
	public ForeignKey(String table, String[] primaryKeys) {
		this.table = table;
		this.primaryKeys = primaryKeys;
	}
	
	public String getTable() {
		return table;
	}

	public String[] getPrimaryKeys() {
		return primaryKeys;
	}

	public ArrayList<Condition> getRelation() {
		return relation;
	}

	public ArrayList<ForeignKey> getChildren() {
		return children;
	}

	public static class Condition {
		private String primaryField = null;
		private String foreignField = null;
		public Condition(String foreignField, String parentField) {
			this.foreignField = foreignField;
			this.primaryField = parentField;
		}
		public String getPrimaryField() {
			return primaryField;
		}
		public String getForeignField() {
			return foreignField;
		}
		
	}
	
	
	public ForeignKey addRelation(String foreignField, String parentField) {
		relation.add(new Condition(foreignField, parentField));
		return this;
	}
	
	public void addChild(ForeignKey child) {
		children.add(child);
	}
}
