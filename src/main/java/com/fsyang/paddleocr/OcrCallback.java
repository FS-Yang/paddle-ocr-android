package com.fsyang.paddleocr;

import java.util.List;

/**
 * OCR 识别回调接口
 */
public interface OcrCallback {
    
    /**
     * 识别成功
     * @param results 识别结果列表
     */
    void onSuccess(List<OcrResult> results);
    
    /**
     * 识别失败
     * @param errorCode 错误码
     * @param errorMsg 错误信息
     */
    void onError(int errorCode, String errorMsg);
}
