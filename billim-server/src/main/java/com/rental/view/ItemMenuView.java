package main.java.com.rental.view;

import java.util.List;
import java.util.Collections;
import java.util.Scanner;

import main.java.com.rental.category.controller.CategoryController;
import main.java.com.rental.category.entity.Category;
import main.java.com.rental.common.util.ViewUtil;
import main.java.com.rental.common.util.ConsoleInput;
import main.java.com.rental.item.controller.ItemController;
import main.java.com.rental.item.dto.ItemCreateRequest;
import main.java.com.rental.item.entity.Item;
import main.java.com.rental.session.Session;

public class ItemMenuView {
	Scanner sc = ConsoleInput.scanner();
	ItemController itemController = new ItemController();
	String id = Session.getInstance().getLoginUser().getId();
	List<Item> list = null;
	CategoryController categoryController = new CategoryController();
	List<Category> listBC = null;
	
	public ItemMenuView() {
		refreshItems();
		listBC = categoryController.getBigCategories();
		System.out.flush();// console.clear
	}

	/**
	 * [물품 관리 메뉴] 등록, 수정, 삭제 선택
	 */
	public void itemMenu() {
		boolean status = true;
		while (status) {
			System.out.println("\n========================================");
			System.out.println("              물품 관리 메뉴");
			System.out.println("========================================");
			System.out.println(" 1. 새 물품 및 게시글 등록");
			System.out.println(" 2. 내가 등록한 물품 목록");
			System.out.println(" 3. 등록 물품 정보 수정");
			System.out.println(" 4. 등록 물품 삭제");
			System.out.println(" 0. 상위 메뉴로 이동");
			System.out.println("----------------------------------------");
			System.out.print("메뉴를 선택해주세요 >> ");

			switch (sc.nextLine().trim()) {
			case "1":
				inputItemInsert();
				break;
			case "2":
				refreshItems();
				if (list.isEmpty()) {
					System.out.println("등록한 물품이 없습니다.");
				} else {
					SuccessView.printEntityList(list);
				}
				break;
			case "3":
				inputItemUpdate();
				break;
			case "4":
				inputItemDelete();
				break;
			case "0":
				status = false;
				break;
			default:
				System.out.println("잘못된 입력입니다. 다시 입력해주세요.");
				break;
			}
		}
	}

	/**
	 * 새 물품 등록 정보 입력
	 */
	public void inputItemInsert() {
		try {
			System.out.println("\n[물품 등록]");
			System.out.print("물품명: ");
			String itemName = sc.nextLine().trim();
			if (itemName.isBlank() || itemName.length() > 50) {
				System.out.println("물품명은 1~50자로 입력해주세요.");
				return;
			}

			String smallCategoryCode = selectCategory();
			if (smallCategoryCode == null) {
				System.out.println("카테고리 선택이 취소되어 등록을 중단합니다.");
				return;
			}

			ItemCreateRequest item = new ItemCreateRequest(itemName, true, smallCategoryCode, id);

			// Controller를 호출하여 Service의 itemInsert 로직을 수행
			int itemNum = itemController.itemInsert(item);
			if (itemNum == 0) return;
			refreshItems();
			System.out.println("이어서 등록한 물품의 게시글 정보를 입력해주세요.");
			new PostMenuView().createPostForItem(itemNum);
		} catch (NumberFormatException e) {
			System.out.println("숫자만 입력 가능합니다.");
		}
	}

	/**
	 * 등록 물품 정보 수정 입력 기존 조회 객체(existing)의 모든 필드를 보존한 상태에서 물품명을 변경하여 전달합니다.
	 */
	public void inputItemUpdate() {
		try {
			refreshItems();
			if (list.isEmpty()) {
				System.out.println("수정할 물품이 없습니다.");
				return;
			}
			Item item =null;
			while(true) {
				System.out.println("\n[물품 수정]");
				// 수정 대상 물품 확인을 위해 전체 물품 목록 출력
				SuccessView.printEntityList(list);
				
				System.out.print("수정할 물품 번호: ");
				int itemNo = Integer.parseInt(sc.nextLine().trim());
				
				item = ViewUtil.checkedItemNum(this.list, itemNo);//list에 있는지 확인
				if(item!=null)  break;
				System.out.flush();
				System.out.println("번호를 다시 입력해주세요.");
			}
			System.out.print("수정할 물품명: ");
			String updateName = sc.nextLine().trim();
			if (updateName.isBlank() || updateName.length() > 50) {
				System.out.println("물품명은 1~50자로 입력해주세요.");
				return;
			}
			item.setItemName(updateName);
			
			//카테고리 선택
			String smallCategoryCode = selectCategory(item);
			if (smallCategoryCode == null) {
				System.out.println("카테고리 선택이 취소되어 등록을 중단합니다.");
				return;
			}
			
			// Controller를 호출하여 Service의 itemUpdate 로직을 수행
			itemController.itemUpdate(item);
			refreshItems();
		} catch (NumberFormatException e) {
			System.out.println("물품 번호는 숫자만 입력 가능합니다.");
		}
	}

	/**
	 * 물품 삭제 번호 입력
	 */
	public void inputItemDelete() {
		try {
			refreshItems();
			if (list.isEmpty()) {
				System.out.println("삭제할 물품이 없습니다.");
				return;
			}
			System.out.println("\n[물품 삭제]");
			// 삭제 대상 물품 확인을 위해 전체 물품 목록 출력
			SuccessView.printEntityList(list);
			System.out.print("삭제할 물품 번호: ");
			int itemNo = Integer.parseInt(sc.nextLine().trim());
			Item item = ViewUtil.checkedItemNum(this.list,itemNo); 
			if (item == null) {
				System.out.println("목록에 존재하는 물품 번호를 입력해주세요.");
				return;
			}
			// Controller를 호출하여 Service의 itemDelete 로직을 수행
			itemController.itemDelete(item.getItemNum(), item.getLenderID());
			refreshItems();
		} catch (NumberFormatException e) {
			System.out.println("물품 번호는 숫자만 입력 가능합니다.");
		}
	}
	
	//처음 지정할 때
	private String selectCategory() {
		try {
			//큰 카테고리값 저장 
			String bigCategory = selectBigCategory();
//			만약 null이면 사용자가 잘못된 입력으로 null리턴 
			if (bigCategory==null)return null;
			//bigCategory에 따른 ListSmallCategory출력
			List<Category> listSc = categoryController.getSmallCategories(bigCategory);
			if (listSc.isEmpty()) return null;
			SuccessView.printIndexCategoryList(listSc);
			// 사용자 선택
			System.out.print("카테고리 번호 >> ");
			int sel = Integer.parseInt(sc.nextLine().trim());
			
			if (sel < 1 || sel > listSc.size()) {
				System.out.println("목록에 있는 카테고리 번호를 입력해주세요.");
				return null;
			}
			String smallCategory = listSc.get(sel-1).getCode();
			return smallCategory;
		} catch (NumberFormatException e) {
			System.out.println("지원되지 않는 값을 입력했습니다.");
			return null;
		}
	}
	
	//카테고리 변경할 때
	private String selectCategory(Item item) {
		String smallCategory=selectCategory();
		if(smallCategory==null) return null;
		item.setSmallCategoryCode(smallCategory);
		return smallCategory;
	}
	private String selectBigCategory() {
		try {
			if (listBC == null || listBC.isEmpty()) return null;
			SuccessView.printIndexCategoryList(listBC);
			System.out.print("카테고리 번호 >> ");
			int sel = Integer.parseInt(sc.nextLine().trim());
			if (sel < 1 || sel > listBC.size()) {
				System.out.println("목록에 있는 카테고리 번호를 입력해주세요.");
				return null;
			}
			String bigCategory = listBC.get(sel-1).getCode();
			return bigCategory;
		} catch (NumberFormatException e) {
			System.out.println("지원되지 않는 값을 입력했습니다.");
			return null;
		}
	}

	private void refreshItems() {
		List<Item> current = itemController.selectByUserId(id);
		list = current == null ? Collections.emptyList() : current;
	}
}
