package main.java.com.rental.rental.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import main.java.com.rental.common.exception.RentalException;
import main.java.com.rental.common.util.DBManager;
import main.java.com.rental.rental.entity.Rental;
import main.java.com.rental.rental.enums.RentalStatus;
import main.java.com.rental.session.Session;
import main.java.com.rental.rental.dto.RentalCreateRequest;
import main.java.com.rental.rental.dto.RentalDetail;

public class RentalRepositoryImpl implements RentalRepository {
	/**
	 * Rental 생성
	 * 
	 * @throws RentalException
	 */
	@Override
	public int rentalCreate(RentalCreateRequest rentalCreateRequest) throws RentalException {

		String sql = """
				INSERT INTO Rental (BorrowerId, PostNum)
				SELECT ?, p.PostNum
				FROM Post p
				JOIN Item i ON p.ItemNum = i.ItemNum
				WHERE p.PostNum = ?
				  AND i.Status = TRUE
				  AND i.LenderID <> ?
				  AND NOT EXISTS (
				      SELECT 1 FROM Rental r
				      WHERE r.BorrowerId = ? AND r.PostNum = p.PostNum
				        AND r.Status IN (100, 101, 110, 200, 201, 210)
				  )
				""";
		try (Connection conn = DBManager.getConnection()) {
			// 같은 게시글에 대한 신청을 한 번에 하나씩 처리 (동시에 같은 신청이 두 번 들어와도 1건만 생기도록)
			conn.setAutoCommit(false);
			try {
				lockPost(conn, rentalCreateRequest.getPostNum());
				int result;
				try (PreparedStatement ps = conn.prepareStatement(sql)) {
					ps.setString(1, rentalCreateRequest.getBorrowerId());
					ps.setInt(2, rentalCreateRequest.getPostNum());
					ps.setString(3, rentalCreateRequest.getBorrowerId());
					ps.setString(4, rentalCreateRequest.getBorrowerId());
					result = ps.executeUpdate();
				}
				if (result == 0) {
					String reason = rentalCreateFailReason(conn, rentalCreateRequest);
					conn.rollback();
					throw new RentalException(reason);
				}
				conn.commit();
				return result;
			} catch (SQLException e) {
				conn.rollback();
				throw e;
			}
		} catch (SQLException e) {
			// e.printStackTrace();
			throw new RentalException();
		}
	}

	/** 게시글 행을 잠가서 같은 게시글에 대한 신청·승인이 동시에 처리되지 않게 한다 (트랜잭션 안에서 호출) */
	@Override
	public void lockPost(Connection con, int postNum) throws SQLException {
		try (PreparedStatement ps = con.prepareStatement("SELECT PostNum FROM Post WHERE PostNum = ? FOR UPDATE")) {
			ps.setInt(1, postNum);
			ps.executeQuery().close();
		}
	}

	/** 대여 신청이 만들어지지 않은 이유를 사용자에게 알려줄 문구로 바꾼다 */
	private String rentalCreateFailReason(Connection con, RentalCreateRequest request) throws SQLException {
		String sql = """
				SELECT i.LenderID, i.Status,
				       EXISTS (SELECT 1 FROM Rental r WHERE r.BorrowerId = ? AND r.PostNum = p.PostNum
				               AND r.Status IN (100, 101, 110, 200, 201, 210)) AS alreadyRequested
				FROM Post p JOIN Item i ON i.ItemNum = p.ItemNum
				WHERE p.PostNum = ?
				""";
		try (PreparedStatement ps = con.prepareStatement(sql)) {
			ps.setString(1, request.getBorrowerId());
			ps.setInt(2, request.getPostNum());
			try (ResultSet rs = ps.executeQuery()) {
				if (!rs.next()) return "존재하지 않는 게시글입니다.";
				if (request.getBorrowerId().equals(rs.getString("LenderID"))) return "본인 물품은 대여 신청할 수 없습니다.";
				if (rs.getBoolean("alreadyRequested")) return "이미 신청했거나 진행 중인 대여가 있습니다.";
				if (!rs.getBoolean("Status")) return "지금은 대여 중인 물품입니다.";
				return "대여 신청을 처리하지 못했습니다.";
			}
		}
	}

	/**
	 * rentalRequest status ==100, LenderId가 session~getId()인
	 * 
	 * @throws RentalException
	 */
	public List<Rental> selectRentalRequestListByLender() throws RentalException {
		List<Rental> list = new ArrayList<>();
		String sql = """
				SELECT RentalNum,
				       BorrowerId,
				       Status,
				       PostNum
				FROM View_Rental_LenderID
				WHERE LenderId = ? AND Status =100
				""";

		try (Connection conn = DBManager.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setString(1, Session.getInstance().getLoginUser().getId());
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					list.add(mapRental(rs));
				}
			}
		} catch (SQLException e) {
			// e.printStackTrace();
			throw new RentalException();
		}

		return list;
	}

	@Override
	public Rental selectByRentalNum(int rentalNum) throws RentalException {
		Rental re = null;
		String sql = """
				SELECT RentalNum,
				       BorrowerId,
				       Status,
				       PostNum
				FROM View_Rental_LenderID
				WHERE LenderId = ? AND RentalNum = ?
				""";

		try (Connection conn = DBManager.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setString(1, Session.getInstance().getLoginUser().getId());
			ps.setInt(2, rentalNum);
			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next()) {
					re = mapRental(rs);
				}
			}
		} catch (SQLException e) {
			// e.printStackTrace();
			throw new RentalException();
		}

		return re;
	}

//사용
	@Override
	public List<Rental> selectByStatus(int status) throws RentalException {
		List<Rental> list = new ArrayList<>();
		String ownerColumn = ownerColumnForStatus(status);
		String sql = "SELECT RentalNum, BorrowerId, Status, PostNum "
				+ "FROM View_Rental_LenderID WHERE " + ownerColumn + " = ? AND Status = ?";

		try (Connection conn = DBManager.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setString(1, Session.getInstance().getLoginUser().getId());
			ps.setInt(2, status);
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					list.add(mapRental(rs));
				}
			}
		} catch (SQLException e) {
			// e.printStackTrace();
			throw new RentalException();
		}

		return list;
	}

	/**
	 * 대여 신청 현황
	 *
	 * status == 100
	 * 
	 * @throws RentalException
	 */
	@Override
	public List<Rental> selectRentalRequestList() throws RentalException {
		List<Rental> list = new ArrayList<>();
		String sql = """
				SELECT RentalNum,
				       BorrowerID,
				       Status,
				       PostNum
				FROM View_Rental_LenderID
				WHERE Status = 100
				  AND (BorrowerId = ?OR LenderId =?)
				""";
		try (Connection conn = DBManager.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
			String id = Session.getInstance().getLoginUser().getId();
			ps.setString(1, id);
			ps.setString(2, id);
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					list.add(mapRental(rs));
				}
			}
		} catch (SQLException e) {
			// e.printStackTrace();
			throw new RentalException();
		}

		return list;
	}

	/**
	 * 현재 대여 현황
	 *
	 * 실제 대여 중인 상태 = 110
	 */
	@Override
	public List<Rental> selectCurrentRentalList() throws RentalException {
		List<Rental> list = new ArrayList<>();
		String sql = """
				SELECT RentalNum,
				       BorrowerId,
				       Status,
				       PostNum
				FROM View_Rental_LenderID
				WHERE Status = 110 AND BorrowerId = ?
				""";

		try (Connection conn = DBManager.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setString(1, Session.getInstance().getLoginUser().getId());
			try (ResultSet rs = ps.executeQuery()) {

				while (rs.next()) {
					list.add(mapRental(rs));
				}
			}
		} catch (SQLException e) {
			// e.printStackTrace();
			throw new RentalException();
		}

		return list;
	}

	@Override
	public List<Rental> selectMyRentalHistory() throws RentalException {
		List<Rental> list = new ArrayList<>();
		String sql = "SELECT RentalNum, BorrowerId, Status, PostNum "
				+ "FROM View_Rental_LenderID "
				+ "WHERE BorrowerId = ? ORDER BY RentalNum DESC";

		try (Connection conn = DBManager.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setString(1, Session.getInstance().getLoginUser().getId());
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					list.add(mapRental(rs));
				}
			}
		} catch (SQLException e) {
			throw new RentalException("내 대여 내역 조회에 실패했습니다.");
		}
		return list;
	}

	@Override
	public List<RentalDetail> selectRelatedRentalHistory() throws RentalException {
		List<RentalDetail> list = new ArrayList<>();
		// 탈퇴한 상대방은 아이디 대신 "탈퇴회원"으로 보여준다 (본인은 로그인 중이라 탈퇴 상태일 수 없음 → 역할 판별에 영향 없음)
		String sql = """
				SELECT r.RentalNum, r.PostNum, r.Status,
				       CASE WHEN b.Withdrawn THEN '탈퇴회원' ELSE r.BorrowerId END AS BorrowerId,
				       CASE WHEN l.Withdrawn THEN '탈퇴회원' ELSE i.LenderID END AS LenderID,
				       i.ItemName, p.Title, p.RentDate, p.ReturnDate, p.Addr
				FROM Rental r
				JOIN Post p ON p.PostNum = r.PostNum
				JOIN Item i ON i.ItemNum = p.ItemNum
				JOIN User b ON b.ID = r.BorrowerId
				JOIN User l ON l.ID = i.LenderID
				WHERE r.BorrowerId = ? OR i.LenderID = ?
				ORDER BY r.RentalNum DESC
				""";
		String id = Session.getInstance().getLoginUser().getId();
		try (Connection conn = DBManager.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setString(1, id);
			ps.setString(2, id);
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					list.add(new RentalDetail(rs.getInt("RentalNum"), rs.getInt("PostNum"),
							rs.getString("BorrowerId"), rs.getString("LenderID"),
							RentalStatus.fromCode(rs.getInt("Status")), rs.getString("ItemName"),
							rs.getString("Title"), rs.getString("RentDate"),
							rs.getString("ReturnDate"), rs.getString("Addr")));
				}
			}
		} catch (SQLException e) {
			throw new RentalException("대여 상세 내역 조회에 실패했습니다.");
		}
		return list;
	}

	@Override
	public int[] selectGlobalStats() throws RentalException {
		String sql = """
				SELECT
				  (SELECT COUNT(*) FROM View_Available_Post) AS AvailableCount,
				  (SELECT COUNT(*) FROM Rental WHERE Status IN (100,101,110,200,201,210)) AS ActiveCount,
				  (SELECT COUNT(*) FROM Rental WHERE Status = 211) AS CompletedCount
				""";
		try (Connection conn = DBManager.getConnection(); PreparedStatement ps = conn.prepareStatement(sql);
				ResultSet rs = ps.executeQuery()) {
			if (rs.next()) {
				return new int[] { rs.getInt("AvailableCount"), rs.getInt("ActiveCount"),
						rs.getInt("CompletedCount") };
			}
		} catch (SQLException e) {
			throw new RentalException("서비스 현황 조회에 실패했습니다.");
		}
		return new int[] { 0, 0, 0 };
	}

	@Override
	public boolean approveRental(Connection con, int rentalNum) throws RentalException {
		String sql = "UPDATE Rental SET status = 101 WHERE rentalNum = ? AND status = 100";
		PreparedStatement ps = null;
		int result = 0;
		try {
			if (!isAuthorized(con, rentalNum, 100)) return false;
			ps = con.prepareStatement(sql);

			ps.setInt(1, rentalNum);

			result = ps.executeUpdate();
		} catch (SQLException e) {
			// e.printStackTrace();
			throw new RentalException();
		}
		return result > 0;
	}

	// 같은 게시글의 다른 대여 요청 거절
	@Override
	public boolean rejectOtherRentals(Connection con, int postNum, int rentalNum) throws RentalException {
		String sql = "UPDATE Rental " + "SET status = 102 " + "WHERE postNum = ? " + "AND rentalNum <> ? "
				+ "AND status = 100";
		PreparedStatement ps = null;
		try {
			ps = con.prepareStatement(sql);
			ps.setInt(1, postNum);
			ps.setInt(2, rentalNum);
			ps.executeUpdate();
			return true;

		} catch (SQLException e) {
			// e.printStackTrace();
			throw new RentalException();
		} finally {
			DBManager.close(null, ps);
		}
	}

	@Override
	public boolean updateItemStatusByPost(Connection con, int postNum, boolean available) throws RentalException {
		String sql = "UPDATE Item i JOIN Post p ON p.ItemNum = i.ItemNum "
				+ "SET i.Status = ? WHERE p.PostNum = ?";
		try (PreparedStatement ps = con.prepareStatement(sql)) {
			ps.setBoolean(1, available);
			ps.setInt(2, postNum);
			return ps.executeUpdate() > 0;
		} catch (SQLException e) {
			throw new RentalException();
		}
	}

	@Override
	public int updateStatusRentalNum(int setStatus, int rentalNum, int status) throws RentalException {
		Connection con = null;
		PreparedStatement ps = null;
		String sql = "UPDATE Rental SET status = ? WHERE rentalNum = ? AND status = ?";
		try {
			con = DBManager.getConnection();
			con.setAutoCommit(false);
			if (!isAuthorized(con, rentalNum, status)) {
				con.rollback();
				return 0;
			}
			ps = con.prepareStatement(sql);

			ps.setInt(1, setStatus);
			ps.setInt(2, rentalNum);
			ps.setInt(3, status);

			int result = ps.executeUpdate();
			if (result == 0) {
				con.rollback();
				return 0;
			}
			if (setStatus == RentalStatus.COMPLETED.getCode()) {
				int postNum = findPostNum(con, rentalNum);
				if (postNum == 0 || !updateItemStatusByPost(con, postNum, true)) {
					con.rollback();
					return 0;
				}
			}
			con.commit();
			return result;

		} catch (SQLException e) {
			try { if (con != null) con.rollback(); } catch (SQLException ignored) { }
			throw new RentalException();

		} finally {
			DBManager.close(con, ps);
		}
	}

	/**
	 * 로그인시 현재 승인 대기중인 목록 출력
	 * 
	 * @throws RentalException
	 */
	@Override
	public List<Rental> getPendingApprovals() throws RentalException {
		String sql = """
				SELECT *
				FROM View_Rental_LenderID
				WHERE status in (100,200) and LenderID =?
				order by LenderID, PostNum
				""";
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		String LenderID = Session.getInstance().getLoginUser().getId();
		List<Rental> list = new ArrayList<>();
		try {
			con = DBManager.getConnection();
			ps = con.prepareStatement(sql);
			ps.setString(1, LenderID);
			rs = ps.executeQuery();
			while (rs.next()) {
				list.add(mapRental(rs));
			}
		} catch (SQLException e) {
			// e.printStackTrace();
			throw new RentalException();
		} finally {
			DBManager.close(con, ps, rs);
		}
		return list;
	}

	private Rental mapRental(ResultSet rs) throws SQLException {

		Rental rental = new Rental(rs.getInt("RentalNum"), rs.getString("BorrowerId"),
				RentalStatus.fromCode(rs.getInt("Status")), rs.getInt("PostNum"));

		return rental;
	}

	private int findPostNum(Connection con, int rentalNum) throws SQLException {
		try (PreparedStatement ps = con.prepareStatement("SELECT PostNum FROM Rental WHERE RentalNum = ?")) {
			ps.setInt(1, rentalNum);
			try (ResultSet rs = ps.executeQuery()) {
				return rs.next() ? rs.getInt(1) : 0;
			}
		}
	}

	private String ownerColumnForStatus(int status) throws RentalException {
		return switch (status) {
		case 100, 101, 200, 210 -> "LenderID";
		case 110, 201 -> "BorrowerID";
		default -> throw new RentalException("지원하지 않는 대여 상태입니다.");
		};
	}

	private boolean isAuthorized(Connection con, int rentalNum, int status) throws SQLException, RentalException {
		String ownerColumn = ownerColumnForStatus(status);
		String sql = "SELECT 1 FROM View_Rental_LenderID WHERE RentalNum = ? AND Status = ? AND "
				+ ownerColumn + " = ?";
		try (PreparedStatement ps = con.prepareStatement(sql)) {
			ps.setInt(1, rentalNum);
			ps.setInt(2, status);
			ps.setString(3, Session.getInstance().getLoginUser().getId());
			try (ResultSet rs = ps.executeQuery()) {
				return rs.next();
			}
		}
	}
}
