package main.java.com.rental.admin.service;

import java.util.List;

import main.java.com.rental.admin.dto.AdminDtos.AdminAccount;
import main.java.com.rental.admin.dto.AdminDtos.CategoryRow;
import main.java.com.rental.admin.dto.AdminDtos.ItemRow;
import main.java.com.rental.admin.dto.AdminDtos.PostRow;
import main.java.com.rental.admin.dto.AdminDtos.RentalRow;
import main.java.com.rental.admin.dto.AdminDtos.Summary;
import main.java.com.rental.admin.dto.AdminDtos.UserRow;
import main.java.com.rental.admin.repository.AdminRepository;
import main.java.com.rental.admin.repository.AdminRepositoryImpl;
import main.java.com.rental.common.exception.AdminException;

public class AdminServiceImpl implements AdminService {
	private static final AdminServiceImpl instance = new AdminServiceImpl();
	private final AdminRepository repository = new AdminRepositoryImpl();

	private AdminServiceImpl() {
	}

	public static AdminServiceImpl getInstance() {
		return instance;
	}

	@Override
	public AdminAccount login(String id, String password) throws AdminException {
		if (blank(id) || blank(password)) throw new AdminException("관리자 아이디와 비밀번호를 입력해주세요.");
		AdminAccount account = repository.login(id.trim(), password);
		if (account == null) throw new AdminException("관리자 정보가 올바르지 않습니다.");
		return account;
	}

	@Override
	public Summary summary() throws AdminException {
		return repository.summary();
	}

	@Override
	public List<UserRow> users() throws AdminException {
		return repository.selectUsers();
	}

	@Override
	public void resetUserPassword(String userId, String newPassword) throws AdminException {
		if (blank(userId) || newPassword == null || newPassword.length() < 4 || newPassword.length() > 20)
			throw new AdminException("임시 비밀번호는 4~20자로 입력해주세요.");
		if (repository.resetUserPassword(userId, newPassword) == 0)
			throw new AdminException("해당 회원을 찾을 수 없습니다.");
	}

	@Override
	public List<PostRow> posts() throws AdminException {
		return repository.selectPosts();
	}

	@Override
	public void deletePost(int postNum) throws AdminException {
		if (repository.deletePost(postNum) == 0) throw new AdminException("해당 게시글을 찾을 수 없습니다.");
	}

	@Override
	public List<RentalRow> rentals() throws AdminException {
		return repository.selectRentals();
	}

	@Override
	public void rejectRental(int rentalNum) throws AdminException {
		if (repository.rejectRequestedRental(rentalNum) == 0)
			throw new AdminException("승인 대기 중인 대여 신청만 거절할 수 있습니다.");
	}

	@Override
	public List<ItemRow> items() throws AdminException {
		return repository.selectItems();
	}

	@Override
	public void deleteItem(int itemNum) throws AdminException {
		if (repository.deleteItem(itemNum) == 0) throw new AdminException("해당 물품을 찾을 수 없습니다.");
	}

	@Override
	public void restoreItem(int itemNum) throws AdminException {
		if (repository.restoreItemAvailable(itemNum) == 0)
			throw new AdminException("진행 중인 대여가 있거나 이미 대여 가능 상태인 물품입니다.");
	}

	@Override
	public List<CategoryRow> categories() throws AdminException {
		return repository.selectCategories();
	}

	@Override
	public String addBigCategory(String name) throws AdminException {
		return repository.insertBigCategory(validCategoryName(name));
	}

	@Override
	public String addSmallCategory(String bigCode, String name) throws AdminException {
		if (blank(bigCode)) throw new AdminException("대분류를 선택해주세요.");
		return repository.insertSmallCategory(bigCode.trim(), validCategoryName(name));
	}

	@Override
	public void deleteSmallCategory(String smallCode) throws AdminException {
		if (repository.deleteSmallCategory(smallCode) == 0) throw new AdminException("해당 소분류를 찾을 수 없습니다.");
	}

	@Override
	public void deleteBigCategory(String bigCode) throws AdminException {
		if (repository.deleteBigCategory(bigCode) == 0) throw new AdminException("해당 대분류를 찾을 수 없습니다.");
	}

	// 카테고리 이름: 1~10자 (DB 컬럼 VARCHAR(10))
	private static String validCategoryName(String name) throws AdminException {
		if (blank(name) || name.trim().length() > 10) throw new AdminException("카테고리 이름은 1~10자로 입력해주세요.");
		return name.trim();
	}

	private static boolean blank(String value) {
		return value == null || value.isBlank();
	}
}
