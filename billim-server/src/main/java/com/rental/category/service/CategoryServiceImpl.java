package main.java.com.rental.category.service;

import java.util.List;

import main.java.com.rental.category.entity.Category;
import main.java.com.rental.category.repository.CategoryRepository;
import main.java.com.rental.category.repository.CategoryRepositoryImpl;
import main.java.com.rental.common.exception.NotFoundException;

public class CategoryServiceImpl implements CategoryService {

	public CategoryRepository categoryRepository = new CategoryRepositoryImpl();

	@Override
	public List<Category> getBigCategories() throws NotFoundException {
		List<Category> categoryList = categoryRepository.getBigCategories();
		if (categoryList == null || categoryList.isEmpty()) {
			throw new NotFoundException("등록된 대분류 카테고리가 없습니다.");
		}
		return categoryList;
	}

	@Override
	public List<Category> getSmallCategories(String bigCategoryCode) throws NotFoundException {
		if (bigCategoryCode == null || bigCategoryCode.isBlank()) {
			throw new NotFoundException("선택한 대분류 카테고리 코드가 없습니다.");
		}

		List<Category> categoryList = categoryRepository.getSmallCategories(bigCategoryCode);
		if (categoryList == null || categoryList.isEmpty()) {
			throw new NotFoundException("등록된 소분류 카테고리가 없습니다.");
		}
		return categoryList;
	}
}
