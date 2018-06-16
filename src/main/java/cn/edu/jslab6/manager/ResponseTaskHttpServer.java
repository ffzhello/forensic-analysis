package main.java.cn.edu.jslab6.manager;

import com.sun.net.httpserver.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;

import main.java.cn.edu.jslab6.config.SystemConfig;
/**
 * @author Andy
 * @date 18-6-17 上午7:28
 * @description
 */
public class ResponseTaskHttpServer extends Thread {
    private static final Logger LOG = LoggerFactory.getLogger(ResponseTaskHttpServer.class);
    private String serverIP = "127.0.0.1";
    private int serverPort = 8080;

    public ResponseTaskHttpServer(String serverIP, int serverPort) {
        this.serverIP = serverIP;
        this.serverPort = serverPort;
    }

    public ResponseTaskHttpServer(SystemConfig config) {
        serverIP = config.getServerIP();
        serverPort = config.getServerPort();
    }

    @Override
    public void run() {
        //String serverIP = InetAddress.getLocalHost().getHostAddress();
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(serverIP, serverPort), 200);
            server.createContext("/hydra", new ResponseTaskHttpHandler());
            server.setExecutor(null);
            server.start();
            LOG.info("Start the server at {}:{}...", serverIP, serverPort);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}