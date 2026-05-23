
package com.github.star.mcp.autoconfigure.controller;

import com.github.star.mcp.autoconfigure.McpProperties;
import com.github.star.mcp.core.executor.ToolExecutor;
import com.github.star.mcp.core.model.ToolCallRequest;
import com.github.star.mcp.core.model.ToolCallResponse;
import com.github.star.mcp.core.model.ToolDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("${mcp.base-path:/mcp}")
public class McpController {

    private static final Logger logger = LoggerFactory.getLogger(McpController.class);

    private final ToolExecutor toolExecutor;
    private final McpProperties properties;

    @Autowired
    public McpController(ToolExecutor toolExecutor, McpProperties properties) {
        this.toolExecutor = toolExecutor;
        this.properties = properties;
    }

    @GetMapping("/tools")
    public ResponseEntity<List<ToolDefinition>> listTools() {
        logger.debug("Listing all MCP tools");
        List<ToolDefinition> tools = toolExecutor.listTools();
        return ResponseEntity.ok(tools);
    }

    @PostMapping("/call")
    public ResponseEntity<ToolCallResponse> callTool(@RequestBody ToolCallRequest request) {
        logger.debug("Calling tool: {}", request.getToolName());
        ToolCallResponse response = toolExecutor.execute(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("version", "1.0.0-SNAPSHOT");
        health.put("tools", toolExecutor.listTools().size());
        return ResponseEntity.ok(health);
    }

    @GetMapping("/schema")
    public ResponseEntity<List<Map<String, Object>>> getSchema() {
        List<ToolDefinition> tools = toolExecutor.listTools();
        List<Map<String, Object>> schemas = tools.stream()
            .map(this::convertToSchema)
            .toList();
        return ResponseEntity.ok(schemas);
    }

    private Map<String, Object> convertToSchema(ToolDefinition tool) {
        Map<String, Object> schema = new HashMap<>();
        schema.put("name", tool.getName());
        schema.put("description", tool.getDescription());
        schema.put("streaming", tool.isStreaming());
        
        Map<String, Object> parameters = new HashMap<>();
        Map<String, Object> properties = new HashMap<>();
        List<String> required = new java.util.ArrayList<>();
        
        tool.getParameters().forEach(param -> {
            Map<String, Object> paramSchema = new HashMap<>();
            paramSchema.put("type", convertType(param.getType()));
            paramSchema.put("description", param.getDescription());
            if (!param.getDefaultValue().isEmpty()) {
                paramSchema.put("default", param.getDefaultValue());
            }
            properties.put(param.getName(), paramSchema);
            if (param.isRequired()) {
                required.add(param.getName());
            }
        });
        
        parameters.put("type", "object");
        parameters.put("properties", properties);
        parameters.put("required", required);
        
        schema.put("parameters", parameters);
        return schema;
    }

    private String convertType(String javaType) {
        return switch (javaType.toLowerCase()) {
            case "int", "integer" -> "integer";
            case "long" -> "integer";
            case "double", "float" -> "number";
            case "boolean" -> "boolean";
            default -> "string";
        };
    }
}
