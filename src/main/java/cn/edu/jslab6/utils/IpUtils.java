package main.java.cn.edu.jslab6.utils;

/**
 * @author Andy
 * @date 18-6-16 上午7:53
 * @description
 */

public class IpUtils {
    public static Long ipToLong(String ipStr) {
        if (ipStr == null || ipStr.equals(""))
            return null;

        //若此ip是掩码形式
        if (ipStr.contains("/")) {
            ipStr = ipStr.replaceAll("/.*", "");
        }

        String[] ipStrArr = ipStr.split("\\.");
        if (ipStrArr.length < 4)
            return null;

        Long ipAddr = (Long.parseLong(ipStrArr[0]) << 24)
                | (Long.parseLong(ipStrArr[1]) << 16)
                | (Long.parseLong(ipStrArr[2]) << 8)
                | (Long.parseLong(ipStrArr[3]));

        return ipAddr;
    }

    public static Integer getMask(String maskIp) {
        if (maskIp == null || maskIp.equals("") || !(maskIp.contains("/")))
            return null;

        int type = Integer.parseInt(maskIp.replaceAll(".*/", ""));
        int mask = 0xFFFFFFFF << (32 - type);
        return mask;
    }

    /**
     * 获取起始ip主机号
     * @param ip
     * @return
     */
    private static String startIP(String ip) {
        String string = ip.replaceAll("1", "0");
        return string;
    }

    /**
     * 获取终止ip主机号
     * @param ip
     * @return
     */
    private static String endIP(String ip) {
        String string = ip.replaceAll("0", "1");
        return string;
    }

    /**
     * 将二进制ip转换成点分十进制形式
     * @param ip
     * @return
     */
    private static String changeBinIpToIpStr(String ip) {
        String str = "";
        for (int j = 0; j < ip.length(); j += 8) {
            String ip1 = ip.substring(j, j + 8);
            ip1 = Integer.valueOf(ip1, 2).toString();
            str += ip1 + ".";
        }
        str = str.substring(0, str.length() - 1);
        return str;
    }

    /**
     * 根据ip地址段获取起始地址
     * @param ip 格式："211.65.193.129/32"
     * @return
     */
    public static long[] getStartAndEndIp(String ip) {

        long[] startAndEndIp = {0,0};

        String[] ipString = ip.split("\\/");
        if (ipString.length < 2)
            return null;

        ip = ipString[0];
        int i = Integer.parseInt(ipString[1]);
        String[] strings = ip.split("\\.");
        String str, str1 = "";
        for (String string : strings) {
            str = Integer.toBinaryString(Integer.parseInt(string));
            if (str.length() < 8) {
                int j = (int) Math.pow(10, 8 - str.length());
                str = ("" + j + str);
                str = str.substring(1, str.length());
            }
            str1 += str;
        }

        ipString[0] = changeBinIpToIpStr(str1.substring(0, i) + startIP(str1.substring(i, 32)));
        ipString[1] = changeBinIpToIpStr(str1.substring(0, i) + endIP(str1.substring(i, 32)));

        startAndEndIp[0] = ipToLong(ipString[0]);
        startAndEndIp[1] = ipToLong(ipString[1]);

        return startAndEndIp;
    }

    public static void main(String[] args) {
        System.out.println(ipToLong("8.8.8.8/32"));
        System.out.println(ipToLong("8.8.8.8"));
    }
}