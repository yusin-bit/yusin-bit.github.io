package main.java.com.rental.item.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import main.java.com.rental.common.exception.ItemException;
import main.java.com.rental.common.exception.NotFoundException;
import main.java.com.rental.common.util.DBManager;
import main.java.com.rental.item.dto.ItemCreateRequest;
import main.java.com.rental.item.entity.Item;

public class ItemRepositoryImpl implements ItemRepository {

	// 물품 전체 목록 조회
	@Override
	public List<Item> itemSelect() throws NotFoundException {
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		List<Item> list = new ArrayList<>();

		try {
			con = DBManager.getConnection();
			ps = con.prepareStatement("SELECT * FROM Item ORDER BY ItemNum");
			rs = ps.executeQuery();

			while (rs.next()) {
				list.add(mapRow(rs));
			}
		} catch (SQLException e) {
			// e.printStackTrace();
			throw new NotFoundException("물품 목록 조회 중 데이터베이스 오류가 발생했습니다.");
		} finally {
			DBManager.close(con, ps, rs);
		}
		return list;
	}

	// 물품 번호 기준 단건 조회
	@Override
	public Item itemSelectByitemNum(int itemNum) throws NotFoundException {
		String sql = "SELECT * FROM Item WHERE ItemNum = ?";
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;

		try {
			con = DBManager.getConnection();
			ps = con.prepareStatement(sql);
			ps.setInt(1, itemNum);
			rs = ps.executeQuery();

			if (rs.next()) {
				return mapRow(rs);
			}
		} catch (SQLException e) {
			// e.printStackTrace();
			throw new NotFoundException("물품 상세 조회 중 데이터베이스 오류가 발생했습니다.");
		} finally {
			DBManager.close(con, ps, rs);
		}
		return null;
	}

	// 물품명 키워드 검색
	@Override
	public List<Item> itemSearch(String keyword) throws NotFoundException {
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		List<Item> list = new ArrayList<>();

		try {
			con = DBManager.getConnection();
			ps = con.prepareStatement("SELECT * FROM Item WHERE ItemName LIKE ? ORDER BY ItemNum");
			String likeKeyword = "%" + keyword + "%";
			ps.setString(1, likeKeyword);
			rs = ps.executeQuery();

			while (rs.next()) {
				list.add(mapRow(rs));
			}
		} catch (SQLException e) {
			// e.printStackTrace();
			throw new NotFoundException("물품 검색 중 데이터베이스 오류가 발생했습니다.");
		} finally {
			DBManager.close(con, ps, rs);
		}
		return list;
	}

	// 물품 신규 등록
	@Override
	public int itemInsert(ItemCreateRequest item) throws ItemException {
		// 기본키(ItemNum) 자동 채번 환경에 맞추어 컬럼에서 제외하고 삽입
		String sql = "INSERT INTO Item (ItemName, Status, SmallCategoryCode, LenderID) VALUES (?, ?, ?, ?)";

		Connection con = null;
		PreparedStatement ps = null;
		int result = 0;
		try {
			con = DBManager.getConnection();
			ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
			ps.setString(1, item.getItemName());
			ps.setBoolean(2, item.isStatus());
			ps.setString(3, item.getSmallCategoryCode());
			ps.setString(4, item.getLenderID());

			result = ps.executeUpdate();
			if (result > 0) {
				try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
					if (generatedKeys.next()) return generatedKeys.getInt(1);
				}
			}
		} catch (SQLException e) {
			throw new ItemException();
		} finally {
			DBManager.close(con, ps);
		}
		return 0;
	}

	// 물품 정보 수정
	@Override
	public int itemUpdate(Item item) throws ItemException {
		Connection con = null;
		PreparedStatement ps = null;
		int result = 0;

		String sql = "UPDATE Item SET ItemName = ?, SmallCategoryCode = ? WHERE ItemNum = ? AND LenderID = ?";

		try {
			con = DBManager.getConnection();
			ps = con.prepareStatement(sql);

			ps.setString(1, item.getItemName());
			ps.setString(2, item.getSmallCategoryCode());
			ps.setInt(3, item.getItemNum());
			ps.setString(4, item.getLenderID());

			result = ps.executeUpdate();
		} catch (SQLException e) {
			// e.printStackTrace();
			throw new ItemException();
		} finally {
			DBManager.close(con, ps);
		}
		return result;
	}

	// 물품 삭제
	@Override
	public int itemDelete(int itemNum, String userId) throws ItemException {
		Connection con = null;
		PreparedStatement ps = null;
		try {
			con = DBManager.getConnection();
			ps = con.prepareStatement("DELETE FROM Item WHERE ItemNum = ? AND LenderID = ?");
			ps.setInt(1, itemNum);
			ps.setString(2, userId);
			
			return ps.executeUpdate();
		} catch (SQLException e) {
			if ("23000".equals(e.getSQLState()))
				throw new ItemException("게시글 또는 대여 내역이 있는 물품은 삭제할 수 없습니다.");
			throw new ItemException();
		} finally {
			DBManager.close(con, ps);
		}
	}

	// 대여 상태 변경
	@Override
	public int itemUpdateStatus(int itemNum, boolean status) throws ItemException {
		Connection con = null;
		PreparedStatement ps = null;
		try {
			con = DBManager.getConnection();
			ps = con.prepareStatement("UPDATE Item SET Status = ? WHERE ItemNum = ? AND LenderID = ?");
			ps.setBoolean(1, status);
			ps.setInt(2, itemNum);
			ps.setString(3, main.java.com.rental.session.Session.getInstance().getLoginUser().getId());
			return ps.executeUpdate();
		} catch (SQLException e) {
			// e.printStackTrace();
			throw new ItemException();
		} finally {
			DBManager.close(con, ps);
		}
	}
	
	//사용자 아이디 기준 아이템 리스트 조회
	@Override
	public List<Item> selectByUserId(String userId) throws ItemException {
		String sql = "SELECT * FROM Item WHERE LenderID = ?";
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		List<Item> list = new ArrayList<>();
		try {
			con = DBManager.getConnection();
			ps = con.prepareStatement(sql);
			ps.setString(1, userId);
			rs = ps.executeQuery();
			
			while(rs.next()) {
				list.add(mapRow(rs));
			}
			
		} catch (SQLException e) {
			// e.printStackTrace();
			throw new ItemException();
		} finally {
			DBManager.close(con, ps, rs);
		}
		return list;
	}

	// ResultSet 결과 행을 Item 엔티티 객체로 변환
	private Item mapRow(ResultSet rs) throws SQLException {
		Item item = new Item();
		item.setItemNum(rs.getInt("ItemNum"));
		item.setItemName(rs.getString("ItemName"));
		item.setStatus(rs.getBoolean("Status"));
		item.setSmallCategoryCode(rs.getString("SmallCategoryCode"));
		item.setLenderID(rs.getString("LenderID"));
		return item;
	}
}
