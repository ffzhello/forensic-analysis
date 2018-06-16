package main.java.cn.edu.jslab6.manager;

/**
 * @author Andy
 * @date 18-6-16 下午5:29
 * @description
 */

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;

import main.java.cn.edu.jslab6.entity.ActiveTask;
import main.java.cn.edu.jslab6.entity.ResponseTask;
import main.java.cn.edu.jslab6.enums.ActiveTaskStatus;

/**
 * 数据库采集任务管理
 * Created by ffzheng on 2017/7/19.
 */

public class ActiveTaskManager {
    private static final Logger LOG = LoggerFactory.getLogger(ActiveTask.class);
    private static DBConnectionManager dbConnectionManager = new DBConnectionManager();
    private static Connection conn = dbConnectionManager.getConn();

    private static String sql = null;

    /**
     * 验证数据库连接
     */
    private static void validateConn() {
        if (!DBConnectionManager.validate(conn)) {
            dbConnectionManager.initMysql();
            conn = dbConnectionManager.getConn();
        }
    }

    /**
     *  将新采集任务存入数据库
     * @param task
     */

    public static void addActivetaskToDB(ResponseTask task) {
        if (task == null)
            return;

    }

    /**
     * 通过状态读取数据库中的采集任务
     * @param status
     * @return
     */
    public static List<ActiveTask> getTaskListfromDB(ActiveTaskStatus status) {

        List<ActiveTask> taskList = new ArrayList<ActiveTask>();
        if (status == null)
            return taskList;

        sql = "select * from activetask where status = " + status.getValue();
        Statement statement = null;
        ResultSet resultSet = null;
        try {
            //validate
            validateConn();
            statement = conn.createStatement();
            resultSet = statement.executeQuery(sql);
            while(resultSet.next()) {
                ActiveTask activeTask = new ActiveTask();
                activeTask.setId(resultSet.getInt("id"));
                activeTask.setTicketid(resultSet.getInt("ticketid"));
                activeTask.setIpString(resultSet.getString("iplist"));
                activeTask.setRawActions(resultSet.getString("action"));
                activeTask.setPriority(resultSet.getInt("priority"));
                activeTask.setFlowDirection(resultSet.getInt("inoutflag"));
                activeTask.setSrcIP(resultSet.getInt("srcipflag"));
                activeTask.setSrcIPDstIP(resultSet.getInt("srcipdstip"));
                activeTask.setDstIP(resultSet.getInt("dstipflag"));
                activeTask.setSrcPort(resultSet.getInt("srcport"));
                activeTask.setSrcPortDstPort(resultSet.getInt("srcportdstport"));
                activeTask.setDstPort(resultSet.getInt("dstport"));
                activeTask.setProtocol(resultSet.getInt("protocol"));
                activeTask.setStarttime(resultSet.getLong("starttime"));
                activeTask.setEndtime(resultSet.getLong("endtime"));
                activeTask.setNumPkts(resultSet.getInt("numpkts"));
                activeTask.setThresholdPkts(resultSet.getInt("thresholdpkts"));
                activeTask.setUsername(resultSet.getString("username"));
                activeTask.setStatus(resultSet.getInt("status"));
                activeTask.setDirPath("/home/monster/AutoResponse/HydraSensor/data/hydra_sensor/" + activeTask.getId() + "/");
                activeTask.setLastInteractTime(System.currentTimeMillis()/1000);
                taskList.add(activeTask);

                //更新任务状态
                if (activeTask.getStatus() == 0) {
                    updateActiveTaskStatusByTaskId(activeTask.getId(), ActiveTaskStatus.SENSORING);
                    LOG.debug("从数据库获得新采集任务[id：" + activeTask.getId() + "]");
                }
            }
        }catch(SQLException e) {
            e.printStackTrace();
        }finally {
            //close
            if (statement != null) {
                try {
                    statement.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            if (resultSet != null) {
                try {
                    resultSet.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
        return taskList;
    }

    /**
     * 获取数据库中强制停止的任务标识列表
     * @return 任务ID
     */
    public static Set<Integer> getForceTaskIdSetFromDB() {
        Set<Integer> forceSet = new HashSet<>();
        //验证
        validateConn();

        sql = "select id from activetask where status = " + ActiveTaskStatus.FORCE.getValue();
        Statement statement = null;
        ResultSet resultSet = null;
        try {
            statement = conn.createStatement();
            resultSet = statement.executeQuery(sql);
            while(resultSet.next()) {
                int id = resultSet.getInt("id");
                forceSet.add(id);
                LOG.debug("从数据库获取强制停止任务[id: " + id + "].");
            }
        }catch(SQLException e) {
            LOG.debug("从数据库中获取强制停止任务异常...");
            e.printStackTrace();
        } finally {
            if (statement != null) {
                try {
                    statement.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            if (resultSet != null) {
                try {
                    resultSet.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
        return forceSet;
    }

    /**
     * 通过任务ID更新任务状态
     * @param taskId  任务标识
     * @param status  待更新状态
     */
    public static void updateActiveTaskStatusByTaskId(Integer taskId, ActiveTaskStatus status) {
        if (taskId == null || status == null)
            return;

        sql = "update activetask set status = " + status.getValue() + " where id = " + taskId;
        Statement statement = null;
        try {
            validateConn();
            statement = conn.createStatement();
            statement.executeUpdate(sql);
        }catch (SQLException e) {
            LOG.debug("更新数据库采集任务[id:]" + taskId + "]失败...");
            e.printStackTrace();
        }finally {
            if (statement != null) {
                try {
                    statement.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 通过案件ID更新任务状态
     * @param ticketId
     */
    public static void updateTaskStatusByTicketId(Integer ticketId, ActiveTaskStatus status) {
        if (ticketId == null || status == null)
            return;

        sql = "update activetask set status = " + status.getValue() + " where ticketid = " + ticketId ;
        Statement statement = null;
        try {
            validateConn();
            statement = conn.createStatement();
            statement.executeUpdate(sql);
        } catch (Exception e) {
            LOG.debug("更新数据库采集任务[ticketid: "+ ticketId +"]失败...");
            e.printStackTrace();
        } finally {
            if (statement != null) {
                try {
                    statement.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 通过任务更新数据库任务
     * @param task
     */
    public static void updateActiveTaskByTask(ActiveTask task) {
        if (task == null)
            return;

        sql = "update activetask set numpkts = " + task.getNumPkts()  + ", firstpkttime = "+ task.getFirstpkttime()
                + ", lastpkttime = " + task.getLastpkttime() + " where id = " + task.getId();
        Statement statement = null;
        try {
            validateConn();
            statement = conn.createStatement();
            statement.executeUpdate(sql);
        }catch (SQLException e) {
            LOG.debug("更新数据库采集任务[id:" + task.getId() + "]失败...");
            e.printStackTrace();
        }finally {
            if (statement != null) {
                try {
                    statement.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}