package main.java.com.rental.view;

import java.util.List;
import java.util.Scanner;

import main.java.com.rental.common.util.ViewUtil;
import main.java.com.rental.common.util.ConsoleInput;
import main.java.com.rental.rental.controller.RentalController;
import main.java.com.rental.rental.entity.Rental;
import main.java.com.rental.rental.enums.RentalStatus;

public class ReturnMenuView {

	Scanner sc = ConsoleInput.scanner();
	RentalController rentalController = new RentalController();

	public void returnMenuView() {
		System.out.flush();
		while (true) {
			System.out.println("=================================");
			System.out.println("       물품 반납 내역 관리");
			System.out.println("=================================");
			System.out.println("1. 반납 신청 (내가 빌린 물품)");// 110 -> 200
			System.out.println("2. 반납 신청 승인 (내가 빌려준 물품)"); // 200 ->201
			System.out.println("3. 반납 거래 신청 "); // 201 ->210
			System.out.println("4. 반납 확인 및 평가 (대여자 최종 확인)"); // 210 -> 211
			System.out.println("0. 메인 메뉴로 이동");
			System.out.println("=================================");
			System.out.print("메뉴 선택 : ");
			switch (sc.nextLine().trim()) {
			case "1":
				returnRequest();
				break;

			case "2":
				approveReturn();
				break;

			case "3":
				confirmReturn();
				break;

			case "4":
				confirmReturnFinal();
				break;

			case "0":
				System.out.flush();
				return;

			default:
				System.out.println("잘못된 메뉴를 선택했습니다.");
			}
		}
	}

	/**
	 * 1. 반납 신청 내가 빌린 물품의 대여 번호를 입력하여 반납 신청
	 */
	private void returnRequest() {
		RentalStatus status = RentalStatus.RETURN_REQUESTED;
		System.out.flush();
		System.out.println("========== 반납 신청 ==========");
		List<Rental> list = rentalController.selectByStatus(110);
		if (list.isEmpty())
			return;
		System.out.print("반납할 대여 번호를 입력하세요 (취소: 0) : ");
		String input = sc.nextLine().trim();

		if ("0".equals(input)) {
			return;
		}

		try {
			int rentalNum = Integer.parseInt(input);
			Rental rental = ViewUtil.checkedRentalNum(list, rentalNum);
			if (rental == null) {
				System.out.println("리스트에 존재하는 번호를 입력해주세요.");
				return;
			}
			if (rentalController.updateStatusRentalNum(status.getCode(), rental.getRentalNum(),
					rental.getStatus().getCode()) > 0)
				System.out.println("반납 신청이 완료되었습니다.");
		} catch (NumberFormatException e) {
			System.out.println("대여 번호는 숫자로 입력해주세요.");
		}
	}

	/**
	 * 2. 반납 신청 승인 내가 빌려준 물품의 반납 신청을 승인
	 */
	private void approveReturn() {
		RentalStatus status = RentalStatus.RETURN_APPROVED;
		System.out.flush();
		System.out.println("====== 반납 신청 승인 ======");
		// 대여자가 받은 반납 요청 목록 조회
		List<Rental> list = rentalController.selectByStatus(200);
		if (list.isEmpty())
			return;
		System.out.print("승인할 대여 번호를 입력하세요 (0. 취소) : ");
		String input = sc.nextLine().trim();

		if ("0".equals(input)) {
			return;
		}

		try {
			int rentalNum = Integer.parseInt(input);
			Rental rental = ViewUtil.checkedRentalNum(list, rentalNum);
			if (rental == null) {
				System.out.println("리스트에 존재하는 번호를 입력해주세요.");
				return;
			}
			if (rentalController.updateStatusRentalNum(status.getCode(), rental.getRentalNum(),
					rental.getStatus().getCode()) > 0)
				System.out.println("반납 신청을 승인했습니다.");
		} catch (NumberFormatException e) {
			System.out.println("대여 번호는 숫자로 입력해주세요.");
		}
	}

	/**
	 * 3. 반납 신청 승인이 된 물품 거래 신청
	 */
	private void confirmReturn() {
		RentalStatus status = RentalStatus.BORROWER_CONFIRMED;
		System.out.flush();
		System.out.println("========== 반납 거래신청 ==========");
		List<Rental> list = rentalController.selectByStatus(201);
		if (list.isEmpty())
			return;
		System.out.print("반납 거래 신청을 할 번호를 입력하세요 (취소: 0) : ");
		String input = sc.nextLine().trim();

		if ("0".equals(input)) {
			return;
		}

		try {
			int rentalNum = Integer.parseInt(input);
			Rental rental = ViewUtil.checkedRentalNum(list, rentalNum);
			if (rental == null) {
				System.out.println("리스트에 존재하는 번호를 입력해주세요.");
				return;
			}
			if (rentalController.updateStatusRentalNum(status.getCode(), rental.getRentalNum(),
					rental.getStatus().getCode()) > 0)
				System.out.println("반납 거래 확인이 완료되었습니다.");
		} catch (NumberFormatException e) {
			System.out.println("숫자로 입력해주세요.");
		}
	}

	/**
	 * 9. 반납 확인 및 평가 대여자가 최종적으로 반납을 확인
	 */
	private void confirmReturnFinal() {
		RentalStatus status = RentalStatus.COMPLETED;
		System.out.flush();
		System.out.println("========== 반납 확인 ==========");
		List<Rental> list = rentalController.selectByStatus(210);
		if (list.isEmpty())
			return;
		System.out.print("반납 거래 신청을 할 번호를 입력하세요 (취소: 0) : ");
		String input = sc.nextLine().trim();

		if ("0".equals(input)) {
			return;
		}

		try {
			int rentalNum = Integer.parseInt(input);
			Rental rental = ViewUtil.checkedRentalNum(list, rentalNum);
			if (rental == null) {
				System.out.println("리스트에 존재하는 번호를 입력해주세요.");
				return;
			}
			if (rentalController.updateStatusRentalNum(status.getCode(), rental.getRentalNum(),
					rental.getStatus().getCode()) > 0)
				System.out.println("반납이 최종 완료되었습니다.");
		} catch (NumberFormatException e) {
			System.out.println("숫자로 입력해주세요.");
		}
	}
}
