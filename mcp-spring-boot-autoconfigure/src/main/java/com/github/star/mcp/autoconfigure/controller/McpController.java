
package com.github.star.mcp.autoconfigure.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.star.mcp.core.executor.ToolExecutor;
import com.github.star.mcp.core.model.ToolCallRequest;
import com.github.star.mcp.core.model.ToolCallResponse;
import com.github.star.mcp.core.model.ToolDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private final ObjectMapper objectMapper;

    private final ConcurrentHashMap<String, SseEmitter> emitters = new ConcurrentHashMap<>();
    private final AtomicLong emitterIdCounter = new AtomicLong(0);

    public McpController(ToolExecutor toolExecutor, ObjectMapper objectMapper) {
        this.toolExecutor = toolExecutor;
        this.objectMapper = objectMapper;
        logger.info("MCP Controller initialized with {} tools", toolExecutor.listTools().size());
    }

    @GetMapping("/tools")
    public ResponseEntity<List<ToolDefinition>> listTools() {
        logger.debug("Listing all MCP tools");
        return ResponseEntity.ok(toolExecutor.listTools());
    }

    @PostMapping("/call")
    public ResponseEntity<ToolCallResponse> callTool(@RequestBody ToolCallRequest request) {
        logger.debug("Calling tool: {}", request.getToolName());
        return ResponseEntity.ok(toolExecutor.execute(request));
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("version", "1.0.0-SNAPSHOT");
        health.put("tools", toolExecutor.listTools().size());
        return ResponseEntity.ok(health);
    }

    @GetMapping(value = "/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter sseConnection() {
        String emitterId = "sse-" + emitterIdCounter.incrementAndGet();
        logger.info("New SSE connection request: {}", emitterId);
        
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        emitters.put(emitterId, emitter);
        
        emitter.onCompletion(() -> {
            logger.info("SSE connection completed: {}", emitterId);
            emitters.remove(emitterId);
        });
        
        emitter.onTimeout(() -> {
            logger.warn("SSE connection timeout: {}", emitterId);
            emitters.remove(emitterId);
        });
        
        emitter.onError((e) -> {
            logger.error("SSE connection error: {}", emitterId, e);
            emitters.remove(emitterId);
        });
        
        try {
            emitter.send(SseEmitter.event()
                .name("endpoint")
                .data("/mcp/message"));
            logger.info("SSE endpoint event sent to: {}", emitterId);
        } catch (IOException e) {
            logger.error("Failed to send SSE endpoint event", e);
            emitters.remove(emitterId);
            return null;
        }
        
        return emitter;
    }

    @PostMapping(value = "/message", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> handleMessage(@RequestBody Map<String, Object> message) {
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
                return ResponseEntity.ok(message);
            }
            return ResponseEntity.badRequest().body(createError(-32600, "Invalid Request", null));
        }
        
        switch (method) {
            case "initialize":
                response.put("result", createInitResponse());
                break;
                
            case "notifications/initialized":
                logger.info("Client initialized");
                return ResponseEntity.ok().build();
                
            case "tools/list":
                response.put("result", createToolsListResponse());
                break;
                
            case "tools/call":
                try {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> params = (Map<String, Object>) message.get("params");
                    response.put("result", handleToolCall(params));
                } catch (Exception e) {
                    logger.error("Tool call failed", e);
                    response.put("error", createError(-32603, e.getMessage(), null));
                }
                break;
                
            case "ping":
                response.put("result", new HashMap<>());
                break;
                
            default:
                response.put("error", createError(-32601, "Method not found: " + method, null));
        }
        
        broadcastToAll(response);
        return ResponseEntity.ok(response);
    }

    private Map<String, Object> createError(int code, String message, Object data) {
        Map<String, Object> error = new HashMap<>();
        error.put("code", code);
        error.put("message", message);
        if (data != null) {
            error.put("data", data);
        }
        return error;
    }

    private Map<String, Object> createInitResponse() {
        Map<String, Object> serverInfo = new HashMap<>();
        serverInfo.put("name", "mcp-sample-server");
        serverInfo.put("version", "1.0.0-SNAPSHOT");
        
        Map<String, Object> capabilities = new HashMap<>();
        capabilities.put("tools", new HashMap<String, Boolean>() {{
            put("listChanged", true);
        }});
        
        Map<String, Object> result = new HashMap<>();
        result.put("protocolVersion", "2024-11-05");
        result.put("serverInfo", serverInfo);
        result.put("capabilities", capabilities);
        return result;
    }

    private Map<String, Object> createToolsListResponse() {
        List<ToolDefinition> tools = toolExecutor.listTools();
        List<Map<String, Object>> toolList = tools.stream()
            .map(this::convertToolDefinition)
            .toList();
        
        Map<String, Object> result = new HashMap<>();
        result.put("tools", toolList);
        return result;
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
        if (!required.isEmpty()) {
            inputSchema.put("required", required);
        }
        
        definition.put("inputSchema", inputSchema);
        return definition;
    }

    private Map<String, Object> handleToolCall(Map<String, Object> params) {
        String toolName = (String) params.get("name");
        @SuppressWarnings("unchecked")
        Map<String, Object> arguments = (Map<String, Object>) params.getOrDefault("arguments", new HashMap<>());
        
        logger.info("Executing tool: {} with args: {}", toolName, arguments);
        
        ToolCallRequest request = new ToolCallRequest();
        request.setToolName(toolName);
        request.setArguments(arguments);
        
        ToolCallResponse toolResponse = toolExecutor.execute(request);
        
        Map<String, Object> textContent = new HashMap<>();
        textContent.put("type", "text");
        textContent.put("text", toolResponse.getResult() != null ? 
            toolResponse.getResult().toString() : "");
        
        Map<String, Object> result = new HashMap<>();
        result.put("content", new Object[] { textContent });
        return result;
    }

    private String convertType(String javaType) {
        if (javaType == null) return "string";
        return switch (javaType.toLowerCase()) {
            case "int", "integer", "long" -> "integer";
            case "double", "float" -> "number";
            case "boolean" -> "boolean";
            default -> "string";
        };
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
