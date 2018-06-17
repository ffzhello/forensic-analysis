package main.java.cn.edu.jslab6.entity;

/**
 * @author Andy
 * @date 18-6-16 上午7:39
 * @description Ip、掩码格式定义
 */
public class IpMask {
    private long ip;
    private int mask;

    public IpMask(long ip, int mask) {
        this.ip = ip;
        this.mask = mask;
    }

    public void setIp(long ip) {
        this.ip = ip;
    }
    public long getIp() {
        return ip;
    }

    public void setMask(int mask) {
        this.mask = mask;
    }

    public int getMask() {
        return mask;
    }

}
