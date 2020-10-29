package com.pyrus.pyrusservicedesk.log;

import androidx.annotation.NonNull;
import androidx.core.util.Consumer;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

class Utils {

    public static List<File> getListFiles(File parentDir) {
        ArrayList<File> inFiles = new ArrayList<>();
        File[] files = parentDir.listFiles();
        for (File file : files) {
            if (file.isDirectory())
                inFiles.addAll(getListFiles(file));
            else
                inFiles.add(file);
        }
        return inFiles;
    }

    public static class Arrays {

        public static boolean isEmpty(Object[] array) {
            return array == null || array.length == 0;
        }

        public static <T> void forEach(T[] array, @NonNull Consumer<T> onEach) {
            if (isEmpty(array))
                return;
            for (T item : array)
                onEach.accept(item);
        }
    }
}
