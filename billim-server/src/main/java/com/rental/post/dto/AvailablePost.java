package main.java.com.rental.post.dto;

public class AvailablePost {
	private int postNum;
    private String title;
    private String content;
    private String rentDate;
    private String returnDate;
    private String addr;
    private String itemName;
    private String category;
    
    
    public AvailablePost() {
		
	}
    
	public AvailablePost(int postNum, String title, String content, String rentDate, String returnDate, String addr,
			String itemName, String category) {
		super();
		this.postNum = postNum;
		this.title = title;
		this.content = content;
		this.rentDate = rentDate;
		this.returnDate = returnDate;
		this.addr = addr;
		this.itemName = itemName;
		this.category = category;
	}
	public int getPostNum() {
		return postNum;
	}
	public void setPostNum(int postNum) {
		this.postNum = postNum;
	}
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	public String getContent() {
		return content;
	}
	public void setContent(String content) {
		this.content = content;
	}
	public String getRentDate() {
		return rentDate;
	}
	public void setRentDate(String rentDate) {
		this.rentDate = rentDate;
	}
	public String getReturnDate() {
		return returnDate;
	}
	public void setReturnDate(String returnDate) {
		this.returnDate = returnDate;
	}
	public String getAddr() {
		return addr;
	}
	public void setAddr(String addr) {
		this.addr = addr;
	}
	public String getItemName() {
		return itemName;
	}
	public void setItemName(String itemName) {
		this.itemName = itemName;
	}
	public String getCategory() {
		return category;
	}
	public void setCategory(String category) {
		this.category = category;
	}

	@Override
	public String toString() {
		return "[대여 가능 게시글 #" + postNum + "] " + title + "\n"
				+ "물품명 : " + itemName + "  |  카테고리 : " + category + "\n"
				+ "대여기간 : " + rentDate + " ~ " + returnDate + "\n"
				+ "거래장소 : " + addr + "  |  내용 : " + content;
	}
    
    
}
