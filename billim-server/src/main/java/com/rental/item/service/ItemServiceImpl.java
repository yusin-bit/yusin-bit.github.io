package main.java.com.rental.item.service;

import java.util.ArrayList;
import java.util.List;

import main.java.com.rental.common.exception.InvalidRentalStatusException;
import main.java.com.rental.common.exception.ItemException;
import main.java.com.rental.common.exception.NotFoundException;
import main.java.com.rental.item.dto.ItemCreateRequest;
import main.java.com.rental.item.entity.Item;
import main.java.com.rental.item.repository.ItemRepository;
import main.java.com.rental.item.repository.ItemRepositoryImpl;

public class ItemServiceImpl implements ItemService {

    // 데이터베이스 작업을 수행할 리포지토리 객체
    private ItemRepository ir = new ItemRepositoryImpl();
    
    // 싱글톤(Singleton) 객체 관리
    private ItemServiceImpl() {}
    public static ItemService getInstance() {
        return new ItemServiceImpl();
    }

    // 물품 전체 목록 조회
    @Override
    public List<Item> itemSelect() throws NotFoundException {
        List<Item> itemList = ir.itemSelect();

        // 리스트 null 여부 및 비어있는 상태 검증
        if (itemList == null || itemList.isEmpty()) {
            throw new NotFoundException("목록에 물품이 없습니다.");
        }

        return itemList;
    }

    // 물품 번호 기준 단건 조회
    @Override
    public Item itemSelectByitemNum(int itemNum) throws NotFoundException {
        Item item = ir.itemSelectByitemNum(itemNum);

        if (item == null) {
            throw new NotFoundException("해당 번호의 물품 정보가 없습니다.");
        }

        return item;
    }

    // 키워드 기준 물품 검색
    @Override
    public List<Item> itemSearch(String keyword) throws NotFoundException {
        List<Item> itemList = ir.itemSearch(keyword);

        if (itemList == null || itemList.isEmpty()) {
            throw new NotFoundException("검색 결과가 없습니다.");
        }

        return itemList;
    }

    // 신규 물품 등록
    @Override
	public int itemInsert(ItemCreateRequest item) throws ItemException {
		int result = ir.itemInsert(item);

        if (result == 0) {
			throw new InvalidRentalStatusException("등록되지 않았습니다.");
		}
		return result;
	}

    // 물품 정보 수정
    @Override
    public void itemUpdate(Item item) throws ItemException, NotFoundException {
        int result = ir.itemUpdate(item);

        // 업데이트 영향 행 수가 0인 경우 수정 대상 누락 예외 발생
        if (result == 0) {
            throw new NotFoundException("수정 대상 물품이 없거나 변경되지 않았습니다.");
        }
    }

    // 대여 가능 상태 변경
    @Override
    public void itemUpdateStatus(int itemNum, boolean status) throws ItemException, NotFoundException {
        int result = ir.itemUpdateStatus(itemNum, status);

        if (result == 0) {
            throw new NotFoundException("상태를 변경할 물품을 찾을 수 없습니다.");
        }
    }

    // 물품 정보 삭제
    @Override
    public void itemDelete(int itemNum,String userId) throws ItemException, NotFoundException {
        int result = ir.itemDelete(itemNum, userId);

        if (result == 0) {
            throw new NotFoundException("삭제 대상 물품 정보를 찾을 수 없습니다.");
        }
    }
    
    
	@Override
	public List<Item> selectByUserId(String userId) throws ItemException,NotFoundException {
		List<Item> list = ir.selectByUserId(userId);
		return list;
	}
    
    
    
    
}
