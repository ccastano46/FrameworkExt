package eci.arem.webframework;

import eci.arem.server.HttpServer;
import eci.arem.server.Router;
import eci.arem.server.WebService;
import eci.arem.service.StaticFileService;

import java.io.IOException;
import java.net.URISyntaxException;

public class WebFramework {

    private WebFramework(){}

    public static void get(String route, WebService ws){
      Router.register(route, ws);
    }

    public static void staticFiles(String baseFolderPath){
        StaticFileService.setBasePath(baseFolderPath);
    }


    public static void start(int port) throws IOException, URISyntaxException {
        HttpServer.main(new String[]{String.valueOf(port)});
    }

    public static void stop(){
        HttpServer.stop();
    }
}
