package com.federation.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConfigurationProperties(prefix = "app.cors")
public class CorsProperties {

    private List<String> allowedOrigins = List.of("http://localhost:3000");
    private String allowedMethods = "GET,POST,PUT,PATCH,DELETE,OPTIONS";
    private String allowedHeaders = "*";
    private long maxAge = 3600L;

    public List<String> getAllowedOrigins() { return allowedOrigins; }
    public void setAllowedOrigins(List<String> allowedOrigins) { this.allowedOrigins = allowedOrigins; }

    public String getAllowedMethods() { return allowedMethods; }
    public void setAllowedMethods(String allowedMethods) { this.allowedMethods = allowedMethods; }

    public String getAllowedHeaders() { return allowedHeaders; }
    public void setAllowedHeaders(String allowedHeaders) { this.allowedHeaders = allowedHeaders; }

    public long getMaxAge() { return maxAge; }
    public void setMaxAge(long maxAge) { this.maxAge = maxAge; }
}
