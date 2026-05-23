
package com.github.trae.mcp.core.annotation;

import org.springframework.stereotype.Component;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Tool {

    String value() default "";

    String name() default "";

    String description() default "";

    boolean streaming() default false;
}
