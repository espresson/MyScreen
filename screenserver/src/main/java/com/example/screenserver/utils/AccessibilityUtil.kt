package com.example.accessibilitytest.utils

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Context
import android.graphics.Path
import android.os.Build
import android.os.Handler
import android.provider.Settings
import android.provider.Settings.SettingNotFoundException
import android.text.TextUtils.SimpleStringSplitter
import androidx.annotation.RequiresApi
import com.example.screenserver.service.DYAccessibilityService


object AccessibilityUtil {

    fun isAccessibilitySettingsOn(mContext: Context): Boolean {
        var accessibilityEnabled = 0
        val service = mContext.packageName + "/" + DYAccessibilityService::class.java.canonicalName
        try {
            accessibilityEnabled = Settings.Secure.getInt(mContext.applicationContext.contentResolver,
                    Settings.Secure.ACCESSIBILITY_ENABLED)
        } catch (e: SettingNotFoundException) {
            e.printStackTrace()
        }
        val mStringColonSplitter = SimpleStringSplitter(':')
        if (accessibilityEnabled == 1) {
            val settingValue = Settings.Secure.getString(mContext.applicationContext.contentResolver,
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
            if (settingValue != null) {
                mStringColonSplitter.setString(settingValue)
                while (mStringColonSplitter.hasNext()) {
                    val accessibilityService = mStringColonSplitter.next()
                    if (accessibilityService.equals(service, ignoreCase = true)) {
                        return true
                    }
                }
            }
        }
        return false
    }

    /**
     * 通过AccessibilityService在屏幕上模拟手势
     * @param path 手势路径
     */
    @RequiresApi(Build.VERSION_CODES.N)
    fun AccessibilityService.gestureOnScreen(
            path: Path,
            startTime:Long = 0,
            duration:Long = 100,
            callback:AccessibilityService.GestureResultCallback,
            handler: Handler? = null
    ){
        val builder = GestureDescription.Builder()
        builder.addStroke(GestureDescription.StrokeDescription(path, startTime, duration))
        val gesture = builder.build()
        dispatchGesture(gesture, callback, handler)
    }

    /**
     * 通过AccessibilityService在屏幕上某个位置单击
     */
    @RequiresApi(Build.VERSION_CODES.N)
    fun AccessibilityService.clickOnScreen(
            x:Float,
            y:Float,
            callback:AccessibilityService.GestureResultCallback,
            handler: Handler? = null
    ){
        val p = Path()
        p.moveTo(x,y)
        gestureOnScreen(p,callback = callback,handler = handler)
    }

    //自动开启无障碍服务
    fun autoOpenAccessibilityService(context: Context){
        Settings.Secure.putString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
                "com.example.accessibilitytest/com.example.accessibilitytest.service.DYAccessibilityService"
        )//例子："com.xxx.xxxx/com.xxx.xxxx.service.MyAccessibilityService"
        Settings.Secure.putString(
                context.contentResolver,
                Settings.Secure.ACCESSIBILITY_ENABLED, "1"
        )
    }
    //自动关闭无障碍服务
    fun autoCloseAccessibilityService(context: Context){
        Settings.Secure.putString(
                context.contentResolver,
                Settings.Secure.ACCESSIBILITY_ENABLED,
                "com.example.accessibilitytest/com.example.accessibilitytest.service.DYAccessibilityService"
        )//例子："com.xxx.xxxx/com.xxx.xxxx.service.MyAccessibilityService"
        Settings.Secure.putString(
                context.contentResolver,
                Settings.Secure.ACCESSIBILITY_ENABLED, "0"
        )
    }
}