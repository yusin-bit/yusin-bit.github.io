package main.java.com.rental.post.entity;

public class Post {
	private int postNum;
	private int itemNum;
	private String title;
	private String content;
	private String createAt;
	private String updateAt;
	private String rentDate;
	private String returnDate;
	private String addr;
	
	
	public Post() {}
	public Post(int postNum, int itemNum, String title, String content, String createAt, String updateAt,
			String rentDate, String returnDate, String addr) {
		super();
		this.postNum = postNum;
		this.itemNum = itemNum;
		this.title = title;
		this.content = content;
		this.createAt = createAt;
		this.updateAt = updateAt;
		this.rentDate = rentDate;
		this.returnDate = returnDate;
		this.addr = addr;
	}
	
	
	
	public int getPostNum() {
		return postNum;
	}
	public void setPostNum(int postNum) {
		this.postNum = postNum;
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
	@Override
	public String toString() {
		return "[게시글 #" + postNum + "] " + title + "\n"
				+ "물품번호 : " + itemNum + "  |  대여기간 : " + rentDate + " ~ " + returnDate + "\n"
				+ "거래장소 : " + addr + "  |  내용 : " + content;
	}
	
}
