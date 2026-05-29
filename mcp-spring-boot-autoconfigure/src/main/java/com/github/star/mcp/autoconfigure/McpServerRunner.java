package com.github.star.mcp.autoconfigure;

import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.concurrent.CountDownLatch;

public class McpServerRunner {

    private static final Logger logger = LoggerFactory.getLogger(McpServerRunner.class);

    public static void run(Class<?> primarySource, String[] args) {
        SpringApplication app = new SpringApplication(primarySource);
        app.setWebApplicationType(WebApplicationType.NONE);
        app.setBannerMode(org.springframework.boot.Banner.Mode.OFF);

        ConfigurableApplicationContext context = app.run(args);

        McpSyncServer server = context.getBean(McpSyncServer.class);
        CountDownLatch latch = context.getBean(CountDownLatch.class);

        logger.info("MCP STDIO server started. Waiting for input...");

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Shutting down MCP server...");
            server.close();
        }));

        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            context.close();
        }
    }
}
