
package com.github.star.mcp.sample.tool;

import com.github.star.mcp.autoconfigure.annotation.McpTool;
import com.github.star.mcp.autoconfigure.annotation.McpToolParam;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Component
@McpTool
public class WeatherTools {

    private static final Map<String, String> WEATHER_DATA = Map.of(
        "北京", "晴天，温度 25°C",
        "上海", "多云，温度 28°C",
        "广州", "阴天，温度 32°C",
        "深圳", "小雨，温度 30°C",
        "杭州", "晴天，温度 26°C"
    );

    @McpTool(name = "getWeather", description = "获取指定城市的当前天气")
    public String getWeather(
            @McpToolParam(name = "city", description = "城市名称") String city) {
        String weather = WEATHER_DATA.getOrDefault(city, "未知城市: " + city);
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        return String.format("[%s] %s的天气：%s", time, city, weather);
    }

    @McpTool(name = "getWeatherDetail", description = "获取指定城市的详细天气信息")
    public String getWeatherDetail(
            @McpToolParam(name = "city", description = "城市名称") String city) {
        Map<String, Object> detail = new HashMap<>();
        detail.put("city", city);
        detail.put("temperature", 25 + (int) (Math.random() * 10));
        detail.put("humidity", 40 + (int) (Math.random() * 40));
        detail.put("windSpeed", 5 + (Math.random() * 15));
        detail.put("condition", "晴");
        detail.put("updateTime", LocalDateTime.now().toString());
        return detail.toString();
    }
}
