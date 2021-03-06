package org.aop.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface RequestPermission {

    String[] value();

    String negativeText() default "取消";

    String negativeTextColor() default "";

    String positiveText() default "去设置";

    String positiveTextColor() default "";

    String title() default "提示";

    String[] tips() default {"当前操作缺少必要的权限。\n请点击\"设置\"-\"权限\"打开所需权限。"};

    //用户拒绝授权后 是否通过 @PermissionDenied 注解callback
    boolean handleForbidden() default true;

    //用户拒绝并且禁止再弹框申请权限时是否提示跳转系统设置页面请求再次授权
    boolean toSettingAgain() default true;

}
