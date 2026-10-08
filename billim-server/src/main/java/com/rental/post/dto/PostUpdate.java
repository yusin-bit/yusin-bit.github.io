package main.java.com.rental.post.dto;

public class PostUpdate {
	//게시글 수정에 필요한 데이터 목록
	private int postNum;
	private String title;
	private String content;
	private String updateAt;
	private String rentDate;
	private String returnDate;
	private String addr;
	
	
	public PostUpdate() {}
	public PostUpdate(int postNum, String title, String content, String updateAt, String rentDate, String returnDate,
			String addr) {
		super();
		this.postNum = postNum;
		this.title = title;
		this.content = content;
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
