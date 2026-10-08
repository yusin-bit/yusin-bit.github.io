package main.java.com.rental.admin.repository;

import java.util.List;

import main.java.com.rental.admin.dto.AdminDtos.AdminAccount;
import main.java.com.rental.admin.dto.AdminDtos.CategoryRow;
import main.java.com.rental.admin.dto.AdminDtos.ItemRow;
import main.java.com.rental.admin.dto.AdminDtos.PostRow;
import main.java.com.rental.admin.dto.AdminDtos.RentalRow;
import main.java.com.rental.admin.dto.AdminDtos.Summary;
import main.java.com.rental.admin.dto.AdminDtos.UserRow;
import main.java.com.rental.common.exception.AdminException;

public interface AdminRepository {
	/** 아이디·비밀번호가 맞으면 관리자 정보, 아니면 null */
	AdminAccount login(String id, String password) throws AdminException;

	Summary summary() throws AdminException;

	List<UserRow> selectUsers() throws AdminException;

	int resetUserPassword(String userId, String newPassword) throws AdminException;

	List<PostRow> selectPosts() throws AdminException;

	int deletePost(int postNum) throws AdminException;

	List<RentalRow> selectRentals() throws AdminException;

	/** 승인 대기(100) 중인 대여 신청만 거절(102) 처리 */
	int rejectRequestedRental(int rentalNum) throws AdminException;

	List<ItemRow> selectItems() throws AdminException;

	/** 게시글이 없는 물품만 삭제된다 (게시글이 있으면 예외) */
	int deleteItem(int itemNum) throws AdminException;

	/** 진행 중인 대여가 없는데 '대여 중'으로 남은 물품을 대여 가능으로 되돌린다 */
	int restoreItemAvailable(int itemNum) throws AdminException;

	List<CategoryRow> selectCategories() throws AdminException;

	String insertBigCategory(String name) throws AdminException;

	String insertSmallCategory(String bigCode, String name) throws AdminException;

	int deleteSmallCategory(String smallCode) throws AdminException;

	int deleteBigCategory(String bigCode) throws AdminException;
}
