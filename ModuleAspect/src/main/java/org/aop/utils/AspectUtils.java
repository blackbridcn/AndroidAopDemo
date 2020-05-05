package org.aop.utils;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.RequiresApi;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

public class AspectUtils {
    private static final int ns = 1000 * 1000;
    private static final String TAG = "AspectUtils";

    public static String buildLogMessage(String methodName, long methodDuration) {

        if (methodDuration > 10 * ns) {
            return String.format("%s() take %d ms", methodName, methodDuration / ns);
        } else if (methodDuration > ns) {
            return String.format("%s() take %dms %dns", methodName, methodDuration / ns,
                    methodDuration % ns);
        } else {
            return String.format("%s() take %dns", methodName, methodDuration % ns);
        }
    }

    private static String lastClickTag;
    private static int lastClickViewId;
    private static long lastClickTime;

    public static boolean isDoubleClick(String tag, int interval) {
        long time = SystemClock.elapsedRealtime();
        if (TextUtils.equals(tag, lastClickTag)) {
            if (time - lastClickTime > interval) {
                lastClickTime = time;
                return false;
            }
            return true;
        } else {
            lastClickTime = time;
            lastClickTag = tag;
            return false;
        }
    }

    public static boolean isDoubleClick(int viewId, long interval) {
        long time = SystemClock.elapsedRealtime();
        if (viewId == lastClickViewId) {
            if (time - lastClickTime > interval) {
                lastClickTime = time;
                return false;
            }
            return true;
        } else {
            lastClickTime = time;
            lastClickViewId = viewId;
            return false;
        }
    }

    public static boolean isNotEmpty(CharSequence cs) {
        return cs != null && cs.length() > 0;
    }


    // 循环遍历查看权限数组
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private boolean checkPermissionHandler(Activity activity, String... permissions) {
        Class activityCompat = null;
        Method checkSelfPermissionMethod = null;
        Boolean isChecked = false;
        try {
            activityCompat = Class.forName("android.support.v4.app.ActivityCompat");
            checkSelfPermissionMethod = activityCompat.getMethod("checkSelfPermission", Context.class, String.class);
            isChecked = true;
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            Log.e(TAG, e.getMessage());
        }
        if (!isChecked) {
            try {
                activityCompat = Class.forName("androidx.core.app.ActivityCompat");
                checkSelfPermissionMethod = activityCompat.getMethod("checkSelfPermission", Context.class, String.class);
            } catch (ClassNotFoundException | NoSuchMethodException e) {
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
                } catch (IllegalAccessException | InvocationTargetException e) {
                    e.printStackTrace();
                }
            }
        }
        return true;
    }


    @NotNull
    public static Boolean isDoubleClick() {

        return null;
    }
}
