package com.tran.base;

import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

/**
 * File: BaseActivity.java
 * Author: yuzhuzhang
 * Create: 2020/5/4 12:08 PM
 * Description: TODO
 * -----------------------------------------------------------------
 * 2020/5/4 : Create BaseActivity.java (yuzhuzhang);
 * -----------------------------------------------------------------
 */
public class BaseActivity extends AppCompatActivity {
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log.e("TAG", "onRequestPermissionsResult: ------------------>  " );
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.e("TAG", "onActivityResult: ------------------>  " );
    }
}
