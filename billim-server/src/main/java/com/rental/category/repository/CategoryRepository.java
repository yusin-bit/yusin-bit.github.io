package main.java.com.rental.category.repository;

import java.util.List;

import main.java.com.rental.category.entity.Category;
import main.java.com.rental.common.exception.NotFoundException;

public interface CategoryRepository {

	// 대분류 카테고리 목록 조회
	List<Category> getBigCategories() throws NotFoundException;

	// 선택한 대분류에 속한 소분류 카테고리 목록 조회
	List<Category> getSmallCategories(String bigCategoryCode) throws NotFoundException;
}
