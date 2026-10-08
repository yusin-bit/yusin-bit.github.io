package main.java.com.rental.user.repository;

import java.sql.SQLException;

import main.java.com.rental.common.exception.UserException;
import main.java.com.rental.user.dto.FindIdRequest;
import main.java.com.rental.user.dto.PasswordChangeRequest;
import main.java.com.rental.user.dto.UserLoginRequest;
import main.java.com.rental.user.dto.UserSignUpRequest;
import main.java.com.rental.user.entity.User;

public interface UserRepository {
	
	  /**
	   * 로그인하기
	   * */

	  User login(UserLoginRequest request) throws UserException;
	  
	  /**
	   * 회원 가입 하기 
	   * */
	  
	  int signUp(UserSignUpRequest request) throws UserException;

	  boolean existsById(String id) throws UserException;

	  /**
	   * 아이디 찾기
	   * */
	  
	  String findId(String phone) throws UserException;
	  
	  /**
	   * 비밀번호 바꾸기
	   * */
	  
	  int updatePassword(PasswordChangeRequest request) throws UserException;

	 
			
	}

