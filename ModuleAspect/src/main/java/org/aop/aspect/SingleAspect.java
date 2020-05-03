package org.aop.aspect;

import android.util.Log;
import android.view.View;

import org.aop.annotation.SingleClick;
import org.aop.utils.AspectUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;

/**
 * File: SingleAspect.java
 * Author: yuzhuzhang
 * Create: 2020/5/3 8:30 PM
 * Description: TODO
 * -----------------------------------------------------------------
 * 2020/5/3 : Create SingleAspect.java (yuzhuzhang);
 * -----------------------------------------------------------------
 */
@Aspect
public class SingleAspect {
    private final static String TAG = "SingleAspect ";

    @Pointcut("execution(@org.aop.annotation.SingleClick * *(..))")
    public void onClickBehaviorMethod() {
    }

    @Around("onClickBehaviorMethod()")
    public void onClickBehaviorJoinPoint(ProceedingJoinPoint joinPoint) throws Throwable {
        // 获取签名方法
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        // 获取方法所属的类名
       // String className = methodSignature.getDeclaringType().getSimpleName();
        // 获取方法名
        //String methodName = methodSignature.getName();
        // 获取方法的注解值(需要统计的用户行为)
        SingleClick click = methodSignature.getMethod().getAnnotation(SingleClick.class);

        View view = null;
        for (Object o : joinPoint.getArgs()) {
            if (o instanceof View) {
                view = (View) o;
                break;
            }
        }
        if (view == null) {
            if (!AspectUtils.isDoubleClick(click.tag(), click.value())) {
                joinPoint.proceed();
            }else {
                Log.e(TAG, "onClickBehaviorJoinPoint: ---------->  SingleClick :"+ click.tag());
            }
        } else if (!AspectUtils.isDoubleClick(view.getId(), click.value())) {
            joinPoint.proceed();
        }

    }

    //https://www.jianshu.com/p/3de31e13f2fa


    /*  *//*方法一：*//*
    MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
    SingleClick singleClick = methodSignature.getMethod().getAnnotation(SingleClick.class);
        singleClick.clickIntervals();
    *//*方法二：*//*
    @Around("executionSingleClick() && @annotation(singleClick)")
    public void executionSingleClickAround(ProceedingJoinPoint joinPoint, SingleClick singleClick) throws Throwable {
        Log.d(TAG, "executionSingleClickAround:  "+"   clickIntervals  "+singleClick.clickIntervals());

    }*/

}
