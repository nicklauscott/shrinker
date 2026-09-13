package com.eclipsett.shrinker.config

import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class WebConfig : WebMvcConfigurer {
    override fun addViewControllers(registry: ViewControllerRegistry) {
        // Redirects /api-docs to /v3/api-docs
        registry.addRedirectViewController("/docs", "/v3/api-docs")
        registry.addRedirectViewController("/v1/docs", "/v3/api-docs")
        registry.addRedirectViewController("/api-docs", "/v3/api-docs")
        registry.addRedirectViewController("/v1/api-docs", "/v3/api-docs")
    }
}
