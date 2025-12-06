package com.fsyang.paddleocr;

import java.util.Arrays;

/**
 * OCR 识别结果（简化版）
 */
public class OcrResult {
    
    /** 识别的文本内容 */
    public final String text;
    
    /** 置信度 0-1 */
    public final float confidence;
    
    /** 文本框坐标 [x1,y1, x2,y2, x3,y3, x4,y4] 四个角点 */
    public final float[] box;

    public OcrResult(String text, float confidence, float[] box) {
        this.text = text;
        this.confidence = confidence;
        this.box = box;
    }

    @Override
    public String toString() {
        return "OcrResult{" +
                "text='" + text + '\'' +
                ", confidence=" + confidence +
                ", box=" + Arrays.toString(box) +
                '}';
    }
}
