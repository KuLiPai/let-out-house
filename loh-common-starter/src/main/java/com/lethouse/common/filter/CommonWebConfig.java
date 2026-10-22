package com.lethouse.common.filter;

import com.lethouse.common.interceptor.TenantCheckInterceptor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class CommonWebConfig implements WebMvcConfigurer {
    @Bean
    public FilterRegistrationBean<TenantFilter> tenantFilter(){
        FilterRegistrationBean<TenantFilter> bean = new FilterRegistrationBean<>(new TenantFilter());
        bean.addUrlPatterns("/*");
        bean.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return bean;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry){
        registry.addInterceptor(new TenantCheckInterceptor())
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/auth/login");
    }
}
