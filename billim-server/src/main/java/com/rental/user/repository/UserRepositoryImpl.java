package main.java.com.rental.user.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import main.java.com.rental.user.dto.FindIdRequest;
import main.java.com.rental.user.dto.PasswordChangeRequest;
import main.java.com.rental.user.dto.UserLoginRequest;
import main.java.com.rental.user.dto.UserSignUpRequest;
import main.java.com.rental.user.entity.User;
import main.java.com.rental.common.exception.NotFoundException;
import main.java.com.rental.common.exception.PasswordUpdateException;
import main.java.com.rental.common.exception.UserException;
import main.java.com.rental.common.util.DBManager;
import main.java.com.rental.common.util.PasswordHasher;

public class UserRepositoryImpl implements UserRepository {

	// 로그인
	@Override
	public User login(UserLoginRequest request) throws UserException {
		User user = null;
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;

		try {
			con = DBManager.getConnection();
			ps = con.prepareStatement("select * from User where ID=?");
			ps.setString(1, request.getId());

			rs = ps.executeQuery();

			if (rs.next() && PasswordHasher.matches(request.getPassword(), rs.getString("PassWord"))) {
				// 관리자가 이용 정지한 계정은 비밀번호가 맞아도 로그인할 수 없다 (웹·콘솔 공통)
				if (rs.getBoolean("Suspended")) {
					String reason = rs.getString("SuspendReason");
					throw new UserException("이용이 정지된 계정입니다." + (reason == null || reason.isBlank() ? "" : " 사유: " + reason));
				}
				user = new User(rs.getString("ID"), rs.getString("PassWord"), rs.getString("NickName"),
						rs.getString("Name"), rs.getString("Phone"));
				// 예전 평문 비밀번호는 로그인 성공 시 해시로 바꿔 저장
				if (PasswordHasher.needsUpgrade(user.getPassword())) {
					try (PreparedStatement upgrade = con.prepareStatement("update User set PassWord = ? where ID = ?")) {
						upgrade.setString(1, PasswordHasher.hash(request.getPassword()));
						upgrade.setString(2, user.getId());
						upgrade.executeUpdate();
					}
				}
			}
		} catch (SQLException e) {
			// e.printStackTrace();
			throw new UserException();
		} finally {
			DBManager.close(con, ps, rs);
		}
		return user;
	}

	// 회원 가입
	@Override
	public int signUp(UserSignUpRequest request) throws UserException {
		Connection con = null;
		PreparedStatement ps = null;
		int result = 0;
		String sql = "insert into User(ID, PassWord, NickName, Name, Phone) values (?, ?, ?, ?, ?) ";
		try {
			con = DBManager.getConnection();
			ps = con.prepareStatement(sql);
			ps.setString(1, request.getId());
			ps.setString(2, PasswordHasher.hash(request.getPassword()));
			ps.setString(3, request.getNickName());
			ps.setString(4, request.getName());
			ps.setString(5, request.getPhoneNo());

			result = ps.executeUpdate();
		} catch (SQLException e) {
			if ("23000".equals(e.getSQLState())) {
				// 아이디는 서비스에서 먼저 확인하므로, 여기서 나는 중복은 대부분 전화번호(UQ_User_Phone)
				String detail = String.valueOf(e.getMessage());
				throw new UserException(detail.contains("Phone") ? "이미 가입된 전화번호입니다." : "이미 사용 중인 아이디입니다.");
			}
			throw new UserException();
		} finally {
			DBManager.close(con, ps);
		}
		return result;
	}

	@Override
	public boolean existsById(String id) throws UserException {
		String sql = "SELECT 1 FROM User WHERE ID = ?";
		try (Connection con = DBManager.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
			ps.setString(1, id);
			try (ResultSet rs = ps.executeQuery()) {
				return rs.next();
			}
		} catch (SQLException e) {
			throw new UserException("아이디 중복 확인 중 오류가 발생했습니다.");
		}
	}

	// 아이디 찾기
	@Override
	public String findId(String phone) throws UserException {
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		String result = null;
		try {
			con = DBManager.getConnection();
			ps = con.prepareStatement("select ID from User where Phone = ?");
			ps.setString(1, phone);
			rs = ps.executeQuery();

			if (rs.next()) {
				result = rs.getString("ID");
			}
		} catch (SQLException e) {
			// e.printStackTrace();
			throw new UserException();
		} finally {
			DBManager.close(con, ps, rs);
		}
		return result;
	}

	@Override
	// 비밀번호 수정
	public int updatePassword(PasswordChangeRequest request) throws UserException {
		Connection con = null;
		PreparedStatement ps = null;
		int result = 0;
		try {
			con = DBManager.getConnection();
			ps = con.prepareStatement("update User set PassWord = ? where ID = ? and Phone = ?");
			ps.setString(1, PasswordHasher.hash(request.getNewPassword()));
			ps.setString(2, request.getId());
			ps.setString(3, request.getPhoneNo());

			result = ps.executeUpdate();
		} catch (SQLException e) {
			// e.printStackTrace();
			throw new UserException();
		} finally {
			DBManager.close(con, ps);
		}
		return result;
	}
}
