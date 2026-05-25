
package com.github.star.mcp.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.json.jackson.JacksonMcpJsonMapper;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

@Configuration
@EnableConfigurationProperties(McpProperties.class)
@ConditionalOnProperty(prefix = "mcp", name = "enabled", havingValue = "true", matchIfMissing = true)
public class McpAutoConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(McpAutoConfiguration.class);

    @Autowired(required = false)
    private McpToolRegistrar toolRegistrar;

    public McpAutoConfiguration() {
        logger.info("MCP AutoConfiguration initializing");
    }

    @Bean
    @ConditionalOnMissingBean
    public StdioServerTransportProvider mcpTransportProvider(ObjectMapper objectMapper) {
        logger.info("Creating StdioServerTransportProvider for MCP server");
        McpJsonMapper jsonMapper = new JacksonMcpJsonMapper(objectMapper);
        return new StdioServerTransportProvider(jsonMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public McpSyncServer mcpServer(StdioServerTransportProvider transportProvider,
                                    @Autowired(required = false) List<McpServerFeatures.SyncToolSpecification> beanToolSpecifications) {
        List<McpServerFeatures.SyncToolSpecification> allTools = new ArrayList<>();
        
        if (toolRegistrar != null) {
            allTools.addAll(toolRegistrar.getToolSpecifications());
        }
        
        if (beanToolSpecifications != null) {
            allTools.addAll(beanToolSpecifications);
        }
        
        logger.info("Creating McpSyncServer with {} tools", allTools.size());
        
        var builder = McpServer.sync(transportProvider)
            .serverInfo("mcp-sample-server", "1.0.0-SNAPSHOT")
            .capabilities(McpSchema.ServerCapabilities.builder()
                .tools(true)
                .build())
            .tools(allTools.toArray(new McpServerFeatures.SyncToolSpecification[0]));

        McpSyncServer server = builder.build();
        logger.info("MCP Server created successfully with {} tools", allTools.size());
        return server;
    }

    @Bean
    public CommandLineRunner mcpKeepAliveRunner(McpSyncServer server) {
        return args -> {
            logger.info("MCP STDIO server started. Waiting for input...");
            // Use System.in read to block - this doesn't interfere with reactor's STDIO transport
            // since System.in is already being consumed by StdioServerTransportProvider
            // We just need to keep the JVM alive
            try {
                // Read from a separate stream to keep main thread alive without blocking reactor
                Thread.sleep(Long.MAX_VALUE);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        };
    }
}
