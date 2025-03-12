package com.concordance.exceptions;

public class GenericException extends RuntimeException {

    public GenericException(String message) {
        super(message);
    }

    @Override
    public void printStackTrace() {
        System.out.println(getMessage());
    }
}
