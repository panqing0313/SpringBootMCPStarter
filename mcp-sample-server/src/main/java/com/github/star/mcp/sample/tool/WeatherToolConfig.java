
package com.github.star.mcp.sample.tool;

import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Configuration
@Profile("bean-tools")
public class WeatherToolConfig {

    private static final Map<String, String> WEATHER_DATA = Map.of(
        "北京", "晴天，温度 25°C",
        "上海", "多云，温度 28°C",
        "广州", "阴天，温度 32°C",
        "深圳", "小雨，温度 30°C",
        "杭州", "晴天，温度 26°C"
    );

    @Bean
    public McpServerFeatures.SyncToolSpecification getWeatherTool() {
        McpSchema.JsonSchema inputSchema = new McpSchema.JsonSchema(
            "object",
            Map.of(
                "city", Map.of("type", "string", "description", "城市名称")
            ),
            List.of("city"),
            null,
            null,
            null
        );

        return new McpServerFeatures.SyncToolSpecification(
            new McpSchema.Tool("getWeather", "获取指定城市的当前天气", null, inputSchema, null, null, null),
            (exchange, args) -> {
                String city = (String) args.get("city");
                String weather = WEATHER_DATA.getOrDefault(city, "未知城市: " + city);
                String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                return new McpSchema.CallToolResult(
                    List.of(new McpSchema.TextContent(
                        String.format("[%s] %s的天气：%s", time, city, weather))),
                    false
                );
            }
        );
    }

    @Bean
    public McpServerFeatures.SyncToolSpecification getWeatherDetailTool() {
        McpSchema.JsonSchema inputSchema = new McpSchema.JsonSchema(
            "object",
            Map.of(
                "city", Map.of("type", "string", "description", "城市名称")
            ),
            List.of("city"),
            null,
            null,
            null
        );

        return new McpServerFeatures.SyncToolSpecification(
            new McpSchema.Tool("getWeatherDetail", "获取指定城市的详细天气信息", null, inputSchema, null, null, null),
            (exchange, args) -> {
                String city = (String) args.get("city");
                Map<String, Object> detail = new HashMap<>();
                detail.put("city", city);
                detail.put("temperature", 25 + (int) (Math.random() * 10));
                detail.put("humidity", 40 + (int) (Math.random() * 40));
                detail.put("windSpeed", 5 + (Math.random() * 15));
                detail.put("condition", "晴");
                detail.put("updateTime", LocalDateTime.now().toString());
                return new McpSchema.CallToolResult(
                    List.of(new McpSchema.TextContent(detail.toString())),
                    false
                );
            }
        );
    }
}
