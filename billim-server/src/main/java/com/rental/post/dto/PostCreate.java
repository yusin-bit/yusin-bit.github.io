package main.java.com.rental.post.dto;

public class PostCreate {
	//게시글 생성에 필요한 데이터 목록
	private int itemNum;
	private String title;
	private String content;
	private String createAt;
	private String updateAt;
	private String rentDate;
	private String returnDate;
	private String addr;
	
	public PostCreate() {}
	public PostCreate(int itemNum, String title, String content, String createAt, String updateAt, String rentDate,
			String returnDate, String addr) {
		super();
		this.itemNum = itemNum;
		this.title = title;
		this.content = content;
		this.createAt = createAt;
		this.updateAt = updateAt;
		this.rentDate = rentDate;
		this.returnDate = returnDate;
		this.addr = addr;
	}
	
	public int getItemNum() {
		return itemNum;
	}
	public void setItemNum(int itemNum) {
		this.itemNum = itemNum;
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
	public String getCreateAt() {
		return createAt;
	}
	public void setCreateAt(String createAt) {
		this.createAt = createAt;
	}
	public String getUpdateAt() {
		return updateAt;
	}
	public void setUpdateAt(String updateAt) {
		this.updateAt = updateAt;
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
	
	
}
