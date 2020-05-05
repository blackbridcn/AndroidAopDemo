package com.aop.aspect

import android.util.Log
import android.view.View
import com.moduleaspect.BuildConfig
import org.aop.annotation.SingleClick
import org.aop.aspect.SingleAspect
import org.aop.utils.AspectUtils
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Pointcut
import org.aspectj.lang.reflect.MethodSignature
import java.util.*


/**
 * File: SingleClickAspect.java
 * Author: yuzhuzhang
 * Create: 2020/5/5 10:23 PM
 * Description: TODO
 * -----------------------------------------------------------------
 * 2020/5/5 : Create SingleClickAspect.java (yuzhuzhang);
 * -----------------------------------------------------------------
 */
@Aspect
class SingleClickAspect {
    @Pointcut("execution(@com.aop.annotation.SingleClick * *(..))")//方法切入点
    fun methodAnnotated() {

    }

    /**
     * joinPoint.proceed() 执行注解所标识的代码
     * @After 可以在方法前插入代码
     * @Before 可以在方法后插入代码
     * @Around 可以在方法前后各插入代码
     */
    @Around("methodAnnotated()")
    @Throws(Throwable::class)
    fun aroundJoinPoint(joinPoint: ProceedingJoinPoint) {

        // 获取签名方法
        val methodSignature = joinPoint.signature as MethodSignature
        // 获取方法所属的类名
        // String className = methodSignature.getDeclaringType().getSimpleName();
        // 获取方法名
        //String methodName = methodSignature.getName();
        // 获取方法的注解值(需要统计的用户行为)
        val click = methodSignature.method.getAnnotation(SingleClick::class.java)


        var view: View? = null
        for (o in joinPoint.args) {
            if (o is View) {
                view = o
                break
            }
        }
        if (view == null) {
            if (!AspectUtils.isDoubleClick(click.tag, click.value)) {
                joinPoint.proceed()
            } else {
                Log.e("TAG", "onClickBehaviorJoinPoint: ---------->  SingleClick :" + click.tag)
            }
        } else if (!AspectUtils.isDoubleClick(view.id, click.value.toLong())) {
            joinPoint.proceed()
        }

    }

}