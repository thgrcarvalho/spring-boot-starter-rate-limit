package io.github.thgrcarvalho.ratelimit;

import io.github.thgrcarvalho.ratelimit.internal.InMemoryRateLimitStore;
import io.github.thgrcarvalho.ratelimit.internal.RateLimitInterceptor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Spring Boot auto-configuration for the rate-limit starter. */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class RateLimitAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(RateLimitStore.class)
    InMemoryRateLimitStore inMemoryRateLimitStore() {
        return new InMemoryRateLimitStore();
    }

    @Bean
    RateLimitInterceptor rateLimitInterceptor(RateLimitStore store) {
        return new RateLimitInterceptor(store);
    }

    @Bean
    WebMvcConfigurer rateLimitWebMvcConfigurer(RateLimitInterceptor interceptor) {
        return new WebMvcConfigurer() {
            @Override
            public void addInterceptors(InterceptorRegistry registry) {
                registry.addInterceptor(interceptor);
            }
        };
    }
}
