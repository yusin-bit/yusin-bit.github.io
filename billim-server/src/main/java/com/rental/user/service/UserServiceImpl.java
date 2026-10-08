package main.java.com.rental.user.service;

import java.sql.SQLException;

import main.java.com.rental.common.exception.PasswordUpdateException;
import main.java.com.rental.common.exception.UserException;
import main.java.com.rental.common.exception.NotFoundException;
import main.java.com.rental.session.Session;
import main.java.com.rental.user.dto.FindIdRequest;
import main.java.com.rental.user.dto.PasswordChangeRequest;
import main.java.com.rental.user.dto.UserLoginRequest;
import main.java.com.rental.user.dto.UserResponse;
import main.java.com.rental.user.dto.UserSignUpRequest;
import main.java.com.rental.user.entity.User;
import main.java.com.rental.user.repository.UserRepository;
import main.java.com.rental.user.repository.UserRepositoryImpl;

public class UserServiceImpl implements UserService {

	private Session session = Session.getInstance();
	UserRepository userRepository = new UserRepositoryImpl();

	@Override
	public UserResponse login(UserLoginRequest request) throws UserException {
		if (request == null || blank(request.getId()) || blank(request.getPassword()))
			throw new UserException("아이디와 비밀번호를 입력해주세요.");
		User user = userRepository.login(request);
		if (user == null) {
			throw new NotFoundException("회원님의 정보를 찾을 수 없습니다");
		}

		UserResponse response = new UserResponse(user.getId(), user.getNickName(), user.getName(), user.getPhoneNo());

		session.setLoginUser(response);

		return response;

	}

	@Override
	public int signUp(UserSignUpRequest request) throws UserException {
		validateSignUp(request);
		if (userRepository.existsById(request.getId()))
			throw new UserException("이미 사용 중인 아이디입니다.");
		int result = userRepository.signUp(request);
		if (result == 0) {
			throw new UserException("ID가 중복입니다. 다시 입력해주세요.");
		}
		return result;
	}

	@Override
	public boolean isIdAvailable(String id) throws UserException {
		if (!validId(id))
			throw new UserException("아이디는 영문·숫자·_ 4~20자로 입력해주세요.");
		return !userRepository.existsById(id);
	}

	// 아이디: 영문·숫자·_ 4~20자 (공백이나 특수문자로 다른 아이디처럼 보이게 만드는 것 방지)
	private static boolean validId(String id) {
		return id != null && id.matches("[A-Za-z0-9_]{4,20}");
	}

	/**
	 * 전화번호를 010-1234-5678 형식으로 맞춘다. 숫자만 입력해도 되고, 휴대폰 번호 형식이 아니면 null.
	 */
	public static String normalizePhone(String phone) {
		if (phone == null) return null;
		String digits = phone.replaceAll("[\\s-]", "");
		if (!digits.matches("01[0-9]\\d{7,8}")) return null;
		int middle = digits.length() - 4; // 010-1234-5678 / 011-123-4567: 마지막 4자리 앞까지가 가운데
		return digits.substring(0, 3) + "-" + digits.substring(3, middle) + "-" + digits.substring(middle);
	}

	@Override
	public String findId(String phone) throws UserException {
		String result = userRepository.findId(phone);

		if (result == null) {
			throw new NotFoundException("회원님의 ID를 찾을 수 없습니다");
		}

		return result;

	}

	@Override
	public int updatePassword(PasswordChangeRequest request) throws UserException {
		if (request == null || blank(request.getId()) || blank(request.getPhoneNo())
				|| !validPasswordLength(request.getNewPassword()))
			throw new PasswordUpdateException("아이디, 전화번호와 새 비밀번호(4~20자)를 확인해주세요.");
		int result = userRepository.updatePassword(request);

		if (result == 0) {
			throw new PasswordUpdateException("정보를 다시 입력해 주세요");

		}
		return result;
	}

	@Override
	public void logout() {
		session.logout();
	}

	private void validateSignUp(UserSignUpRequest request) {
		if (request == null || !validId(request.getId())
				|| !validPasswordLength(request.getPassword())
				|| blank(request.getNickName()) || request.getNickName().length() > 10
				|| blank(request.getName()) || request.getName().length() > 10)
			throw new UserException("회원정보 형식을 확인해주세요. ID는 영문·숫자·_ 4~20자, PW는 4~20자, 이름/닉네임은 10자 이내입니다.");
		if (normalizePhone(request.getPhoneNo()) == null || !request.getPhoneNo().equals(normalizePhone(request.getPhoneNo())))
			throw new UserException("전화번호는 010-1234-5678 형식으로 입력해주세요.");
	}

	// 비밀번호는 4~20자 (회원가입 화면 안내 "4자 이상 입력"과 같은 기준)
	private boolean validPasswordLength(String password) {
		return !blank(password) && password.length() >= 4 && password.length() <= 20;
	}

	private boolean blank(String value) {
		return value == null || value.isBlank();
	}

}
