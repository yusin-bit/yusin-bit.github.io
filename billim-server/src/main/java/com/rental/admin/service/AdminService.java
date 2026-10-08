package main.java.com.rental.admin.service;

import java.util.List;

import main.java.com.rental.admin.dto.AdminDtos.AdminAccount;
import main.java.com.rental.admin.dto.AdminDtos.AdminRow;
import main.java.com.rental.admin.dto.AdminDtos.CategoryRow;
import main.java.com.rental.admin.dto.AdminDtos.ItemRow;
import main.java.com.rental.admin.dto.AdminDtos.LogRow;
import main.java.com.rental.admin.dto.AdminDtos.NoticeRow;
import main.java.com.rental.admin.dto.AdminDtos.PostRow;
import main.java.com.rental.admin.dto.AdminDtos.RentalRow;
import main.java.com.rental.admin.dto.AdminDtos.StatRow;
import main.java.com.rental.admin.dto.AdminDtos.Summary;
import main.java.com.rental.admin.dto.AdminDtos.UserRow;
import main.java.com.rental.common.exception.AdminException;

/**
 * 관리자 기능
 * - 현황·통계, 활동 기록
 * - 회원: 임시 비밀번호, 이용 정지/해제, 정보 수정, 삭제
 * - 물품: 정보 수정, 삭제, 대여 상태 복구 / 카테고리: 추가·삭제
 * - 게시글: 수정, 삭제 / 대여: 승인 대기 거절, 강제 취소·완료
 * - 공지사항 관리, 관리자 계정 관리
 */
public interface AdminService {
	AdminAccount login(String id, String password) throws AdminException;

	Summary summary() throws AdminException;

	// 회원
	List<UserRow> users() throws AdminException;

	void resetUserPassword(String userId, String newPassword) throws AdminException;

	void suspendUser(String userId, String reason) throws AdminException;

	void unsuspendUser(String userId) throws AdminException;

	void updateUser(String userId, String nickName, String name, String phone) throws AdminException;

	void deleteUser(String userId) throws AdminException;

	// 게시글
	List<PostRow> posts() throws AdminException;

	void updatePost(int postNum, String title, String content, String rentDate, String returnDate, String addr) throws AdminException;

	void deletePost(int postNum) throws AdminException;

	// 대여
	List<RentalRow> rentals() throws AdminException;

	void rejectRental(int rentalNum) throws AdminException;

	void forceCancelRental(int rentalNum) throws AdminException;

	void forceCompleteRental(int rentalNum) throws AdminException;

	// 물품
	List<ItemRow> items() throws AdminException;

	void updateItem(int itemNum, String itemName, String smallCategoryCode) throws AdminException;

	void deleteItem(int itemNum) throws AdminException;

	void restoreItem(int itemNum) throws AdminException;

	// 카테고리
	List<CategoryRow> categories() throws AdminException;

	String addBigCategory(String name) throws AdminException;

	String addSmallCategory(String bigCode, String name) throws AdminException;

	void deleteSmallCategory(String smallCode) throws AdminException;

	void deleteBigCategory(String bigCode) throws AdminException;

	// 관리자 계정
	List<AdminRow> admins() throws AdminException;

	void addAdmin(String id, String name, String password) throws AdminException;

	void changeOwnPassword(String adminId, String currentPassword, String newPassword) throws AdminException;

	void deleteAdmin(String requesterId, String targetId) throws AdminException;

	// 공지사항
	List<NoticeRow> notices(boolean activeOnly) throws AdminException;

	void addNotice(String title, String content, String adminId) throws AdminException;

	void updateNotice(int noticeNum, String title, String content) throws AdminException;

	void setNoticeActive(int noticeNum, boolean active) throws AdminException;

	void deleteNotice(int noticeNum) throws AdminException;

	// 활동 기록 / 통계
	void log(String adminId, String action, String detail, int result, String ip);

	List<LogRow> logs(int limit) throws AdminException;

	List<StatRow> topCategories() throws AdminException;

	List<StatRow> topLenders() throws AdminException;

	List<StatRow> topBorrowers() throws AdminException;
}
