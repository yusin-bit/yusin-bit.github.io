package main.java.com.rental.rental.enums;

public enum RentalStatus {

	REQUESTED(100, "대여 신청 중"), // 대여 신청
	APPROVED(101, "대여 승인"), // 대여 승인
	REJECTED(102, "대여 거절"), // 대여 거절

	RENTED(110, "대여 중"), // 대여 중

	RETURN_REQUESTED(200, "반납 신청 대기중"), // 임차인 반납 신청
	RETURN_APPROVED(201, "반납 신청 승인"), // 대여자 반납 승인

	BORROWER_CONFIRMED(210, "반납 확인 대기중"), // 임차인 반납 확인
	COMPLETED(211, "반납 확인"); // 대여자 확인까지 완료 = 최종 완료

	private final int code;
	private final String name;

	RentalStatus(int code, String name) {
		this.code = code;
		this.name = name;
	}

	public int getCode() {
		return code;
	}

	public String getName() {
		return name;
	}

	public static RentalStatus fromCode(int code) {
		for (RentalStatus status : RentalStatus.values()) {
			if (status.getCode() == code) {
				return status;
			}
		}

		throw new IllegalArgumentException("존재하지 않는 RentalStatus code: " + code);
	}

}
