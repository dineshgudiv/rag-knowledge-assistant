package com.companyname.ragassistant.util;

public final class HashEmbedding {
    private HashEmbedding() {
    }

    public static float[] embed(String text, int dim) {
        int size = Math.max(8, dim);
        float[] vector = new float[size];
        if (text == null || text.isBlank()) {
            return vector;
        }

        String[] tokens = text.toLowerCase().split("\\W+");
        for (String token : tokens) {
            if (token.isBlank()) {
                continue;
            }
            int hash = token.hashCode();
            int idx = Math.floorMod(hash, size);
            int sign = ((hash >>> 31) == 0) ? 1 : -1;
            vector[idx] += sign;
        }
        return VectorMath.normalize(vector);
    }
}
