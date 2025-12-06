package com.fsyang.paddleocr;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 身份证识别结果
 */
public class IdCardResult {

    // 正面信息
    public String name;        // 姓名
    public String gender;      // 性别
    public String nation;      // 民族
    public String birthDate;   // 出生日期
    public String address;     // 住址

    // 反面信息
    public String idNumber;    // 身份证号
    public String validPeriod; // 有效期限
    public String authority;   // 签发机关

    // 是否正面
    public boolean isFront;

    // 原始 OCR 结果
    public List<OcrResult> rawResults;

    /**
     * 从 OCR 结果解析身份证正面
     */
    public static IdCardResult parseFromFront(List<OcrResult> results) {
        IdCardResult card = new IdCardResult();
        card.isFront = true;
        card.rawResults = results;

        StringBuilder addressBuilder = new StringBuilder();

        for (OcrResult r : results) {
            String text = r.text.trim();

            // 姓名
            if (text.contains("姓名") || text.startsWith("名")) {
                card.name = text.replaceAll("姓名|名", "").trim();
            }
            // 性别
            else if (text.contains("性别") || text.contains("男") || text.contains("女")) {
                if (text.contains("男")) card.gender = "男";
                else if (text.contains("女")) card.gender = "女";
            }
            // 民族
            else if (text.contains("民族")) {
                card.nation = text.replaceAll("民族", "").trim();
            }
            // 出生日期
            else if (text.matches(".*\\d{4}.*年.*\\d{1,2}.*月.*\\d{1,2}.*日.*")) {
                card.birthDate = text.replaceAll("[出生]", "").trim();
            }
            // 住址
            else if (text.contains("住址") || text.contains("址")) {
                String addr = text.replaceAll("住址|址", "").trim();
                if (!addr.isEmpty()) addressBuilder.append(addr);
            }
            // 可能是地址的一部分
            else if (text.contains("省") || text.contains("市") || text.contains("县") ||
                    text.contains("区") || text.contains("镇") || text.contains("村") ||
                    text.contains("街") || text.contains("路") || text.contains("号")) {
                addressBuilder.append(text);
            }
        }

        if (addressBuilder.length() > 0) {
            card.address = addressBuilder.toString();
        }

        return card;
    }

    /**
     * 从 OCR 结果解析身份证反面
     */
    public static IdCardResult parseFromBack(List<OcrResult> results) {
        IdCardResult card = new IdCardResult();
        card.isFront = false;
        card.rawResults = results;

        Pattern idPattern = Pattern.compile("\\d{17}[\\dXx]");

        for (OcrResult r : results) {
            String text = r.text.trim().replace(" ", "");

            // 身份证号
            Matcher matcher = idPattern.matcher(text);
            if (matcher.find()) {
                card.idNumber = matcher.group().toUpperCase();
            }
            // 有效期限
            else if (text.contains("有效期") || text.matches(".*\\d{4}\\.\\d{2}\\.\\d{2}.*")) {
                card.validPeriod = text.replaceAll("有效期限|有效期", "").trim();
            }
            // 签发机关
            else if (text.contains("签发机关") || text.contains("公安局")) {
                card.authority = text.replaceAll("签发机关", "").trim();
            }
        }

        return card;
    }

    /**
     * 判断是否识别成功（有足够的关键信息）
     */
    public boolean isValid() {
        if (isFront) {
            // 正面需要至少有姓名
            return name != null && !name.isEmpty();
        } else {
            // 反面需要有身份证号
            return idNumber != null && idNumber.length() == 18;
        }
    }

    @Override
    public String toString() {
        if (isFront) {
            return "【身份证正面】\n" +
                    "姓名: " + (name != null ? name : "") + "\n" +
                    "性别: " + (gender != null ? gender : "") + "\n" +
                    "民族: " + (nation != null ? nation : "") + "\n" +
                    "出生: " + (birthDate != null ? birthDate : "") + "\n" +
                    "住址: " + (address != null ? address : "");
        } else {
            return "【身份证反面】\n" +
                    "身份证号: " + (idNumber != null ? idNumber : "") + "\n" +
                    "有效期: " + (validPeriod != null ? validPeriod : "") + "\n" +
                    "签发机关: " + (authority != null ? authority : "");
        }
    }
}
