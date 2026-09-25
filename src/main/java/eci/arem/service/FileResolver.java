package eci.arem.service;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FileResolver {

    static String basePath;

    private FileResolver(){}

    static byte[] getFileBytes(String file) throws IOException {
        Path base = Paths.get(basePath).toAbsolutePath().normalize();
        Path pathFile = base.resolve("." + file).normalize();

        if (!pathFile.startsWith(base) || !Files.exists(pathFile) || Files.isDirectory(pathFile))
            throw new FileNotFoundException();

        return Files.readAllBytes(pathFile);
    }

    public static String getFileType(String path){
        if(path.endsWith(".js")) return "application/javascript";
        else if(path.endsWith(".css")) return "text/css";
        else if(path.endsWith(".png")) return "image/png";
        else if(path.endsWith(".jpg")) return "image/jpeg";
        else if(path.endsWith(".json")) return "application/json";
        else if(path.endsWith(".html")) return "text/html";
        else return "text/plain";
    }


}
