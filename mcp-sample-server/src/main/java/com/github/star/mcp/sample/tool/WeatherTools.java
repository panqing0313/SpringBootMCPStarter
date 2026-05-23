
package com.github.star.mcp.sample.tool;

import com.github.star.mcp.core.annotation.Tool;
import com.github.star.mcp.core.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Component
public class WeatherTools {

    private static final Map<String, String> WEATHER_DATA = new HashMap<>();
    
    static {
        WEATHER_DATA.put("北京", "晴天，温度 25°C");
        WEATHER_DATA.put("上海", "多云，温度 28°C");
        WEATHER_DATA.put("广州", "阴天，温度 32°C");
        WEATHER_DATA.put("深圳", "小雨，温度 30°C");
        WEATHER_DATA.put("杭州", "晴天，温度 26°C");
    }

    @Tool("获取指定城市的当前天气")
    public String getWeather(
            @ToolParam(value = "city", description = "城市名称") String city) {
        String weather = WEATHER_DATA.getOrDefault(city, "未知城市");
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        return String.format("[%s] %s的天气：%s", time, city, weather);
    }

    @Tool(name = "getWeatherDetail", description = "获取指定城市的详细天气信息")
    public Map<String, Object> getWeatherDetail(
            @ToolParam(value = "city", description = "城市名称", required = true) String city) {
        Map<String, Object> detail = new HashMap<>();
        detail.put("city", city);
        detail.put("temperature", 25 + (int) (Math.random() * 10));
        detail.put("humidity", 40 + (int) (Math.random() * 40));
        detail.put("windSpeed", 5 + (Math.random() * 15));
        detail.put("condition", WEATHER_DATA.getOrDefault(city, "晴"));
        detail.put("updateTime", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        return detail;
    }
}
