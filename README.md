# Spring Boot MCP Starter

[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](https://github.com/star/mcp-spring-boot-starter/blob/main/LICENSE)
[![Maven Central](https://img.shields.io/maven-central/v/com.github.star/mcp-spring-boot-starter.svg)](https://search.maven.org/artifact/com.github.star/mcp-spring-boot-starter)

一个基于 **官方 MCP SDK** 的 Spring Boot Starter，让你通过注解快速将 Java 服务暴露为 MCP（Model Context Protocol）Tool，供 AI Agent（如 Trae、Claude Desktop 等）调用。

## 🎯 功能特性

- **官方 SDK**：基于 `io.modelcontextprotocol.sdk:mcp` 官方 SDK 实现，完全遵循 MCP 协议规范
- **STDIO 传输**：使用标准输入/输出与 MCP 客户端通信，兼容 Trae、Claude Desktop 等主流客户端
- **注解驱动**：使用 `@McpTool` 和 `@McpToolParam` 注解快速定义工具，无需手动注册
- **自动扫描**：Spring 启动时自动扫描并注册所有标记的工具
- **自动配置**：零配置开箱即用，只需添加依赖
- **Bean 兼容**：同时支持 Bean 方式定义工具（兼容 `McpServerFeatures.SyncToolSpecification`）

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

使用注解方式（推荐）：

```java
@Component
@McpTool
public class CalculatorTools {

    @McpTool(name = "add", description = "加法运算")
    public int add(
            @McpToolParam(name = "a", description = "第一个数", type = "integer") int a,
            @McpToolParam(name = "b", description = "第二个数", type = "integer") int b) {
        return a + b;
    }

    @McpTool(name = "getWeather", description = "获取指定城市的天气")
    public String getWeather(
            @McpToolParam(name = "city", description = "城市名称") String city) {
        return weatherService.query(city);
    }
}
```

### 3. 启动应用

```java
@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

### 4. 配置 MCP 客户端（以 Trae 为例）

在 Trae 的 MCP 配置文件中添加：

```json
{
  "mcpServers": {
    "my-mcp-server": {
      "command": "java",
      "args": [
        "--enable-native-access=ALL-UNNAMED",
        "-jar",
        "your-app.jar"
      ]
    }
  }
}
```

> **注意**：`--enable-native-access=ALL-UNNAMED` 是必须的 JVM 参数，用于避免 Tomcat JNI 加载警告。

## 📝 注解说明

### @McpTool

标记一个类或方法为 MCP Tool。

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `name` | String | 方法名 | 工具名称 |
| `description` | String | "" | 工具描述 |

**用法一：类级别注解**
类上添加 `@McpTool`，类中所有公共方法都会注册为工具。

**用法二：方法级别注解**
方法上添加 `@McpTool`，精确控制哪些方法作为工具暴露。

### @McpToolParam

标记方法参数为 Tool 参数。

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `name` | String | 参数名 | 参数名称 |
| `description` | String | "" | 参数描述 |
| `required` | boolean | true | 是否必填 |
| `type` | String | "string" | JSON Schema 类型（integer/number/string/boolean） |

## 📋 Bean 方式定义工具（高级用法）

如果需要更精细的控制，可以使用官方 SDK 的 Bean 方式：

```java
@Configuration
public class AdvancedToolConfig {

    @Bean
    public McpServerFeatures.SyncToolSpecification complexTool() {
        return new McpServerFeatures.SyncToolSpecification(
            McpSchema.Tool.builder()
                .name("complexTool")
                .description("复杂工具示例")
                .inputSchema(...)
                .build(),
            (exchange, args) -> {
                // 自定义处理逻辑
                return new McpSchema.CallToolResult(
                    List.of(new McpSchema.TextContent("result")),
                    false
                );
            }
        );
    }
}
```

## ⚙️ 配置项

| 配置项 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| `mcp.enabled` | boolean | true | 是否启用 MCP 功能 |
| `mcp.base-path` | String | `/mcp` | 路径前缀（保留字段） |

**示例配置（application.yml）：**

```yaml
mcp:
  enabled: true

spring:
  main:
    web-application-type: none  # 禁用Web服务器，使用纯STDIO模式
```

## 🏗️ 项目结构

```
mcp-spring-boot-starter/
├── mcp-spring-boot-autoconfigure/     # 自动配置
│   ├── annotation/
│   │   ├── McpTool.java              # @McpTool 注解
│   │   └── McpToolParam.java         # @McpToolParam 注解
│   ├── McpToolRegistrar.java         # 注解工具注册器
│   ├── McpAutoConfiguration.java     # 自动配置类
│   └── McpProperties.java            # 配置属性
├── mcp-spring-boot-starter/           # 聚合 Starter
└── mcp-sample-server/                 # 示例项目
    └── tool/
        ├── CalculatorTools.java       # 注解方式示例
        └── WeatherTools.java          # 注解方式示例
```

## 🚀 构建与运行

### 构建项目

```bash
cd SpringBootMCPStarter
mvn clean package -DskipTests
```

### 运行示例服务

```bash
cd mcp-sample-server/target
java --enable-native-access=ALL-UNNAMED -jar mcp-sample-server-1.0.0-SNAPSHOT.jar
```

### 测试 STDIO 通信

```bash
echo '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2024-11-05","capabilities":{},"clientInfo":{"name":"test","version":"1.0"}}}' | \
java --enable-native-access=ALL-UNNAMED -jar mcp-sample-server-1.0.0-SNAPSHOT.jar
```

## 🔧 依赖版本

- **Java**: 17+
- **Spring Boot**: 3.2.0
- **MCP SDK**: io.modelcontextprotocol.sdk:mcp 0.16.0
- **Maven**: 3.8+

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

**star**

- GitHub: [@star](https://github.com/star)

---

**让 Java 服务轻松接入 AI Agent 生态！** 🚀
