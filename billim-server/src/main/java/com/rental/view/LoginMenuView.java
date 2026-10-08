package main.java.com.rental.view;

import java.util.Scanner;

import main.java.com.rental.common.util.ViewUtil;
import main.java.com.rental.common.util.ConsoleInput;
import main.java.com.rental.session.Session;
import main.java.com.rental.user.controller.UserController;
import main.java.com.rental.user.dto.PasswordChangeRequest;
import main.java.com.rental.user.dto.UserLoginRequest;
import main.java.com.rental.user.dto.UserSignUpRequest;

public class LoginMenuView {

	Scanner sc = ConsoleInput.scanner();
	UserController userController = new UserController();

	/**
	 * [시작 메뉴] 로그인, 회원가입, 아이디 찾기 처리
	 */
	public void loginMenuView() {
		while (true) {
			System.out.flush();
			System.out.println("\n========================================");
			System.out.println("   개인 간 물품 대여 서비스");
			System.out.println("========================================");
			System.out.println(" 1. 회원가입");
			System.out.println(" 2. 로그인");
			System.out.println(" 3. 아이디 찾기");
			System.out.println(" 4. 비밀번호 변경");
			System.out.println(" 0. 프로그램 종료");
			System.out.println("----------------------------------------");
			System.out.print("메뉴를 선택해주세요 >> ");

			switch (sc.nextLine().trim()) {
			case "1":
				userSignUp();
				break;
			case "2":
				login();
				// 세션에 로그인 사용자 정보가 저장되면 시작 메뉴 루프 종료
				if (Session.getInstance().getLoginUser() != null) {
					return;
				}
				break;
			case "3":
				findId();
				break;
			case "4":
				changePassWord();
				break;
			case "0":
				System.out.println("프로그램을 종료합니다.");
				System.exit(0);
			default:
				System.out.println("지원하지 않는 메뉴 번호입니다. 다시 입력해주세요.");
				break;
			}
		}
	}

	/**
	 * 로그인 계정 정보 입력
	 */
	private void login() {
		System.out.println("\n[로그인]");
		System.out.print("ID: ");
		String id = sc.nextLine().trim();

		System.out.print("PW: ");
		String password = sc.nextLine().trim();

		UserLoginRequest login = new UserLoginRequest(id, password);
		userController.login(login);
	}

	/**
	 * 회원 가입 정보 입력 id, pw, nickname, name, phoneNo 입력받음 controller.signUp(signUp)으로
	 * 데이터 전달
	 */
	private void userSignUp() {
		System.out.println("\n[회원가입]");
		System.out.print("ID: ");
		String id = sc.nextLine().trim();

		System.out.print("PW: ");
		String password = sc.nextLine().trim();

		System.out.print("닉네임: ");
		String nickName = sc.nextLine().trim();

		System.out.print("이름: ");
		String name = sc.nextLine().trim();

		String phone = null;
//      번호 입력이 정상적인지 확인하기 위함.
		boolean phoneStatus = true;
		while (phoneStatus) {
			System.out.print("핸드폰 번호: ");
			phone = sc.nextLine().trim();
			// "-" 제거
			phone = ViewUtil.formatPhone(phone);
			if (phone != null)
				phoneStatus = false;
		}

		UserSignUpRequest signup = new UserSignUpRequest(id, password, nickName, name, phone);
		userController.signUp(signup);
	}

	/**
	 * 아이디 찾기 요청 입력
	 */
	private void findId() {
		boolean status = true;
		System.out.println("\n[아이디 찾기]");
		String phone = null;
		while (status) {
			System.out.print("가입 시 등록한 핸드폰 번호: ");
			phone = sc.nextLine().trim(); // 양끝 공백 제거
			phone = ViewUtil.formatPhone(phone); // 3-4-4 format
			if (phone != null)
				status = false;
		}
		userController.findId(phone);
	}

	/**
	 * 비밀번호를 잊어버렸을 시 비밀번호 변경
	 */
	private void changePassWord() {
		boolean status = true;
		System.out.println("\n[비밀번호 변경]");
		System.out.print("ID: ");
		String id = sc.nextLine().trim();
		String phone = null;
		while (status) {
			System.out.print("가입 시 등록한 핸드폰 번호: ");
			phone = sc.nextLine().trim(); // 양끝 공백 제거
			phone = ViewUtil.formatPhone(phone); // 3-4-4 format
			if (phone != null)
				status = false;
		}
		System.out.print("새 비밀번호: ");
		String newPassword = sc.nextLine().trim();
		if (newPassword.isBlank() || newPassword.length() > 20) {
			System.out.println("새 비밀번호는 1~20자로 입력해주세요.");
			return;
		}

		PasswordChangeRequest request = new PasswordChangeRequest(id, phone, newPassword);
		userController.updatePassword(request);
	}

}
