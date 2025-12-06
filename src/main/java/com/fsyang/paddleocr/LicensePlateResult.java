package com.fsyang.paddleocr;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 车牌识别结果
 */
public class LicensePlateResult {

    public String plateNumber;  // 车牌号
    public float confidence;    // 置信度
    public float[] box;         // 边框坐标

    // 车牌正则表达式
    private static final Pattern PLATE_PATTERN = Pattern.compile(
            "^[京津沪渝冀豫云辽黑湘皖鲁新苏浙赣鄂桂甘晋蒙陕吉闽贵粤青藏川宁琼使领]" +
            "[A-Z][A-Z0-9]{5,6}$"
    );

    // 新能源车牌
    private static final Pattern NEW_ENERGY_PATTERN = Pattern.compile(
            "^[京津沪渝冀豫云辽黑湘皖鲁新苏浙赣鄂桂甘晋蒙陕吉闽贵粤青藏川宁琼使领]" +
            "[A-Z][DF][A-Z0-9]{5}$"
    );

    /**
     * 从 OCR 结果中提取车牌
     */
    public static List<LicensePlateResult> extractFromOcrResults(List<OcrResult> results) {
        List<LicensePlateResult> plates = new ArrayList<>();

        for (OcrResult r : results) {
            String text = r.text.replace(" ", "").toUpperCase();
            
            // 检查是否匹配车牌格式
            if (isValidPlate(text)) {
                LicensePlateResult plate = new LicensePlateResult();
                plate.plateNumber = text;
                plate.confidence = r.confidence;
                plate.box = r.box;
                plates.add(plate);
            }
        }

        return plates;
    }

    /**
     * 判断是否是有效的车牌号
     */
    public static boolean isValidPlate(String text) {
        if (text == null || text.length() < 7 || text.length() > 8) {
            return false;
        }
        return PLATE_PATTERN.matcher(text).matches() || NEW_ENERGY_PATTERN.matcher(text).matches();
    }

    @Override
    public String toString() {
        return "车牌: " + plateNumber + " (" + String.format("%.1f", confidence * 100) + "%)";
    }
}
