
package com.github.star.mcp.autoconfigure.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.star.mcp.autoconfigure.McpProperties;
import com.github.star.mcp.core.executor.ToolExecutor;
import com.github.star.mcp.core.model.ToolCallRequest;
import com.github.star.mcp.core.model.ToolCallResponse;
import com.github.star.mcp.core.model.ToolDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@RestController
@RequestMapping("${mcp.base-path:/mcp}")
public class McpController {

    private static final Logger logger = LoggerFactory.getLogger(McpController.class);

    private final ToolExecutor toolExecutor;
    private final McpProperties properties;
    private final ObjectMapper objectMapper;

    private final ConcurrentHashMap<String, SseEmitter> emitters = new ConcurrentHashMap<>();
    private final AtomicLong emitterIdCounter = new AtomicLong(0);

    @Autowired
    public McpController(ToolExecutor toolExecutor, McpProperties properties, ObjectMapper objectMapper) {
        this.toolExecutor = toolExecutor;
        this.properties = properties;
        this.objectMapper = objectMapper;
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

    @GetMapping(value = "/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter sseConnection() {
        String emitterId = "sse-" + emitterIdCounter.incrementAndGet();
        logger.info("Creating SSE connection: {}", emitterId);
        
        SseEmitter emitter = new SseEmitter(0L);
        emitters.put(emitterId, emitter);
        
        emitter.onCompletion(() -> {
            logger.info("SSE connection completed: {}", emitterId);
            emitters.remove(emitterId);
        });
        
        emitter.onTimeout(() -> {
            logger.info("SSE connection timeout: {}", emitterId);
            emitters.remove(emitterId);
        });
        
        emitter.onError((e) -> {
            logger.error("SSE connection error: {}", emitterId, e);
            emitters.remove(emitterId);
        });
        
        try {
            Map<String, Object> initEvent = new HashMap<>();
            initEvent.put("jsonrpc", "2.0");
            initEvent.put("method", "initialized");
            initEvent.put("result", new HashMap<String, Object>() {{
                put("protocolVersion", "2024-11-05");
                put("serverInfo", new HashMap<String, String>() {{
                    put("name", "mcp-sample-server");
                    put("version", "1.0.0-SNAPSHOT");
                }});
                put("capabilities", new HashMap<String, Object>() {{
                    put("tools", new HashMap<String, Boolean>() {{
                        put("listChanged", true);
                    }});
                }});
            }});
            
            emitter.send(SseEmitter.event()
                .name("endpoint")
                .data("/mcp/message"));
            
            logger.info("SSE connection established: {}", emitterId);
        } catch (IOException e) {
            logger.error("Failed to send initialization event", e);
        }
        
        return emitter;
    }

    @PostMapping(value = "/message")
    public ResponseEntity<Map<String, Object>> receiveMessage(@RequestBody Map<String, Object> message) {
        String method = (String) message.get("method");
        Object id = message.get("id");
        
        logger.debug("Received MCP message: method={}, id={}", method, id);
        
        Map<String, Object> response = new HashMap<>();
        response.put("jsonrpc", "2.0");
        
        if (id != null) {
            response.put("id", id);
        }
        
        if (method == null) {
            if (message.containsKey("result")) {
                logger.debug("Received result notification");
                return ResponseEntity.ok(message);
            }
            return ResponseEntity.badRequest().build();
        }
        
        switch (method) {
            case "initialize":
                Map<String, Object> initResult = new HashMap<>();
                initResult.put("protocolVersion", "2024-11-05");
                initResult.put("serverInfo", new HashMap<String, String>() {{
                    put("name", "mcp-sample-server");
                    put("version", "1.0.0-SNAPSHOT");
                }});
                initResult.put("capabilities", new HashMap<String, Object>() {{
                    put("tools", new HashMap<String, Boolean>() {{
                        put("listChanged", true);
                    }});
                }});
                response.put("result", initResult);
                break;
                
            case "tools/list":
                List<ToolDefinition> tools = toolExecutor.listTools();
                List<Map<String, Object>> toolList = tools.stream()
                    .map(this::convertToolDefinition)
                    .toList();
                
                Map<String, Object> toolsResult = new HashMap<>();
                toolsResult.put("tools", toolList);
                response.put("result", toolsResult);
                break;
                
            case "tools/call":
                Map<String, Object> params = (Map<String, Object>) message.get("params");
                String toolName = (String) params.get("name");
                Map<String, Object> arguments = (Map<String, Object>) params.get("arguments");
                
                ToolCallRequest request = new ToolCallRequest();
                request.setToolName(toolName);
                request.setArguments(arguments);
                
                ToolCallResponse toolResponse = toolExecutor.execute(request);
                
                Map<String, Object> textContent = new HashMap<>();
                textContent.put("type", "text");
                textContent.put("text", toolResponse.getResult() != null ? 
                    toolResponse.getResult().toString() : "");
                
                Map<String, Object> callResult = new HashMap<>();
                callResult.put("content", new Object[] { textContent });
                response.put("result", callResult);
                break;
                
            default:
                response.put("error", new HashMap<String, Object>() {{
                    put("code", -32601);
                    put("message", "Method not found: " + method);
                }});
        }
        
        broadcastToAll(response);
        return ResponseEntity.ok(response);
    }

    private Map<String, Object> convertToolDefinition(ToolDefinition tool) {
        Map<String, Object> definition = new HashMap<>();
        definition.put("name", tool.getName());
        definition.put("description", tool.getDescription());
        
        Map<String, Object> inputSchema = new HashMap<>();
        inputSchema.put("type", "object");
        
        Map<String, Object> properties = new HashMap<>();
        List<String> required = new java.util.ArrayList<>();
        
        tool.getParameters().forEach(param -> {
            Map<String, Object> paramSchema = new HashMap<>();
            paramSchema.put("type", convertType(param.getType()));
            paramSchema.put("description", param.getDescription());
            properties.put(param.getName(), paramSchema);
            if (param.isRequired()) {
                required.add(param.getName());
            }
        });
        
        inputSchema.put("properties", properties);
        inputSchema.put("required", required);
        definition.put("inputSchema", inputSchema);
        
        return definition;
    }

    private void broadcastToAll(Map<String, Object> message) {
        try {
            String data = objectMapper.writeValueAsString(message);
            emitters.forEach((id, emitter) -> {
                try {
                    emitter.send(SseEmitter.event().data(data));
                } catch (IOException e) {
                    logger.error("Failed to send SSE event to {}", id, e);
                    emitters.remove(id);
                }
            });
        } catch (IOException e) {
            logger.error("Failed to serialize message", e);
        }
    }
}
