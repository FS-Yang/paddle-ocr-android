package com.fsyang.paddleocr;

import android.graphics.Point;
import java.util.ArrayList;
import java.util.List;

/**
 * OCR 识别结果模型
 */
public class OcrResultModel {
    private List<Point> points;
    private List<Integer> wordIndex;
    private String label;
    private float confidence;
    private float clsIdx;
    private String clsLabel;
    private float clsConfidence;

    public OcrResultModel() {
        super();
        points = new ArrayList<>();
        wordIndex = new ArrayList<>();
    }

    public void addPoints(int x, int y) {
        Point point = new Point(x, y);
        points.add(point);
    }

    public void addWordIndex(int index) {
        wordIndex.add(index);
    }

    public List<Point> getPoints() {
        return points;
    }

    public List<Integer> getWordIndex() {
        return wordIndex;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public float getConfidence() {
        return confidence;
    }

    public void setConfidence(float confidence) {
        this.confidence = confidence;
    }

    public float getClsIdx() {
        return clsIdx;
    }

    public void setClsIdx(float idx) {
        this.clsIdx = idx;
    }

    public String getClsLabel() {
        return clsLabel;
    }

    public void setClsLabel(String label) {
        this.clsLabel = label;
    }

    public float getClsConfidence() {
        return clsConfidence;
    }

    public void setClsConfidence(float confidence) {
        this.clsConfidence = confidence;
    }
    
    /**
     * 获取文本框的边界坐标
     * @return [minX, minY, maxX, maxY]
     */
    public int[] getBounds() {
        if (points.isEmpty()) {
            return new int[]{0, 0, 0, 0};
        }
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE;
        for (Point p : points) {
            minX = Math.min(minX, p.x);
            minY = Math.min(minY, p.y);
            maxX = Math.max(maxX, p.x);
            maxY = Math.max(maxY, p.y);
        }
        return new int[]{minX, minY, maxX, maxY};
    }
}
