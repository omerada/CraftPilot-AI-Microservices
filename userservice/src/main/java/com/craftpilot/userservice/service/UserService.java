package com.craftpilot.userservice.service;

import com.craftpilot.userservice.model.user.Token;
import com.craftpilot.userservice.model.user.dto.request.LoginRequest;
import com.craftpilot.userservice.model.user.entity.UserEntity;


public interface UserService {

    /**
     * Handles user login, validates credentials, and generates a token.
     * Publishes an event when the login is successful.
     *
     * @param loginRequest the login request containing the credentials
     * @return the generated token for the user
     */
    Token login(LoginRequest loginRequest);

    /**
     * Finds a user by email.
     *
     * @param email the email of the user
     * @return the UserEntity found by the email
     */
    UserEntity findByEmail(String email);

    /**
     * Kullanıcı oluşturma işlemi.
     *
     * @param userEntity Oluşturulacak kullanıcı bilgileri
     * @return Oluşturulan kullanıcı nesnesi
     */
    UserEntity createUser(UserEntity userEntity);

    /**
     * Kullanıcı güncelleme işlemi.
     *
     * @param userId Güncellenecek kullanıcının ID'si
     * @param userEntity Güncellenmiş kullanıcı bilgileri
     * @return Güncellenen kullanıcı nesnesi
     */
    UserEntity updateUser(String userId, UserEntity userEntity);

}
