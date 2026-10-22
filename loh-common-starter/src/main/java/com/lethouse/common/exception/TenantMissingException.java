package com.lethouse.common.exception;

public class TenantMissingException extends RuntimeException{
    public TenantMissingException(String msg){
        super(msg);
    }
}
