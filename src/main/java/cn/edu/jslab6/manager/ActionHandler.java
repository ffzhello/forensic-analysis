package main.java.cn.edu.jslab6.manager;

import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;

import main.java.cn.edu.jslab6.entity.ActiveTask;
import main.java.cn.edu.jslab6.config.SystemConfig;
import main.java.cn.edu.jslab6.entity.ResponseResult;
import main.java.cn.edu.jslab6.entity.PcapResponseResult;
import main.java.cn.edu.jslab6.enums.ResponseAction;
import main.java.cn.edu.jslab6.utils.Utils;

/**
 * @author Andy
 * @date 18-6-17 上午7:20
 * @description
 */
public class ActionHandler {
    private static Logger LOG  = LoggerFactory.getLogger(ActionHandler.class);
    private static SystemConfig systemConfig = new SystemConfig();
    private static ResponseResultSender resultSender = null;

    //构造器，读取配置文件
    static {
        try {
            systemConfig = new SystemConfig("./system.properties");
        }catch (IOException e) {
            e.printStackTrace();
        }
        resultSender = new ResponseResultSender(systemConfig.getTaskSendUrl());
    }

    //采集完成,返回结果，并做相应的离线检测
    public static void returnResults(ActiveTask task) {
        if (task == null)
            return;
        //回送采包结果
        task.pcapResponseResult= new PcapResponseResult();
        task.pcapResponseResult.ticketid = String.valueOf(task.getTicketid());
        task.pcapResponseResult.actionResult.put(ResponseAction.PcapCap, true);
        task.pcapResponseResult.attach.firstpkttime = task.getFirstpkttime();
        task.pcapResponseResult.attach.lastpkttime = task.getLastpkttime();

        int start = task.getMegedPatFileCount() + 1;
        int end = task.getPatFileCount();

        if (start <= end) {
            for (; start <= end; start ++) {
                String file = task.getDirPath() + "PAT" + String.valueOf(start) + ".pcap";
                byte[] b;
                if ((b = Utils.readBinaryFile(file)) != null) {
                    task.pcapResponseResult.files.fileContent = new String(b, StandardCharsets.ISO_8859_1);
                    // LOG.debug("Convert file {} to Latin-1 format, binary size = {}, string size = {} ", result.files.fileName, b.length, result.files.fileContent.length());
                    if (task.getUsername().equals("CHAIRS")) {
                        String pcapResult = new Gson().toJson(task.pcapResponseResult);
                        try {
                            resultSender.send(pcapResult);
                            LOG.debug("Task[id: " + task.getId() + "] Return " + task.pcapResponseResult.files.fileContent.length() + " bytes packets to CHAIRS success. ");
                        }catch (IOException e) {
                            e.printStackTrace();
                        }
                    } else {
                        //  LOG.debug("Finsh pcap capture, but response task are not from CHAIRS!");
                        LOG.debug("Task[id: " + task.getId() + "] Return " + task.pcapResponseResult.files.fileContent.length() + " bytes packets to WHO success. " );
                    }
                }
            }
        }else {
            task.pcapResponseResult.files.fileContent = "";
            if (task.getUsername().equals("CHAIRS")) {
                String pcapResult = new Gson().toJson(task.pcapResponseResult);
                try {
                    resultSender.send(pcapResult);
                    // LOG.debug("Task {} finish pcap capture, ID = {}", task.getTicketId(), task.getId());
                    LOG.debug("Task[id：" + task.getId() + "] Return 0 bytes packets to CHAIRS.");
                }catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                //  LOG.debug("Finsh pcap capture, but response task are not from CHAIRS!");
                LOG.debug("Task[id: " + task.getId() + "] Return 0 bytes packets to WHO.");
            }
        }

        if (task.getUsername().equals("CHAIRS")) {
            // 将IDS相关检测内容封装成ResponseResult格式，发往CHAIRS系统。
            String idsDetectResult = new Gson().toJson(task.responseResult);
            try {
                resultSender.send(idsDetectResult);
            } catch (IOException e) {
                e.printStackTrace();
            }
            LOG.debug("Task[id：" + task.getId() +  "] Return IDS Results to CHAIRS success.");
        } else {
            // FIXME: 不是CHAIRS发送的案件，有需求的话，可以做一些额外的工作。
            LOG.debug("Task[id：" + task.getId() +  "] Return IDS Results to WHO success.");
        }
        //LOG.debug("Task {} finish IDSAction detect, ID = {}", task.getTicketId(), task.getId());

        //初始化内存任务
        int patCount = task.getPatFileCount();
        task.setMegedPatFileCount(patCount);

        task.setTmpFilename("");
        task.setTmpFileSize(0);
        task.setMegFilename("");

        //更新数据库
        ActiveTaskManager.updateActiveTaskByTask(task);

    }



    //test
    public static void main(String[] args) {
        /*ActiveTask task = new ActiveTask();
        task.setId(1);
        task.setTicketId(2);
        task.setFirstpkttime(1111);
        task.setLastpkttime(2222);
        task.setCurFilename("D:/0.pcap");

        task.setCurFilename("D:/1.pcap");

        task.setUsername("CHAIRS");

        ActionHandler ah = new ActionHandler();*/
    }
}