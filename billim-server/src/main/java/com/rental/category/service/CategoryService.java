package main.java.com.rental.category.service;

import java.util.List;

import main.java.com.rental.category.entity.Category;
import main.java.com.rental.common.exception.NotFoundException;

public interface CategoryService {

	// 대분류 카테고리 목록 조회
	List<Category> getBigCategories() throws NotFoundException;

	// 선택한 대분류에 속한 소분류 카테고리 목록 조회
	List<Category> getSmallCategories(String bigCategoryCode) throws NotFoundException;
}
