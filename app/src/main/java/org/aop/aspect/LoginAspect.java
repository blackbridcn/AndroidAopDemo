package org.aop.aspect;

import android.app.Activity;
import android.util.Log;

import androidx.fragment.app.Fragment;

import org.aop.annotation.CheckLogin;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;

import java.lang.reflect.Method;

/**
 * File: LoginAspect.java
 * Author: yuzhuzhang
 * Create: 2020/5/7 4:17 PM
 * Description: TODO
 * -----------------------------------------------------------------
 * 2020/5/7 : Create LoginAspect.java (yuzhuzhang);
 * -----------------------------------------------------------------
 */
@Aspect
public class LoginAspect {

    private static final String TAG = "LoginAspect";
    /**
     * 定义切点，标记切点为所有被@SingleClick注解的方法
     * 注意：这里com.util.click.SingleClick是你自己项目中SingleClick这个类的全路径
     * 注意：这里的 * * 表示任意方法
     * （..）表示任意参数
     */
    @Pointcut("execution(@org.aop.annotation.CheckLogin * *(..))")
    public void checkLogin() {
    }

    @Around("checkLogin()")
    public void checkLoginPoint(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
        //1.获取函数的签名信息，获取方法信息
        MethodSignature signature = (MethodSignature) proceedingJoinPoint.getSignature();
        Method method = signature.getMethod();
        //2.检查是否存在我们定义的CheckLogin注解
        CheckLogin annotation = method.getAnnotation(CheckLogin.class);
        Log.i(TAG, " 这里不需要检查登录状态~~~~~~");
        assert annotation != null;
        if (annotation.isSkip()) {//判断是要跳过检查
            //LogUtils.i(TAG, "isSkip =true 这里不需要检查登录状态~~~~~~");
            Log.i(TAG, "isSkip =true 这里不需要检查登录状态~~~~~~");
            proceedingJoinPoint.proceed();
        } else {
            if (true/*ModuleDataManager.getInstance().isLogin()*/) {
                proceedingJoinPoint.proceed();
               // LogUtils.i(TAG, "您已经登录过了~~~~");
               Log.i(TAG, "您已经登录过了~~~~");
            } else {
                //拦截的实体类
                Object target = proceedingJoinPoint.getTarget();
                Log.i(TAG, "isSkip =true 这里不需要检查登录状态~~~~~~"+target.getClass().getSimpleName());
                if (target != null) {
                    //LogUtils.i(TAG, "这里还未登陆准备跳转到登陆页了~~~~ " + target.getClass().getSimpleName());
                    Log.i(TAG, "这里还未登陆准备跳转到登陆页了~~~~ " + target.getClass().getSimpleName());
                    if (target instanceof Activity) {
                        //LoginHomeActivity.gotoLoginPage((Activity) target);
                    } else if (target instanceof Fragment) {
                        //LoginHomeActivity.goToLoginPage(((Fragment) target).getContext());
                    } else {
                       // LoginHomeActivity.gotoNewTaskLoginPage(BaseApplication.getContext(), "您还没有登录哦！");
                    }
                }
            }
        }
    }

}
