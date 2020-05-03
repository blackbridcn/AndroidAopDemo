package org.aop.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * File: SingleClick.java
 * Author: yuzhuzhang
 * Create: 2020/5/3 8:30 PM
 * Description: TODO
 * -----------------------------------------------------------------
 * 2020/5/3 : Create SingleClick.java (yuzhuzhang);
 * -----------------------------------------------------------------
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface SingleClick {

    int value() default 1000;

    String tag() default "";
}
