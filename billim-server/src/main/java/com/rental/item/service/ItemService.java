package main.java.com.rental.item.service;

import java.util.List;

import main.java.com.rental.common.exception.ItemException;
import main.java.com.rental.common.exception.NotFoundException;
import main.java.com.rental.item.dto.ItemCreateRequest;
import main.java.com.rental.item.entity.Item;

public interface ItemService {

	// 물품 전체 목록 조회
	List<Item> itemSelect() throws NotFoundException;
	
	// 물품 번호 기준 단건 조회 
	Item itemSelectByitemNum(int itemNum) throws NotFoundException;

	// 키워드 기준 물품 검색
	List<Item> itemSearch(String keyword) throws NotFoundException;

	// 물품 등록
	int itemInsert(ItemCreateRequest item) throws ItemException;

	// 물품 정보 수정 
	void itemUpdate(Item item) throws ItemException, NotFoundException;

	// 대여 상태 변경
	void itemUpdateStatus(int itemNum, boolean status) throws ItemException, NotFoundException;

	// 물품 삭제
	void itemDelete(int itemNum,String userId) throws ItemException, NotFoundException;
	
	//사용자 아이디 기준 아이템 조회
	List<Item> selectByUserId(String userId) throws ItemException, NotFoundException;
	
}
