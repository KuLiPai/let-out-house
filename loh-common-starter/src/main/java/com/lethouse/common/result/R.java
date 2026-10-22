package com.lethouse.common.result;

import lombok.Data;
import org.slf4j.MDC;

import java.io.Serializable;

@Data
public class R<T> implements Serializable{

  private static final long serialVersionUID=1L;
  
  private Integer code;
  private String msg;
  private T data;
  private String traceId;

  private  R() {
  }

  private  R(Integer code, String message, T data, String traceId) {
    this.code = code;
    this.msg = message;
    this.data = data;
    this.traceId = traceId;
  }

  public static <T> R<T> ok(){
    String traceId1 = MDC.get("traceId");
    return new R<>(0,"success",null,traceId1);
  }
  public static <T> R<T> ok(T data){
    String traceId1 = MDC.get("traceId");
    return new R<>(0,"success",data,traceId1);
  }
  public static <T> R<T> ok(T data, String msg){
    String traceId1 = MDC.get("traceId");
    return new R<>(0,msg,data,traceId1);
  }
  public static <T> R<T> fail(int code, String msg){
    String traceId1 = MDC.get("traceId");
    return new R<>(code,msg,null,traceId1);
  }
}
