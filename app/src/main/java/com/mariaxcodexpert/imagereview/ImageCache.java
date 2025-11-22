package com.mariaxcodexpert.imagereview;

import java.util.ArrayList;
import java.util.List;

public class ImageCache {
    private static ImageCache instance;
    private final List<ReviewImageActivity.ImageData> images = new ArrayList<>();

    private ImageCache() {}

    public static synchronized ImageCache getInstance() {
        if (instance == null) instance = new ImageCache();
        return instance;
    }

    public List<ReviewImageActivity.ImageData> getImages() {
        return new ArrayList<>(images); // return a copy for safety
    }

    public void setImages(List<ReviewImageActivity.ImageData> list) {
        images.clear();
        if (list != null) images.addAll(list);
    }

    public void clear() {
        images.clear();
    }
}
