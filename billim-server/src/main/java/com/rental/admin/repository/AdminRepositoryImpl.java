package main.java.com.rental.admin.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import main.java.com.rental.admin.dto.AdminDtos.AdminAccount;
import main.java.com.rental.admin.dto.AdminDtos.CategoryRow;
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
				SELECT u.ID, u.NickName, u.Name, u.Phone,
				       (SELECT COUNT(*) FROM Item i WHERE i.LenderID = u.ID) AS itemCount,
				       (SELECT COUNT(*) FROM Rental r WHERE r.BorrowerID = u.ID) AS rentalCount
				FROM User u
				ORDER BY u.ID
				""";
		List<UserRow> list = new ArrayList<>();
		try (Connection con = DBManager.getConnection();
				PreparedStatement ps = con.prepareStatement(sql);
				ResultSet rs = ps.executeQuery()) {
			while (rs.next()) {
				list.add(new UserRow(rs.getString("ID"), rs.getString("NickName"), rs.getString("Name"),
						rs.getString("Phone"), rs.getInt("itemCount"), rs.getInt("rentalCount")));
			}
		} catch (SQLException e) {
			throw new AdminException();
		}
		return list;
	}

	@Override
	public int resetUserPassword(String userId, String newPassword) throws AdminException {
		try (Connection con = DBManager.getConnection();
				PreparedStatement ps = con.prepareStatement("UPDATE User SET PassWord = ? WHERE ID = ?")) {
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
				SELECT p.PostNum, p.Title, i.ItemName, i.LenderID, p.RentDate, p.ReturnDate, p.Addr, i.Status,
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
				list.add(new PostRow(rs.getInt("PostNum"), rs.getString("Title"), rs.getString("ItemName"),
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
				SELECT i.ItemNum, i.ItemName, i.LenderID, i.Status, b.Category AS bigName, s.Category AS smallName,
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
				list.add(new ItemRow(rs.getInt("ItemNum"), rs.getString("ItemName"), rs.getString("LenderID"),
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
}
