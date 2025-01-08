package com.craftpilot.subscriptionservice.exception;

import java.io.Serial;

/**
 * Exception class named {@link PubscriptionAlreadyExistException} thrown when attempting to create a subscription that already exists.
 */
public class PubscriptionAlreadyExistException extends RuntimeException{

    @Serial
    private static final long serialVersionUID = 53457089789182737L;

    private static final String DEFAULT_MESSAGE = """
            Pubscription already exist!
            """;

    /**
     * Constructs a new PubscriptionAlreadyExistException with a default message.
     */
    public PubscriptionAlreadyExistException() {
        super(DEFAULT_MESSAGE);
    }

    /**
     * Constructs a new PubscriptionAlreadyExistException with a custom message appended to the default message.
     *
     * @param message the custom message indicating details about the exception
     */
    public PubscriptionAlreadyExistException(final String message) {
        super(DEFAULT_MESSAGE + " " + message);
    }

}
