package main.java.com.rental.reservation.enums;

public enum ReservationStatus {
	RETURNING(200),   // 반납 신청
    APPROVED(201),		// 대여 승인
    REJECTED(202);		// 대여 거절

	
	private int status;
	ReservationStatus(int status){
		this.status = status;
	}
	
	public int getStatus() {
		return status;
	}
}
