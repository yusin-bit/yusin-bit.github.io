package main.java.com.rental.item.dto;

public class ItemUpdateRequest {

	private int itemNum;
	private String itemName;
	private boolean status;
	private String smallCategoryCode;
	
	public ItemUpdateRequest() {}

	public ItemUpdateRequest(int itemNum, String itemName, boolean status, String smallCategoryCode) {
		super();
		this.itemNum = itemNum;
		this.itemName = itemName;
		this.status = status;
		this.smallCategoryCode = smallCategoryCode;
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

	public boolean getStatus() {
		return status;
	}

	public void setStatus(boolean status) {
		this.status = status;
	}

	public String getSmallCategoryCode() {
		return smallCategoryCode;
	}

	public void setSmallCategoryCode(String smallCategoryCode) {
		this.smallCategoryCode = smallCategoryCode;
	}

	
	

	
}
