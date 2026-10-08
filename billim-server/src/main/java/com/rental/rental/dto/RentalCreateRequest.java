package main.java.com.rental.rental.dto;

public class RentalCreateRequest {

	private int postNum;
	private String borrowerId;

	public RentalCreateRequest() {
	}

	public RentalCreateRequest(int postNum, String borrowerId) {
		super();
		this.postNum = postNum;
		this.borrowerId = borrowerId;
	}

	public int getPostNum() {
		return postNum;
	}

	public void setPostNum(int postNum) {
		this.postNum = postNum;
	}
	
	public String getBorrowerId() {
		return borrowerId;
	}

	public void setBorrowerId(String borrowerId) {
		this.borrowerId = borrowerId;
	}

}