package com.kx0101;

import java.util.function.Function;

public class StorageOptions {
    public Function<String, String> pathTransformFunc;

    public StorageOptions() {
        this.pathTransformFunc = this::defaultPathTransformFunc;
    }

    public String defaultPathTransformFunc(String key) {
        return key;
    }
}
