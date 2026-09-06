package com.aiecomm.camp.core;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

import java.util.UUID;

@Component
@RequestScope
@Slf4j
public class TenantContext  {

    private static  final ThreadLocal currenThread=new ThreadLocal();
    private static Logger logger= LoggerFactory.getLogger(TenantContext.class);

    private String tenantId;


    public static void setTenantId(UUID tenantId){
        currenThread.set(tenantId);
        logger.info("set tenant id for the context ",Thread.currentThread()+ " tenantid "+ tenantId);
    }

    public static UUID getTenantId(){
      return (UUID) currenThread.get();
    }

    public static ThreadLocal getCurrenThread() {
        return currenThread;
    }


    public static void clear() {
        currenThread.remove(); // use remove(), not set(null) - actually frees the entry
    }

}
