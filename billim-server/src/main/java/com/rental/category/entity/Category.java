package main.java.com.rental.category.entity;

public class Category {

	public String code;
	public String name;

	public Category() {
	}

	
	public Category(String code, String name) {
		this.code = code;
		this.name = name;
	}


	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	@Override
	public String toString() {
		return name + " (" + code + ")";
	}
}
