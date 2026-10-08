package main.java.com.rental.user.service;

import java.sql.SQLException;

import main.java.com.rental.common.exception.PasswordUpdateException;
import main.java.com.rental.common.exception.UserException;
import main.java.com.rental.common.exception.NotFoundException;
import main.java.com.rental.user.dto.FindIdRequest;
import main.java.com.rental.user.dto.PasswordChangeRequest;
import main.java.com.rental.user.dto.UserLoginRequest;
import main.java.com.rental.user.dto.UserResponse;
import main.java.com.rental.user.dto.UserSignUpRequest;

public interface UserService {

	/**
	   * 로그인
	   * */
	UserResponse login(UserLoginRequest request) throws UserException;
	
	/**
	   * 회원 가입 
	   * */
	int signUp(UserSignUpRequest request) throws UserException;

	boolean isIdAvailable(String id) throws UserException;
	
	/**
	   * ID 찾기 
	   * */
	String findId(String phone) throws UserException;

	/**
	   * 비밀번호 재설정 
	   * */
	int updatePassword(PasswordChangeRequest request) throws UserException;


	/**
	   * 로그아웃 
	   * */
	void logout();





}
