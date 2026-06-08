package com.javarush.jira.common.internal.config;

import com.javarush.jira.login.AuthUser;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.ui.ModelMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.context.request.WebRequestInterceptor;
import org.springframework.web.filter.ForwardedHeaderFilter;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;
import org.springframework.web.servlet.handler.WebRequestHandlerInterceptorAdapter;
import org.springframework.web.servlet.i18n.CookieLocaleResolver;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;
import org.springframework.web.servlet.mvc.UrlFilenameViewController;

import java.time.Duration;
import java.util.Locale;
import java.util.Properties;

@EnableAutoConfiguration
@Configuration
@AllArgsConstructor
@Slf4j
public class MvcConfig implements WebMvcConfigurer {
    private final AppProperties appProperties;

    // Add authUser to view model
    private final HandlerInterceptor authInterceptor = new WebRequestHandlerInterceptorAdapter(new WebRequestInterceptor() {
        @Override
        public void postHandle(WebRequest request, ModelMap model) {
            if (model != null) {
                AuthUser authUser = AuthUser.safeGet();
                if (authUser != null) {
                    model.addAttribute("authUser", authUser);
                }
            }
        }

        @Override
        public void afterCompletion(WebRequest request, Exception ex) {
        }

        @Override
        public void preHandle(WebRequest request) {
        }
    });

    @Bean
    public LocaleResolver localeResolver() {
        CookieLocaleResolver slr = new CookieLocaleResolver();
        slr.setDefaultLocale(Locale.ENGLISH);
        slr.setCookieName("lang");
        return slr;
    }

    @Bean
    public LocaleChangeInterceptor localeChangeInterceptor() {
        LocaleChangeInterceptor lci = new LocaleChangeInterceptor();
        lci.setParamName("lang");
        return lci;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor).excludePathPatterns("/api/**");
        registry.addInterceptor(localeChangeInterceptor());
    }

    @Bean
    public SimpleUrlHandlerMapping getUrlHandlerMapping() {
        return new SimpleUrlHandlerMapping() {{
            setMappings(new Properties() {{
                put("/view/**", new UrlFilenameViewController());
            }});
            setOrder(0);
            setInterceptors(authInterceptor);
        }};
    }

    @Bean
    ForwardedHeaderFilter forwardedHeaderFilter() {
        return new ForwardedHeaderFilter();
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/").setViewName("index");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/static/**").addResourceLocations("file:./resources/static/");
        registry.setOrder(Integer.MAX_VALUE);
    }

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder restTemplateBuilder) {
        return restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(10))
                .setReadTimeout(Duration.ofSeconds(10))
                .build();
    }
}