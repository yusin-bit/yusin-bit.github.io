package main.java.com.rental.admin.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import main.java.com.rental.admin.dto.AdminDtos.AdminAccount;
import main.java.com.rental.admin.dto.AdminDtos.AdminRow;
import main.java.com.rental.admin.dto.AdminDtos.LogRow;
import main.java.com.rental.admin.dto.AdminDtos.NoticeRow;
import main.java.com.rental.admin.dto.AdminDtos.StatRow;
import main.java.com.rental.admin.dto.AdminDtos.CategoryRow;
import main.java.com.rental.admin.dto.AdminDtos.DeleteResult;
import main.java.com.rental.admin.dto.AdminDtos.ItemRow;
import main.java.com.rental.admin.dto.AdminDtos.PostRow;
import main.java.com.rental.admin.dto.AdminDtos.RentalRow;
import main.java.com.rental.admin.dto.AdminDtos.Summary;
import main.java.com.rental.admin.dto.AdminDtos.UserRow;
import main.java.com.rental.common.exception.AdminException;
import main.java.com.rental.common.util.DBManager;
import main.java.com.rental.common.util.PasswordHasher;

public class AdminRepositoryImpl implements AdminRepository {

	@Override
	public AdminAccount login(String id, String password) throws AdminException {
		try (Connection con = DBManager.getConnection();
				PreparedStatement ps = con.prepareStatement("SELECT ID, PassWord, Name FROM Admin WHERE ID = ?")) {
			ps.setString(1, id);
			try (ResultSet rs = ps.executeQuery()) {
				if (!rs.next() || !PasswordHasher.matches(password, rs.getString("PassWord"))) return null;
				// 평문으로 저장돼 있던 관리자 비밀번호는 로그인 성공 시 해시로 바꿔 저장
				if (PasswordHasher.needsUpgrade(rs.getString("PassWord"))) {
					try (PreparedStatement upgrade = con.prepareStatement("UPDATE Admin SET PassWord = ? WHERE ID = ?")) {
						upgrade.setString(1, PasswordHasher.hash(password));
						upgrade.setString(2, id);
						upgrade.executeUpdate();
					}
				}
				return new AdminAccount(rs.getString("ID"), rs.getString("Name"));
			}
		} catch (SQLException e) {
			throw new AdminException();
		}
	}

	@Override
	public Summary summary() throws AdminException {
		String sql = """
				SELECT (SELECT COUNT(*) FROM User) AS users,
				       (SELECT COUNT(*) FROM Item) AS items,
				       (SELECT COUNT(*) FROM Post) AS posts,
				       (SELECT COUNT(*) FROM Rental) AS rentals,
				       (SELECT COUNT(*) FROM Rental WHERE Status = 100) AS requested,
				       (SELECT COUNT(*) FROM Rental WHERE Status IN (101, 110, 200, 201, 210)) AS inProgress,
				       (SELECT COUNT(*) FROM Rental WHERE Status = 211) AS completed,
				       (SELECT COUNT(*) FROM Rental WHERE Status = 102) AS rejected
				""";
		try (Connection con = DBManager.getConnection();
				PreparedStatement ps = con.prepareStatement(sql);
				ResultSet rs = ps.executeQuery()) {
			rs.next();
			return new Summary(rs.getInt("users"), rs.getInt("items"), rs.getInt("posts"), rs.getInt("rentals"),
					rs.getInt("requested"), rs.getInt("inProgress"), rs.getInt("completed"), rs.getInt("rejected"));
		} catch (SQLException e) {
			throw new AdminException();
		}
	}

	@Override
	public List<UserRow> selectUsers() throws AdminException {
		String sql = """
				SELECT u.ID, u.NickName, u.Name, u.Phone, u.Suspended, u.SuspendReason, u.Withdrawn,
				       (SELECT COUNT(*) FROM Item i WHERE i.LenderID = u.ID) AS itemCount,
				       (SELECT COUNT(*) FROM Rental r WHERE r.BorrowerID = u.ID) AS rentalCount,
				       (SELECT COUNT(*) FROM Post p JOIN Item i ON i.ItemNum = p.ItemNum WHERE i.LenderID = u.ID) AS postCount,
				       (SELECT COUNT(*) FROM Rental r JOIN Post p ON p.PostNum = r.PostNum JOIN Item i ON i.ItemNum = p.ItemNum
				        WHERE i.LenderID = u.ID) AS lentCount,
				       (SELECT COUNT(*) FROM Rental r JOIN Post p ON p.PostNum = r.PostNum JOIN Item i ON i.ItemNum = p.ItemNum
				        WHERE (r.BorrowerID = u.ID OR i.LenderID = u.ID) AND r.Status IN (101, 110, 200, 201, 210)) AS activeCount
				FROM User u
				ORDER BY u.Withdrawn, u.ID
				""";
		List<UserRow> list = new ArrayList<>();
		try (Connection con = DBManager.getConnection();
				PreparedStatement ps = con.prepareStatement(sql);
				ResultSet rs = ps.executeQuery()) {
			while (rs.next()) {
				list.add(new UserRow(rs.getString("ID"), rs.getString("NickName"), rs.getString("Name"),
						rs.getString("Phone"), rs.getInt("itemCount"), rs.getInt("rentalCount"),
						rs.getBoolean("Suspended"), rs.getString("SuspendReason"), rs.getInt("postCount"),
						rs.getInt("lentCount"), rs.getInt("activeCount"), rs.getBoolean("Withdrawn")));
			}
		} catch (SQLException e) {
			throw new AdminException();
		}
		return list;
	}

	@Override
	public int resetUserPassword(String userId, String newPassword) throws AdminException {
		try (Connection con = DBManager.getConnection();
				PreparedStatement ps = con.prepareStatement("UPDATE User SET PassWord = ? WHERE ID = ? AND Withdrawn = FALSE")) {
			ps.setString(1, PasswordHasher.hash(newPassword));
			ps.setString(2, userId);
			return ps.executeUpdate();
		} catch (SQLException e) {
			throw new AdminException();
		}
	}

	@Override
	public List<PostRow> selectPosts() throws AdminException {
		String sql = """
				SELECT p.PostNum, p.Title, p.Content, i.ItemName, i.LenderID, p.RentDate, p.ReturnDate, p.Addr, i.Status,
				       (SELECT COUNT(*) FROM Rental r WHERE r.PostNum = p.PostNum) AS rentalCount
				FROM Post p
				JOIN Item i ON i.ItemNum = p.ItemNum
				ORDER BY p.PostNum DESC
				""";
		List<PostRow> list = new ArrayList<>();
		try (Connection con = DBManager.getConnection();
				PreparedStatement ps = con.prepareStatement(sql);
				ResultSet rs = ps.executeQuery()) {
			while (rs.next()) {
				list.add(new PostRow(rs.getInt("PostNum"), rs.getString("Title"), rs.getString("Content"), rs.getString("ItemName"),
						rs.getString("LenderID"), rs.getString("RentDate"), rs.getString("ReturnDate"),
						rs.getString("Addr"), rs.getBoolean("Status"), rs.getInt("rentalCount")));
			}
		} catch (SQLException e) {
			throw new AdminException();
		}
		return list;
	}

	@Override
	public int deletePost(int postNum) throws AdminException {
		try (Connection con = DBManager.getConnection();
				PreparedStatement ps = con.prepareStatement("DELETE FROM Post WHERE PostNum = ?")) {
			ps.setInt(1, postNum);
			return ps.executeUpdate();
		} catch (SQLException e) {
			if ("23000".equals(e.getSQLState()))
				throw new AdminException("대여 내역이 있는 게시글은 삭제할 수 없습니다.");
			throw new AdminException();
		}
	}

	@Override
	public List<RentalRow> selectRentals() throws AdminException {
		String sql = """
				SELECT r.RentalNum, r.PostNum, i.ItemName, i.LenderID, r.BorrowerID, r.Status
				FROM Rental r
				JOIN Post p ON p.PostNum = r.PostNum
				JOIN Item i ON i.ItemNum = p.ItemNum
				ORDER BY r.RentalNum DESC
				""";
		List<RentalRow> list = new ArrayList<>();
		try (Connection con = DBManager.getConnection();
				PreparedStatement ps = con.prepareStatement(sql);
				ResultSet rs = ps.executeQuery()) {
			while (rs.next()) {
				list.add(new RentalRow(rs.getInt("RentalNum"), rs.getInt("PostNum"), rs.getString("ItemName"),
						rs.getString("LenderID"), rs.getString("BorrowerID"), rs.getInt("Status")));
			}
		} catch (SQLException e) {
			throw new AdminException();
		}
		return list;
	}

	@Override
	public List<ItemRow> selectItems() throws AdminException {
		String sql = """
				SELECT i.ItemNum, i.ItemName, i.LenderID, i.SmallCategoryCode, i.Status, b.Category AS bigName, s.Category AS smallName,
				       (SELECT COUNT(*) FROM Post p WHERE p.ItemNum = i.ItemNum) AS postCount,
				       (SELECT COUNT(*) FROM Rental r JOIN Post p ON p.PostNum = r.PostNum
				         WHERE p.ItemNum = i.ItemNum AND r.Status IN (101, 110, 200, 201, 210)) AS activeRentals
				FROM Item i
				JOIN SmallCategory s ON s.SmallCategoryCode = i.SmallCategoryCode
				JOIN BigCategory b ON b.BigCategoryCode = s.BigCategoryCode
				ORDER BY i.ItemNum DESC
				""";
		List<ItemRow> list = new ArrayList<>();
		try (Connection con = DBManager.getConnection();
				PreparedStatement ps = con.prepareStatement(sql);
				ResultSet rs = ps.executeQuery()) {
			while (rs.next()) {
				list.add(new ItemRow(rs.getInt("ItemNum"), rs.getString("ItemName"), rs.getString("LenderID"), rs.getString("SmallCategoryCode"),
						rs.getString("bigName"), rs.getString("smallName"), rs.getBoolean("Status"),
						rs.getInt("postCount"), rs.getInt("activeRentals")));
			}
		} catch (SQLException e) {
			throw new AdminException();
		}
		return list;
	}

	@Override
	public int deleteItem(int itemNum) throws AdminException {
		try (Connection con = DBManager.getConnection();
				PreparedStatement ps = con.prepareStatement("DELETE FROM Item WHERE ItemNum = ?")) {
			ps.setInt(1, itemNum);
			return ps.executeUpdate();
		} catch (SQLException e) {
			if ("23000".equals(e.getSQLState()))
				throw new AdminException("대여 글이 있는 물품입니다. 게시글을 먼저 삭제해주세요.");
			throw new AdminException();
		}
	}

	@Override
	public int restoreItemAvailable(int itemNum) throws AdminException {
		String sql = """
				UPDATE Item i SET i.Status = TRUE
				WHERE i.ItemNum = ? AND i.Status = FALSE
				  AND NOT EXISTS (SELECT 1 FROM User u WHERE u.ID = i.LenderID AND u.Withdrawn = TRUE)
				  AND NOT EXISTS (SELECT 1 FROM Rental r JOIN Post p ON p.PostNum = r.PostNum
				                  WHERE p.ItemNum = i.ItemNum AND r.Status IN (101, 110, 200, 201, 210))
				""";
		try (Connection con = DBManager.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
			ps.setInt(1, itemNum);
			return ps.executeUpdate();
		} catch (SQLException e) {
			throw new AdminException();
		}
	}

	@Override
	public List<CategoryRow> selectCategories() throws AdminException {
		String sql = """
				SELECT b.BigCategoryCode, b.Category AS bigName, s.SmallCategoryCode, s.Category AS smallName,
				       (SELECT COUNT(*) FROM Item i WHERE i.SmallCategoryCode = s.SmallCategoryCode) AS itemCount
				FROM BigCategory b
				LEFT JOIN SmallCategory s ON s.BigCategoryCode = b.BigCategoryCode
				ORDER BY b.BigCategoryCode, s.SmallCategoryCode
				""";
		List<CategoryRow> list = new ArrayList<>();
		try (Connection con = DBManager.getConnection();
				PreparedStatement ps = con.prepareStatement(sql);
				ResultSet rs = ps.executeQuery()) {
			while (rs.next()) {
				list.add(new CategoryRow(rs.getString("BigCategoryCode"), rs.getString("bigName"),
						rs.getString("SmallCategoryCode"), rs.getString("smallName"), rs.getInt("itemCount")));
			}
		} catch (SQLException e) {
			throw new AdminException();
		}
		return list;
	}

	@Override
	public String insertBigCategory(String name) throws AdminException {
		// 코드 규칙: B01, B02 ... (기존 최대 번호 + 1)
		try (Connection con = DBManager.getConnection()) {
			if (exists(con, "SELECT 1 FROM BigCategory WHERE Category = ?", name))
				throw new AdminException("같은 이름의 대분류가 이미 있습니다.");
			int next = nextNumber(con, "SELECT MAX(CAST(SUBSTRING(BigCategoryCode, 2) AS UNSIGNED)) FROM BigCategory", null);
			String code = String.format("B%02d", next);
			try (PreparedStatement ps = con.prepareStatement("INSERT INTO BigCategory (BigCategoryCode, Category) VALUES (?, ?)")) {
				ps.setString(1, code);
				ps.setString(2, name);
				ps.executeUpdate();
			}
			return code;
		} catch (SQLException e) {
			throw new AdminException();
		}
	}

	@Override
	public String insertSmallCategory(String bigCode, String name) throws AdminException {
		// 코드 규칙: S + 대분류 번호 2자리 + 순번 2자리 (예: B01 → S0105)
		try (Connection con = DBManager.getConnection()) {
			if (!exists(con, "SELECT 1 FROM BigCategory WHERE BigCategoryCode = ?", bigCode))
				throw new AdminException("대분류를 찾을 수 없습니다.");
			try (PreparedStatement ps = con.prepareStatement("SELECT 1 FROM SmallCategory WHERE BigCategoryCode = ? AND Category = ?")) {
				ps.setString(1, bigCode);
				ps.setString(2, name);
				try (ResultSet rs = ps.executeQuery()) {
					if (rs.next()) throw new AdminException("같은 이름의 소분류가 이미 있습니다.");
				}
			}
			int next = nextNumber(con, "SELECT MAX(CAST(SUBSTRING(SmallCategoryCode, 4) AS UNSIGNED)) FROM SmallCategory WHERE BigCategoryCode = ?", bigCode);
			String code = "S" + bigCode.substring(1) + String.format("%02d", next);
			try (PreparedStatement ps = con.prepareStatement("INSERT INTO SmallCategory (SmallCategoryCode, BigCategoryCode, Category) VALUES (?, ?, ?)")) {
				ps.setString(1, code);
				ps.setString(2, bigCode);
				ps.setString(3, name);
				ps.executeUpdate();
			}
			return code;
		} catch (SQLException e) {
			throw new AdminException();
		}
	}

	@Override
	public int deleteSmallCategory(String smallCode) throws AdminException {
		try (Connection con = DBManager.getConnection();
				PreparedStatement ps = con.prepareStatement("DELETE FROM SmallCategory WHERE SmallCategoryCode = ?")) {
			ps.setString(1, smallCode);
			return ps.executeUpdate();
		} catch (SQLException e) {
			if ("23000".equals(e.getSQLState()))
				throw new AdminException("이 소분류를 쓰는 물품이 있어 삭제할 수 없습니다.");
			throw new AdminException();
		}
	}

	@Override
	public int deleteBigCategory(String bigCode) throws AdminException {
		try (Connection con = DBManager.getConnection();
				PreparedStatement ps = con.prepareStatement("DELETE FROM BigCategory WHERE BigCategoryCode = ?")) {
			ps.setString(1, bigCode);
			return ps.executeUpdate();
		} catch (SQLException e) {
			if ("23000".equals(e.getSQLState()))
				throw new AdminException("소분류가 남아 있는 대분류는 삭제할 수 없습니다.");
			throw new AdminException();
		}
	}

	private static boolean exists(Connection con, String sql, String value) throws SQLException {
		try (PreparedStatement ps = con.prepareStatement(sql)) {
			ps.setString(1, value);
			try (ResultSet rs = ps.executeQuery()) {
				return rs.next();
			}
		}
	}

	private static int nextNumber(Connection con, String sql, String param) throws SQLException {
		try (PreparedStatement ps = con.prepareStatement(sql)) {
			if (param != null) ps.setString(1, param);
			try (ResultSet rs = ps.executeQuery()) {
				return rs.next() ? rs.getInt(1) + 1 : 1;
			}
		}
	}

	@Override
	public int rejectRequestedRental(int rentalNum) throws AdminException {
		try (Connection con = DBManager.getConnection();
				PreparedStatement ps = con.prepareStatement("UPDATE Rental SET Status = 102 WHERE RentalNum = ? AND Status = 100")) {
			ps.setInt(1, rentalNum);
			return ps.executeUpdate();
		} catch (SQLException e) {
			throw new AdminException();
		}
	}

	// ===================== 확장 기능 =====================

	/** UPDATE/INSERT/DELETE 한 줄 실행 (중복 키 23000 은 duplicateMessage 로, 참조 무결성은 fkMessage 로) */
	private static int update(String duplicateMessage, String fkMessage, String sql, Object... params) throws AdminException {
		try (Connection con = DBManager.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
			for (int i = 0; i < params.length; i++) ps.setObject(i + 1, params[i]);
			return ps.executeUpdate();
		} catch (SQLException e) {
			if ("23000".equals(e.getSQLState())) {
				// 1062 = 중복 키, 그 외(1451 등) = 다른 데이터가 참조 중
				if (e.getErrorCode() == 1062 && duplicateMessage != null) throw new AdminException(duplicateMessage);
				if (fkMessage != null) throw new AdminException(fkMessage);
			}
			throw new AdminException();
		}
	}

	@Override
	public int setUserSuspended(String userId, boolean suspended, String reason) throws AdminException {
		return update(null, null, "UPDATE User SET Suspended = ?, SuspendReason = ? WHERE ID = ? AND Withdrawn = FALSE",
				suspended, suspended ? reason : null, userId);
	}

	@Override
	public int updateUser(String userId, String nickName, String name, String phone) throws AdminException {
		return update("이미 다른 회원이 쓰는 전화번호입니다.", null,
				"UPDATE User SET NickName = ?, Name = ?, Phone = ? WHERE ID = ? AND Withdrawn = FALSE", nickName, name, phone, userId);
	}

	// 이 회원 물품에 달린 게시글 번호 (대여 기록의 "빌려준 쪽")
	private static final String POSTS_OF_USER = "SELECT p.PostNum FROM Post p JOIN Item i ON i.ItemNum = p.ItemNum WHERE i.LenderID = ?";

	/**
	 * 탈퇴·완전 삭제 공통 준비: 회원 행과 관련 게시글·대여 행을 잠그고, 진행 중인 대여가 있으면 막는다.
	 * (잠그는 동안 다른 요청이 새 신청·승인을 끼워 넣지 못한다)
	 * @return 회원의 Withdrawn 값, 회원이 없으면 null
	 */
	private Boolean lockUserForRemoval(Connection con, String userId) throws SQLException, AdminException {
		Boolean withdrawn;
		try (PreparedStatement ps = con.prepareStatement("SELECT Withdrawn FROM User WHERE ID = ? FOR UPDATE")) {
			ps.setString(1, userId);
			try (ResultSet rs = ps.executeQuery()) {
				if (!rs.next()) return null;
				withdrawn = rs.getBoolean(1);
			}
		}
		try (PreparedStatement ps = con.prepareStatement(POSTS_OF_USER + " FOR UPDATE")) {
			ps.setString(1, userId);
			ps.executeQuery().close();
		}
		try (PreparedStatement ps = con.prepareStatement("SELECT RentalNum FROM Rental WHERE BorrowerID = ? FOR UPDATE")) {
			ps.setString(1, userId);
			ps.executeQuery().close();
		}
		try (PreparedStatement ps = con.prepareStatement("SELECT COUNT(*) FROM Rental WHERE Status IN (101, 110, 200, 201, 210) "
				+ "AND (BorrowerID = ? OR PostNum IN (" + POSTS_OF_USER + "))")) {
			ps.setString(1, userId);
			ps.setString(2, userId);
			try (ResultSet rs = ps.executeQuery()) {
				rs.next();
				int active = rs.getInt(1);
				if (active > 0)
					throw new AdminException("진행 중인 대여가 " + active + "건 있습니다. 대여 관리에서 먼저 강제 완료 또는 강제 취소해주세요.");
			}
		}
		return withdrawn;
	}

	private static int execute(Connection con, String sql, Object... params) throws SQLException {
		try (PreparedStatement ps = con.prepareStatement(sql)) {
			for (int i = 0; i < params.length; i++) ps.setObject(i + 1, params[i]);
			return ps.executeUpdate();
		}
	}

	@Override
	public int withdrawUser(String userId, String placeholderPhone, String randomPasswordHash) throws AdminException {
		try (Connection con = DBManager.getConnection()) {
			con.setAutoCommit(false);
			try {
				Boolean withdrawn = lockUserForRemoval(con, userId);
				if (withdrawn == null) { con.rollback(); return 0; }
				if (withdrawn) { con.rollback(); throw new AdminException("이미 탈퇴 처리된 회원입니다."); }
				// 승인 대기 신청은 양쪽 모두 거절 처리 (끝난 기록은 그대로 둔다)
				execute(con, "UPDATE Rental SET Status = 102 WHERE Status = 100 AND (BorrowerID = ? OR PostNum IN (" + POSTS_OF_USER + "))",
						userId, userId);
				// 물품은 남기되 대여 목록에서 빠지도록 대여 불가로
				execute(con, "UPDATE Item SET Status = FALSE WHERE LenderID = ?", userId);
				// 개인정보를 가리고, 비밀번호를 아무도 모르는 값으로 바꿔 로그인·재설정을 막는다
				int changed = execute(con, """
						UPDATE User SET Withdrawn = TRUE, Suspended = TRUE, SuspendReason = '탈퇴 처리된 계정',
						       Name = '탈퇴회원', NickName = '탈퇴회원', Phone = ?, PassWord = ?
						WHERE ID = ?
						""", placeholderPhone, randomPasswordHash, userId);
				con.commit();
				return changed;
			} catch (SQLException | AdminException e) {
				con.rollback();
				throw e;
			}
		} catch (SQLException e) {
			throw new AdminException();
		}
	}

	@Override
	public DeleteResult deleteUserCascade(String userId) throws AdminException {
		try (Connection con = DBManager.getConnection()) {
			con.setAutoCommit(false);
			try {
				if (lockUserForRemoval(con, userId) == null) { con.rollback(); return null; }
				// 참조하는 쪽부터: 대여 기록(이 회원이 빌린 것 + 이 회원 물품을 남이 빌린 것) → 게시글 → 물품 → 회원
				int rentals = execute(con, "DELETE FROM Rental WHERE BorrowerID = ? OR PostNum IN (" + POSTS_OF_USER + ")", userId, userId);
				int posts = execute(con, "DELETE FROM Post WHERE ItemNum IN (SELECT ItemNum FROM Item WHERE LenderID = ?)", userId);
				int items = execute(con, "DELETE FROM Item WHERE LenderID = ?", userId);
				execute(con, "DELETE FROM User WHERE ID = ?", userId);
				con.commit();
				return new DeleteResult(items, posts, rentals);
			} catch (SQLException | AdminException e) {
				con.rollback();
				throw e;
			}
		} catch (SQLException e) {
			throw new AdminException();
		}
	}

	@Override
	public int updatePost(int postNum, String title, String content, String rentDate, String returnDate, String addr) throws AdminException {
		return update(null, null, "UPDATE Post SET Title = ?, Content = ?, RentDate = ?, ReturnDate = ?, Addr = ? WHERE PostNum = ?",
				title, content, rentDate, returnDate, addr, postNum);
	}

	@Override
	public int updateItem(int itemNum, String itemName, String smallCategoryCode) throws AdminException {
		return update(null, "존재하지 않는 소분류입니다.", "UPDATE Item SET ItemName = ?, SmallCategoryCode = ? WHERE ItemNum = ?",
				itemName, smallCategoryCode, itemNum);
	}

	@Override
	public int forceRentalStatus(int rentalNum, boolean complete) throws AdminException {
		// 진행 중 상태: 신청(100, 취소만 가능) / 승인~반납 확인 대기(101~210)
		String allowed = complete ? "101, 110, 200, 201, 210" : "100, 101, 110, 200, 201, 210";
		try (Connection con = DBManager.getConnection()) {
			con.setAutoCommit(false);
			try {
				int postNum;
				try (PreparedStatement ps = con.prepareStatement("SELECT PostNum FROM Rental WHERE RentalNum = ?")) {
					ps.setInt(1, rentalNum);
					try (ResultSet rs = ps.executeQuery()) {
						if (!rs.next()) { con.rollback(); return 0; }
						postNum = rs.getInt(1);
					}
				}
				// 같은 게시글에 대한 회원의 신청·승인과 동시에 처리되지 않도록 게시글 행을 잠근다
				try (PreparedStatement ps = con.prepareStatement("SELECT PostNum FROM Post WHERE PostNum = ? FOR UPDATE")) {
					ps.setInt(1, postNum);
					ps.executeQuery().close();
				}
				int changed;
				try (PreparedStatement ps = con.prepareStatement("UPDATE Rental SET Status = ? WHERE RentalNum = ? AND Status IN (" + allowed + ")")) {
					ps.setInt(1, complete ? 211 : 102);
					ps.setInt(2, rentalNum);
					changed = ps.executeUpdate();
				}
				if (changed == 0) { con.rollback(); return 0; }
				// 이 게시글에 진행 중인 다른 대여가 없으면 물품을 대여 가능으로 되돌린다
				try (PreparedStatement ps = con.prepareStatement("""
						UPDATE Item i JOIN Post p ON p.ItemNum = i.ItemNum SET i.Status = TRUE
						WHERE p.PostNum = ? AND NOT EXISTS (SELECT 1 FROM Rental r WHERE r.PostNum = p.PostNum
						                                    AND r.Status IN (101, 110, 200, 201, 210))
						""")) {
					ps.setInt(1, postNum);
					ps.executeUpdate();
				}
				con.commit();
				return changed;
			} catch (SQLException e) {
				con.rollback();
				throw e;
			}
		} catch (SQLException e) {
			throw new AdminException();
		}
	}

	@Override
	public List<AdminRow> selectAdmins() throws AdminException {
		List<AdminRow> list = new ArrayList<>();
		try (Connection con = DBManager.getConnection();
				PreparedStatement ps = con.prepareStatement("SELECT ID, Name FROM Admin ORDER BY ID");
				ResultSet rs = ps.executeQuery()) {
			while (rs.next()) list.add(new AdminRow(rs.getString("ID"), rs.getString("Name")));
		} catch (SQLException e) {
			throw new AdminException();
		}
		return list;
	}

	@Override
	public int insertAdmin(String id, String name, String password) throws AdminException {
		return update("이미 있는 관리자 아이디입니다.", null, "INSERT INTO Admin (ID, PassWord, Name) VALUES (?, ?, ?)",
				id, PasswordHasher.hash(password), name);
	}

	@Override
	public int updateAdminPassword(String id, String newPassword) throws AdminException {
		return update(null, null, "UPDATE Admin SET PassWord = ? WHERE ID = ?", PasswordHasher.hash(newPassword), id);
	}

	@Override
	public int deleteAdmin(String id) throws AdminException {
		// 마지막 관리자는 지울 수 없도록 한 문장 안에서 확인
		return update(null, null, "DELETE FROM Admin WHERE ID = ? AND (SELECT c FROM (SELECT COUNT(*) AS c FROM Admin) t) > 1", id);
	}

	@Override
	public List<NoticeRow> selectNotices(boolean activeOnly) throws AdminException {
		String sql = "SELECT NoticeNum, Title, Content, Active, AdminID, CreateAt, UpdateAt FROM Notice"
				+ (activeOnly ? " WHERE Active = TRUE" : "") + " ORDER BY NoticeNum DESC";
		List<NoticeRow> list = new ArrayList<>();
		try (Connection con = DBManager.getConnection(); PreparedStatement ps = con.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
			while (rs.next()) {
				list.add(new NoticeRow(rs.getInt("NoticeNum"), rs.getString("Title"), rs.getString("Content"),
						rs.getBoolean("Active"), rs.getString("AdminID"), rs.getString("CreateAt"), rs.getString("UpdateAt")));
			}
		} catch (SQLException e) {
			throw new AdminException();
		}
		return list;
	}

	@Override
	public int insertNotice(String title, String content, String adminId) throws AdminException {
		return update(null, null, "INSERT INTO Notice (Title, Content, AdminID) VALUES (?, ?, ?)", title, content, adminId);
	}

	@Override
	public int updateNotice(int noticeNum, String title, String content) throws AdminException {
		return update(null, null, "UPDATE Notice SET Title = ?, Content = ? WHERE NoticeNum = ?", title, content, noticeNum);
	}

	@Override
	public int setNoticeActive(int noticeNum, boolean active) throws AdminException {
		return update(null, null, "UPDATE Notice SET Active = ? WHERE NoticeNum = ?", active, noticeNum);
	}

	@Override
	public int deleteNotice(int noticeNum) throws AdminException {
		return update(null, null, "DELETE FROM Notice WHERE NoticeNum = ?", noticeNum);
	}

	@Override
	public void insertLog(String adminId, String action, String detail, int result, String ip) throws AdminException {
		update(null, null, "INSERT INTO AdminLog (AdminID, Action, Detail, Result, Ip) VALUES (?, ?, ?, ?, ?)",
				adminId, action, detail, result, ip);
	}

	@Override
	public List<LogRow> selectLogs(int limit) throws AdminException {
		List<LogRow> list = new ArrayList<>();
		try (Connection con = DBManager.getConnection();
				PreparedStatement ps = con.prepareStatement("SELECT LogNum, AdminID, Action, Detail, Result, Ip, CreateAt FROM AdminLog ORDER BY LogNum DESC LIMIT ?")) {
			ps.setInt(1, limit);
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					list.add(new LogRow(rs.getInt("LogNum"), rs.getString("AdminID"), rs.getString("Action"), rs.getString("Detail"),
							rs.getInt("Result"), rs.getString("Ip"), rs.getString("CreateAt")));
				}
			}
		} catch (SQLException e) {
			throw new AdminException();
		}
		return list;
	}

	private static List<StatRow> stats(String sql, int limit) throws AdminException {
		List<StatRow> list = new ArrayList<>();
		try (Connection con = DBManager.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
			ps.setInt(1, limit);
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) list.add(new StatRow(rs.getString(1), rs.getInt(2)));
			}
		} catch (SQLException e) {
			throw new AdminException();
		}
		return list;
	}

	@Override
	public List<StatRow> topCategories(int limit) throws AdminException {
		// 대여 신청이 많이 들어온 소분류
		return stats("""
				SELECT CONCAT(b.Category, ' · ', s.Category), COUNT(r.RentalNum) AS cnt
				FROM SmallCategory s JOIN BigCategory b ON b.BigCategoryCode = s.BigCategoryCode
				JOIN Item i ON i.SmallCategoryCode = s.SmallCategoryCode
				JOIN Post p ON p.ItemNum = i.ItemNum JOIN Rental r ON r.PostNum = p.PostNum
				GROUP BY s.SmallCategoryCode, b.Category, s.Category ORDER BY cnt DESC LIMIT ?
				""", limit);
	}

	@Override
	public List<StatRow> topLenders(int limit) throws AdminException {
		// 대여를 끝까지 완료(211)시킨 횟수가 많은 빌려주는 회원
		return stats("""
				SELECT i.LenderID, COUNT(*) AS cnt FROM Rental r JOIN Post p ON p.PostNum = r.PostNum
				JOIN Item i ON i.ItemNum = p.ItemNum WHERE r.Status = 211
				GROUP BY i.LenderID ORDER BY cnt DESC LIMIT ?
				""", limit);
	}

	@Override
	public List<StatRow> topBorrowers(int limit) throws AdminException {
		// 대여 신청을 많이 한 회원
		return stats("SELECT BorrowerID, COUNT(*) AS cnt FROM Rental GROUP BY BorrowerID ORDER BY cnt DESC LIMIT ?", limit);
	}
}
