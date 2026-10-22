package com.lethouse.common.handler;


import com.lethouse.common.exception.BusinessException;
import com.lethouse.common.exception.TenantMissingException;
import com.lethouse.common.result.R;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice//专门捕获Controller的异常，并打包成R《T》
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class GlobalExceptionHandler {


    private static final Logger log= LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 租户缺失异常：403
     * @param e
     * @return
     */
    @ExceptionHandler(TenantMissingException.class)
    public R<Void> handleTenantMissingException(TenantMissingException e){
        return R.fail(403,e.getMessage());
    }

    /**
     * 参数校验异常：400
     * @param e
     * @return
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public R<Void> handleValidException(MethodArgumentNotValidException e){
        FieldError firstError = e.getBindingResult().getFieldError();
        String msg = firstError==null?"参数校验失败":firstError.getField()+":"+firstError.getDefaultMessage();
        return R.fail(400,msg);
    }

    /**
     * 通用业务异常，自带业务code
     * @param e
     * @return
     */
    @ExceptionHandler(BusinessException.class)
    public R<Void> handleBusinessException(BusinessException e){
        return R.fail(e.getCode(),e.getMessage());
    }

    /**
     * 请求不存在的接口异常
     * @param e
     * @return
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public R<Void> handleNotFound(NoResourceFoundException e){
        return R.fail(404,"接口不存在:"+e.getResourcePath());
    }

    /**
     * 除了以上异常，皆在此捕获，判断traceId是否获取
     * @param e
     * @return
     */
    @ExceptionHandler(Exception.class)
    public R<Void> handleException(Exception e){
        String traceId = MDC.get("traceId");
        log.error("[traceId={}],未捕获异常",traceId,e);
        return R.fail(500,"系统繁忙，请稍后重试");
    }
}
