
package com.github.star.mcp.sample.tool;

import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.List;
import java.util.Map;

@Configuration
@Profile("bean-tools")
public class CalculatorToolConfig {

    @Bean
    public McpServerFeatures.SyncToolSpecification addTool() {
        McpSchema.JsonSchema inputSchema = new McpSchema.JsonSchema(
            "object",
            Map.of(
                "a", Map.of("type", "integer", "description", "第一个数"),
                "b", Map.of("type", "integer", "description", "第二个数")
            ),
            List.of("a", "b"),
            null,
            null,
            null
        );

        return new McpServerFeatures.SyncToolSpecification(
            new McpSchema.Tool("add2", "加法运算", null, inputSchema, null, null, null),
            (exchange, args) -> {
                int a = ((Number) args.get("a")).intValue();
                int b = ((Number) args.get("b")).intValue();
                int result = a + b;
                return new McpSchema.CallToolResult(
                    List.of(new McpSchema.TextContent(String.valueOf(result))),
                    false
                );
            }
        );
    }

    @Bean
    public McpServerFeatures.SyncToolSpecification subtractTool() {
        McpSchema.JsonSchema inputSchema = new McpSchema.JsonSchema(
            "object",
            Map.of(
                "a", Map.of("type", "integer", "description", "被减数"),
                "b", Map.of("type", "integer", "description", "减数")
            ),
            List.of("a", "b"),
            null,
            null,
            null
        );

        return new McpServerFeatures.SyncToolSpecification(
            new McpSchema.Tool("subtract", "减法运算", null, inputSchema, null, null, null),
            (exchange, args) -> {
                int a = ((Number) args.get("a")).intValue();
                int b = ((Number) args.get("b")).intValue();
                int result = a - b;
                return new McpSchema.CallToolResult(
                    List.of(new McpSchema.TextContent(String.valueOf(result))),
                    false
                );
            }
        );
    }

    @Bean
    public McpServerFeatures.SyncToolSpecification multiplyTool() {
        McpSchema.JsonSchema inputSchema = new McpSchema.JsonSchema(
            "object",
            Map.of(
                "a", Map.of("type", "integer", "description", "第一个因数"),
                "b", Map.of("type", "integer", "description", "第二个因数")
            ),
            List.of("a", "b"),
            null,
            null,
            null
        );

        return new McpServerFeatures.SyncToolSpecification(
            new McpSchema.Tool("multiply", "乘法运算", null, inputSchema, null, null, null),
            (exchange, args) -> {
                int a = ((Number) args.get("a")).intValue();
                int b = ((Number) args.get("b")).intValue();
                int result = a * b;
                return new McpSchema.CallToolResult(
                    List.of(new McpSchema.TextContent(String.valueOf(result))),
                    false
                );
            }
        );
    }

    @Bean
    public McpServerFeatures.SyncToolSpecification divideTool() {
        McpSchema.JsonSchema inputSchema = new McpSchema.JsonSchema(
            "object",
            Map.of(
                "numerator", Map.of("type", "integer", "description", "被除数"),
                "denominator", Map.of("type", "integer", "description", "除数")
            ),
            List.of("numerator", "denominator"),
            null,
            null,
            null
        );

        return new McpServerFeatures.SyncToolSpecification(
            new McpSchema.Tool("divide", "除法运算", null, inputSchema, null, null, null),
            (exchange, args) -> {
                int numerator = ((Number) args.get("numerator")).intValue();
                int denominator = ((Number) args.get("denominator")).intValue();
                if (denominator == 0) {
                    throw new IllegalArgumentException("除数不能为零");
                }
                double result = (double) numerator / denominator;
                return new McpSchema.CallToolResult(
                    List.of(new McpSchema.TextContent(String.valueOf(result))),
                    false
                );
            }
        );
    }
}
