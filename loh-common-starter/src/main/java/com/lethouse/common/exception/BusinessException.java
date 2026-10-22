package com.lethouse.common.exception;

public class BusinessException extends RuntimeException{
    private final int code;

    public BusinessException(int code,String msg){
        super(msg);
        this.code=code;

    }

    public int getCode(){
        return code;
    }
}
