package com.example.screenserver;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.widget.Toast;

import com.example.accessibilitytest.utils.AccessibilityUtil;
import com.example.screenserver.service.DYAccessibilityService;
import com.example.screenserver.service.PushService;

public class MainActivity extends AppCompatActivity {
    private final String TAG = "MainActivity";
    public static final int REQUEST_CODE = 1;

    /**
     * 录屏的 manger
     */
    private MediaProjectionManager mProjectionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // 避免从桌面启动程序后，会重新实例化入口类的activity
        if (!this.isTaskRoot()) { // 如果不是栈内第一个activity则直接关闭
            Intent intent = getIntent();
            if (intent != null) {
                String action = intent.getAction();
                if (intent.hasCategory(Intent.CATEGORY_LAUNCHER) && Intent.ACTION_MAIN.equals(action)) {
                    finish();
                    return;
                }
            }
        }

        mProjectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        startService(new Intent(this, DYAccessibilityService.class));
    }

    @Override
    protected void onResume() {
        super.onResume();
//        AccessibilityUtil.INSTANCE.autoOpenAccessibilityService(getApplicationContext());
//        startLive();
        if(AccessibilityUtil.INSTANCE.isAccessibilitySettingsOn(this)){
            startLive();
        }else {
            showConfirmDialog("需要开启无障碍权限。", (dialog, which) -> startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        }
    }

    public void startLive() {
        // 请求录屏权限
        Intent intent = mProjectionManager.createScreenCaptureIntent();
        startActivityForResult(intent, REQUEST_CODE);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE && resultCode == Activity.RESULT_OK) {

            Intent service = new Intent(MainActivity.this, PushService.class);
            service.putExtra("code", resultCode);
            service.putExtra("data", data);
            startService(service);
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    startActivity(new Intent(MainActivity.this, StartActivity.class));
                    MainActivity.this.finish();
                }
            },1);
        }else{
            Toast.makeText(this,"请打开录屏权限",Toast.LENGTH_SHORT).show();
        }
    }

    private void showConfirmDialog(String msg, DialogInterface.OnClickListener listener){
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setMessage(msg)
                .setPositiveButton("确定",listener)
                .setNegativeButton("取消",null)
                .show();
    }
}