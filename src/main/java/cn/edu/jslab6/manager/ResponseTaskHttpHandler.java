
package main.java.cn.edu.jslab6.manager;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.*;

import main.java.cn.edu.jslab6.entity.ResponseTask;
import main.java.cn.edu.jslab6.enums.ActiveTaskStatus;
/**
 * @author Andy
 * @date 18-6-17 上午7:31
 * @description
 */
public class ResponseTaskHttpHandler implements HttpHandler {
    private static final Logger LOG = LoggerFactory.getLogger(ResponseTaskHttpHandler.class);

    public void handle(HttpExchange t) throws IOException {
        // 根据http method和相关URI调用相应的Handler.
        LOG.debug("Method = " + t.getRequestMethod());
        LOG.debug("Request Url = " + t.getRequestURI());
        String httpMethod = t.getRequestMethod();
        String requestURI = t.getRequestURI().toString();
        if (httpMethod.equals("POST") && requestURI.equals("/hydra/do-forensic-task")) {
            new RecvTaskHandler().handle(t);
        }else if(httpMethod.equals("POST") && requestURI.equals("/hydra/del-forensic-task")) {
            //停止任务
            new DelTaskHandler().handle(t);
        } else {
            new DefaultHandler().handle(t);
        }
    }
}

class DefaultHandler implements HttpHandler {
    public void handle(HttpExchange t) throws IOException {
        String response = "Oops, you got it!";
        t.sendResponseHeaders(200, response.length());
        OutputStream os = t.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }
}

class RecvTaskHandler implements HttpHandler {
    private static final Logger LOG = LoggerFactory.getLogger(RecvTaskHandler.class);

    public void handle(HttpExchange t) throws IOException {
        LOG.debug("取得新案件!");
        InputStream is = t.getRequestBody();
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = bufferedReader.readLine()) != null) {
            sb.append(line).append('\n');
        }
        bufferedReader.close();

        // 接着做本地的一些工作，如根据字段填充相应字段，并根据action等字段进行相关操作。
        Gson gson = new Gson();
        ResponseTask task = gson.fromJson(sb.toString(), ResponseTask.class);

        //将采集任务存到数据库，等待采集
        try {
            ResponseTaskManager.addUnhandledTask(task);
        }catch (InterruptedException e) {
            e.printStackTrace();
        }

        // 回送消息
        String response = sb.toString();
        t.sendResponseHeaders(200, response.length());
        OutputStream os = t.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }
}

class DelTaskHandler implements HttpHandler {

    private static final Logger LOG = LoggerFactory.getLogger(DelTaskHandler.class);
    @Override
    public void handle(HttpExchange t) throws IOException {

        InputStream is = t.getRequestBody();
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = bufferedReader.readLine()) != null) {
            sb.append(line).append('\n');
        }
        bufferedReader.close();

        // 接着做本地的一些工作，如根据字段填充相应字段，并根据action等字段进行相关操作。
        Gson gson = new Gson();
        ResponseTask task = gson.fromJson(sb.toString(), ResponseTask.class);

        //更新状态
        ActiveTaskManager.updateTaskStatusByTicketId(task.getTicketid(), ActiveTaskStatus.FORCE);
        LOG.debug("取得FORCE任务,待停止任务[ticketid: " + task.getTicketid() +"]");
        task = null;

        // 回送消息
        String response = sb.toString();
        t.sendResponseHeaders(200, response.length());
        OutputStream os = t.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }
}