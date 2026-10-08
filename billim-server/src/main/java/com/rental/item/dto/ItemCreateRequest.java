package main.java.com.rental.item.dto;

public class ItemCreateRequest {
	
	private String itemName;
	private boolean status;
	private String smallCategoryCode;
	private String lenderID;
	

	public ItemCreateRequest() {}


	public ItemCreateRequest(String itemName, boolean status, String smallCategoryCode, String lenderID) {
		super();
		this.itemName = itemName;
		this.status = status;
		this.smallCategoryCode = smallCategoryCode;
		this.lenderID = lenderID;
	}

	public String getItemName() {
		return itemName;
	}


	public void setItemName(String itemName) {
		this.itemName = itemName;
	}


	public boolean isStatus() {
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


	public String getLenderID() {
		return lenderID;
	}


	public void setLenderID(String lenderID) {
		this.lenderID = lenderID;
	}


			
}
