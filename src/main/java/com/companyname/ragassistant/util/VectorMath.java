package com.companyname.ragassistant.util;

public final class VectorMath {
    private VectorMath() {
    }

    public static float[] normalize(float[] in) {
        if (in == null || in.length == 0) {
            return in;
        }
        double norm = 0d;
        for (float v : in) {
            norm += v * v;
        }
        norm = Math.sqrt(norm);
        if (norm == 0d) {
            return in;
        }
        float[] out = new float[in.length];
        for (int i = 0; i < in.length; i++) {
            out[i] = (float) (in[i] / norm);
        }
        return out;
    }

    public static double cosineSimilarity(float[] a, float[] b) {
        if (a == null || b == null || a.length == 0 || b.length == 0) {
            return 0d;
        }
        int len = Math.min(a.length, b.length);
        double dot = 0d;
        double an = 0d;
        double bn = 0d;
        for (int i = 0; i < len; i++) {
            dot += a[i] * b[i];
            an += a[i] * a[i];
            bn += b[i] * b[i];
        }
        if (an == 0d || bn == 0d) {
            return 0d;
        }
        return dot / (Math.sqrt(an) * Math.sqrt(bn));
    }

    public static double cosineToUnitScore(double cos) {
        double score = (cos + 1d) / 2d;
        if (score < 0d) {
            return 0d;
        }
        if (score > 1d) {
            return 1d;
        }
        return score;
    }
}
