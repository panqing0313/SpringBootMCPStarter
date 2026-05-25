
package com.github.star.mcp.sample.tool;

import com.github.star.mcp.autoconfigure.annotation.McpTool;
import com.github.star.mcp.autoconfigure.annotation.McpToolParam;
import org.springframework.stereotype.Component;

@Component
@McpTool
public class CalculatorTools {

    @McpTool(name = "add1", description = "加法运算")
    public int add(
            @McpToolParam(name = "a", description = "第一个数", type = "integer") int a,
            @McpToolParam(name = "b", description = "第二个数", type = "integer") int b) {
        return a + b;
    }

    @McpTool(name = "subtract", description = "减法运算")
    public int subtract(
            @McpToolParam(name = "a", description = "被减数", type = "integer") int a,
            @McpToolParam(name = "b", description = "减数", type = "integer") int b) {
        return a - b;
    }

    @McpTool(name = "multiply", description = "乘法运算")
    public int multiply(
            @McpToolParam(name = "a", description = "第一个因数", type = "integer") int a,
            @McpToolParam(name = "b", description = "第二个因数", type = "integer") int b) {
        return a * b;
    }

    @McpTool(name = "divide", description = "除法运算")
    public double divide(
            @McpToolParam(name = "numerator", description = "被除数", type = "integer") int numerator,
            @McpToolParam(name = "denominator", description = "除数", type = "integer") int denominator) {
        if (denominator == 0) {
            throw new IllegalArgumentException("除数不能为零");
        }
        return (double) numerator / denominator;
    }
}
