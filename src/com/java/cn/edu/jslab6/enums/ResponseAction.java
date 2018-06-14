package com.java.cn.edu.jslab6.enums;

/**
 * @Author: Andy
 * @Date: 18-6-14 下午9:19
 * @Description: 响应动作
 */
public enum ResponseAction {
    PcapPcap, SuricataDetect, BroDetect;

    public String toString() {
        return name();
    }

    public static void main(String[] args) {
        for (ResponseAction action: values()) {
            System.out.println(action);
        }
    }
}
