package com.craftpilot.userservice.model.user.event;

import com.craftpilot.userservice.model.user.entity.UserEntity;
import org.springframework.context.ApplicationEvent;

/**
 * Kullanıcı güncellendiğinde tetiklenen bir event sınıfıdır.
 * Bu sınıf, güncellenen kullanıcı bilgisini içerir.
 */
public class UserUpdatedEvent extends ApplicationEvent {

    private final UserEntity updatedUser;

    /**
     * Constructor
     *
     * @param updatedUser Güncellenmiş kullanıcı bilgisi
     */
    public UserUpdatedEvent(UserEntity updatedUser) {
        super(updatedUser);  // Event, kullanıcıyı bir kaynak olarak kabul eder.
        this.updatedUser = updatedUser;
    }

    /**
     * Güncellenmiş kullanıcı bilgisini döner.
     *
     * @return Güncellenmiş kullanıcı
     */
    public UserEntity getUpdatedUser() {
        return updatedUser;
    }
}

