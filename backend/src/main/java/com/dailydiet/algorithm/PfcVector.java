package com.dailydiet.algorithm;

/**
 * PFC 向量工具 — 碳水/蛋白/脂肪 三维向量归一化与余弦相似度
 */
public class PfcVector {

    private final double carbo;
    private final double protein;
    private final double fat;

    public PfcVector(double carbo, double protein, double fat) {
        this.carbo = carbo;
        this.protein = protein;
        this.fat = fat;
    }

    /** 归一化（L2） */
    public PfcVector normalize() {
        double norm = Math.sqrt(carbo * carbo + protein * protein + fat * fat);
        if (norm == 0) return new PfcVector(0, 0, 0);
        return new PfcVector(carbo / norm, protein / norm, fat / norm);
    }

    /** 余弦相似度 ∈ [0, 1]（PFC 均为非负数，所以最小值为 0） */
    public double cosineSimilarity(PfcVector other) {
        PfcVector a = this.normalize();
        PfcVector b = other.normalize();
        return a.carbo * b.carbo + a.protein * b.protein + a.fat * b.fat;
    }

    public double getCarbo()   { return carbo; }
    public double getProtein() { return protein; }
    public double getFat()     { return fat; }
}
