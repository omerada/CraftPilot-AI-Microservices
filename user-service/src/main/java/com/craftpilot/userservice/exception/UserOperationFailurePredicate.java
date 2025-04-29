package com.craftpilot.userservice.exception;

import java.util.function.Predicate;

/**
 * Resilience4j circuit breaker için hangi istisnaların hata olarak sayılacağını belirleyen sınıf.
 * ValidationException ve UserNotFoundException gibi "beklenen" hatalar devre kesici tarafından
 * başarısızlık olarak sayılmayacaktır.
 */
public class UserOperationFailurePredicate implements Predicate<Throwable> {

    @Override
    public boolean test(Throwable throwable) {
        // ValidationException ve UserNotFoundException gibi "beklenen" hatalar devre kesici için
        // başarısızlık olarak değerlendirilmeyecek
        if (throwable instanceof ValidationException ||
            throwable instanceof UserNotFoundException ||
            throwable instanceof NotFoundException) {
            return false;
        }
        
        // Diğer tüm istisnalar gerçek hata olarak kabul edilecek
        return true;
    }
}
