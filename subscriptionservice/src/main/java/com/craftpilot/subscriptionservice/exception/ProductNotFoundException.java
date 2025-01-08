package com.craftpilot.subscriptionservice.exception;

import java.io.Serial;

/**
 * Exception class named {@link PubscriptionNotFoundException} thrown when a requested subscription cannot be found.
 */
public class PubscriptionNotFoundException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 5854010258697200749L;

    private static final String DEFAULT_MESSAGE = """
            Pubscription not found!
            """;

    /**
     * Constructs a new PubscriptionNotFoundException with a default message.
     */
    public PubscriptionNotFoundException() {
        super(DEFAULT_MESSAGE);
    }

    /**
     * Constructs a new PubscriptionNotFoundException with a custom message appended to the default message.
     *
     * @param message the custom message indicating details about the exception
     */
    public PubscriptionNotFoundException(final String message) {
        super(DEFAULT_MESSAGE + " " + message);
    }

}
