package com.mariaxcodexpert.imagereview;

import java.util.ArrayList;
import java.util.List;

public class ImageCache {
    private static ImageCache instance;
    private final List<HybridImageSelector.ImageData> images = new ArrayList<>();

    private ImageCache() {}

    public static synchronized ImageCache getInstance() {
        if (instance == null) instance = new ImageCache();
        return instance;
    }

    public List<HybridImageSelector.ImageData> getImages() {
        return images;
    }

    public void setImages(List<HybridImageSelector.ImageData> list) {
        images.clear();
        images.addAll(list);
    }

    public void clear() {
        images.clear();
    }
}
