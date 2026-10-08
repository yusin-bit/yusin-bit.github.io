package main.java.com.rental.category.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import main.java.com.rental.category.entity.Category;
import main.java.com.rental.common.exception.NotFoundException;
import main.java.com.rental.common.util.DBManager;

public class CategoryRepositoryImpl implements CategoryRepository {

	@Override
	public List<Category> getBigCategories() throws NotFoundException {
		String sql = "SELECT BigCategoryCode, Category FROM BigCategory ORDER BY BigCategoryCode";
		return queryCategory(sql, null);
	}

	@Override
	public List<Category> getSmallCategories(String bigCategoryCode) throws NotFoundException {
		String sql = "SELECT SmallCategoryCode, Category FROM SmallCategory "
				+ "WHERE BigCategoryCode = ? ORDER BY SmallCategoryCode";
		return queryCategory(sql, bigCategoryCode);
	}

	/**
	 * 카테고리 조회 결과를 Category 엔티티 목록으로 변환
	 */
	public List<Category> queryCategory(String sql, String bigCategoryCode) throws NotFoundException {
		List<Category> list = new ArrayList<>();
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;

		try {
			con = DBManager.getConnection();
			ps = con.prepareStatement(sql);
			if (bigCategoryCode != null) {
				ps.setString(1, bigCategoryCode);
			}
			rs = ps.executeQuery();

			while (rs.next()) {
				list.add(new Category(rs.getString(1), rs.getString(2)));
			}
		} catch (SQLException e) {
			// e.printStackTrace();
			throw new NotFoundException();
		} finally {
			DBManager.close(con, ps, rs);
		}

		return list;
	}
}
