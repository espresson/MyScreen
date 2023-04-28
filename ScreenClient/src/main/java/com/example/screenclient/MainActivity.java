package com.example.screenclient;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.screenclient.Utils.SPUtils;


public class MainActivity extends Activity {
    private final static String TAG = MainActivity.class.getSimpleName();

    private EditText editText;

    private Button button;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //全屏显示，去掉title
        getWindow().setFlags(android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN, android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //避免从桌面启动程序后，会重新实例化入口类的activity
        if(!this.isTaskRoot()){ // 如果不是栈内第一个activity则直接关闭
            Intent intent= getIntent();
            if(intent !=null){
                String action=intent.getAction();
                if(intent.hasCategory(Intent.CATEGORY_LAUNCHER)&& Intent.ACTION_MAIN.equals(action)){
                    finish();
                    return;
                }
            }

        }

        editText = findViewById(R.id.et_input);
        button = findViewById(R.id.btn_connect);

        String saveIP = SPUtils.getString(this,"serverIP","192.168.1.13");
        editText.setText(saveIP);

        button.setOnClickListener(v -> {
            String ip = editText.getText().toString();
            if(ip == null || ip.length() < 11){
                Toast.makeText(this,"请输入正确的IP地址",Toast.LENGTH_LONG).show();
                return;
            }
            SPUtils.putString(this,"serverIP",ip);
            Intent intent = new Intent(MainActivity.this, ScreenClientActivity.class);
            intent.putExtra("ipAddress", ip);
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

    }
}