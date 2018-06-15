package com.java.cn.edu.jslab6.entity;

/**
 * @author Andy
 * @date 18-6-16 上午7:39
 * @description Ip、掩码格式定义
 */
public class IpMask {
    private int ip;
    private int mask;

    public void setIp(int ip) {
        this.ip = ip;
    }
    public int getIp() {
        return ip;
    }

    public void setMask(int mask) {
        this.mask = mask;
    }

    public int getMask() {
        return mask;
    }

}
