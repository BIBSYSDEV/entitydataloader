package no.greenall.entitydataloader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class Utils {
    public static void writeToLoggingFile(String filename, String data) {
        try {
            File file = new File(filename);
            FileOutputStream fileOutputStream = new FileOutputStream(filename);
            if (!file.exists()) {
                file.createNewFile();
            }
            byte[] bytes = data.getBytes();
            //fileOutputStream.write();

        } catch (IOException e) {
            e.printStackTrace();
        }


    }
}
