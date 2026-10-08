package main.java.com.rental.view;

import java.util.List;
import java.util.Scanner;

import main.java.com.rental.common.util.ViewUtil;
import main.java.com.rental.common.util.ConsoleInput;
import main.java.com.rental.post.controller.PostController;
import main.java.com.rental.post.dto.AvailablePost;
import main.java.com.rental.rental.controller.RentalController;
import main.java.com.rental.rental.dto.RentalCreateRequest;
import main.java.com.rental.rental.entity.Rental;
import main.java.com.rental.rental.enums.RentalStatus;
import main.java.com.rental.session.Session;

public class RentalMenuView {

	Scanner sc = ConsoleInput.scanner();
	RentalController rentalController = new RentalController();
	PostController postController = new PostController();
	String id = Session.getInstance().getLoginUser().getId();

	/**
	 * Rental 메뉴
	 */
	public void rentalMenu() {
		System.out.flush();
		while (true) {
			System.out.println("\n========================================");
			System.out.println("          물품 대여 내역 관리");
			System.out.println("========================================");
			System.out.println(" 1. 대여 신청한 목록 조회"); // id값이 있는것들 조회
			System.out.println(" 2. 대여 신청"); // 대여 신청가능한 리스트가 출력되고 postnum 선택
			System.out.println(" 3. 대여 신청 승인"); // 대여 신청 대기중(100)인 리스트 출력, rentalController.approveRental(int rentalNum,int postNum)호출
			System.out.println(" 4. 대여 신청 거절"); // 대여 신청 대기중 리스트 출력, rentalController.rejectRental(int rentalNum)호출
			System.out.println(" 5. 대여 시작 처리 (물품 인도 확인)"); // 메소드 만들어야함.
			System.out.println(" 6. 대여 중 물품 조회"); // rentalController.selectCurrentRentalList(String userId)호출
			System.out.println(" 0. 상위 메뉴로 이동");
			System.out.println("----------------------------------------");
			System.out.print("메뉴를 선택해주세요 >> ");

			switch (sc.nextLine().trim()) {

			case "1":
				System.out.flush();
				rentalController.selectRentalRequestList();
				break;

			case "2":
				rentItem();
				break;

			case "3":
				approveRental();
				break;

			case "4":
				rejectRental();
				break;

			case "5":
				startRental();
				break;

			case "6":
				rentalController.selectCurrentRentalList();
				break;

			case "0":
				return;

			default:
				System.out.println("잘못된 입력입니다. 다시 입력해주세요.");
			}
		}
	}

	/**
	 * [대여 메뉴] 물품 목록 조회 및 대여 신청
	 */
	private void rentItem() {
		System.out.flush();
		System.out.println("\n[물품 대여 - 목록 조회 및 대여 신청]");
		List<AvailablePost> list = postController.selectAvailablePost();
		if (list.isEmpty()) return;
		SuccessView.printEntityList(list);
		System.out.print("대여 신청할 게시글 번호(PostNum) 입력 (취소: 0) >>  ");
		try {
			int postNum = Integer.parseInt(sc.nextLine().trim());
			if (postNum == 0) {
				return;
			}
			AvailablePost post = checkedPostNum(list, postNum);
			if (post == null) {
				System.out.println("존재하지 않는 게시글 번호입니다.");
				return;
			}
			RentalCreateRequest request = new RentalCreateRequest(postNum, id);
			rentalController.rentalCreate(request);

		} catch (NumberFormatException e) {
			System.out.println("게시글 번호는 숫자만 입력 가능합니다.");
		}
	}

	/**
	 * 3. 대여 신청 승인 대여 신청 대기중(status == 100)인 목록을 출력하고 사용자가 선택한 rentalNum, postNum을
	 * Controller 로 전달
	 */
	private void approveRental() {
		System.out.flush();
		System.out.println("\n[대여 신청 승인]");
		// 대여 신청 대기중인 목록 조회
		List<Rental> list = rentalController.selectByStatus(100);
		if (list.isEmpty()) return;
		System.out.println(" 0. 상위 메뉴로 이동");
		System.out.print("승인할 대여 번호를 입력해주세요 >> ");
		int rentalNum = 0; 
		try {
			rentalNum = Integer.parseInt(sc.nextLine());	
		} catch (NumberFormatException e) {
			System.out.println("숫자를 입력해주세요");
			return;
		}
		if (rentalNum == 0) {
			return;
		}
		
		Rental rental = ViewUtil.checkedRentalNum(list, rentalNum);
		if(rental ==null) return;
		int postNum = rental.getPostNum();

		System.out.println("선택한 대여 신청 >> " + rental);
		System.out.print("대여 신청을 승인하시겠습니까? (Y/N) : ");
		String answer = sc.nextLine();

		if (!answer.equalsIgnoreCase("Y")) {
			System.out.println("승인을 취소했습니다.");
			return;
		}
		//트랜잭션관리
		rentalController.approveRental(rentalNum, postNum);
	}
	
	/**
	 * 4. 대여 신청 거절
	 * 대여 신청 대기중(status == 100)인 목록을 출력하고
	 * 선택한 rentalNum을 Controller 로 전달
	 */
	private void rejectRental() {
		RentalStatus status = RentalStatus.REJECTED;
		System.out.flush();
	    System.out.println("\n[대여 신청 거절]");
	    List<Rental> list = rentalController.selectByStatus(100);
	    if (list.isEmpty()) return;
	    
	    System.out.println(" 0. 상위 메뉴로 이동");
	    System.out.print("거절할 대여 번호를 입력해주세요 >> ");
	    int rentalNum;
	    try {
			rentalNum = Integer.parseInt(sc.nextLine().trim());
	    } catch (NumberFormatException e) {
			System.out.println("대여 번호는 숫자로 입력해주세요.");
			return;
	    }

	    if (rentalNum == 0) {
	        return;
	    }
	    Rental rental = ViewUtil.checkedRentalNum(list, rentalNum);
		if(rental ==null) return;
		
	    System.out.println("선택한 대여 거절 내역");
	    System.out.println(rental);

	    System.out.print("대여 신청을 거절하시겠습니까? (Y/N) : ");
	    String answer = sc.nextLine();

	    if (!answer.equalsIgnoreCase("Y")) {
	        System.out.println("거절을 취소했습니다.");
	        return;
	    }
	    if (rentalController.updateStatusRentalNum(status.getCode(),rental.getRentalNum(),rental.getStatus().getCode()) > 0)
			System.out.println("대여 신청이 거절되었습니다.");
	}
	
	/**
	 * 5. 대여 시작 처리
	 * 승인된 대여(status == 101) 목록을 출력하고
	 * 물품을 실제로 인도했는지 확인 후 대여 시작 처리
	 */
	private void startRental() {
		RentalStatus status = RentalStatus.RENTED;
		System.out.flush();
	    System.out.println("\n[대여 시작 처리]");

	    // 승인된 대여 목록
	    List<Rental> list = rentalController.selectByStatus(101);

	    if (list.isEmpty()) {
	        return;
	    }

	    System.out.println(" 0. 상위 메뉴로 이동");
	    System.out.print("대여 시작할 대여 번호를 입력해주세요 >> ");
	   
	    int rentalNum;
	    try {
			rentalNum = Integer.parseInt(sc.nextLine().trim());
	    } catch (NumberFormatException e) {
			System.out.println("대여 번호는 숫자로 입력해주세요.");
			return;
	    }
	    if (rentalNum == 0) {
	        return;
	    }

	    Rental rental = ViewUtil.checkedRentalNum(list, rentalNum);
		if(rental ==null) return;

	    System.out.println();
	    System.out.println("선택한 대여 정보");
	    System.out.println(rental);

	    System.out.print("물품을 인도받았습니까? (Y/N) : ");
	    String answer = sc.nextLine();

	    if (!answer.equalsIgnoreCase("Y")) {
	        System.out.println("대여 시작을 취소했습니다.");
	        return;
	    }

	    if (rentalController.updateStatusRentalNum(status.getCode(),rental.getRentalNum(),rental.getStatus().getCode()) > 0)
			System.out.println("대여가 시작되었습니다.");
	}
	
	//대여가능한 게시글중 postNum 해당하는지 확인
	private AvailablePost checkedPostNum(List<AvailablePost> list, int postNum) {
		for (AvailablePost post : list) {
			if (post.getPostNum() == postNum) {
				return post;
			}
		}
		return null;
	}
}
