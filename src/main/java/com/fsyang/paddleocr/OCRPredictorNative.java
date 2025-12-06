package com.fsyang.paddleocr;

import android.graphics.Bitmap;
import android.util.Log;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * OCR 原生预测器 - JNI 接口
 */
public class OCRPredictorNative {

    private static final String TAG = "OCRPredictorNative";
    private static final AtomicBoolean isSOLoaded = new AtomicBoolean();

    public static void loadLibrary() throws RuntimeException {
        if (!isSOLoaded.get() && isSOLoaded.compareAndSet(false, true)) {
            try {
                System.loadLibrary("Native");
                Log.i(TAG, "libNative.so loaded successfully");
            } catch (Throwable e) {
                isSOLoaded.set(false);
                throw new RuntimeException("Load libNative.so failed", e);
            }
        }
    }

    private Config config;
    private long nativePointer = 0;

    public OCRPredictorNative(Config config) {
        this.config = config;
        loadLibrary();
        nativePointer = init(
            config.detModelFilename, 
            config.recModelFilename, 
            config.clsModelFilename, 
            config.useOpencl,
            config.cpuThreadNum, 
            config.cpuPower
        );
        Log.i(TAG, "Native init success, pointer: " + nativePointer);
    }

    /**
     * 运行 OCR 识别
     * @param originalImage 原始图片
     * @param maxSizeLen 最大边长
     * @param runDet 是否运行检测 1/0
     * @param runCls 是否运行方向分类 1/0
     * @param runRec 是否运行识别 1/0
     * @return 识别结果列表
     */
    public ArrayList<OcrResultModel> runImage(Bitmap originalImage, int maxSizeLen, int runDet, int runCls, int runRec) {
        Log.i(TAG, "Begin to run image, size: " + originalImage.getWidth() + "x" + originalImage.getHeight());
        float[] rawResults = forward(nativePointer, originalImage, maxSizeLen, runDet, runCls, runRec);
        return postprocess(rawResults);
    }

    /**
     * 配置类
     */
    public static class Config {
        public int useOpencl = 0;
        public int cpuThreadNum = 4;
        public String cpuPower = "LITE_POWER_HIGH";
        public String detModelFilename;
        public String recModelFilename;
        public String clsModelFilename;
    }

    public void destroy() {
        if (nativePointer != 0) {
            release(nativePointer);
            nativePointer = 0;
            Log.i(TAG, "Native released");
        }
    }

    // Native 方法
    protected native long init(String detModelPath, String recModelPath, String clsModelPath, 
                               int useOpencl, int threadNum, String cpuMode);
    protected native float[] forward(long pointer, Bitmap originalImage, int maxSizeLen, 
                                     int runDet, int runCls, int runRec);
    protected native void release(long pointer);

    /**
     * 后处理：解析 native 返回的 float 数组
     */
    private ArrayList<OcrResultModel> postprocess(float[] raw) {
        ArrayList<OcrResultModel> results = new ArrayList<>();
        if (raw == null || raw.length == 0) {
            return results;
        }
        
        int begin = 0;
        while (begin < raw.length) {
            int pointNum = Math.round(raw[begin]);
            int wordNum = Math.round(raw[begin + 1]);
            OcrResultModel res = parse(raw, begin + 2, pointNum, wordNum);
            begin += 2 + 1 + pointNum * 2 + wordNum + 2;
            results.add(res);
        }
        return results;
    }

    private OcrResultModel parse(float[] raw, int begin, int pointNum, int wordNum) {
        int current = begin;
        OcrResultModel res = new OcrResultModel();
        res.setConfidence(raw[current]);
        current++;
        
        for (int i = 0; i < pointNum; i++) {
            res.addPoints(Math.round(raw[current + i * 2]), Math.round(raw[current + i * 2 + 1]));
        }
        current += (pointNum * 2);
        
        for (int i = 0; i < wordNum; i++) {
            int index = Math.round(raw[current + i]);
            res.addWordIndex(index);
        }
        current += wordNum;
        
        res.setClsIdx(raw[current]);
        res.setClsConfidence(raw[current + 1]);
        return res;
    }
}
