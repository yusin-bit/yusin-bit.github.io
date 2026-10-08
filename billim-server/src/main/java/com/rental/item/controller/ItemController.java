package main.java.com.rental.item.controller;

import java.util.List;
import java.util.Collections;

import main.java.com.rental.common.exception.ItemException;
import main.java.com.rental.common.exception.NotFoundException;
import main.java.com.rental.item.dto.ItemCreateRequest;
import main.java.com.rental.item.entity.Item;
import main.java.com.rental.item.service.ItemService;
import main.java.com.rental.item.service.ItemServiceImpl;
import main.java.com.rental.view.FailView;
import main.java.com.rental.view.SuccessView;

public class ItemController  {

	ItemService itemService = ItemServiceImpl.getInstance();

	// 전체 목록 조회
	public void itemSelect() {
		try {
			List<Item> list = itemService.itemSelect();
			SuccessView.printEntityList(list);
		} catch (NotFoundException e) {
			FailView.FailMessage(e.getMessage());
		}
	}

	// 물품 번호 조회
	public void itemSelectByitemNum(int itemNum) {
		try {
			Item item = itemService.itemSelectByitemNum(itemNum);
			SuccessView.printEntity(item);
		} catch (NotFoundException e) {
			FailView.FailMessage(e.getMessage());
		}
	}

	// 키워드 검색
	public void itemSearch(String keyword) {
		try {
			List<Item> list = itemService.itemSearch(keyword);
			SuccessView.printEntityList(list);
		} catch (NotFoundException e) {
			FailView.FailMessage(e.getMessage());
		} 
	}

	// 물품 등록
	public int itemInsert(ItemCreateRequest item) {
		try {
			int itemNum = itemService.itemInsert(item);
			SuccessView.printMessage("물품이 등록되었습니다");
			return itemNum;
		} catch (ItemException | NotFoundException e) {
			FailView.FailMessage(e.getMessage());
		} 
		return 0;
	}

	// 물품 수정
	public void itemUpdate(Item item) {
		try {
			itemService.itemUpdate(item);
			SuccessView.printMessage("물품이 수정되었습니다");
		} catch (ItemException | NotFoundException e) {
			FailView.FailMessage(e.getMessage());
		} 
	}

	// 대여 상태 변경
	public void itemUpdateStatus(int itemNum, boolean status) {
		try {
			itemService.itemUpdateStatus(itemNum, status);
			SuccessView.printMessage("상태가 변경되었습니다");
		} catch (ItemException | NotFoundException e) {
			FailView.FailMessage(e.getMessage());
		} 
	}

	// 물품 삭제
	public void itemDelete(int itemNum,String userId) {
		try {
			itemService.itemDelete(itemNum, userId);
			SuccessView.printMessage("물품이 삭제되었습니다");
		} catch (ItemException e) {
			FailView.FailMessage(e.getMessage());
		}
	}
	
	//유저 아이디 기준 아이템 정보
	public List<Item> selectByUserId(String userId){
		List<Item> list=null;
		try {
			list = itemService.selectByUserId(userId);
//			SuccessView.printEntityList(list);
		} catch (NotFoundException | ItemException e) {
			FailView.FailMessage(e.getMessage());
		}
		return list == null ? Collections.emptyList() : list;
	}
}
