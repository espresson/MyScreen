package com.example.screenserver.utils;

import android.graphics.Bitmap;
import android.util.Base64;

import java.io.ByteArrayOutputStream;
import java.io.IOException;


public class BitmapUtil {

    /**
     * 图片先压缩到不超过350kb，再转成base64码
     * @param bitmap
     * @return
     */
    public synchronized static String bitmapToBase64(Bitmap bitmap) {
        String result = null;
        ByteArrayOutputStream baos = null;
        try {
            if (bitmap != null) {
                baos = new ByteArrayOutputStream();
                int options = 60;
                bitmap.compress(Bitmap.CompressFormat.JPEG, options, baos);// 质量压缩方法，这里100表示不压缩，把压缩后的数据存放到baos中
                while (baos.toByteArray().length/1024 > 500) { // 循环判断如果压缩后图片是否大于500kb,大于继续压缩
                    baos.reset(); // 重置baos即清空baos
                    bitmap.compress(Bitmap.CompressFormat.PNG, options, baos);// 这里压缩options%，把压缩后的数据存放到baos中
                    options -= 20;// 每次都减少20
                }
                LogUtils.d("DYAccessibilityService",String.format("图片压缩后大小%skb",baos.toByteArray().length/1024));

                baos.flush();
                baos.close();
                byte[] bitmapBytes = baos.toByteArray();
                result = Base64.encodeToString(bitmapBytes, Base64.DEFAULT);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (baos != null) {
                    baos.flush();
                    baos.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return result;
    }


}
