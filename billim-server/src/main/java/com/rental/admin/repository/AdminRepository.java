package main.java.com.rental.admin.repository;

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

public interface AdminRepository {
	/** 아이디·비밀번호가 맞으면 관리자 정보, 아니면 null */
	AdminAccount login(String id, String password) throws AdminException;

	Summary summary() throws AdminException;

	// ---- 회원
	List<UserRow> selectUsers() throws AdminException;

	int resetUserPassword(String userId, String newPassword) throws AdminException;

	int setUserSuspended(String userId, boolean suspended, String reason) throws AdminException;

	int updateUser(String userId, String nickName, String name, String phone) throws AdminException;

	/** 물품·대여 내역이 없는 회원만 삭제된다 (있으면 예외) */
	int deleteUser(String userId) throws AdminException;

	// ---- 게시글
	List<PostRow> selectPosts() throws AdminException;

	int updatePost(int postNum, String title, String content, String rentDate, String returnDate, String addr) throws AdminException;

	int deletePost(int postNum) throws AdminException;

	// ---- 대여
	List<RentalRow> selectRentals() throws AdminException;

	/** 승인 대기(100) 중인 대여 신청만 거절(102) 처리 */
	int rejectRequestedRental(int rentalNum) throws AdminException;

	/** 진행 중인 대여를 강제로 취소(102) 또는 완료(211) 처리하고 물품을 대여 가능으로 되돌린다 */
	int forceRentalStatus(int rentalNum, boolean complete) throws AdminException;

	// ---- 물품
	List<ItemRow> selectItems() throws AdminException;

	int updateItem(int itemNum, String itemName, String smallCategoryCode) throws AdminException;

	/** 게시글이 없는 물품만 삭제된다 (게시글이 있으면 예외) */
	int deleteItem(int itemNum) throws AdminException;

	/** 진행 중인 대여가 없는데 '대여 중'으로 남은 물품을 대여 가능으로 되돌린다 */
	int restoreItemAvailable(int itemNum) throws AdminException;

	// ---- 카테고리
	List<CategoryRow> selectCategories() throws AdminException;

	String insertBigCategory(String name) throws AdminException;

	String insertSmallCategory(String bigCode, String name) throws AdminException;

	int deleteSmallCategory(String smallCode) throws AdminException;

	int deleteBigCategory(String bigCode) throws AdminException;

	// ---- 관리자 계정
	List<AdminRow> selectAdmins() throws AdminException;

	int insertAdmin(String id, String name, String password) throws AdminException;

	int updateAdminPassword(String id, String newPassword) throws AdminException;

	int deleteAdmin(String id) throws AdminException;

	// ---- 공지사항
	List<NoticeRow> selectNotices(boolean activeOnly) throws AdminException;

	int insertNotice(String title, String content, String adminId) throws AdminException;

	int updateNotice(int noticeNum, String title, String content) throws AdminException;

	int setNoticeActive(int noticeNum, boolean active) throws AdminException;

	int deleteNotice(int noticeNum) throws AdminException;

	// ---- 활동 기록 / 통계
	void insertLog(String adminId, String action, String detail, int result, String ip) throws AdminException;

	List<LogRow> selectLogs(int limit) throws AdminException;

	List<StatRow> topCategories(int limit) throws AdminException;

	List<StatRow> topLenders(int limit) throws AdminException;

	List<StatRow> topBorrowers(int limit) throws AdminException;
}
