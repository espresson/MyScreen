package com.example.screenserver.live;

import static android.media.MediaFormat.KEY_BIT_RATE;
import static android.media.MediaFormat.KEY_FRAME_RATE;
import static android.media.MediaFormat.KEY_I_FRAME_INTERVAL;


import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.projection.MediaProjection;
import android.view.Surface;

import com.example.screenserver.Application;
import com.example.screenserver.utils.LogUtils;
import com.example.screenserver.utils.ScreenUtil;
import com.example.screenserver.websocket.SocketService;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class CodecH264 extends Thread {

    private static final String TAG = "CodecLiveH264";
    private MediaCodec mMediaCodec;

    private static  int WIDTH = 1000;
    private static  int HEIGHT = 500;
    /**
     * 用来录屏
     */
    private final MediaProjection mMediaProjection;
    VirtualDisplay virtualDisplay;
    private final SocketService mSocket;

    public CodecH264(SocketService socketLive, MediaProjection mediaProjection) {
        this.mMediaProjection = mediaProjection;
        this.mSocket = socketLive;
    }

    public void startLive() {
        WIDTH= ScreenUtil.getScreenWidth(Application.getmApp());
        HEIGHT=ScreenUtil.getScreenHeight(Application.getmApp());
        LogUtils.d(TAG, String.format("WIDTH = %s, HEIGHT = %s",WIDTH, HEIGHT));
        try {
            /////////////////////////////////// 如果是H265 这里换成 MIMETYPE_VIDEO_HEVC //////////////////////////////////////////////
            //配置mediacodec的配置信息 设置 为 264  使用DSP芯片解析
            MediaFormat format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC,  WIDTH,HEIGHT);
            // 设置颜色格式
            format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
            // 设置码率  码率越大 视频越清晰。控制在编码或者解码视频画面的清晰度 ，编码一帧的长度会不一样
            format.setInteger(KEY_BIT_RATE, WIDTH *HEIGHT );
            // 每秒15帧
            format.setInteger(KEY_FRAME_RATE, 15);
            // 设置I帧的间隔
            format.setInteger(KEY_I_FRAME_INTERVAL, 1);
            // 使用 H264 编码格式 去 编码
            /////////////////////////////////// 如果是H265 这里换成 MIMETYPE_VIDEO_HEVC //////////////////////////////////////////////
            mMediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC);
            // 设置格式 要编码
            mMediaCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            // 创建一个虚拟的 Surface
            Surface surface = mMediaCodec.createInputSurface();
            // 把录屏的 mediaProjection 和 Surface关联起来，把录制好的每帧数据丢到 Surface中
            virtualDisplay = mMediaProjection.createVirtualDisplay(
                    "-display",
                    WIDTH, HEIGHT, 1,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC, surface, null, null);

        } catch (IOException e) {
            e.printStackTrace();
            // 不支持 就走软解 ，或者 没有这个视频的格式
        }


        start();
    }

    @Override
    public void run() {
        mMediaCodec.start();
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        while (true) {
            try {
                int outputBufferId = mMediaCodec.dequeueOutputBuffer(bufferInfo, 10000);
                if (outputBufferId >= 0) {
                    ByteBuffer byteBuffer = mMediaCodec.getOutputBuffer(outputBufferId);
                    // 拿到每一帧 如果是I帧 则在I帧前面插入 sps pps
                    dealFrame(byteBuffer, bufferInfo);
                    mMediaCodec.releaseOutputBuffer(outputBufferId, false);
                }
            } catch (Exception e) {
                e.printStackTrace();
                break;
            }

        }
    }

    /////////////////////////////////// 这里的算法要换 //////////////////////////////////////////////
    /////////////////////////////////// 如果是H265 NAL_I=19 NAL_SPS=32//////////////////////////////////////////////

    public static final int NAL_I = 5;
    public static final int NAL_SPS = 7;
    private byte[] sps_pps_buf;

    /**
     * 绘制每一帧，因为录屏 只有第一帧有 sps 、pps 和 vps，所以我们需要在每一 I 帧 之前插入 sps 、pps 和 vps 的内容
     *
     * @param byteBuffer
     * @param bufferInfo
     */
    private void dealFrame(ByteBuffer byteBuffer, MediaCodec.BufferInfo bufferInfo) {
        int offset = 4;
        if (byteBuffer.get(2) == 0x01) {
            offset = 3;
        }

        int type = byteBuffer.get(offset) & 0x1f;
        /////////////////////////////////// 如果是H265 这里type 要换 //////////////////////////////////////////////
//        int type = (byteBuffer.get(offset) & 0x7E) >> 1;
        // sps_pps_buf 帧记录下来
        if (type == NAL_SPS) {
            sps_pps_buf = new byte[bufferInfo.size];
            byteBuffer.get(sps_pps_buf);
        } else if (type == NAL_I) {
            // I 帧 ，把 vps_sps_pps 帧塞到 I帧之前一起发出去
            final byte[] bytes = new byte[bufferInfo.size];
            byteBuffer.get(bytes);

            byte[] newBuf = new byte[sps_pps_buf.length + bytes.length];
            System.arraycopy(sps_pps_buf, 0, newBuf, 0, sps_pps_buf.length);
            System.arraycopy(bytes, 0, newBuf, sps_pps_buf.length, bytes.length);
            mSocket.sendData(newBuf);
            LogUtils.v(TAG, "I帧 视频数据  " + Arrays.toString(bytes));
        } else {
            // B 帧 P 帧 直接发送
            final byte[] bytes = new byte[bufferInfo.size];
            byteBuffer.get(bytes);
            mSocket.sendData(bytes);

        }

    }

}
