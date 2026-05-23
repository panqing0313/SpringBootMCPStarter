
package com.github.trae.mcp.autoconfigure;

import com.github.trae.mcp.autoconfigure.controller.McpController;
import com.github.trae.mcp.autoconfigure.executor.DefaultToolExecutor;
import com.github.trae.mcp.autoconfigure.registry.DefaultToolRegistry;
import com.github.trae.mcp.autoconfigure.scanner.ToolScanner;
import com.github.trae.mcp.core.executor.ToolExecutor;
import com.github.trae.mcp.core.registry.ToolRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(McpProperties.class)
@ConditionalOnProperty(prefix = "mcp", name = "enabled", havingValue = "true", matchIfMissing = true)
public class McpAutoConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(McpAutoConfiguration.class);

    public McpAutoConfiguration() {
        logger.info("=== MCP AutoConfiguration is being initialized ===");
    }

    @Bean
    @ConditionalOnMissingBean
    public ToolRegistry toolRegistry() {
        logger.info("=== Creating ToolRegistry Bean ===");
        return new DefaultToolRegistry();
    }

    @Bean
    @ConditionalOnMissingBean
    public ToolExecutor toolExecutor(ToolRegistry toolRegistry) {
        logger.info("=== Creating ToolExecutor Bean ===");
        return new DefaultToolExecutor(toolRegistry);
    }

    @Bean
    public ToolScanner toolScanner(org.springframework.context.ApplicationContext applicationContext, ToolRegistry toolRegistry) {
        logger.info("=== Creating ToolScanner Bean ===");
        return new ToolScanner(applicationContext, toolRegistry);
    }

    @Bean
    @ConditionalOnProperty(prefix = "mcp", name = "expose-api", havingValue = "true", matchIfMissing = true)
    public McpController mcpController(ToolExecutor toolExecutor, McpProperties properties) {
        logger.info("=== Creating McpController Bean ===");
        return new McpController(toolExecutor, properties);
    }
}
