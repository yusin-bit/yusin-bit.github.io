package main.java.com.rental.view;

import java.util.List;
import java.util.Scanner;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

import main.java.com.rental.common.util.ViewUtil;
import main.java.com.rental.common.util.ConsoleInput;
import main.java.com.rental.item.controller.ItemController;
import main.java.com.rental.item.entity.Item;
import main.java.com.rental.post.controller.PostController;
import main.java.com.rental.post.dto.PostCreate;
import main.java.com.rental.post.dto.PostUpdate;
import main.java.com.rental.post.entity.Post;
import main.java.com.rental.session.Session;

public class PostMenuView {

	private final Scanner sc = ConsoleInput.scanner();

	private final PostController postController = new PostController();

	/**
	 * 게시글 메뉴
	 */
	public void postMenu() {
		System.out.flush();
		while (true) {
			System.out.println("\n========================================");
			System.out.println("              게시글 관리 메뉴");
			System.out.println("========================================");
			System.out.println(" 1. 새 게시글 등록");
			System.out.println(" 2. 등록 게시글 정보 수정");
			System.out.println(" 3. 등록 게시글 삭제");
			System.out.println(" 4. 게시글 목록 조회");
			System.out.println(" 5. 게시글 검색");
			System.out.println(" 0. 상위 메뉴로 이동");
			System.out.println("----------------------------------------");
			System.out.print("메뉴를 선택해주세요 >> ");

			switch (sc.nextLine().trim()) {

			case "1":
				createPost();
				break;

			case "2":
				updatePost();
				break;

			case "3":
				deletePost();
				break;

			case "4":
				selectPostList();
				break;

			case "5":
				searchPost();
				break;

			case "0":
				return;

			default:
				System.out.println("잘못된 입력입니다. 다시 입력해주세요.");
			}
		}
	}

	/**
	 * 1. 게시글 등록
	 */
	private void createPost() {

		System.out.flush();
		System.out.println("\n[게시글 등록]");

		List<Item> list = new ItemController().selectByUserId(Session.getInstance().getLoginUser().getId());
		if (list.isEmpty()) {
			System.out.println("게시글로 등록할 물품이 없습니다.");
			return;
		}
		SuccessView.printEntityList(list);

		System.out.println(" 0. 상위 메뉴로 이동");
		System.out.print("물품 번호를 입력해주세요 >> ");
		int itemNum = 0;
		try {
			itemNum = Integer.parseInt(sc.nextLine());
		} catch (NumberFormatException e) {
			System.out.println("숫자를 입력해주세요.");
			return;
		}

		if (itemNum == 0) {
			return;
		}
		Item item = ViewUtil.checkedItemNum(list, itemNum);
		if (item == null) {
			System.out.println(itemNum + "에 해당하는 물품이 없습니다. ");
			return;
		}
		createPostForItem(item.getItemNum());
	}

	public void createPostForItem(int itemNum) {
		System.out.println("\n[게시글 정보 등록]");
		System.out.print("제목 : ");
		String title = sc.nextLine();

		System.out.print("설명 : ");
		String content = sc.nextLine();

		System.out.print("대여일 : ");
		String rentDate = sc.nextLine();

		System.out.print("반납일 : ");
		String returnDate = sc.nextLine();

		System.out.print("주소 : ");
		String addr = sc.nextLine();
		if (!validPostInput(title, content, rentDate, returnDate, addr)) return;

		PostCreate postCreate = new PostCreate();

		postCreate.setItemNum(itemNum);
		postCreate.setTitle(title);
		postCreate.setContent(content);
		postCreate.setRentDate(rentDate);
		postCreate.setReturnDate(returnDate);
		postCreate.setAddr(addr);

		postController.postCreate(postCreate);
	}

	/**
	 * 2. 게시글 수정
	 */
	private void updatePost() {

		System.out.println("\n[게시글 수정]");
		List<Post> list = postController.selectById();
		if (list.isEmpty()) return;
		System.out.println(" 0. 상위 메뉴로 이동");
		System.out.print("수정할 게시글 번호를 입력해주세요 >> ");
		int postNum = 0;
		try {
			postNum = Integer.parseInt(sc.nextLine());
		} catch (NumberFormatException e) {
			System.out.println("숫자를 입력해주세요.");
			return;
		}
		if (postNum == 0) {
			return;
		}
		Post post = ViewUtil.checkedPostNum(list, postNum);
		if (post == null) {
			return;
		}

		/*
		 * 수정할 게시글의 정보 입력
		 */
		System.out.print("제목 : ");
		String title = sc.nextLine();

		System.out.print("설명 : ");
		String content = sc.nextLine();

		System.out.print("대여일 : ");
		String rentDate = sc.nextLine();

		System.out.print("반납일 : ");
		String returnDate = sc.nextLine();

		System.out.print("주소 : ");
		String addr = sc.nextLine();
		if (!validPostInput(title, content, rentDate, returnDate, addr)) return;

		PostUpdate postUpdate = new PostUpdate();

		postUpdate.setPostNum(postNum);
		postUpdate.setTitle(title);
		postUpdate.setContent(content);
		postUpdate.setRentDate(rentDate);
		postUpdate.setReturnDate(returnDate);
		postUpdate.setAddr(addr);

		postController.postUpdate(postUpdate);
	}

	/**
	 * 3. 게시글 삭제
	 */
	private void deletePost() {

		System.out.println("\n[게시글 삭제]");
		List<Post> list = postController.selectById();
		if (list.isEmpty()) return;
		System.out.println(" 0. 상위 메뉴로 이동");
		System.out.print("삭제할 게시글 번호를 입력해주세요 >> ");

		int postNum = 0;
		try {
			postNum = Integer.parseInt(sc.nextLine());
		} catch (NumberFormatException e) {
			System.out.println("숫자를 입력해주세요.");
			return;
		}
		if (postNum == 0) {
			return;
		}
		Post post = ViewUtil.checkedPostNum(list, postNum);
		if (post == null) {
			return;
		}

		System.out.print("정말 삭제하시겠습니까? (Y/N) : ");
		String answer = sc.nextLine();

		if (!answer.equalsIgnoreCase("Y")) {
			System.out.println("삭제를 취소했습니다.");
			return;
		}

		postController.postDelete(postNum);
	}

	/**
	 * 4. 게시글 목록 조회
	 *
	 * 현재 PostController에는 전체 게시글을 반환하는 selectAll() 메소드가 없기 때문에 현재 Controller에서 제공하는
	 * 기능을 기준으로 구성할 경우 별도의 전체조회 메소드가 필요함.
	 */
	private void selectPostList() {

		System.out.println("\n[게시글 목록 조회]");
		postController.selectAll();

	}

	/**
	 * 5. 게시글 검색
	 */
	private void searchPost() {
		System.out.flush();
		while (true) {
			System.out.println("\n========================================");
			System.out.println("              게시글 검색 메뉴");
			System.out.println("========================================");
			System.out.println(" 1. 물품 번호로 검색");
			System.out.println(" 2. 제목으로 검색");
			System.out.println(" 3. 내용으로 검색");
			System.out.println(" 4. 대여일로 검색");
			System.out.println(" 5. 주소로 검색");
			System.out.println(" 0. 상위 메뉴로 이동");
			System.out.println("----------------------------------------");

			System.out.print("검색 방법을 선택해주세요 >> ");

			switch (sc.nextLine().trim()) {

			case "1":
				searchByItemNum();
				break;

			case "2":
				searchByTitle();
				break;

			case "3":
				searchByContent();
				break;

			case "4":
				searchByRentDate();
				break;

			case "5":
				searchByAddr();
				break;

			case "0":
				return;

			default:
				System.out.println("잘못된 입력입니다. 다시 입력해주세요.");
			}
		}
	}

	/**
	 * 물품번호 검색
	 */
	private void searchByItemNum() {

		System.out.print("물품번호 : ");

		int itemNum;
		try {
			itemNum = Integer.parseInt(sc.nextLine().trim());
		} catch (NumberFormatException e) {
			System.out.println("물품 번호는 숫자로 입력해주세요.");
			return;
		}

		if (itemNum == 0) {
			return;
		}

		postController.selectByItemNum(itemNum);
	}

	/**
	 * 제목 검색
	 */
	private void searchByTitle() {

		System.out.print("제목 검색어 : ");

		String keyword = sc.nextLine();

		if (keyword.equals("0")) {
			return;
		}

		postController.selectByTitleKeyword(keyword);
	}

	/**
	 * 내용 검색
	 */
	private void searchByContent() {

		System.out.print("내용 검색어 : ");

		String keyword = sc.nextLine();

		if (keyword.equals("0")) {
			return;
		}

		postController.selectByContentKeyword(keyword);
	}

	/**
	 * 대여일 검색
	 */
	private void searchByRentDate() {

		System.out.print("대여일 : ");

		String rentDate = sc.nextLine();

		if (rentDate.equals("0")) {
			return;
		}

		postController.selectByRentDate(rentDate);
	}

	/**
	 * 주소 검색
	 */
	private void searchByAddr() {

		System.out.print("주소 : ");

		String addr = sc.nextLine();

		if (addr.equals("0")) {
			return;
		}

		postController.selectByAddr(addr);
	}

	private boolean validPostInput(String title, String content, String rentDate, String returnDate, String addr) {
		if (title == null || title.isBlank() || title.length() > 50 || content == null || content.isBlank()
				|| addr == null || addr.isBlank() || addr.length() > 255) {
			System.out.println("제목(1~50자), 설명, 주소(1~255자)를 올바르게 입력해주세요.");
			return false;
		}
		try {
			LocalDateTime start = LocalDateTime.parse(rentDate.trim().replace(' ', 'T'));
			LocalDateTime end = LocalDateTime.parse(returnDate.trim().replace(' ', 'T'));
			if (!end.isAfter(start)) {
				System.out.println("반납일은 대여일보다 늦어야 합니다.");
				return false;
			}
		} catch (DateTimeParseException e) {
			System.out.println("날짜는 yyyy-MM-ddTHH:mm 또는 yyyy-MM-dd HH:mm 형식으로 입력해주세요.");
			return false;
		}
		return true;
	}

}
