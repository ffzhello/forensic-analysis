package main.java.cn.edu.jslab6.manager;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

import main.java.cn.edu.jslab6.entity.PcapFileInfo;
import main.java.cn.edu.jslab6.entity.ActiveTask;
import main.java.cn.edu.jslab6.utils.Utils;

/**
 * @author Andy
 * @date 18-6-16 下午4:55
 * @description
 */
public class PcapFileManager {

    private static final Logger LOG = LoggerFactory.getLogger(PcapFileManager.class);
    private static DBConnectionManager dbConnectionManager = new DBConnectionManager(); //保持一个数据库连接,从数据库读取离线pcap文件
    private static Connection conn = dbConnectionManager.getConn();

    private static String sql = null;

    /**
     * 验证连接
     */
    private static void validateConnection() {
        //判断数据库是否连接
        if(!(DBConnectionManager.validate(conn))) {
            dbConnectionManager.initMysql();
            conn = dbConnectionManager.getConn();
        }
    }

    /**
     * 从数据库中读取未处理的pcap文件信息
     * @return
     */
    public static PcapFileInfo getUnhandlerPcapFile() {
        PcapFileInfo fileInfo = new PcapFileInfo();
        sql = "select id, filepath, firstpkttime, lastpkttime from pcapfileinfo where status = 0 limit 1" ;
        Statement statement = null;
        ResultSet resultSet = null;
        try {
            validateConnection();
            statement = conn.createStatement();
            resultSet = statement.executeQuery(sql);
            while (resultSet.next()) {
                fileInfo.setId(resultSet.getInt("id"));
                fileInfo.setFilepath(resultSet.getString("filepath"));
                fileInfo.setFirstPktTime(resultSet.getInt("firstpkttime"));
                fileInfo.setLastPktTime(resultSet.getInt("lastpkttime"));
                LOG.debug("从数据库获得PCAP文件[文件名：" + fileInfo.getFilepath() +"]" );
                return fileInfo;
            }
        }catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (statement != null) {
                try {
                    statement.close();
                }catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            if (resultSet != null) {
                try {
                    resultSet.close();
                }catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    /**
     * 从数据库读出所有未处理的pcap文件信息
     * @return
     */
    public static List<PcapFileInfo> getUnhanlderedPcapFilefromDB() {
        List<PcapFileInfo> pcapFileInfoList = new ArrayList<PcapFileInfo>();

        sql = "select id, filepath, firstpkttime, lastpkttime from pcapfileinfo where status = 0" ;
        Statement statement = null;
        ResultSet resultSet = null;
        try{
            validateConnection();
            statement = conn.createStatement();
            resultSet = statement.executeQuery(sql);
            while (resultSet.next()) {
                PcapFileInfo fi = new PcapFileInfo();
                fi.setId(resultSet.getInt("id"));
                fi.setFilepath(resultSet.getString("filepath"));
                fi.setFirstPktTime(resultSet.getInt("firstpkttime"));
                fi.setLastPktTime(resultSet.getInt("lastpkttime"));
                //插入list中
                pcapFileInfoList.add(fi);
                LOG.debug("从数据库获得PCAP文件[文件名：" + fi.getFilepath() +"]" );
            }
        }catch (SQLException e){
            e.printStackTrace();
        } finally {
            if (statement != null) {
                try {
                    statement.close();
                }catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            if (resultSet != null) {
                try {
                    resultSet.close();
                }catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
        return pcapFileInfoList;
    }

    /**
     * 更新数据库pcap文件状态
     * @param id
     */
    public static void updatePcapFileInfoFromDB(Integer id) {
        if(id == null || id < 0) {
            return;
        }

        sql = "update pcapfileinfo set status = 1 where id = " + id;
        Statement statement = null;
        try {
            validateConnection();
            statement = conn.createStatement();
            statement.executeUpdate(sql);
        }catch (SQLException e) {
            LOG.debug("更新PCAP文件[id：" + id + "]失败.");
            e.printStackTrace();
        } finally {
            if (statement != null) {
                try {
                    statement.close();
                }catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     *
     * @param task
     * @param tmp = 0:合并周期内pcap   tmp = 1:合并总pcap文件
     * @throws IOException
     */
    public static void mergePcapfiles(ActiveTask task, boolean tmp) throws IOException {

        if(task == null)
            return ;

        String dir = task.getDirPath();
        byte[] totalPcap = null;

        int start = 1;
        int end ;
        String identy;

        if (tmp == true) {
            start = task.getMegedPatFileCount() + 1;
            end = task.getPatFileCount();
            identy = "PAT";
        }else {
            end = task.getMegFileCount();
            identy = "MEG";
        }

        if (end < start)
            return;

        boolean first = true;
        for( ; start <= end; start ++) {
            System.out.println("start: " + start);
            if (first) {
                totalPcap = Utils.readBinaryFile(task.getDirPath() + identy + String.valueOf(start) + ".pcap");
                first = false;
            }else {
                byte[] pcapcontent = Utils.readBinaryFile(task.getDirPath() + identy + String.valueOf(start) + ".pcap");
                if (pcapcontent.length > 24)
                    totalPcap = Utils.concatBytes(totalPcap, Arrays.copyOfRange(pcapcontent, 24, pcapcontent.length));
            }
        }

        if (totalPcap != null) {
            Path outPath ;
            if(tmp == true) {
                int megfilecount = task.getMegFileCount();
                task.setMegFileCount(++ megfilecount);

                String filename = dir + "MEG" + String.valueOf(megfilecount) + ".pcap";
                outPath = Paths.get(filename);

                BufferedOutputStream bos = new BufferedOutputStream(Files.newOutputStream(outPath));
                bos.write(totalPcap);
                bos.close();

                task.setMegFilename(filename);
            }else {
                String filename = dir + String.valueOf(task.getTicketid() + ".pcap");

                outPath = Paths.get(filename);
                BufferedOutputStream bos = new BufferedOutputStream(Files.newOutputStream(outPath));

                bos.write(totalPcap);
                bos.close();
                task.setFilename(filename);
            }
        }
    }

    public static void main(String[] args) {

    }
}