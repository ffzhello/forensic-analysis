package main.java.cn.edu.jslab6.entity;

import main.java.cn.edu.jslab6.enums.ResponseAction;
import main.java.cn.edu.jslab6.utils.IpUtils;
import org.pcap4j.core.PcapDumper;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author Andy
 * @Date 18-6-14 下午9:57
 * @Description 采集任务格式定义
 */
public class ActiveTask extends ResponseTask{

    private int status = 0; //采集任务状态
    private String dirPath = ""; //采集任务文件路径
    private int tmpDirCount = 0; //采集任务目录个数
    private String ipString = null;

    //当前文件周期pcap文件
    private String tmpFilename = "";
    //当前文件周期pcap文件大小
    private int tmpFileSize = 0;

    //临时文件个数
    private int patFileCount = 0;
    //已合并的临时文件个数
    private int megedPatFileCount = 0;

    //合并文件个数
    private int megFileCount = 0;
    //当前文件周期合并文件
    private String megFilename = "";

    //任务的总pcapwenjain
    private String filename = "";

    //任务上次心跳时间
    private long lastInteractTime = 0L;

    //采集任务起始时间
    private long starttime = 0;
    private long endtime = 0;

    //任务采集报文的起始时间
    private long firstpkttime = 0;
    private long lastpkttime = 0;

    //任务已采集到的数据包个数
    private int sensorBytes = 0;

    //写数据包到文件
    public PcapDumper dumper = null;

    private List<IpMask> ipMaskList = new ArrayList<IpMask>();

    private List<Long> ipLongList = new ArrayList<>();

    public ActiveTask () {

    }

    public int getSensorBytes() {
        return sensorBytes;
    }

    public void setSensorBytes(int sensorBytes) {
        this.sensorBytes = sensorBytes;
    }


    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getDirPath() {
        return dirPath;
    }

    public void setDirPath(String dirPath) {
        this.dirPath = dirPath;
    }

    public int getTmpDirCount() {
        return tmpDirCount;
    }

    public void setTmpDirCount(int tmpDirCount) {
        this.tmpDirCount = tmpDirCount;
    }

    public String getTmpFilename() {
        return tmpFilename;
    }

    public void setTmpFilename(String tmpFilename) {
        this.tmpFilename = tmpFilename;
    }

    public int getTmpFileSize() {
        return tmpFileSize;
    }

    public void setTmpFileSize(int tmpFileSize) {
        this.tmpFileSize = tmpFileSize;
    }

    public int getPatFileCount() {
        return patFileCount;
    }

    public void setPatFileCount(int patFileCount) {
        this.patFileCount = patFileCount;
    }

    public int getMegedPatFileCount() {
        return megedPatFileCount;
    }

    public void setMegedPatFileCount(int megedPatFileCount) {
        this.megedPatFileCount = megedPatFileCount;
    }

    public int getMegFileCount() {
        return megFileCount;
    }

    public void setMegFileCount(int megFileCount) {
        this.megFileCount = megFileCount;
    }

    public String getMegFilename() {
        return megFilename;
    }

    public void setMegFilename(String megFilename) {
        this.megFilename = megFilename;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public long getLastInteractTime() {
        return lastInteractTime;
    }

    public void setLastInteractTime(long lastInteractTime) {
        this.lastInteractTime = lastInteractTime;
    }

    public void setStarttime(long starttime) {
        this.starttime = starttime;
    }

    public long getStarttime() {
        return starttime;
    }

    public void setEndtime(long endtime) {
        this.endtime = endtime;
    }

    public long getEndtime() {
        return endtime;
    }

    public long getFirstpkttime() {
        return firstpkttime;
    }

    public void setFirstpkttime(long firstpkttime) {
        this.firstpkttime = firstpkttime;
    }

    public long getLastpkttime() {
        return lastpkttime;
    }

    public void setLastpkttime(long lastpkttime) {
        this.lastpkttime = lastpkttime;
    }

    public void setIpString(String ipString) {
        this.ipString = ipString;
        //set iplist
        String[] ipArr;
        if (ipString.contains(";")) {
            ipArr = ipString.split(";");
        }else {
            ipArr = new String[]{ipString};
        }

        for (String ip: ipArr) {
            Long ipLong = IpUtils.ipToLong(ip);
            ipLongList.add(ipLong);
        }
    }

    public List<Long> getIpLongList() {
        return ipLongList;
    }

    public String getIpString() {
        return ipString;
    }

    public PcapDumper getDumper() {
        return dumper;
    }

    public void setDumper(PcapDumper dumper) {
        this.dumper = dumper;
    }

    public void setIpMaskList(String  ipString) {
        if (ipString == null || ipString.isEmpty())
            return;

        String[] ipArr = ipString.split(";");
        if (ipArr.length > 0) {
            long ip =  0;
            int mask = 0;
            for (String str: ipArr) {
                ip = IpUtils.ipToLong(str);
                mask = IpUtils.getMask(str);

                IpMask ipMask = new IpMask(ip,mask);
                if (ipMask != null)
                    ipMaskList.add(ipMask);
            }
        }
    }

    public List<IpMask> getIpMaskList() {
        return ipMaskList;
    }

    public static void main(String[] args) {
        ActiveTask task = new ActiveTask();
        task.setIpString("11.11.11.11/24;2.2.2.2/3");

        for (IpMask ipMask: task.getIpMaskList() ) {
            System.out.print("ip: " + ipMask.getIp());
        }

        task.setRawActions("PcapCap;BroDetect");

        for (ResponseAction ac: task.getActions()) {
            System.out.println(ac);
        }

    }
}
