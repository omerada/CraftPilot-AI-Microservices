package com.craftpilot.userservice.model.user.event;


import com.craftpilot.userservice.model.user.entity.UserEntity;
import org.springframework.context.ApplicationEvent;

/**
 * Kullanıcı oluşturulduğunda tetiklenen bir event sınıfıdır.
 * Bu sınıf, oluşturulan kullanıcı bilgisini içerir.
 */
public class UserCreatedEvent extends ApplicationEvent {

    private final UserEntity createdUser;

    /**
     * Constructor
     *
     * @param createdUser Oluşturulan kullanıcı bilgisi
     */
    public UserCreatedEvent(UserEntity createdUser) {
        super(createdUser);  // Event, kullanıcıyı bir kaynak olarak kabul eder.
        this.createdUser = createdUser;
    }

    /**
     * Oluşturulan kullanıcı bilgisini döner.
     *
     * @return Oluşturulan kullanıcı
     */
    public UserEntity getCreatedUser() {
        return createdUser;
    }
}

