package main.java.cn.edu.jslab6.manager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;

import main.java.cn.edu.jslab6.config.SystemConfig;

/**
 * @author Andy
 * @date 18-6-17 上午7:27
 * @description
 */
public class AutoResponseMain {
    private static final Logger LOG = LoggerFactory.getLogger(AutoResponseMain.class);
    private static final String CONF_FILE = "./system.properties";
    private static final int NCPU = Runtime.getRuntime().availableProcessors();

    public static void main(String[] args) throws InterruptedException, IOException {
        //读取配置文件
        SystemConfig systemConfig = new SystemConfig(CONF_FILE);

        /*
         用来从CHAIRS接收案件信息, 并将响应任务存入TaskManager中.
          */
        ResponseTaskHttpServer httpServer = new ResponseTaskHttpServer(systemConfig);
        httpServer.start();
        LOG.info("启动任务接收HTTP服务器成功..");

        /*
        启动任务管理线程
        将Chairs发送过来的任务生成活动任务，存入数据库
         */
        ResponseTaskManager taskManager = new ResponseTaskManager();
        Thread taskManagerThread = new Thread(taskManager);
        taskManagerThread.start();
        LOG.info("Start task manager...");
        /**
         启动pcap文件离线切分线程
         离线分离pcap文件，匹配相应任务
         */
        PcapFileOfflineSplit offlineSplit = new PcapFileOfflineSplit();
        Thread splitThread = new Thread(offlineSplit);
        splitThread.start();
        LOG.info("启动离线分割线程成功..");

    }
}