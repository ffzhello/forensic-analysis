package com.java.cn.edu.jslab6.config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * @author Andy
 * @date 18-6-16 上午11:13
 * @description 系统配置
 */
public class SystemConfig {

    private String mysqlIP = "127.0.0.1";
    private int mysqlPort = 3306;
    private String mysqlDatabase = "autoresponse";
    private String mysqlUsername = "root";
    private String mysqlPasswd = "rootofmysql";

    public String getMysqlIP() {
        return mysqlIP;
    }

    public void setMysqlIP(String mysqlIP) {
        this.mysqlIP = mysqlIP;
    }

    public int getMysqlPort() {
        return mysqlPort;
    }

    public void setMysqlPort(int mysqlPort) {
        this.mysqlPort = mysqlPort;
    }

    public String getMysqlDatabase() {
        return mysqlDatabase;
    }

    public void setMysqlDatabase(String mysqlDatabase) {
        this.mysqlDatabase = mysqlDatabase;
    }

    public String getMysqlUsername() {
        return mysqlUsername;
    }

    public void setMysqlUsername(String mysqlUsername) {
        this.mysqlUsername = mysqlUsername;
    }

    public String getMysqlPasswd() {
        return mysqlPasswd;
    }

    public void setMysqlPasswd(String mysqlPasswd) {
        this.mysqlPasswd = mysqlPasswd;
    }

    public SystemConfig() {

    }

    public SystemConfig(String confFile) throws IOException {
        load(confFile);
    }


    public void load(String confFile) throws IOException {
        Properties pps = new Properties();
        pps.load(new FileInputStream(confFile));

        if (pps.getProperty("mysqlIP") != null) {
            mysqlIP = pps.getProperty("mysqlIP");
        }

        if (pps.getProperty("mysqlPort") != null) {
            mysqlPort = Integer.parseInt(pps.getProperty("mysqlPort"));
        }

        if (pps.getProperty("mysqlDatabase") != null) {
            mysqlDatabase = pps.getProperty("mysqlDatabase");
        }

        if (pps.getProperty("mysqlUsername") != null) {
            mysqlUsername = pps.getProperty("mysqlUsername");
        }

        if (pps.getProperty("mysqlPasswd") != null) {
            mysqlPasswd = pps.getProperty("mysqlPasswd");
        }

    }
}
