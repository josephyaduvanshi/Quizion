package com.shaivites.quizion.utils;

import java.util.*;

public class DiceBearAvatarGenerator {

    private static final String BASE_URL = "https://api.dicebear.com/9.x/thumbs/svg";
    private final Map<String, String> params = new LinkedHashMap<>();

    public DiceBearAvatarGenerator setSeed(String seed) {
        params.put("seed", seed);
        return this;
    }

    public DiceBearAvatarGenerator setFlip(boolean flip) {
        params.put("flip", String.valueOf(flip));
        return this;
    }

    public DiceBearAvatarGenerator setRotate(int degree) {
        params.put("rotate", String.valueOf(degree));
        return this;
    }

    public DiceBearAvatarGenerator setScale(int scale) {
        params.put("scale", String.valueOf(scale));
        return this;
    }

    public DiceBearAvatarGenerator setRadius(int radius) {
        params.put("radius", String.valueOf(radius));
        return this;
    }

    public DiceBearAvatarGenerator setSize(int size) {
        params.put("size", String.valueOf(size));
        return this;
    }

    public DiceBearAvatarGenerator setBackgroundColor(String... colors) {
        params.put("backgroundColor", String.join(",", colors));
        return this;
    }

    public DiceBearAvatarGenerator setBackgroundType(String... types) {
        params.put("backgroundType", String.join(",", types));
        return this;
    }

    public DiceBearAvatarGenerator setBackgroundRotation(int min, int max) {
        params.put("backgroundRotation", min + "," + max);
        return this;
    }

    public DiceBearAvatarGenerator setTranslateX(int value) {
        params.put("translateX", String.valueOf(value));
        return this;
    }

    public DiceBearAvatarGenerator setTranslateY(int value) {
        params.put("translateY", String.valueOf(value));
        return this;
    }

    public DiceBearAvatarGenerator setClip(boolean clip) {
        params.put("clip", String.valueOf(clip));
        return this;
    }

    public DiceBearAvatarGenerator setRandomizeIds(boolean randomize) {
        params.put("randomizeIds", String.valueOf(randomize));
        return this;
    }

    public DiceBearAvatarGenerator setEyes(String... variants) {
        params.put("eyes", String.join(",", variants));
        return this;
    }

    public DiceBearAvatarGenerator setEyesColor(String... colors) {
        params.put("eyesColor", String.join(",", colors));
        return this;
    }

    public DiceBearAvatarGenerator setFace(String... variants) {
        params.put("face", String.join(",", variants));
        return this;
    }

    public DiceBearAvatarGenerator setFaceOffsetX(int min, int max) {
        params.put("faceOffsetX", min + "," + max);
        return this;
    }

    public DiceBearAvatarGenerator setFaceOffsetY(int min, int max) {
        params.put("faceOffsetY", min + "," + max);
        return this;
    }

    public DiceBearAvatarGenerator setFaceRotation(int min, int max) {
        params.put("faceRotation", min + "," + max);
        return this;
    }

    public DiceBearAvatarGenerator setMouth(String... variants) {
        params.put("mouth", String.join(",", variants));
        return this;
    }

    public DiceBearAvatarGenerator setMouthColor(String... colors) {
        params.put("mouthColor", String.join(",", colors));
        return this;
    }

    public DiceBearAvatarGenerator setShape(String... variants) {
        params.put("shape", String.join(",", variants));
        return this;
    }

    public DiceBearAvatarGenerator setShapeColor(String... colors) {
        params.put("shapeColor", String.join(",", colors));
        return this;
    }

    public DiceBearAvatarGenerator setShapeOffsetX(int min, int max) {
        params.put("shapeOffsetX", min + "," + max);
        return this;
    }

    public DiceBearAvatarGenerator setShapeOffsetY(int min, int max) {
        params.put("shapeOffsetY", min + "," + max);
        return this;
    }

    public DiceBearAvatarGenerator setShapeRotation(int min, int max) {
        params.put("shapeRotation", min + "," + max);
        return this;
    }

    public String buildUrl() {
        if (params.isEmpty()) {
            return BASE_URL;
        }
        StringBuilder urlBuilder = new StringBuilder(BASE_URL + "?");
        for (Map.Entry<String, String> entry : params.entrySet()) {
            urlBuilder.append(entry.getKey()).append("=").append(entry.getValue()).append("&");
        }
        return urlBuilder.substring(0, urlBuilder.length() - 1); // remove trailing &
    }

    public static void main(String[] args) {
        DiceBearAvatarGenerator avatar = new DiceBearAvatarGenerator()
                .setSeed("Felix")
                .setSize(128)
                .setFlip(true)
                .setRotate(90)
                .setBackgroundColor("c0aede", "ffd5dc")
                .setBackgroundType("gradientLinear")
                .setEyes("variant1W10", "variant2W14")
                .setMouth("variant1")
                .setShape("default")
                .setShapeColor("69d2e7");

        System.out.println("Avatar URL: " + avatar.buildUrl());
    }
}
