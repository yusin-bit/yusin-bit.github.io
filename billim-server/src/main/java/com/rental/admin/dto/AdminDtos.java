package main.java.com.rental.admin.dto;

/**
 * 관리자 화면에서 쓰는 조회 결과 모음
 */
public final class AdminDtos {
	private AdminDtos() {
	}

	/** 로그인한 관리자 */
	public record AdminAccount(String id, String name) { }

	/** 전체 현황 */
	public record Summary(int users, int items, int posts, int rentals,
			int requested, int inProgress, int completed, int rejected) { }

	/** 회원 목록 한 줄 */
	public record UserRow(String id, String nickName, String name, String phone, int itemCount, int rentalCount) { }

	/** 게시글 목록 한 줄 */
	public record PostRow(int postNum, String title, String itemName, String lenderId,
			String rentDate, String returnDate, String addr, boolean available, int rentalCount) { }

	/** 대여 목록 한 줄 */
	public record RentalRow(int rentalNum, int postNum, String itemName, String lenderId, String borrowerId, int status) { }

	/** 물품 목록 한 줄: activeRentals = 진행 중(승인~반납 확인 대기) 대여 건수 */
	public record ItemRow(int itemNum, String itemName, String lenderId, String bigCategory, String smallCategory,
			boolean available, int postCount, int activeRentals) { }

	/** 카테고리 한 줄 (대분류 + 소분류, 소분류가 없으면 smallCode 가 null) */
	public record CategoryRow(String bigCode, String bigName, String smallCode, String smallName, int itemCount) { }
}
