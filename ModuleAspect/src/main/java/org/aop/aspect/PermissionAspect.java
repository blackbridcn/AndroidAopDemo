package org.aop.aspect;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;

import org.aop.annotation.PermissionDenied;
import org.aop.annotation.RequestPermission;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

@Aspect
public class PermissionAspect {

    private static final String TAG = "PermissionAspect";

    private final static int REQUEST_PERMISSION_CODE = 0xff01;
    private final static int REQUEST_SETTING_CALL_BACK_CODE = 0xff02;

    private RequestPermission permission;

    private ProceedingJoinPoint pointMethod;

    private boolean handleBidden, settingResuestAgain;

    private Method deniedMethod;
    private String[] deniedPerMisArg;

    Object hostTarget;

    //this(Type) : 判断该JoinPoint所在的类是否是Type类型
    @Pointcut("this(android.app.Activity)")
    public void isActivity() {
    }

    @Pointcut("this(androidx.fragment.app.Fragment) || this(android.support.v4.app.Fragment) || this(android.app.Fragment)")
    public void isFragment() {
    }

    @Pointcut("execution(@org.aop.annotation.RequestPermission * *(..))")
    public void isPermissionAnnotation() {
    }


    @Around("(isActivity()||isFragment()) && isPermissionAnnotation()")
    public void doRequsetPermissionAspectPoint(ProceedingJoinPoint joinPoint) throws Throwable {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
            Method method = methodSignature.getMethod();
            permission = method.getAnnotation(RequestPermission.class);
            handleBidden = permission.handleForbidden();
            settingResuestAgain = permission.toSettingAgain();
            Activity hostActivity;
            hostTarget = joinPoint.getTarget();
            if (hostTarget instanceof Activity) {
                hostActivity = (Activity) hostTarget;
            } else {
                Method[] methods = hostTarget.getClass().getMethods();
                PermissionDenied deniedAnn;
                Method getActivity = null;
                for (Method targetMethod : methods) {
                    if (targetMethod.getName().equals("getActivity")) {
                        getActivity = targetMethod;
                    } else {
                        deniedAnn = targetMethod.getAnnotation(PermissionDenied.class);
                        if (deniedAnn != null) {
                            this.deniedMethod = targetMethod;
                            this.deniedPerMisArg = deniedAnn.value();
                        }
                    }
                }
                hostActivity = (Activity) getActivity.invoke(hostTarget);
            }

            if (checkPermissionHandler(hostActivity, permission.value())) {
                //TODO already Granted Permission , just do task code
                joinPoint.proceed();
            } else {
                pointMethod = joinPoint;
                //TODO request  permission task
                // queryPermissions(hostActivity, permission.value());
                doRequsetUserPermissionTask(hostActivity, permission.value());
            }

        } else {
            joinPoint.proceed();
        }
    }

    @Pointcut("execution(* *.onRequestPermissionsResult(..))")
    public void onActivityPermissionResult() {
    }


    @Around("onActivityPermissionResult()")
    public void onRequestPermissionsResult(final ProceedingJoinPoint joinPoint) throws Throwable {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Object[] objects = joinPoint.getArgs();
            int resultCode = (int) objects[0];   //resultCode
            if (resultCode == REQUEST_PERMISSION_CODE) {
                int[] grantResults = (int[]) objects[2];  //grantResults
                String[] permissions = (String[]) objects[1];
                List<String> deninePermissions = null;
                boolean isPermissionsGranted = true;
                int len = grantResults.length;
                for (int i = 0; i < len; i++) {
                    if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                        isPermissionsGranted = false;
                        if (deninePermissions == null) {
                            deninePermissions = new ArrayList<>(len);
                        }
                        deninePermissions.add(permissions[i]);
                    }
                }
                if (isPermissionsGranted) {
                    permissionGranted();
                } else {
                    doRefusePermissionTask(joinPoint, deninePermissions);
                }
            }
        }
        joinPoint.proceed();
    }

    /*-----------------------------------------------------------*/
    //这里没有执行 2 问题就是出在这里
    @Before("execution(* *.onActivityResult(..))")
    public void onActivityResult(JoinPoint joinPoint) throws Throwable {
        Object[] objects = joinPoint.getArgs();
        int resultCode = (int) objects[0];
        if (resultCode == REQUEST_SETTING_CALL_BACK_CODE) {
            Object target = joinPoint.getTarget();
            //TODO os system setting page back ， check and request again

            Class activityCompat = null;
            Method checkSelfPermissionMethod = null;
            boolean isChecked = false;
            try {
                activityCompat = Class.forName("android.support.v4.app.ActivityCompat");
                checkSelfPermissionMethod = activityCompat.getMethod("checkSelfPermission", Context.class, String.class);
                isChecked = true;
            } catch (Exception e) {
                Log.e(TAG, e.getMessage());
            }
            if (!isChecked) {
                try {
                    activityCompat = Class.forName("androidx.core.app.ActivityCompat");
                    checkSelfPermissionMethod = activityCompat.getMethod("checkSelfPermission", Context.class, String.class);
                } catch (Exception e) {
                    Log.e(TAG, e.getMessage());
                }
            }
            if (activityCompat != null && checkSelfPermissionMethod != null) {
                String permisItem;
                String[] permissions = permission.value();
                int len = permissions.length;
                for (int i = 0; i < permission.value().length; i++) {
                    permisItem = permissions[i];
                    int permissionStatus = (Integer) checkSelfPermissionMethod.invoke(activityCompat, (Activity) target, permisItem);
                    if (permissionStatus == PackageManager.PERMISSION_GRANTED) {
                        if (i == len - 1) {//全部权限得到授权时 才 callback
                            permissionGranted();
                        }
                    } else {
                        doRefusedCallbackTask(permisItem);
                    }
                }
            }
        }
    }

    //call 申请权限task
    private void doRequsetUserPermissionTask(Activity activity, String... userpermission) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            //TODO  Reflects get  ActivityCompat obejct and  call  requestPermissions method requset userpermission
            try {
                Class activityCompat = Class.forName("android.support.v4.app.ActivityCompat");
                Method requestPermissions = activityCompat.getDeclaredMethod("requestPermissions", Activity.class, String[].class, int.class);
                requestPermissions.invoke(activityCompat, activity, userpermission, REQUEST_PERMISSION_CODE);
                return;
            } catch (InvocationTargetException | NoSuchMethodException | IllegalAccessException | ClassNotFoundException e) {
                Log.e(TAG, "support v4 ActivityCompat requestPermissions Exception:" + e.getMessage());
            }
            try {
                Class activityCompat = Class.forName("androidx.core.app.ActivityCompat");
                Method requestPermissions = activityCompat.getDeclaredMethod("requestPermissions", Activity.class, String[].class, int.class);
                requestPermissions.invoke(activityCompat, activity, userpermission, REQUEST_PERMISSION_CODE);
            } catch (InvocationTargetException | NoSuchMethodException | IllegalAccessException | ClassNotFoundException e) {
                Log.e(TAG, "androidx.core.app.ActivityCompat requestPermissions Exception:" + e.getMessage());
            }
        }
    }

    //拒绝权限后的弹框 请求是否跳转到设置页面去打开权限
    private void doRefusePermissionTask(final JoinPoint joinPoint, List<String> denine) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            final Object target = joinPoint.getTarget();
            final Activity activity = (Activity) target;
            for (String deninePer : denine) {
                Log.e(TAG, "doRefusePermissionAlertTask: " + deninePer);
                Log.e(TAG, "doRefusePermissionAlertTask: " + activity.shouldShowRequestPermissionRationale(deninePer));
                Log.e(TAG, "doRefusePermissionAlertTask: " + isHandleForbidden(deninePer));
                if (isHandleForbidden(deninePer)) {
                    if (!activity.shouldShowRequestPermissionRationale(deninePer)) {
                        if (isGoSettingPageRequsetAgain()) {
                            int length = this.permission.tips().length;
                            StringBuilder messageBuilder = new StringBuilder();
                            if (length > 0) {
                                for (int i = 0; i < length; i++) {
                                    messageBuilder.append(this.permission.tips()[i]);
                                }
                            }
                            onSettingAlertDialog(activity, messageBuilder.toString(), deninePer);
                        } else {
                            doRefusedCallbackTask(deninePer);
                        }
                        break;
                    } else {
                        doRefusedCallbackTask(deninePer);
                    }
                }
            }
        }
    }

    private void onSettingAlertDialog(Activity mActivity, String dialogMsg, String permission) {
        AlertDialog alertDialog = new AlertDialog.Builder(mActivity)
                .setTitle(this.permission.title())
                .setMessage(dialogMsg)
                .setNegativeButton(this.permission.negativeText(), (dialog, which) -> {
                    dialog.dismiss();
                    doRefusedCallbackTask(permission);
                }).setPositiveButton(this.permission.positiveText(), (dialog, which) -> {
                    dialog.dismiss();
                    startAppSettings(mActivity);
                }).create();
        alertDialog.setCancelable(false);
        alertDialog.show();
        if (!TextUtils.isEmpty(this.permission.negativeTextColor())) {
            alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.parseColor(this.permission.negativeTextColor()));
        }
        if (!TextUtils.isEmpty(this.permission.positiveTextColor())) {
            alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.parseColor(this.permission.positiveTextColor()));
        }
    }

    //只用所用权限都被授权才 callback
    private void permissionGranted() throws Throwable {
        if (pointMethod != null) {
            pointMethod.proceed();
            pointMethod = null;
            System.gc();
        }
    }

    private void doRefusedCallbackTask(String denPermission) {
        if (this.deniedMethod != null && hostTarget != null) {
            try {
                if (deniedMethod.getGenericParameterTypes().length == 1) {
                    this.deniedMethod.invoke(hostTarget, denPermission);
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        }
    }

    private boolean isHandleForbidden(String permission) {
        if (deniedMethod != null && handleBidden) {
            //如果是有注解限制callback 那些权限则进行判断否则直接callback
            if (deniedPerMisArg != null && deniedPerMisArg.length > 0) {
                for (int i = deniedPerMisArg.length - 1; i >= 0; i--) {
                    if (TextUtils.equals(permission, deniedPerMisArg[i])) {
                        return true;
                    }
                }
            } else {
                return true;
            }
        }
        return false;
    }


    private boolean isGoSettingPageRequsetAgain() {
        return settingResuestAgain;
    }


    // 循环遍历查看权限数组 如果申请过权限并且已经授权了返回 true
    private boolean checkPermissionHandler(Activity activity, String... permissions) {
        Class activityCompat = null;
        Method checkSelfPermissionMethod = null;
        boolean isChecked = false;
        try {
            activityCompat = Class.forName("android.support.v4.app.ActivityCompat");
            checkSelfPermissionMethod = activityCompat.getMethod("checkSelfPermission", Context.class, String.class);
            isChecked = true;
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
        if (!isChecked) {
            try {
                activityCompat = Class.forName("androidx.core.app.ActivityCompat");
                checkSelfPermissionMethod = activityCompat.getMethod("checkSelfPermission", Context.class, String.class);
            } catch (Exception e) {
                Log.e(TAG, e.getMessage());
            }
        }
        if (activityCompat != null && checkSelfPermissionMethod != null) {
            String[] permissionList = permissions;
            for (String permission : permissionList) {
                try {
                    int permissionStatus = (Integer) checkSelfPermissionMethod.invoke(activityCompat, activity, permission);
                    if (permissionStatus != PackageManager.PERMISSION_GRANTED) {
                        return false;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return true;
    }


    private void startAppSettings(Activity activity) {
        Intent intent;
        try {
            intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.parse("package:" + activity.getPackageName()));
            activity.startActivityForResult(intent, REQUEST_SETTING_CALL_BACK_CODE);
        } catch (ActivityNotFoundException e) {
            intent = new Intent(Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS);
            activity.startActivityForResult(intent, REQUEST_SETTING_CALL_BACK_CODE);
        }
    }

}
