package org.aop.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * File: CheckLogin.java
 * Author: yuzhuzhang
 * Create: 2020/5/7 4:16 PM
 * Description: TODO
 * -----------------------------------------------------------------
 * 2020/5/7 : Create CheckLogin.java (yuzhuzhang);
 * -----------------------------------------------------------------
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface CheckLogin {

    boolean isSkip() default false;
}
