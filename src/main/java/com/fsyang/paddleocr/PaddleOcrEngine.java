package com.fsyang.paddleocr;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 飞桨 OCR 引擎
 * 
 * 使用方法：
 * 1. 初始化：PaddleOcrEngine.init(context)
 * 2. 识别：PaddleOcrEngine.recognize(bitmap) 或 PaddleOcrEngine.recognizeAsync(bitmap, callback)
 * 3. 释放：PaddleOcrEngine.release()
 */
public class PaddleOcrEngine {

    private static final String TAG = "PaddleOcrEngine";

    // 模型文件名
    private static final String DET_MODEL = "det_db.nb";
    private static final String REC_MODEL = "rec_crnn.nb";
    private static final String CLS_MODEL = "cls.nb";
    private static final String KEYS_FILE = "ppocr_keys_v1.txt";

    // 模型目录（assets 中）
    private static final String MODEL_DIR = "models";

    private static Context appContext;
    private static OCRPredictorNative predictor;
    private static Vector<String> wordLabels = new Vector<>();
    private static boolean isInitialized = false;
    private static ExecutorService executor = Executors.newSingleThreadExecutor();

    // 配置参数
    private static int detLongSize = 960;
    private static int cpuThreadNum = 4;

    private PaddleOcrEngine() {}

    /**
     * 初始化 OCR 引擎
     * @param context Android Context
     * @return 是否初始化成功
     */
    public static synchronized boolean init(Context context) {
        return init(context, cpuThreadNum, detLongSize);
    }

    /**
     * 初始化 OCR 引擎
     * @param context Android Context
     * @param threadNum CPU 线程数
     * @param longSize 检测长边尺寸
     * @return 是否初始化成功
     */
    public static synchronized boolean init(Context context, int threadNum, int longSize) {
        if (isInitialized) {
            Log.w(TAG, "Already initialized");
            return true;
        }

        appContext = context.getApplicationContext();
        cpuThreadNum = threadNum;
        detLongSize = longSize;

        try {
            // 复制模型文件到私有目录
            String modelPath = copyModelsToCache();
            if (modelPath == null) {
                Log.e(TAG, "Copy models failed");
                return false;
            }

            // 加载字典
            if (!loadLabels()) {
                Log.e(TAG, "Load labels failed");
                return false;
            }

            // 初始化原生预测器
            OCRPredictorNative.Config config = new OCRPredictorNative.Config();
            config.useOpencl = 0;
            config.cpuThreadNum = cpuThreadNum;
            config.cpuPower = "LITE_POWER_HIGH";
            config.detModelFilename = modelPath + "/" + DET_MODEL;
            config.recModelFilename = modelPath + "/" + REC_MODEL;
            config.clsModelFilename = modelPath + "/" + CLS_MODEL;

            Log.i(TAG, "Model paths: det=" + config.detModelFilename);
            predictor = new OCRPredictorNative(config);
            isInitialized = true;
            Log.i(TAG, "Init success");
            return true;

        } catch (Exception e) {
            Log.e(TAG, "Init failed: " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * 同步识别
     * @param bitmap 输入图片
     * @return 识别结果列表
     */
    public static List<OcrResult> recognize(Bitmap bitmap) {
        return recognize(bitmap, true, true, true);
    }

    /**
     * 同步识别（可控制流程）
     * @param bitmap 输入图片
     * @param runDet 是否运行检测
     * @param runCls 是否运行方向分类
     * @param runRec 是否运行识别
     * @return 识别结果列表
     */
    public static List<OcrResult> recognize(Bitmap bitmap, boolean runDet, boolean runCls, boolean runRec) {
        List<OcrResult> results = new ArrayList<>();
        
        if (!isInitialized || predictor == null) {
            Log.e(TAG, "Not initialized");
            return results;
        }

        if (bitmap == null) {
            Log.e(TAG, "Bitmap is null");
            return results;
        }

        try {
            Bitmap inputBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
            ArrayList<OcrResultModel> rawResults = predictor.runImage(
                inputBitmap, 
                detLongSize, 
                runDet ? 1 : 0,
                runCls ? 1 : 0,
                runRec ? 1 : 0
            );

            // 转换结果
            for (OcrResultModel raw : rawResults) {
                // 解析文字
                StringBuilder word = new StringBuilder();
                for (int index : raw.getWordIndex()) {
                    if (index >= 0 && index < wordLabels.size()) {
                        word.append(wordLabels.get(index));
                    }
                }
                raw.setLabel(word.toString());
                raw.setClsLabel(raw.getClsIdx() == 1 ? "180" : "0");

                // 转换坐标
                float[] box = new float[8];
                List<android.graphics.Point> points = raw.getPoints();
                for (int i = 0; i < Math.min(4, points.size()); i++) {
                    box[i * 2] = points.get(i).x;
                    box[i * 2 + 1] = points.get(i).y;
                }

                results.add(new OcrResult(raw.getLabel(), raw.getConfidence(), box));
            }

            Log.i(TAG, "Recognized " + results.size() + " texts");

        } catch (Exception e) {
            Log.e(TAG, "Recognize failed: " + e.getMessage(), e);
        }

        return results;
    }

    /**
     * 异步识别
     * @param bitmap 输入图片
     * @param callback 回调
     */
    public static void recognizeAsync(final Bitmap bitmap, final OcrCallback callback) {
        executor.execute(() -> {
            try {
                List<OcrResult> results = recognize(bitmap);
                if (callback != null) {
                    callback.onSuccess(results);
                }
            } catch (Exception e) {
                if (callback != null) {
                    callback.onError(-1, e.getMessage());
                }
            }
        });
    }

    /**
     * 释放资源
     */
    public static synchronized void release() {
        if (predictor != null) {
            predictor.destroy();
            predictor = null;
        }
        wordLabels.clear();
        isInitialized = false;
        Log.i(TAG, "Released");
    }

    /**
     * 是否已初始化
     */
    public static boolean isReady() {
        return isInitialized;
    }

    /**
     * 设置检测长边尺寸
     */
    public static void setDetLongSize(int size) {
        detLongSize = size;
    }

    /**
     * 复制模型文件到缓存目录
     */
    private static String copyModelsToCache() {
        try {
            String cachePath = appContext.getCacheDir() + File.separator + MODEL_DIR;
            File cacheDir = new File(cachePath);
            if (!cacheDir.exists()) {
                cacheDir.mkdirs();
            }

            String[] files = {DET_MODEL, REC_MODEL, CLS_MODEL, KEYS_FILE};
            for (String fileName : files) {
                File destFile = new File(cachePath, fileName);
                if (!destFile.exists() || destFile.length() == 0) {
                    // Android assets 路径分隔符必须是 /
                    Log.d(TAG, "Copying " + fileName + " to " + destFile.getAbsolutePath());
                    copyAssetFile(MODEL_DIR + "/" + fileName, destFile);
                    Log.d(TAG, "Copied " + fileName + ", size: " + destFile.length());
                } else {
                    Log.d(TAG, "Skipped " + fileName + ", exists, size: " + destFile.length());
                }
            }

            return cachePath;
        } catch (Exception e) {
            Log.e(TAG, "Copy models failed: " + e.getMessage());
            return null;
        }
    }

    private static void copyAssetFile(String assetPath, File destFile) throws Exception {
        InputStream in = appContext.getAssets().open(assetPath);
        FileOutputStream out = new FileOutputStream(destFile);
        byte[] buffer = new byte[4096];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
        out.flush();
        out.close();
        in.close();
        Log.d(TAG, "Copied: " + assetPath);
    }

    /**
     * 加载字典文件
     */
    private static boolean loadLabels() {
        wordLabels.clear();
        wordLabels.add("black"); // 占位符
        try {
            InputStream in = appContext.getAssets().open(MODEL_DIR + "/" + KEYS_FILE);
            int available = in.available();
            byte[] bytes = new byte[available];
            in.read(bytes);
            in.close();
            
            String content = new String(bytes, "UTF-8");
            String[] lines = content.split("\n");
            for (String line : lines) {
                wordLabels.add(line);
            }
            wordLabels.add(" "); // 空格
            
            Log.i(TAG, "Loaded " + wordLabels.size() + " labels");
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Load labels failed: " + e.getMessage());
            return false;
        }
    }
}
