package com.java.cn.edu.jslab6.entity;

import com.java.cn.edu.jslab6.enums.ResponseAction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 响应任务格式定义
 */
class RawResponseTask {
    String ticketid = "12345";
    List<String> ipList = Arrays.asList(new String[]{"211.65.192.177/32"});
    class Config {
        String action = "PcapCap;SuricataDetect;BroDetect";
        String priority = "5";
        String flowDirection = "2";
        String srcIP = "1";
        String srcPort = "-1";
        String dstIP = "1";
        String dstPort = "-1";
        String srcIPDstIP = "0";
        String srcPortDstPort = "0";
        String protocol = "255";
        String timelen = "300";
        String thresholdPkts = "20000";
    }

    Config config = new Config();
    String username = "CHAIRS";

}
public class ResponseTask implements Comparable<ResponseTask>{

    private int id;
    private int ticketid;
    private List<String> ipList = new ArrayList<>();
    //响应动作
    private String actionStr = null;
    private List<ResponseAction> actionList = new ArrayList<>();

    private int priority = 5;
    private int flowDirection = 2;
    private int srcIP = 1;
    private int srcPort = 65535;
    private int dstIP = 1;
    private int dstPort = 65535;
    private int srcIPDstIP = 0;
    private int srcPortDstPort = 0;
    private int protocol = 255;
    private int timelen = 300;           //采集时长(s)
    private int thresholdPkts = 20000;   //采集报文个数
    private String username = "CHAIRS";  //创建响应任务的用户名

    public ResponseTask() {

    }

    /**
     * @param rawTask
     * @description 最大ip采集数目：10
     */
    public ResponseTask(RawResponseTask rawTask) {
        setTicketid(Integer.parseInt(rawTask.ticketid));

        if (rawTask.ipList.size() <= 10) {
            getIpList().addAll(rawTask.ipList);
        }else {
            getIpList().addAll(rawTask.ipList.subList(0, 10));
        }

        for (String s: rawTask.config.action.split(";")) {

            getActionList().add(Enum.valueOf(ResponseAction.class, s));
        }

        setActionStr(rawTask.config.action);
        setPriority(Integer.parseInt(rawTask.config.priority));
        setFlowDirection(Integer.parseInt(rawTask.config.flowDirection));
        setSrcIP(Integer.parseInt(rawTask.config.srcIP));
        setDstIP(Integer.parseInt(rawTask.config.dstIP));
        setSrcPort(Integer.parseInt(rawTask.config.srcPort));
        setDstPort(Integer.parseInt(rawTask.config.dstPort));
        setSrcIPDstIP(Integer.parseInt(rawTask.config.srcIPDstIP));
        setSrcPortDstPort(Integer.parseInt(rawTask.config.srcPortDstPort));
        setProtocol(Integer.parseInt(rawTask.config.protocol));
        setTimelen(Integer.parseInt(rawTask.config.timelen));
        setThresholdPkts(Integer.parseInt(rawTask.config.thresholdPkts));
        setUsername(rawTask.username);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getTicketid() {
        return ticketid;
    }

    public void setTicketid(int ticketid) {
        this.ticketid = ticketid;
    }

    public List<String> getIpList() {
        return ipList;
    }

    public void setIpList(List<String> ipList) {
        this.ipList = ipList;
    }

    public void setActionStr(String actionStr) {
        this.actionStr = actionStr;

        if (actionStr != null) {
            String[] actionArr = actionStr.split(";");
            if (actionArr.length > 0) {
                for (String ac: actionArr) {
                    ResponseAction ra = ResponseAction.valueOf(ac);
                    if (ra != null)
                        actionList.add(ra);
                }
            }
        }
    }

    public String getActionStr() {
        return actionStr;
    }

    public List<ResponseAction> getActionList() {
        return actionList;
    }

    public void setActionList(List<ResponseAction> actionList) {
        this.actionList = actionList;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public int getFlowDirection() {
        return flowDirection;
    }

    public void setFlowDirection(int flowDirection) {
        this.flowDirection = flowDirection;
    }

    public int getSrcIP() {
        return srcIP;
    }

    public void setSrcIP(int srcIP) {
        this.srcIP = srcIP;
    }

    public int getSrcPort() {
        return srcPort;
    }

    public void setSrcPort(int srcPort) {
        this.srcPort = srcPort;
    }

    public int getDstIP() {
        return dstIP;
    }

    public void setDstIP(int dstIP) {
        this.dstIP = dstIP;
    }

    public int getDstPort() {
        return dstPort;
    }

    public void setDstPort(int dstPort) {
        this.dstPort = dstPort;
    }

    public int getSrcIPDstIP() {
        return srcIPDstIP;
    }

    public void setSrcIPDstIP(int srcIPDstIP) {
        this.srcIPDstIP = srcIPDstIP;
    }

    public int getSrcPortDstPort() {
        return srcPortDstPort;
    }

    public void setSrcPortDstPort(int srcPortDstPort) {
        this.srcPortDstPort = srcPortDstPort;
    }

    public int getProtocol() {
        return protocol;
    }

    public void setProtocol(int protocol) {
        this.protocol = protocol;
    }

    public int getTimelen() {
        return timelen;
    }

    public void setTimelen(int timelen) {
        this.timelen = timelen;
    }

    public int getThresholdPkts() {
        return thresholdPkts;
    }

    public void setThresholdPkts(int thresholdPkts) {
        this.thresholdPkts = thresholdPkts;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    @Override
    public int compareTo(ResponseTask task) {
        if (getPriority() < task.getPriority()) return 1;
        if (getPriority() > task.getPriority()) return -1;
        return 0;
    }



    public String ipListToString() {
        StringBuilder sb = new StringBuilder();
        for (String ip: ipList) {
            sb.append(ip).append(";");
        }

        //去除最后一个分号
        sb.deleteCharAt(sb.length() - 1);
        return sb.toString();
    }

    public static void main(String[] args) {

        RawResponseTask rawResponseTask = new RawResponseTask();
        ResponseTask responseTask = new ResponseTask(rawResponseTask);

        System.out.println(responseTask.ipListToString());
    }


}
