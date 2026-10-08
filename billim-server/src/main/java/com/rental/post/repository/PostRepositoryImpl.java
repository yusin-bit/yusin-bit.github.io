package main.java.com.rental.post.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import main.java.com.rental.common.exception.PostException;
import main.java.com.rental.common.util.DBManager;
import main.java.com.rental.post.dto.AvailablePost;
import main.java.com.rental.post.dto.PostCreate;
import main.java.com.rental.post.dto.PostUpdate;
import main.java.com.rental.post.entity.Post;
import main.java.com.rental.session.Session;

public class PostRepositoryImpl implements PostRepository {

	@Override
	public int postCreate(PostCreate postCreate) throws PostException {
		Connection con = null;
		PreparedStatement ps = null;
		// 로그인한 사용자의 물품일 때만 게시글을 만든다 (다른 사람 물품에 글을 다는 것 방지)
		String sql = "INSERT INTO Post (ItemNum, Title, Content, RentDate, ReturnDate, Addr) "
				+ "SELECT i.ItemNum, ?, ?, ?, ?, ? FROM Item i WHERE i.ItemNum = ? AND i.LenderID = ?";
		int result = 0;
		try {
			con = DBManager.getConnection();
			ps = con.prepareStatement(sql);
			ps.setString(1, postCreate.getTitle());
			ps.setString(2, postCreate.getContent());
			ps.setString(3, postCreate.getRentDate());
			ps.setString(4, postCreate.getReturnDate());
			ps.setString(5, postCreate.getAddr());
			ps.setInt(6, postCreate.getItemNum());
			ps.setString(7, Session.getInstance().getLoginUser().getId());

			result = ps.executeUpdate();

		} catch (SQLException e) {
			// e.printStackTrace();
			if ("23000".equals(e.getSQLState()))
				throw new PostException("이미 대여 글이 등록된 물품입니다.");
			throw new PostException();
		} finally {
			DBManager.close(con, ps);
		}
		return result;
	}

	@Override
	public int postUpdate(PostUpdate postUpdate)throws PostException {
		Connection con = null;
		PreparedStatement ps = null;
		String sql = "UPDATE Post SET Title = ?," 
		+ "Content = ?, RentDate = ?,ReturnDate = ?,"
		+ "Addr = ? WHERE Postnum = ? "
		+ "AND ItemNum IN (SELECT ItemNum FROM Item WHERE LenderID = ?)";
		int result = 0;
		try {
			con = DBManager.getConnection();
			ps = con.prepareStatement(sql);

			ps.setString(1, postUpdate.getTitle());
			ps.setString(2, postUpdate.getContent());
			ps.setString(3, postUpdate.getRentDate());
			ps.setString(4, postUpdate.getReturnDate());
			ps.setString(5, postUpdate.getAddr());
			ps.setInt(6, postUpdate.getPostNum());
			ps.setString(7, Session.getInstance().getLoginUser().getId());

			result = ps.executeUpdate();

		} catch (SQLException e) {
			// e.printStackTrace();
			throw new PostException();
			
		} finally {
			DBManager.close(con, ps);
		}

		return result;
	}

	@Override
	public int postDelete(int postNum) throws PostException{
		String sql = "DELETE FROM Post WHERE Postnum = ? "
				+ "AND ItemNum IN (SELECT ItemNum FROM Item WHERE LenderID = ?)";
		Connection con = null;
		PreparedStatement ps = null;
		int result = 0;
		try {
			con = DBManager.getConnection();
			ps = con.prepareStatement(sql);
			ps.setInt(1, postNum);
			ps.setString(2, Session.getInstance().getLoginUser().getId());
			result = ps.executeUpdate();
		} catch (SQLException e) {
			if ("23000".equals(e.getSQLState()))
				throw new PostException("대여 내역이 있는 게시글은 삭제할 수 없습니다.");
			throw new PostException();
		}finally {
			DBManager.close(con, ps);
		}
		return result;
	}
	
	@Override
	public List<Post> selectAll() throws PostException {
		String sql = """
	            SELECT * FROM Post order by postNum
	            """;
	    List<Post> posts = new ArrayList<>();
	    try (Connection conn = DBManager.getConnection();
	         PreparedStatement pstmt = conn.prepareStatement(sql)) {
	        try (ResultSet rs = pstmt.executeQuery()) {
	            while (rs.next()) {
	                posts.add(mapPost(rs));
	            }
	        }
	    } catch (SQLException e) {
	        // e.printStackTrace();
	        throw new PostException();
	    }
	    return posts;
	}

	@Override
	public List<Post> selectById() throws PostException {
		String sql = """
	            SELECT * FROM Post p join Item i on p.ItemNum = i.ItemNum WHERE LenderID = ?
	            """;
	    List<Post> posts = new ArrayList<>();
	    try (Connection conn = DBManager.getConnection();
	         PreparedStatement pstmt = conn.prepareStatement(sql)) {
	        pstmt.setString(1, Session.getInstance().getLoginUser().getId());
	        try (ResultSet rs = pstmt.executeQuery()) {
	            while (rs.next()) {
	                posts.add(mapPost(rs));
	            }
	        }
	    } catch (SQLException e) {
	        // e.printStackTrace();
	        throw new PostException();
	    }
	    return posts;
	}

	@Override
	public Post selectByItemNum(int itemNum)throws PostException {

	    String sql = "SELECT * FROM Post WHERE ItemNum = ?";
	    Connection con =null;
	    PreparedStatement ps = null;
	    ResultSet rs =null;
	    try {
	    	con = DBManager.getConnection();
	    	ps = con.prepareStatement(sql);
	    	ps.setInt(1, itemNum);
	    	rs= ps.executeQuery();
	    	if (rs.next()) return mapPost(rs);

	    } catch (SQLException e) {
	        // e.printStackTrace();
	        throw new PostException();
	    } finally {
			DBManager.close(con, ps, rs);
		}
	    return null;
	}
	
	@Override
	public List<Post> selectByTitleKeyword(String titleKeyword) throws PostException{
	    String sql = """
	            SELECT *
	            FROM Post
	            WHERE Title LIKE ?
	            ORDER BY Postnum DESC
	            """;
	    List<Post> posts = new ArrayList<>();
	    try (Connection conn = DBManager.getConnection();
	         PreparedStatement pstmt = conn.prepareStatement(sql)) {
	        pstmt.setString(1, "%" + titleKeyword + "%");
	        try (ResultSet rs = pstmt.executeQuery()) {
	            while (rs.next()) {
	                posts.add(mapPost(rs));
	            }
	        }
	    } catch (SQLException e) {
	        // e.printStackTrace();
	        throw new PostException();
	    }
	    return posts;
	}
	
	@Override
	public List<Post> selectByContentKeyword(String contentKeyword) throws PostException{
	    String sql = """
	            SELECT *
	            FROM Post
	            WHERE Content LIKE ?
	            ORDER BY Postnum DESC
	            """;

	    List<Post> posts = new ArrayList<>();
	    try (Connection conn = DBManager.getConnection();
	         PreparedStatement pstmt = conn.prepareStatement(sql)) {
	        pstmt.setString(1, "%" + contentKeyword + "%");
	        try (ResultSet rs = pstmt.executeQuery()) {
	            while (rs.next()) {
	                posts.add(mapPost(rs));
	            }
	        }
	    } catch (SQLException e) {
	        // e.printStackTrace();
	        throw new PostException();
	    }
	    return posts;
	}
	@Override
	public List<Post> selectByRentDate(String rentDate) throws PostException{
	    String sql = """
	            SELECT *
	            FROM Post
	            WHERE DATE(RentDate) = ?
	            ORDER BY Postnum DESC
	            """;

	    List<Post> posts = new ArrayList<>();
	    try (Connection conn = DBManager.getConnection();
	         PreparedStatement pstmt = conn.prepareStatement(sql)) {
	        pstmt.setString(1, rentDate);
	        try (ResultSet rs = pstmt.executeQuery()) {
	            while (rs.next()) {
	                posts.add(mapPost(rs));
	            }
	        }
	    } catch (SQLException e) {
	        // e.printStackTrace();
	        throw new PostException();
	    }
	    return posts;
	}
	
	@Override
	public List<Post> selectByAddr(String addr) throws PostException{

	    String sql = """
	            SELECT *
	            FROM Post
	            WHERE Addr LIKE ?
	            ORDER BY Postnum DESC
	    		""";
	    List<Post> posts = new ArrayList<>();
	    try (Connection conn = DBManager.getConnection();
	         PreparedStatement pstmt = conn.prepareStatement(sql)) {
	        pstmt.setString(1, "%" + addr + "%");
	        try (ResultSet rs = pstmt.executeQuery()) {
	            while (rs.next()) {
	                posts.add(mapPost(rs));
	            }
	        }
	    } catch (SQLException e) {
	        // e.printStackTrace();
	        throw new PostException();
	    }
	    return posts;
	}
	
	@Override
	public List<AvailablePost> selectAvailablePost() throws PostException {

	    List<AvailablePost> list = new ArrayList<>();

	    String sql = """
	            SELECT PostNum,
	                   Title,
	                   Content,
	                   RentDate,
	                   ReturnDate,
	                   Addr,
	                   ItemName,
	                   Category
	            FROM View_Available_Post
	            """;

	    try (
	        Connection conn = DBManager.getConnection();
	        PreparedStatement ps = conn.prepareStatement(sql);
	        ResultSet rs = ps.executeQuery()
	    ) {

	        while (rs.next()) {

	            AvailablePost post = new AvailablePost();

	            post.setPostNum(rs.getInt("PostNum"));
	            post.setTitle(rs.getString("Title"));
	            post.setContent(rs.getString("Content"));
	            post.setRentDate(rs.getString("RentDate"));
	            post.setReturnDate(rs.getString("ReturnDate"));
	            post.setAddr(rs.getString("Addr"));
	            post.setItemName(rs.getString("ItemName"));
	            post.setCategory(rs.getString("Category"));

	            list.add(post);
	        }

	    } catch (SQLException e) {
	        // e.printStackTrace();
	        throw new PostException();
	    }

	    return list;
	}
	
	private Post mapPost(ResultSet rs) throws SQLException {

	    Post post = new Post();

	    post.setPostNum(rs.getInt("Postnum"));
	    post.setItemNum(rs.getInt("ItemNum"));
	    post.setTitle(rs.getString("Title"));
	    post.setContent(rs.getString("Content"));
	    post.setCreateAt(rs.getString("CreateAt"));
	    post.setUpdateAt(rs.getString("UpdateAt"));
	    post.setRentDate(rs.getString("RentDate"));
	    post.setReturnDate(rs.getString("ReturnDate"));
	    post.setAddr(rs.getString("Addr"));

	    return post;
	}

}
