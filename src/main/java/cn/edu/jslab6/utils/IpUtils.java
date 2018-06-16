package main.java.cn.edu.jslab6.utils;

/**
 * @author Andy
 * @date 18-6-16 上午7:53
 * @description
 */

public class IpUtils {
    public static Integer ipToInt(String ipStr) {
        if (ipStr == null || ipStr.equals(""))
            return null;

        //若此ip是掩码形式
        if (ipStr.contains("/")) {
            ipStr = ipStr.replaceAll("/.*", "");
        }

        String[] ipStrArr = ipStr.split("\\.");
        if (ipStrArr.length < 4)
            return null;

        Integer ipAddr = (Integer.parseInt(ipStrArr[0]) << 24)
                | (Integer.parseInt(ipStrArr[1]) << 16)
                | (Integer.parseInt(ipStrArr[2]) << 8)
                | Integer.parseInt(ipStrArr[3]);

        return ipAddr;
    }

    public static Integer getMask(String maskIp) {
        if (maskIp == null || maskIp.equals("") || !(maskIp.contains("/")))
            return null;

        int type = Integer.parseInt(maskIp.replaceAll(".*/", ""));
        int mask = 0xFFFFFFFF << (32 - type);
        return mask;
    }
}