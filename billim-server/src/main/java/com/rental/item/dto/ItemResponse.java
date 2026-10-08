package main.java.com.rental.item.dto;


public class ItemResponse {
	private int itemNum;
	private String itemName;
	private String status;
	private String category;
	
	public ItemResponse() {}

	public ItemResponse(int itemNum, String itemName, String status, String category) {
		super();
		this.itemNum = itemNum;
		this.itemName = itemName;
		this.status = status;
		this.category = category;
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

	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
	}
	
	
}
