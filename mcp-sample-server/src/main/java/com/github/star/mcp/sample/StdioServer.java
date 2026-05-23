
package com.github.star.mcp.sample;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.star.mcp.core.executor.ToolExecutor;
import com.github.star.mcp.core.model.ToolCallRequest;
import com.github.star.mcp.core.model.ToolCallResponse;
import com.github.star.mcp.core.model.ToolDefinition;
import com.github.star.mcp.core.model.ToolParameter;
import com.github.star.mcp.core.registry.ToolRegistry;
import com.github.star.mcp.sample.tool.CalculatorTools;
import com.github.star.mcp.sample.tool.WeatherTools;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StdioServer {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static ToolRegistry toolRegistry;
    private static ToolExecutor toolExecutor;
    private static boolean initialized = false;

    public static void main(String[] args) {
        try {
            initialize();
            System.err.println("MCP StdioServer started with " + toolExecutor.listTools().size() + " tools");
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
            PrintWriter out = new PrintWriter(System.out, true, StandardCharsets.UTF_8);
            
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }
                
                try {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> request = objectMapper.readValue(line, Map.class);
                    Map<String, Object> response = handleRequest(request);
                    
                    if (response != null) {
                        String jsonResponse = objectMapper.writeValueAsString(response);
                        out.println(jsonResponse);
                        out.flush();
                    }
                } catch (Exception e) {
                    Map<String, Object> errorResponse = createErrorResponse(-32700, "Parse error: " + e.getMessage(), null);
                    out.println(objectMapper.writeValueAsString(errorResponse));
                    out.flush();
                }
            }
        } catch (Exception e) {
            System.err.println("MCP StdioServer error: " + e.getMessage());
            e.printStackTrace(System.err);
        }
    }

    private static void initialize() {
        toolRegistry = new com.github.star.mcp.autoconfigure.registry.DefaultToolRegistry();
        toolExecutor = new com.github.star.mcp.autoconfigure.executor.DefaultToolExecutor(toolRegistry);
        
        registerTool(new CalculatorTools());
        registerTool(new WeatherTools());
    }

    private static void registerTool(Object bean) {
        Class<?> beanClass = bean.getClass();
        
        for (Method method : beanClass.getDeclaredMethods()) {
            com.github.star.mcp.core.annotation.Tool toolAnnotation = method.getAnnotation(com.github.star.mcp.core.annotation.Tool.class);
            if (toolAnnotation != null) {
                String toolName = getToolName(toolAnnotation, method);
                String description = getToolDescription(toolAnnotation);
                
                List<ToolParameter> parameters = new ArrayList<>();
                for (java.lang.reflect.Parameter parameter : method.getParameters()) {
                    com.github.star.mcp.core.annotation.ToolParam paramAnnotation = parameter.getAnnotation(com.github.star.mcp.core.annotation.ToolParam.class);
                    
                    String name = paramAnnotation != null && !paramAnnotation.value().isEmpty() 
                        ? paramAnnotation.value() 
                        : parameter.getName();
                    
                    String desc = paramAnnotation != null ? paramAnnotation.description() : "";
                    boolean required = paramAnnotation == null || paramAnnotation.required();
                    String defaultValue = paramAnnotation != null ? paramAnnotation.defaultValue() : "";
                    String type = parameter.getType().getSimpleName();
                    
                    parameters.add(new ToolParameter(name, desc, required, type, defaultValue));
                }
                
                ToolDefinition definition = new ToolDefinition(toolName, description, toolAnnotation.streaming(), parameters);
                toolRegistry.register(definition, bean, method);
                
                System.err.println("Registered tool: " + toolName);
            }
        }
    }

    private static String getToolName(com.github.star.mcp.core.annotation.Tool annotation, Method method) {
        if (!annotation.name().isEmpty()) {
            return annotation.name();
        }
        if (!annotation.value().isEmpty()) {
            return annotation.value();
        }
        return method.getName();
    }

    private static String getToolDescription(com.github.star.mcp.core.annotation.Tool annotation) {
        if (!annotation.description().isEmpty()) {
            return annotation.description();
        }
        return annotation.value();
    }

    private static Map<String, Object> handleRequest(Map<String, Object> request) {
        String method = (String) request.get("method");
        Object id = request.get("id");
        
        if (method == null) {
            return null;
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("jsonrpc", "2.0");
        if (id != null) {
            response.put("id", id);
        }
        
        switch (method) {
            case "initialize":
                response.put("result", createInitResult());
                break;
                
            case "notifications/initialized":
                initialized = true;
                return null;
                
            case "tools/list":
                response.put("result", createToolsListResult());
                break;
                
            case "tools/call":
                try {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> params = (Map<String, Object>) request.get("params");
                    response.put("result", handleToolCall(params));
                } catch (Exception e) {
                    response.put("error", createErrorObj(-32603, "Tool execution failed: " + e.getMessage()));
                }
                break;
                
            case "ping":
                response.put("result", new HashMap<>());
                break;
                
            default:
                response.put("error", createErrorObj(-32601, "Method not found: " + method));
        }
        
        return response;
    }

    private static Map<String, Object> createInitResult() {
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

    private static Map<String, Object> createToolsListResult() {
        List<ToolDefinition> tools = toolExecutor.listTools();
        List<Map<String, Object>> toolList = new ArrayList<>();
        
        for (ToolDefinition tool : tools) {
            Map<String, Object> definition = new HashMap<>();
            definition.put("name", tool.getName());
            definition.put("description", tool.getDescription());
            
            Map<String, Object> inputSchema = new HashMap<>();
            inputSchema.put("type", "object");
            
            Map<String, Object> properties = new HashMap<>();
            List<String> required = new ArrayList<>();
            
            for (ToolParameter param : tool.getParameters()) {
                Map<String, Object> paramSchema = new HashMap<>();
                paramSchema.put("type", convertType(param.getType()));
                paramSchema.put("description", param.getDescription());
                properties.put(param.getName(), paramSchema);
                if (param.isRequired()) {
                    required.add(param.getName());
                }
            }
            
            inputSchema.put("properties", properties);
            if (!required.isEmpty()) {
                inputSchema.put("required", required);
            }
            
            definition.put("inputSchema", inputSchema);
            toolList.add(definition);
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("tools", toolList);
        return result;
    }

    private static Map<String, Object> handleToolCall(Map<String, Object> params) {
        String toolName = (String) params.get("name");
        @SuppressWarnings("unchecked")
        Map<String, Object> arguments = (Map<String, Object>) params.getOrDefault("arguments", new HashMap<>());
        
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

    private static String convertType(String javaType) {
        if (javaType == null) return "string";
        return switch (javaType.toLowerCase()) {
            case "int", "integer", "long" -> "integer";
            case "double", "float" -> "number";
            case "boolean" -> "boolean";
            default -> "string";
        };
    }

    private static Map<String, Object> createErrorObj(int code, String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("code", code);
        error.put("message", message);
        return error;
    }

    private static Map<String, Object> createErrorResponse(int code, String message, Object data) {
        Map<String, Object> response = new HashMap<>();
        response.put("jsonrpc", "2.0");
        response.put("id", null);
        response.put("error", createErrorObj(code, message));
        if (data != null) {
            response.put("data", data);
        }
        return response;
    }
}
