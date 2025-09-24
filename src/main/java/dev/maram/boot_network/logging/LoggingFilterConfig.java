package dev.maram.boot_network.logging;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.web.servlet.FilterRegistrationBean;


@Configuration
public class LoggingFilterConfig {

    @Bean
    public FilterRegistrationBean<MDCFilter> loggingFilter() {

        // Create a new FilterRegistrationBean for MDCFilter
        FilterRegistrationBean<MDCFilter> registrationBean = new FilterRegistrationBean<>();
        // Set the filter instance to be registered
        registrationBean.setFilter(new MDCFilter());

        // Configure URL patterns this filter should apply to ("/*" means all URLs)
        registrationBean.addUrlPatterns("/*");

        return registrationBean;
    }
}