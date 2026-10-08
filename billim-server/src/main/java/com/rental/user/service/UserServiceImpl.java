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
		if (blank(id) || id.length() > 20)
			throw new UserException("아이디는 1~20자로 입력해주세요.");
		return !userRepository.existsById(id);
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
		if (request == null || blank(request.getId()) || request.getId().length() > 20
				|| !validPasswordLength(request.getPassword())
				|| blank(request.getNickName()) || request.getNickName().length() > 10
				|| blank(request.getName()) || request.getName().length() > 10
				|| blank(request.getPhoneNo()))
			throw new UserException("회원정보 형식을 확인해주세요. ID는 20자, PW는 4~20자, 이름/닉네임은 10자 이내입니다.");
	}

	// 비밀번호는 4~20자 (회원가입 화면 안내 "4자 이상 입력"과 같은 기준)
	private boolean validPasswordLength(String password) {
		return !blank(password) && password.length() >= 4 && password.length() <= 20;
	}

	private boolean blank(String value) {
		return value == null || value.isBlank();
	}

}
