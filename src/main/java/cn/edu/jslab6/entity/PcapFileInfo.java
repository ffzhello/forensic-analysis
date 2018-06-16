package main.java.cn.edu.jslab6.entity;

/**
 * @author Andy
 * @date 18-6-16 下午4:47
 * @description
 */
public class PcapFileInfo {
    //field
    private Integer id;
    //pcap文件路径
    private String filepath;
    //pcap文件第一个数据包时间戳
    private Integer firstPktTime;
    //pcap文件最后一个数据包时间戳
    private Integer lastPktTime;

    public PcapFileInfo() {
        this.id = 0;
        this.filepath = "";
        this.firstPktTime = 0;
        this.lastPktTime = 0;
    }

    public PcapFileInfo(Integer id, String filepath, Integer firstPktTime, Integer lastPktTime) {
        if(id == null || id < 0 || filepath == null || filepath.equals("") || firstPktTime == null || firstPktTime < 0 || lastPktTime == null || lastPktTime < 0) {
            //log
            System.out.println("error");
            return;
        }

        this.id = id;
        this.filepath = filepath;
        this.firstPktTime = firstPktTime;
        this.lastPktTime = lastPktTime;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getFilepath() {
        return filepath;
    }

    public void setFilepath(String filepath) {
        this.filepath = filepath;
    }

    public Integer getFirstPktTime() {
        return firstPktTime;
    }

    public void setFirstPktTime(Integer firstPktTime) {
        this.firstPktTime = firstPktTime;
    }

    public Integer getLastPktTime() {
        return lastPktTime;
    }

    public void setLastPktTime(Integer lastPktTime) {
        this.lastPktTime = lastPktTime;
    }

    //method
    public static void main(String[] args) {
        PcapFileInfo p = new PcapFileInfo(1,"",1,3);
    }
}