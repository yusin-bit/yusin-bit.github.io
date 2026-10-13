package main.java.com.rental.item.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import main.java.com.rental.common.exception.ItemException;
import main.java.com.rental.common.util.DBManager;

public class ItemImageRepositoryImpl implements ItemImageRepository {

	@Override
	public ImageData find(int itemNum) throws ItemException {
		try (Connection con = DBManager.getConnection();
				PreparedStatement ps = con.prepareStatement("SELECT Data, ContentType FROM ItemImage WHERE ItemNum = ?")) {
			ps.setInt(1, itemNum);
			try (ResultSet rs = ps.executeQuery()) {
				return rs.next() ? new ImageData(rs.getBytes("Data"), rs.getString("ContentType")) : null;
			}
		} catch (SQLException e) {
			throw new ItemException("사진을 불러오지 못했습니다.");
		}
	}

	@Override
	public Map<Integer, ImageInfo> infoByItem() throws ItemException {
		return infoMap("SELECT ItemNum AS KeyNum, ItemNum, UNIX_TIMESTAMP(UpdateAt) AS Version, Credit FROM ItemImage");
	}

	@Override
	public Map<Integer, ImageInfo> infoByPost() throws ItemException {
		return infoMap("""
				SELECT p.PostNum AS KeyNum, ii.ItemNum, UNIX_TIMESTAMP(ii.UpdateAt) AS Version, ii.Credit
				FROM Post p JOIN ItemImage ii ON ii.ItemNum = p.ItemNum
				""");
	}

	private Map<Integer, ImageInfo> infoMap(String sql) throws ItemException {
		Map<Integer, ImageInfo> map = new HashMap<>();
		try (Connection con = DBManager.getConnection();
				PreparedStatement ps = con.prepareStatement(sql);
				ResultSet rs = ps.executeQuery()) {
			while (rs.next()) {
				map.put(rs.getInt("KeyNum"), new ImageInfo(rs.getInt("ItemNum"), rs.getLong("Version"), rs.getString("Credit")));
			}
		} catch (SQLException e) {
			throw new ItemException("사진 정보를 불러오지 못했습니다.");
		}
		return map;
	}

	@Override
	public String ownerOf(int itemNum) throws ItemException {
		try (Connection con = DBManager.getConnection();
				PreparedStatement ps = con.prepareStatement("SELECT LenderID FROM Item WHERE ItemNum = ?")) {
			ps.setInt(1, itemNum);
			try (ResultSet rs = ps.executeQuery()) {
				return rs.next() ? rs.getString(1) : null;
			}
		} catch (SQLException e) {
			throw new ItemException();
		}
	}

	@Override
	public void save(int itemNum, byte[] data, String contentType, String credit) throws ItemException {
		String sql = """
				INSERT INTO ItemImage (ItemNum, ContentType, Data, Credit) VALUES (?, ?, ?, ?)
				ON DUPLICATE KEY UPDATE ContentType = ?, Data = ?, Credit = ?, UpdateAt = CURRENT_TIMESTAMP
				""";
		try (Connection con = DBManager.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
			ps.setInt(1, itemNum);
			ps.setString(2, contentType);
			ps.setBytes(3, data);
			ps.setString(4, credit);
			ps.setString(5, contentType);
			ps.setBytes(6, data);
			ps.setString(7, credit);
			ps.executeUpdate();
		} catch (SQLException e) {
			if ("23000".equals(e.getSQLState())) throw new ItemException("존재하지 않는 물품입니다.");
			throw new ItemException("사진을 저장하지 못했습니다.");
		}
	}

	@Override
	public int delete(int itemNum) throws ItemException {
		try (Connection con = DBManager.getConnection();
				PreparedStatement ps = con.prepareStatement("DELETE FROM ItemImage WHERE ItemNum = ?")) {
			ps.setInt(1, itemNum);
			return ps.executeUpdate();
		} catch (SQLException e) {
			throw new ItemException("사진을 삭제하지 못했습니다.");
		}
	}
}
