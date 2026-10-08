package main.java.com.rental.item.dto;

public class ItemSearchRequest {
	
	private int itemNum;
	private String itemName;
	private String status;
	private String lenderID;
	
	public ItemSearchRequest() {}

	public ItemSearchRequest(int itemNum, String itemName, String status, String lenderID) {
		super();
		this.itemNum = itemNum;
		this.itemName = itemName;
		this.status = status;
		this.lenderID = lenderID;
	}

	public int getItemNum() {
		return itemNum;
	}

	public void setItemNum(int itemNum) {
		this.itemNum = itemNum;
	}

	public String getItemName() {
		return itemName;
	}

	public void setItemName(String itemName) {
		this.itemName = itemName;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getLenderID() {
		return lenderID;
	}

	public void setLenderID(String lenderID) {
		this.lenderID = lenderID;
	}

	
}
