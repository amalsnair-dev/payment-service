package com.service.payment.exception;

public class StaleFencingTokenException extends RuntimeException{

    public StaleFencingTokenException(String message){
        super(message);
    }
}
