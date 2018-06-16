package main.java.cn.edu.jslab6.manager;

import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import main.java.cn.edu.jslab6.entity.ResponseResult;
import main.java.cn.edu.jslab6.entity.PcapResponseResult;
import main.java.cn.edu.jslab6.enums.ResponseAction;
import main.java.cn.edu.jslab6.utils.Utils;

/**
 * @author Andy
 * @date 18-6-16 下午6:00
 * @description
 */
public class ResponseResultSender {
    private static final Logger LOG = LoggerFactory.getLogger(ResponseResultSender.class);

    private String recvUrl;

    public ResponseResultSender(String url) {
        this.recvUrl = url;
        String the_url;
        if ((the_url = System.getProperty("result.recv_url")) != null) {
            this.recvUrl = the_url;
        }
        LOG.debug("Init ResponseResultSender, Recv Url = {}", this.recvUrl);
    }

    public void send(String responseResult) throws IOException {
        //建立连接
        URL url = new URL(recvUrl);
        HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();

        //设置参数
        httpConn.setDoInput(true);
        httpConn.setDoOutput(true);
        httpConn.setUseCaches(false);
        httpConn.setRequestMethod("POST");

        // 设置请求属性
        //httpConn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        httpConn.setRequestProperty("Content-Type", "application/json");
        httpConn.setRequestProperty("Connection", "Keep-Alive");
        httpConn.setRequestProperty("Charset", "UTF-8");

        // 连接，也可以不用明文connect,使用下面的httpConn.getOutputStream()会自动connect
        httpConn.connect();

        LOG.debug("Send Response Result to {}.", this.recvUrl);
        //建立输入流，向指向的URL传入json格式的案件信息
        OutputStreamWriter osw = new OutputStreamWriter(httpConn.getOutputStream(), "UTF-8");
        osw.write(responseResult);
        //dos.writeUTF(responseResult);
        osw.flush();
        osw.close();

        // 获得响应状态, 并显示回送消息。
        int resultCode = httpConn.getResponseCode();
        if (resultCode == HttpURLConnection.HTTP_OK) {
            StringBuffer sb = new StringBuffer();
            String readLine;
            BufferedReader responseReader = new BufferedReader(new InputStreamReader(
                    httpConn.getInputStream(), "UTF-8"));

            while ((readLine = responseReader.readLine()) != null) {
                sb.append(readLine).append("\n");
            }

            responseReader.close();
            System.out.println(sb.toString());
            //LOG.debug(sb.toString());
        } else {
            LOG.debug("{} return code: {}.", this.recvUrl, resultCode);
        }
    }

    public static void main(String[] args) throws IOException {
        // 测试的响应消息.
        ResponseResult result = new PcapResponseResult();
        result.ticketid = String.valueOf(161560);
        result.actionResult.put(ResponseAction.PcapCap, true);

        result.files.fileName = "E:\\test1.pcap";
        //result.files.fileContent = Utils.readFileContent(result.files.fileName);
        result.files.fileContent = new String(Utils.readBinaryFile(result.files.fileName), StandardCharsets.ISO_8859_1);
        //System.out.println(result.files.fileName);
        ResponseResultSender resultSender =
                new ResponseResultSender("http://211.65.193.55/chairs/DIM/index.php/Interface/autoReceiveMonster");
        String responseContent = new Gson().toJson(result);
        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream("E:\\out.pcap"));
        bos.write(result.files.fileContent.getBytes(StandardCharsets.ISO_8859_1));
        //bos.write(Utils.readBinaryFile(result.files.fileName));
        bos.close();
        /*
        BufferedWriter bw = new BufferedWriter(new FileWriter("E:\\pcaptest\\out.pcap"));
        bw.write(result.files.fileContent);
        bw.close();
        */
        //bos.close();
        resultSender.send(responseContent);
    }
}
