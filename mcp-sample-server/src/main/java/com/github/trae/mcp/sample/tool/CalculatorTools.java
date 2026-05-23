
package com.github.trae.mcp.sample.tool;

import com.github.trae.mcp.core.annotation.Tool;
import com.github.trae.mcp.core.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
public class CalculatorTools {

    @Tool(name = "add", description = "加法运算")
    public int add(
            @ToolParam(value = "a", description = "第一个数") int a,
            @ToolParam(value = "b", description = "第二个数") int b) {
        return a + b;
    }

    @Tool(name = "subtract", description = "减法运算")
    public int subtract(
            @ToolParam(value = "a", description = "被减数") int a,
            @ToolParam(value = "b", description = "减数") int b) {
        return a - b;
    }

    @Tool(name = "multiply", description = "乘法运算")
    public int multiply(
            @ToolParam(value = "a", description = "第一个因数") int a,
            @ToolParam(value = "b", description = "第二个因数") int b) {
        return a * b;
    }

    @Tool(name = "divide", description = "除法运算")
    public double divide(
            @ToolParam(value = "numerator", description = "被除数") int numerator,
            @ToolParam(value = "denominator", description = "除数", required = true) int denominator) {
        if (denominator == 0) {
            throw new IllegalArgumentException("除数不能为零");
        }
        return (double) numerator / denominator;
    }
}
