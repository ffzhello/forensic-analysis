package com.java.cn.edu.jslab6.manager;
import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.List;
import com.java.cn.edu.jslab6.entity.ResponseTask;
import com.java.cn.edu.jslab6.enums.ResponseAction;
import com.java.cn.edu.jslab6.enums.ActiveTaskStatus;

/**
 * @author Andy
 * @date 18-6-16 上午11:07
 * @description 响应任务管理类
 */
public class ResponseTaskManager implements Runnable{
    private static final Logger LOG = LoggerFactory.getLogger(ResponseTaskManager.class);
    private static DBConnectionManager dbConnectionManager = new DBConnectionManager();
    private static Connection conn = null;

    String sql = null;
    private static final int NUM = 3200;
    private static ArrayBlockingQueue<ResponseTask> unHandledTaskQueue = new ArrayBlockingQueue<ResponseTask>(NUM);

    //保存Chairs发送的任务
    public static void addUnhandledTask(ResponseTask task) throws InterruptedException {
        if(task == null) {
            return;
        }
        unHandledTaskQueue.put(task);
        LOG.debug("Task[ticketid:" + task.getTicketid() + "] receive success");
    }

    //取出任务
    private static ResponseTask getUnhandledTask() throws InterruptedException {
        ResponseTask task = unHandledTaskQueue.take();
        return task;
    }

    //将任务存入数据库
    @Override
    public void run() {
        while(true) {
            ResponseTask task = null;
            try {
                task = getUnhandledTask();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            if (task != null) {

                //根据action过滤
                List<ResponseAction> actionList = task.getActions();
                if(actionList.contains(ResponseAction.PcapCap)) {
                    //将活动任务插入数据库activetask活动任务表中
                    long starttime = (System.currentTimeMillis())/1000;
                    long endtime = starttime + task.getTimelen();

                    //去除重复案件
                    sql = "SELECT COUNT(*) FROM activetask WHERE ticketid = " + task.getTicketid() + " AND status < " + ActiveTaskStatus.FINISHED.getValue() +";";
                    try {
                        conn = dbConnectionManager.getConn();
                        if(!(DBConnectionManager.validate(conn))) {
                            LOG.info("Mysql connect have been timed out, restart the connect...");
                            dbConnectionManager.initMysql();
                            conn = dbConnectionManager.getConn();
                        }
                        Statement statement = conn.createStatement();
                        ResultSet rs = statement.executeQuery(sql);

                        int count = 0;
                        while (rs.next()) {
                            count = rs.getInt(1);
                        }

                        if (count == 0) {

                            sql = "INSERT INTO activetask(ticketid, iplist, action, priority, inoutflag," +
                                    "srcipflag, srcipdstip, dstipflag, srcport, srcportdstport, dstport, protocol," +
                                    "starttime, endtime, numpkts, thresholdpkts, filesplit, username, status) VALUES(" +
                                    task.getTicketid() + ",\"" + task.ipListToString() + "\",\"" + task.getRawActions() + "\"," +
                                    task.getPriority() + "," + task.getFlowDirection() + "," + task.getSrcIP() + "," +
                                    task.getSrcIPDstIP() + "," + task.getDstIP() + "," + task.getSrcPort() + "," +
                                    task.getSrcPortDstPort() + "," + task.getDstPort() + "," + task.getProtocol() + "," +
                                    starttime + "," + endtime + "," + 0 + "," + task.getThresholdPkts() + "," + task.getFilesplit() +
                                    ",\"" + task.getUsername() + "\"," + ActiveTaskStatus.WAIT_SENSOR.getValue() + ");";

                            statement.executeUpdate(sql);

                        }else {
                            LOG.debug("数据库已存在该案件[ticketid: "+ task.getTicketid() +"]的采集任务");
                        }
                    }catch (SQLException e) {
                        LOG.debug("新采集任务[ticketid:" + task.getTicketid() + "]插入数据库失败...");
                        e.printStackTrace();
                    }
                }else
                    LOG.debug("新任务不要采集报文..");
            }
        }
    }

    public static void main(String[] args) throws Exception{
        ResponseTaskManager manager = new ResponseTaskManager();
        ResponseTask task = new ResponseTask();
        task.setTicketid(11);
        task.setIpList(Arrays.asList(new String[] {"11.11.11.11/32","22.22.22.22/32"}));
        task.setRawActions("PcapCap;BroDetect");
        manager.addUnhandledTask(task);
        Thread thread = new Thread(manager);
        thread.start();
    }
}
