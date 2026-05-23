
package com.github.trae.mcp.core.annotation;

import java.lang.annotation.*;

@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ToolParam {

    String value() default "";

    String description() default "";

    boolean required() default true;

    String defaultValue() default "";
}
