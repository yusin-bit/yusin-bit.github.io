package main.java.com.rental.rental.entity;

import main.java.com.rental.rental.enums.RentalStatus;

public class Rental {

	private int rentalNum;
	private String borrowerId;
	private RentalStatus status;
	private int postNum;
	
	
	@Override
	public String toString() {
		String statusName = status == null ? "상태 없음" : status.getName();

		return "[대여 #" + rentalNum + "]\n"
				+ "게시글번호 : " + postNum + "  |  상태 : " + statusName + "\n"
				+ "임차인 : " + borrowerId;
	}

	public Rental() {
	}

	public Rental(int rentalNum, String borrowerId, RentalStatus status, int postNum) {
		this.rentalNum = rentalNum;
		this.borrowerId = borrowerId;
		this.status = status;
		this.postNum = postNum;
	}

	public int getRentalNum() {
		return rentalNum;
	}

	public void setRentalNum(int rentalNum) {
		this.rentalNum = rentalNum;
	}

	public String getBorrowerId() {
		return borrowerId;
	}

	public void setBorrowerId(String borrowerId) {
		this.borrowerId = borrowerId;
	}

	public RentalStatus getStatus() {
		return status;
	}

	public void setStatus(RentalStatus status) {
		this.status = status;
	}

	public int getPostNum() {
		return postNum;
	}

	public void setPostNum(int postNum) {
		this.postNum = postNum;
	}
}
