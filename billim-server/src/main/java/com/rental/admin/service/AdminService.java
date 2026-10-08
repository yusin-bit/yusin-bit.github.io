package main.java.com.rental.admin.service;

import java.util.List;

import main.java.com.rental.admin.dto.AdminDtos.AdminAccount;
import main.java.com.rental.admin.dto.AdminDtos.CategoryRow;
import main.java.com.rental.admin.dto.AdminDtos.ItemRow;
import main.java.com.rental.admin.dto.AdminDtos.PostRow;
import main.java.com.rental.admin.dto.AdminDtos.RentalRow;
import main.java.com.rental.admin.dto.AdminDtos.Summary;
import main.java.com.rental.admin.dto.AdminDtos.UserRow;
import main.java.com.rental.common.exception.AdminException;

/**
 * 관리자 기능: 전체 현황, 회원 관리(임시 비밀번호 발급), 물품 관리(삭제·상태 복구),
 * 카테고리 관리(추가·삭제), 게시글 관리(삭제), 대여 관리(승인 대기 신청 거절)
 */
public interface AdminService {
	AdminAccount login(String id, String password) throws AdminException;

	Summary summary() throws AdminException;

	List<UserRow> users() throws AdminException;

	void resetUserPassword(String userId, String newPassword) throws AdminException;

	List<PostRow> posts() throws AdminException;

	void deletePost(int postNum) throws AdminException;

	List<RentalRow> rentals() throws AdminException;

	void rejectRental(int rentalNum) throws AdminException;

	List<ItemRow> items() throws AdminException;

	void deleteItem(int itemNum) throws AdminException;

	void restoreItem(int itemNum) throws AdminException;

	List<CategoryRow> categories() throws AdminException;

	String addBigCategory(String name) throws AdminException;

	String addSmallCategory(String bigCode, String name) throws AdminException;

	void deleteSmallCategory(String smallCode) throws AdminException;

	void deleteBigCategory(String bigCode) throws AdminException;
}
