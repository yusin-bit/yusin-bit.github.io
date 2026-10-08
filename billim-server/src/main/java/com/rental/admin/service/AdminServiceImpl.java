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
import main.java.com.rental.admin.repository.AdminRepository;
import main.java.com.rental.admin.repository.AdminRepositoryImpl;
import main.java.com.rental.common.exception.AdminException;
import main.java.com.rental.user.service.UserServiceImpl;

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

	// ===================== 확장 기능 =====================

	@Override
	public void suspendUser(String userId, String reason) throws AdminException {
		if (blank(reason) || reason.trim().length() > 100) throw new AdminException("정지 사유를 1~100자로 입력해주세요.");
		if (repository.setUserSuspended(userId, true, reason.trim()) == 0) throw new AdminException("해당 회원을 찾을 수 없습니다.");
	}

	@Override
	public void unsuspendUser(String userId) throws AdminException {
		if (repository.setUserSuspended(userId, false, null) == 0) throw new AdminException("해당 회원을 찾을 수 없습니다.");
	}

	@Override
	public void updateUser(String userId, String nickName, String name, String phone) throws AdminException {
		if (blank(nickName) || nickName.trim().length() > 10 || blank(name) || name.trim().length() > 10)
			throw new AdminException("닉네임과 이름은 1~10자로 입력해주세요.");
		String normalized = UserServiceImpl.normalizePhone(phone);
		if (normalized == null) throw new AdminException("전화번호는 010-1234-5678 형식으로 입력해주세요.");
		if (repository.updateUser(userId, nickName.trim(), name.trim(), normalized) == 0)
			throw new AdminException("해당 회원을 찾을 수 없습니다.");
	}

	@Override
	public void deleteUser(String userId) throws AdminException {
		if (repository.deleteUser(userId) == 0) throw new AdminException("해당 회원을 찾을 수 없습니다.");
	}

	@Override
	public void updatePost(int postNum, String title, String content, String rentDate, String returnDate, String addr) throws AdminException {
		if (repository.updatePost(postNum, title, content, rentDate, returnDate, addr) == 0)
			throw new AdminException("해당 게시글을 찾을 수 없습니다.");
	}

	@Override
	public void forceCancelRental(int rentalNum) throws AdminException {
		if (repository.forceRentalStatus(rentalNum, false) == 0)
			throw new AdminException("진행 중인 대여만 강제 취소할 수 있습니다.");
	}

	@Override
	public void forceCompleteRental(int rentalNum) throws AdminException {
		if (repository.forceRentalStatus(rentalNum, true) == 0)
			throw new AdminException("승인 이후 진행 중인 대여만 강제 완료할 수 있습니다.");
	}

	@Override
	public void updateItem(int itemNum, String itemName, String smallCategoryCode) throws AdminException {
		if (blank(itemName) || itemName.trim().length() > 50) throw new AdminException("물품명은 1~50자로 입력해주세요.");
		if (blank(smallCategoryCode)) throw new AdminException("소분류를 선택해주세요.");
		if (repository.updateItem(itemNum, itemName.trim(), smallCategoryCode.trim()) == 0)
			throw new AdminException("해당 물품을 찾을 수 없습니다.");
	}

	@Override
	public List<AdminRow> admins() throws AdminException {
		return repository.selectAdmins();
	}

	@Override
	public void addAdmin(String id, String name, String password) throws AdminException {
		if (id == null || !id.matches("[A-Za-z0-9_]{4,20}")) throw new AdminException("관리자 아이디는 영문·숫자·_ 4~20자로 입력해주세요.");
		if (blank(name) || name.trim().length() > 10) throw new AdminException("관리자 이름은 1~10자로 입력해주세요.");
		validAdminPassword(password);
		repository.insertAdmin(id, name.trim(), password);
	}

	@Override
	public void changeOwnPassword(String adminId, String currentPassword, String newPassword) throws AdminException {
		if (blank(currentPassword) || repository.login(adminId, currentPassword) == null)
			throw new AdminException("현재 비밀번호가 올바르지 않습니다.");
		validAdminPassword(newPassword);
		repository.updateAdminPassword(adminId, newPassword);
	}

	@Override
	public void deleteAdmin(String requesterId, String targetId) throws AdminException {
		if (requesterId.equals(targetId)) throw new AdminException("자기 자신은 삭제할 수 없습니다.");
		if (repository.deleteAdmin(targetId) == 0) throw new AdminException("관리자를 찾을 수 없거나 마지막 관리자입니다.");
	}

	// 관리자 비밀번호는 회원보다 강하게: 8~64자, 영문과 숫자 모두 포함
	private static void validAdminPassword(String password) throws AdminException {
		if (password == null || password.length() < 8 || password.length() > 64
				|| !password.matches(".*[A-Za-z].*") || !password.matches(".*\\d.*"))
			throw new AdminException("관리자 비밀번호는 영문과 숫자를 포함해 8~64자로 입력해주세요.");
	}

	@Override
	public List<NoticeRow> notices(boolean activeOnly) throws AdminException {
		return repository.selectNotices(activeOnly);
	}

	@Override
	public void addNotice(String title, String content, String adminId) throws AdminException {
		validNotice(title, content);
		repository.insertNotice(title.trim(), content.trim(), adminId);
	}

	@Override
	public void updateNotice(int noticeNum, String title, String content) throws AdminException {
		validNotice(title, content);
		if (repository.updateNotice(noticeNum, title.trim(), content.trim()) == 0) throw new AdminException("해당 공지를 찾을 수 없습니다.");
	}

	@Override
	public void setNoticeActive(int noticeNum, boolean active) throws AdminException {
		if (repository.setNoticeActive(noticeNum, active) == 0) throw new AdminException("해당 공지를 찾을 수 없습니다.");
	}

	@Override
	public void deleteNotice(int noticeNum) throws AdminException {
		if (repository.deleteNotice(noticeNum) == 0) throw new AdminException("해당 공지를 찾을 수 없습니다.");
	}

	private static void validNotice(String title, String content) throws AdminException {
		if (blank(title) || title.trim().length() > 100 || blank(content) || content.trim().length() > 1000)
			throw new AdminException("공지 제목은 1~100자, 내용은 1~1000자로 입력해주세요.");
	}

	@Override
	public void log(String adminId, String action, String detail, int result, String ip) {
		try {
			repository.insertLog(adminId, action, detail, result, ip);
		} catch (AdminException e) {
			// 기록 실패가 관리자 작업 자체를 막지 않도록 서버 로그로만 남긴다
			System.err.println("[admin-log] 기록 실패: " + action);
		}
	}

	@Override
	public List<LogRow> logs(int limit) throws AdminException {
		return repository.selectLogs(Math.max(1, Math.min(limit, 200)));
	}

	@Override
	public List<StatRow> topCategories() throws AdminException {
		return repository.topCategories(5);
	}

	@Override
	public List<StatRow> topLenders() throws AdminException {
		return repository.topLenders(5);
	}

	@Override
	public List<StatRow> topBorrowers() throws AdminException {
		return repository.topBorrowers(5);
	}

	private static boolean blank(String value) {
		return value == null || value.isBlank();
	}
}
