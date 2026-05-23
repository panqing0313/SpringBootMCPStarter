
# Spring Boot MCP Starter

[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](https://github.com/trae/mcp-spring-boot-starter/blob/main/LICENSE)
[![Maven Central](https://img.shields.io/maven-central/v/com.github.trae/mcp-spring-boot-starter.svg)](https://search.maven.org/artifact/com.github.trae/mcp-spring-boot-starter)
[![Build Status](https://github.com/trae/mcp-spring-boot-starter/actions/workflows/ci.yml/badge.svg)](https://github.com/trae/mcp-spring-boot-starter/actions)

一个 Spring Boot Starter，让 Java 开发者用最少的代码将自己的服务暴露为 MCP（Model Context Protocol）Tool，供 AI Agent 调用。

## 🎯 功能特性

- **注解驱动**：使用 `@Tool` 和 `@ToolParam` 注解快速定义工具
- **自动扫描**：Spring 启动时自动扫描并注册所有标记的工具
- **标准 Schema**：自动生成符合 MCP 标准的 Tool Schema（JSON Schema）
- **REST API**：提供 RESTful API 供外部调用
- **自动配置**：零配置开箱即用
- **类型转换**：自动处理参数类型转换
- **参数校验**：支持必填参数校验

## 📦 快速开始

### 1. 添加依赖

```xml
<dependency>
    <groupId>com.github.star</groupId>
    <artifactId>mcp-spring-boot-starter</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### 2. 定义工具

```java
@Component
public class WeatherTools {

    @Tool("获取指定城市的当前天气")
    public String getWeather(
            @ToolParam(value = "city", description = "城市名称") String city) {
        return weatherService.query(city);
    }
}
```

### 3. 启动应用

创建 Spring Boot 启动类：

```java
@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

## 🔌 API 接口

### 获取工具列表

```
GET /mcp/tools
```

**响应示例：**

```json
[
  {
    "name": "getWeather",
    "description": "获取指定城市的当前天气",
    "streaming": false,
    "parameters": [
      {
        "name": "city",
        "description": "城市名称",
        "required": true,
        "type": "String",
        "defaultValue": ""
      }
    ]
  }
]
```

### 调用工具

```
POST /mcp/call
```

**请求示例：**

```json
{
  "toolName": "getWeather",
  "arguments": {
    "city": "北京"
  }
}
```

**响应示例：**

```json
{
  "success": true,
  "result": "2024-01-15 10:30:00 北京的天气：晴天，温度 25°C",
  "error": null
}
```

### 获取工具 Schema

```
GET /mcp/schema
```

返回符合 JSON Schema 格式的工具定义，便于 AI Agent 理解和调用。

### 健康检查

```
GET /mcp/health
```

## 📝 注解说明

### @Tool

标记一个方法为 MCP Tool。

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `value` | String | "" | 工具描述（简写） |
| `name` | String | "" | 工具名称（默认使用方法名） |
| `description` | String | "" | 工具详细描述 |
| `streaming` | boolean | false | 是否流式输出 |

### @ToolParam

标记方法参数为 Tool 参数。

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `value` | String | "" | 参数名称（默认使用参数名） |
| `description` | String | "" | 参数描述 |
| `required` | boolean | true | 是否必填 |
| `defaultValue` | String | "" | 默认值 |

## ⚙️ 配置项

| 配置项 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| `mcp.enabled` | boolean | true | 是否启用 MCP 功能 |
| `mcp.base-path` | String | `/mcp` | API 基础路径 |
| `mcp.expose-api` | boolean | true | 是否暴露 REST API |
| `mcp.streaming-enabled` | boolean | true | 是否启用流式输出 |

**示例配置（application.yml）：**

```yaml
mcp:
  enabled: true
  base-path: /mcp
  expose-api: true
  streaming-enabled: true
```

## 🏗️ 项目结构

```
mcp-spring-boot-starter/
├── mcp-spring-boot-core/          # 核心注解与模型
│   ├── annotation/               # @Tool、@ToolParam 注解
│   ├── model/                    # 数据模型
│   ├── executor/                 # 执行器接口
│   └── registry/                 # 注册中心接口
├── mcp-spring-boot-autoconfigure/ # 自动配置
│   ├── McpProperties.java        # 配置属性
│   ├── McpAutoConfiguration.java # 自动配置类
│   ├── registry/                 # 默认注册中心实现
│   ├── scanner/                  # 工具扫描器
│   ├── executor/                 # 默认执行器实现
│   └── controller/               # REST API 控制器
├── mcp-spring-boot-starter/      # 聚合 Starter
└── mcp-sample-server/            # 示例项目
```

## 🚀 运行示例项目

```bash
cd mcp-sample-server
mvn spring-boot:run
```

启动后访问：
- 工具列表：http://localhost:8080/mcp/tools
- 健康检查：http://localhost:8080/mcp/health

## 🤝 贡献指南

欢迎提交 Issue 和 Pull Request！

### 开发环境要求

- JDK 17+
- Maven 3.8+

### 构建项目

```bash
mvn clean install
```

## 📄 许可证

Apache 2.0 License - 详见 [LICENSE](LICENSE)

## 🙋‍♂️ 作者

**trae**

- GitHub: [@star](https://github.com/trae)
- Email: star@github.com

---

**让 Java 服务轻松接入 AI Agent 生态！** 🚀
