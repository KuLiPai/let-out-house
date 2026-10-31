package com.lethouse.conversation.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class BillingClientConfig {
    @Bean
    RestClient billingRestClient(@Value("${loh.billing.base-url}")String base){
        var f=new SimpleClientHttpRequestFactory();
        f.setConnectTimeout(3_000);
        f.setReadTimeout(5_000);
        return RestClient.builder().baseUrl(base).requestFactory(f).build();
    }
}
