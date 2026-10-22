package com.lethouse.common.context;

public final class TenantContext {

    private TenantContext(){}

    private static final ThreadLocal<TenantInfo> HOLDER=new ThreadLocal<>();


    public static void set(Long tenantId,Long userId,String requestId){
        HOLDER.set(new TenantInfo(tenantId,userId,requestId));
    }

    public static Long getTenantId(){
        TenantInfo info = HOLDER.get();
        return info==null?null:info.tenantId();
    }

    public static Long getUserId(){
        TenantInfo info = HOLDER.get();
        return info==null?null:info.userId();
    }

    public static String getRequestId(){
        TenantInfo info = HOLDER.get();
        return info==null?null:info.requestId();
    }

    public static void clear(){
        HOLDER.remove();
    }
}
