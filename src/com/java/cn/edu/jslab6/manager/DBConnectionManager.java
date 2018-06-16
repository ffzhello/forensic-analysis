package com.java.cn.edu.jslab6.manager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import com.java.cn.edu.jslab6.config.SystemConfig;
/**
 * @author Andy
 * @date 18-6-16 上午11:09
 * @description 数据库连接类
 */
public class DBConnectionManager {
    private static final Logger LOG = LoggerFactory.getLogger(DBConnectionManager.class);
    private Connection conn = null;
    private SystemConfig systemConfig = new SystemConfig();

    public Connection getConn() {
        return conn;
    }

    public SystemConfig getSystemConfig() {
        return systemConfig;
    }

    public DBConnectionManager() {
        try {
            systemConfig = new SystemConfig("./system.properties");
            LOG.info("read system.properties");
        }catch (IOException e) {
            e.printStackTrace();
        }
        //初始化数据库
        initMysql();
    }

    //初始化数据库
    public void initMysql() {
        initMysql(systemConfig.getMysqlIP(), systemConfig.getMysqlPort(), systemConfig.getMysqlUsername(),
                systemConfig.getMysqlPasswd(), systemConfig.getMysqlDatabase());
    }

    private void initMysql(String ip, int port, String username, String passwd, String database) {
        try {
            Class.forName("com.mysql.jdbc.Driver");
            //Class.forName("com.mysql.cj.jdbc.Driver");
            try {
                String url = "jdbc:mysql://" + ip + ":" + port + "/" + database;
                conn = DriverManager.getConnection(url, username, passwd);
                //statement = conn.createStatement();
                LOG.info("Connect database {}.", url);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * To detect if the connection is closed by the server as connection is timeout.
     * @author Martin
     * @param conn
     * @return true if the connection is normal, otherwise, return false.
     */
    public static boolean validate(Connection conn)
    {
        boolean isValidated = true;
        try {
            com.mysql.jdbc.Connection c = (com.mysql.jdbc.Connection)conn;
            c.ping();
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            // e.printStackTrace();
            isValidated = false;
        }
        return isValidated;
    }

    public static void main(String[] args) {
        DBConnectionManager dbConnectionManager = new DBConnectionManager();
    }
}
