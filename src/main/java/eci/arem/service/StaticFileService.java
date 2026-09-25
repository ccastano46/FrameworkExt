package eci.arem.service;

import eci.arem.http.HttpRequest;
import eci.arem.http.HttpResponse;


import java.io.IOException;

public class StaticFileService {

    private StaticFileService(){}

    public static void setBasePath(String basePath){
        FileResolver.basePath = basePath;
    }

    public static HttpResponse invokeStaticFiles(HttpRequest request, HttpResponse response) throws IOException {
        String path = request.getPath().equals("/") ? "/async-client.html" : request.getPath();
        response.setBody(FileResolver.getFileBytes(path));
        response.setContentType(FileResolver.getFileType(path));
        return response;
    }
}
