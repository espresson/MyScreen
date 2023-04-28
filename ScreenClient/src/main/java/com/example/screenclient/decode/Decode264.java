package com.example.screenclient.decode;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.view.Surface;

import java.io.IOException;
import java.nio.ByteBuffer;

public class Decode264{
    private static final String TAG = "Decode";
    private Surface mSurface;
    MediaCodec mMediaCodec;
    public Decode264(Surface surface) {
        mSurface = surface;
        initCodec();
    }

    private void initCodec() {
        try {
            // 把 h264 解码成 yuv视频
            /////////////////////////////////// 如果是H265 这里换成 MIMETYPE_VIDEO_HEVC //////////////////////////////////////////////
            mMediaCodec = MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_VIDEO_AVC);
            /////////////////////////////////// 如果是H265 这里换成 MIMETYPE_VIDEO_HEVC //////////////////////////////////////////////
            final MediaFormat format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, 720, 1280);
            format.setInteger(MediaFormat.KEY_BIT_RATE, 1920*1080);
            format.setInteger(MediaFormat.KEY_FRAME_RATE, 20);
            format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);
            // 解码之后直接显示在 Surface 上
            mMediaCodec.configure(format,
                    mSurface,
                    null, 0);
            mMediaCodec.start();
        } catch (
                IOException e) {
            e.printStackTrace();
        }
    }

    public void setData(byte[] data) {
//        Log.e(TAG, Arrays.toString(data));
        int index = mMediaCodec.dequeueInputBuffer(100000);
        if (index >= 0) {
            ByteBuffer inputBuffer = mMediaCodec.getInputBuffer(index);
            inputBuffer.clear();
            inputBuffer.put(data, 0, data.length);
            mMediaCodec.queueInputBuffer(index,
                    0, data.length, System.currentTimeMillis(), 0);
        }
        //  获取数据
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        int outputBufferIndex = mMediaCodec.dequeueOutputBuffer(bufferInfo, 100000);

        while (outputBufferIndex > 0) {
            // true 就是显示在surface上
            mMediaCodec.releaseOutputBuffer(outputBufferIndex, true);
            outputBufferIndex = mMediaCodec.dequeueOutputBuffer(bufferInfo, 0);
        }
    }
}
