package org.aop.aspect;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;

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
import java.util.Arrays;
import java.util.List;

@Aspect
public class PermissionAspect {

    private static final String TAG = "PermissionAspect";

    private final static int REQUEST_PERMISSION_CODE = 0xff01;
    private final static int REQUEST_SETTING_CALL_BACK_CODE = 0xff02;

    private RequestPermission permission;

    private ProceedingJoinPoint pointMethod;

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
            String methodName = methodSignature.getName();
            Class[] types = methodSignature.getParameterTypes();
            Method method = methodSignature.getMethod();
            permission = method.getAnnotation(RequestPermission.class);
            Activity hostActivity;
            Object target = joinPoint.getTarget();
            if (target instanceof Activity) {
                hostActivity = (Activity) target;
            } else {
                // Method getActivity = target.getClass().getDeclaredMethod("getActivity", null);
                Method[] methods = target.getClass().getMethods();
                Method getActivity = null;
                for (Method targetMethod : methods) {
                    if (targetMethod.getName().equals("getActivity")) {
                        getActivity = targetMethod;
                    }
                }
                hostActivity = (Activity) getActivity.invoke(target);
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

   // @Pointcut("execution(* *.onRequestPermissionsResult(..))")
    public void onActivityPermissionResult(){
    }




    /*-----------------------------------------------------------*/
    //这里没有执行 1 问题就是出在这里
    @Around("execution(* *.onRequestPermissionsResult(..))")
    public void onRequestPermissionsResult(JoinPoint joinPoint) throws Throwable {
        Log.e(TAG, "onRequestPermissionsResult---------------- > ");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Object[] objects = joinPoint.getArgs();
            int resultCode = (int) objects[0];   //resultCode
            if (resultCode == REQUEST_PERMISSION_CODE) {
                int[] grantResults = (int[]) objects[2];  //grantResults
                boolean isPermissionsGranted = true;
                for (int i = 0; i < grantResults.length; i++) {
                    if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                        isPermissionsGranted = false;
                        break;
                    }
                }
                if (isPermissionsGranted) {
                    permissionGranted();
                    return;
                } else {
                    doRefusePermissionAlertTask(joinPoint, objects);
                }
                return;
            }
        }
        permissionGranted();
    }

    /*-----------------------------------------------------------*/
    //这里没有执行 2 问题就是出在这里
    @Before("execution(*  *.onActivityResult(..))")
    public void onActivityResult(JoinPoint joinPoint) throws Throwable {
        Object[] objects = joinPoint.getArgs();
        int resultCode = (int) objects[0];
        if (resultCode == REQUEST_SETTING_CALL_BACK_CODE) {
            //TODO os system setting page back ， check and request again
            if (checkPermissionHandler((Activity) joinPoint.getTarget(), permission.value())) {
                permissionGranted();
            } else {
                permissionRefusedBySetting();
            }
        }
    }

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

    private void doRefusePermissionAlertTask(final JoinPoint joinPoint, Object[] objects) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            final Activity activity = (Activity) joinPoint.getTarget();
            for (String permission : Arrays.asList((String[]) objects[1])) {
                if (!activity.shouldShowRequestPermissionRationale(permission) && !handlePermissionForbidden()) {
                    int length = this.permission.tips().length;
                    StringBuilder messageBuilder = new StringBuilder();
                    if (length > 0) {
                        for (int i = 0; i < length; i++) {
                            messageBuilder.append(this.permission.tips()[i]);
                        }
                    }
                    AlertDialog alertDialog = new AlertDialog.Builder((Context) joinPoint.getTarget())
                            .setTitle(this.permission.title())
                            .setMessage(messageBuilder.toString())
                            .setNegativeButton(this.permission.negativeText(), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                    permissionRefused();
                                }
                            }).setPositiveButton(this.permission.positiveText(), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    startAppSettings((Activity) joinPoint.getTarget());
                                }
                            }).create();
                    alertDialog.setCancelable(false);
                    alertDialog.show();
                    if (!TextUtils.isEmpty(this.permission.negativeTextColor())) {
                        alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.parseColor(this.permission.negativeTextColor()));
                    }
                    if (!TextUtils.isEmpty(this.permission.positiveTextColor())) {
                        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.parseColor(this.permission.positiveTextColor()));
                    }
                    break;
                } else {
                    permissionRefused();
                }
            }
        }
    }

    private void permissionGranted() throws Throwable {
        if (pointMethod != null) {
            pointMethod.proceed();
            pointMethod = null;
            System.gc();
        }
    }

    private void permissionRefused() {
        Log.e(TAG, "permissionRefused: ---------------------->permissionRefused ");
       /* if (pointMethod != null && pointMethod.getTarget() != null
                && IPermissionRefuseListener.class.isAssignableFrom(pointMethod.getTarget().getClass())) {
            ((IPermissionRefuseListener) pointMethod.getTarget()).permissionRefused();
        }*/
    }

    private boolean handlePermissionForbidden() {
        Log.e(TAG, "handlePermissionForbidden: ---------------------->handlePermissionForbidden ");
       /* if (pointMethod != null && pointMethod.getTarget() != null
                && IPermissionRefuseListener.class.isAssignableFrom(pointMethod.getTarget().getClass())) {
            return ((IPermissionRefuseListener) pointMethod.getTarget()).permissionForbidden();
        }*/
        return false;
    }

    private void permissionRefusedBySetting() {
        Log.e(TAG, "permissionRefusedBySetting: ---------------------->permissionRefusedBySetting ");

        //TODO 这里需要通过注解回调到页面去中了
     /*   if (pointMethod != null && pointMethod.getTarget() != null
                && IPermissionRefuseListener.class.isAssignableFrom(pointMethod.getTarget().getClass())) {
            ((IPermissionRefuseListener) pointMethod.getTarget()).permissionRefusedBySetting();
        }*/
    }

    // 循环遍历查看权限数组
    private boolean checkPermissionHandler(Activity activity, String... permissions) {
        Class activityCompat = null;
        Method checkSelfPermissionMethod = null;
        Boolean isChecked = false;
        try {
            activityCompat = Class.forName("android.support.v4.app.ActivityCompat");
            checkSelfPermissionMethod = activityCompat.getMethod("checkSelfPermission", Context.class, String.class);
            isChecked = true;
            // } catch (ClassNotFoundException | NoSuchMethodException e) {
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
        if (!isChecked) {
            try {
                activityCompat = Class.forName("androidx.core.app.ActivityCompat");
                checkSelfPermissionMethod = activityCompat.getMethod("checkSelfPermission", Context.class, String.class);
                // } catch (ClassNotFoundException | NoSuchMethodException e) {
            } catch (Exception e) {
                Log.e(TAG, e.getMessage());
            }
        }
        if (activityCompat != null && checkSelfPermissionMethod != null) {
            List<String> permissionList = Arrays.asList(permissions);
            for (String permission : permissionList) {
                try {
                    int permissionStatus = (Integer) checkSelfPermissionMethod.invoke(activityCompat, activity, permission);
                    if (permissionStatus != PackageManager.PERMISSION_GRANTED) {
                        return false;
                    }
                    //} catch (IllegalAccessException | InvocationTargetException e) {
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return true;
    }

    //
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
