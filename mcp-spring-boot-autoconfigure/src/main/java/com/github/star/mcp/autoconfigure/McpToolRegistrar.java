
package com.github.star.mcp.autoconfigure;

import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.annotation.AnnotationUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.github.star.mcp.autoconfigure.annotation.McpTool;
import com.github.star.mcp.autoconfigure.annotation.McpToolParam;

public class McpToolRegistrar implements BeanPostProcessor {

    private static final Logger logger = LoggerFactory.getLogger(McpToolRegistrar.class);

    private final List<McpServerFeatures.SyncToolSpecification> toolSpecifications = new ArrayList<>();

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        Class<?> targetClass = AopUtils.getTargetClass(bean);
        McpTool classAnnotation = AnnotationUtils.findAnnotation(targetClass, McpTool.class);
        
        if (classAnnotation != null) {
            registerClassLevelTools(bean, targetClass);
        } else {
            registerMethodLevelTools(bean, targetClass);
        }
        
        return bean;
    }

    private void registerClassLevelTools(Object bean, Class<?> targetClass) {
        for (Method method : targetClass.getMethods()) {
            if (!isToolCandidate(method)) {
                continue;
            }

            McpTool methodAnnotation = AnnotationUtils.findAnnotation(method, McpTool.class);
            try {
                registerTool(bean, method, methodAnnotation, "class-level");
            } catch (Exception e) {
                logger.warn("Failed to register method as tool: {}.{}", targetClass.getSimpleName(), method.getName(), e);
            }
        }
    }

    private void registerMethodLevelTools(Object bean, Class<?> targetClass) {
        for (Method method : targetClass.getDeclaredMethods()) {
            McpTool methodAnnotation = AnnotationUtils.findAnnotation(method, McpTool.class);
            if (methodAnnotation == null) {
                continue;
            }

            try {
                registerTool(bean, method, methodAnnotation, "method-level");
            } catch (Exception e) {
                logger.error("Failed to register tool method: {}.{}", targetClass.getSimpleName(), method.getName(), e);
            }
        }
    }

    private boolean isToolCandidate(Method method) {
        return Modifier.isPublic(method.getModifiers())
            && method.getDeclaringClass() != Object.class
            && !method.isBridge()
            && !method.isSynthetic();
    }

    private void registerTool(Object bean, Method method, McpTool annotation, String source) throws Exception {
        String toolName = (annotation != null && !annotation.name().isEmpty()) ? annotation.name() : method.getName();
        String description = (annotation != null && !annotation.description().isEmpty())
            ? annotation.description()
            : "Tool: " + toolName;
        
        McpSchema.JsonSchema inputSchema = buildSchemaFromMethod(method);
        McpServerFeatures.SyncToolSpecification spec = createToolSpecification(
            toolName, description, inputSchema, bean, method);
        toolSpecifications.add(spec);
        logger.info("Registered {} tool: {}", source, toolName);
    }

    private McpSchema.JsonSchema buildSchemaFromMethod(Method method) {
        Map<String, Object> properties = new HashMap<>();
        List<String> required = new ArrayList<>();
        
        for (Parameter param : method.getParameters()) {
            McpToolParam paramAnnotation = param.getAnnotation(McpToolParam.class);
            
            String name = (paramAnnotation != null && !paramAnnotation.name().isEmpty()) 
                ? paramAnnotation.name() 
                : param.getName();
            
            String description = (paramAnnotation != null) ? paramAnnotation.description() : "";
            boolean required_flag = (paramAnnotation == null) || paramAnnotation.required();
            String type = (paramAnnotation != null && !paramAnnotation.type().isEmpty()) 
                ? paramAnnotation.type() 
                : mapJavaTypeToJsonSchemaType(param.getType());
            
            Map<String, String> paramSchema = new HashMap<>();
            paramSchema.put("type", type);
            if (!description.isEmpty()) {
                paramSchema.put("description", description);
            }
            
            properties.put(name, paramSchema);
            if (required_flag) {
                required.add(name);
            }
        }
        
        return new McpSchema.JsonSchema("object", properties, required, null, null, null);
    }

    private McpServerFeatures.SyncToolSpecification createToolSpecification(
            String name, String description, McpSchema.JsonSchema inputSchema, 
            Object bean, Method method) {
        
        return new McpServerFeatures.SyncToolSpecification(
            new McpSchema.Tool(name, description, null, inputSchema, null, null, null),
            (exchange, args) -> {
                try {
                    Object[] methodArgs = buildMethodArguments(method, args);
                    Object result = method.invoke(bean, methodArgs);
                    return new McpSchema.CallToolResult(
                        List.of(new McpSchema.TextContent(result != null ? result.toString() : "")),
                        false
                    );
                } catch (Exception e) {
                    return new McpSchema.CallToolResult(
                        List.of(new McpSchema.TextContent("Error: " + e.getMessage())),
                        true
                    );
                }
            }
        );
    }

    private Object[] buildMethodArguments(Method method, Map<String, Object> args) {
        Parameter[] params = method.getParameters();
        Object[] methodArgs = new Object[params.length];
        
        for (int i = 0; i < params.length; i++) {
            McpToolParam paramAnnotation = params[i].getAnnotation(McpToolParam.class);
            String paramName = (paramAnnotation != null && !paramAnnotation.name().isEmpty()) 
                ? paramAnnotation.name() 
                : params[i].getName();
            
            Object value = args.get(paramName);
            methodArgs[i] = convertValue(value, params[i].getType());
        }
        
        return methodArgs;
    }

    private Object convertValue(Object value, Class<?> targetType) {
        if (value == null) return null;
        
        if (targetType.isAssignableFrom(value.getClass())) {
            return value;
        }
        
        if (targetType == int.class || targetType == Integer.class) {
            return ((Number) value).intValue();
        } else if (targetType == long.class || targetType == Long.class) {
            return ((Number) value).longValue();
        } else if (targetType == double.class || targetType == Double.class) {
            return ((Number) value).doubleValue();
        } else if (targetType == float.class || targetType == Float.class) {
            return ((Number) value).floatValue();
        } else if (targetType == boolean.class || targetType == Boolean.class) {
            return Boolean.parseBoolean(value.toString());
        } else if (targetType == String.class) {
            return value.toString();
        }
        
        return value;
    }

    private String mapJavaTypeToJsonSchemaType(Class<?> javaType) {
        if (javaType == int.class || javaType == Integer.class || 
            javaType == long.class || javaType == Long.class) {
            return "integer";
        } else if (javaType == double.class || javaType == Double.class || 
                   javaType == float.class || javaType == Float.class) {
            return "number";
        } else if (javaType == boolean.class || javaType == Boolean.class) {
            return "boolean";
        } else {
            return "string";
        }
    }

    public List<McpServerFeatures.SyncToolSpecification> getToolSpecifications() {
        return toolSpecifications;
    }
}
