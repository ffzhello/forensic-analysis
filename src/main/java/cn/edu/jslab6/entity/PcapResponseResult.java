package main.java.cn.edu.jslab6.entity;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.nio.charset.StandardCharsets;

import main.java.cn.edu.jslab6.enums.ResponseAction;
import main.java.cn.edu.jslab6.utils.Utils;

/**
 * @author Andy
 * @date 18-6-16 下午6:04
 * @description
 */
public class PcapResponseResult extends ResponseResult {
    Attach attach = new Attach();
    class Attach {
        long firstpkttime = 0;
        long lastpkttime = 0;
    }

    public static void main(String[] args) {
        PcapResponseResult result = new PcapResponseResult();
        result.ticketid = String.valueOf(16902);
        result.actionResult.put(ResponseAction.PcapCap, true);
        result.files.fileName = "/home/monster/hydra_sensor/4223/0.pcap";
        result.files.fileContent = new String(Utils.readBinaryFile("E:\\forensictask\\0.pcap"),
                StandardCharsets.ISO_8859_1);
        result.attach.firstpkttime = 1494248170;
        result.attach.lastpkttime = 1494248465;

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        System.out.println(gson.toJson(result));
    }
    /*
    public static void main(String[] args) {
        PcapResponseResult result = new PcapResponseResult();
        result.ticketid = String.valueOf(task.getTicketid());
        result.actionResult.put(ResponseAction.PcapCap, true);
        // 从数据库sensorfile中读取出当前采包的文件路径。
        // XXX: 一个响应任务可能对应有多个文件(文件划分大小的存在),但是在自动应急响应中，报文采集大小
        //      未达到报文分片的大小，所以对于一次采集任务只会有一个pcap文件。
        String sql = "SELECT filename FROM sensorfile WHERE sensortaskid = " + sensorTask.getId();
        String filename = "";
        try {
            ResultSet rs = statement.executeQuery(sql);
            if (rs.first()) {
                filename = rs.getString(1);
            } else {
                result.actionResult.put(ResponseAction.PcapCap, false);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        sensorTask.setFilepath(filename);
        result.files.fileName = filename;
        LOG.debug("Task {} 's pcap file locate at {}.", task.getTicketid(), sensorTask.getFilepath());
        // 读取采集报文样本, 服务器上的报文保存格式为latin-1(ISO_8859_1),发送前将二进制编码为latin-1格式，在服务器端进行
        // 编码转换即可。
        if (!filename.equals("")) {
            byte[] b;
            if ((b = Utils.readBinaryFile(filename)) != null) {
                result.files.fileContent = new String(b, StandardCharsets.ISO_8859_1);
                LOG.debug("Convert file {} to Latin-1 format, binary size = {}, string size = {} ",
                        result.files.fileName, b.length, result.files.fileContent.length());
            }
        } else {
            result.files.fileContent = "";
        }
        LOG.debug("Task {} 's pcap file length = {} bytes.", task.getTicketid(), result.files.fileContent.length());

        try {
            sql = "SELECT firstpkttime, lastpkttime FROM sensortask WHERE id = " + sensorTask.getId();
            LOG.debug(sql);
            ResultSet rs = statement.executeQuery(sql);
            if (rs.first()) {
                result.attach.firstpkttime = rs.getInt(1);
                result.attach.lastpkttime = rs.getInt(2);
            } else {
                result.attach.firstpkttime = 0;
                result.attach.lastpkttime = 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        if (task.getUsername().equals("CHAIRS")) {
            // Send Back pcapResult(in json format) to CHAIRS.
            String pcapResult = new Gson().toJson(result);
            try {
                resultSender.send(pcapResult);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            // FIXME: 不是CHAIRS发送的案件，有需求的话，可以做一些额外的工作。
            LOG.debug("Finsh pcap capture, but response task are not from CHAIRS!");
        }
        //LOG.debug("Back to CHAIRS: {}",  pcapResult);
        LOG.debug("Task {} finish pcap capture, ID = {}", task.getTicketid(), task.getId());
        return sensorTask;
    }
    */
}