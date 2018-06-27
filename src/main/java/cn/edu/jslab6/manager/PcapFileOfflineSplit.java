package main.java.cn.edu.jslab6.manager;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import main.java.cn.edu.jslab6.entity.Constant;
import main.java.cn.edu.jslab6.utils.IpUtils;
import main.java.cn.edu.jslab6.utils.Utils;
import org.pcap4j.core.*;
import org.pcap4j.packet.IllegalRawDataException;
import org.pcap4j.packet.IpV4Packet;
import org.pcap4j.packet.Packet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import main.java.cn.edu.jslab6.entity.ActiveTask;
import main.java.cn.edu.jslab6.entity.PcapFileInfo;
import main.java.cn.edu.jslab6.enums.ActiveTaskStatus;

/**
 * @author Andy
 * @date 18-6-16 下午5:27
 * @description
 */
public class PcapFileOfflineSplit implements Runnable{
    //field
    private static final Logger LOG = LoggerFactory.getLogger(PcapFileOfflineSplit.class);
    //private List<ActiveTask> activeTaskList = new ArrayList<ActiveTask>();   //待处理任务
    //private Set<Integer> forcedTaskIdSet = new HashSet<Integer>();
    //规则与任务的映射表
    private Map<Long,Set> ruleToTasks = new HashMap<Long,Set>();
    //待分离pcap文件信息
    private PcapFileInfo pcapFileInfo = null;
    //线程池
    private ExecutorService executorService = Executors.newCachedThreadPool();


    public PcapFileOfflineSplit() {
        //初始化
        initRuleToTasks();
    }

    /**
     * 初始化系统停掉前的采集任务
     */
    private void initRuleToTasks() {
        List<ActiveTask> activeTasks = ActiveTaskManager.getTaskListfromDB(ActiveTaskStatus.SENSORING);
        activeTasks.addAll(ActiveTaskManager.getTaskListfromDB(ActiveTaskStatus.FORCE));

        if (!activeTasks.isEmpty()) {
            for (ActiveTask task: activeTasks) {

                //恢复现场
                int pCount = 0;
                int mCount = 0;
                File file = new File(task.getDirPath());
                LOG.debug("从数据库恢复采集任务[id: " + task.getId() + "]...");
                if (file.exists()) {
                    File[] files = file.listFiles();
                    for (File f: files) {
                        String name = f.getName();
                        if (name.startsWith("PAT")) {
                            pCount ++;
                        }else if(name.startsWith("MEG")) {
                            mCount ++;
                        }else if(name.startsWith("TMP")) {
                            //
                        }else
                            Utils.deleteDir(f);
                    }
                    task.setPatFileCount(pCount);
                    task.setMegedPatFileCount(pCount);


                    if (mCount > 0) {
                        task.setMegFileCount(mCount);
                        String parent = task.getDirPath();
                        for (int i = 1; i <= mCount; i++) {
                            String m = parent + "MEG" + String.valueOf(i) + ".pcap";
                            LOG.debug("恢复文件: " + m);
                        }
                        task.setMegFileCount(mCount);
                    }
                }else {
                    //创建目录
                    Utils.createDir(task);
                }
                //建立规则任务映射关系
                buildRuleTotasks(task);
            }
        }
    }

    /**
     * 建立规则-任务映射表
     * @param task
     */
    private void buildRuleTotasks(ActiveTask task) {
        if (task == null)
            return;

        if (!task.getIpList().isEmpty()) {
            for (Long ip: task.getIpLongList()) {
                Set<ActiveTask> tasks;
                if (ruleToTasks.containsKey(ip))
                    tasks = ruleToTasks.get(ip);
                else
                    tasks = new HashSet<>();

                tasks.add(task);
                ruleToTasks.put(ip,tasks);
            }
        }
    }

    /**
     * 单个PCAP文件离线分离
     * @param pcapFile
     * @throws PcapNativeException
     * @throws NotOpenException
     * @throws IllegalRawDataException
     * @throws ArrayIndexOutOfBoundsException
     */
    private void splitPcapFile(PcapFileInfo pcapFile) {
        if (pcapFile == null)
            return;

        String pcapFileName = pcapFile.getFilepath();
        if (pcapFileName == null || pcapFileName.equals(""))
            return;

        LOG.debug(pcapFileName + "分离开始.");

        PcapHandle handle = null;
        Packet packet;
        //限定每个文件的大小为5M
        final int MAX_TMPFILE_SIZE = 2 * 1024 * 1024;
        byte[] rawData;
        IpV4Packet ipV4Packet;
        IpV4Packet.IpV4Header header;
        Long srcIp;
        Long dstIp;

        try {
            handle = Pcaps.openOffline(pcapFileName);
            while ((packet = handle.getNextPacket()) != null) {
                int packetSize = packet.length();
                rawData = packet.getRawData();
                int v4HeaderLength = Integer.parseInt(Integer.toHexString(rawData[0]).charAt(1) + "") * 4;
                ipV4Packet = IpV4Packet.newPacket(rawData, 0, v4HeaderLength);
                header = ipV4Packet.getHeader();

                srcIp = IpUtils.ipToLong(header.getSrcAddr().getHostAddress());
                dstIp = IpUtils.ipToLong(header.getDstAddr().getHostAddress());

                //匹配任务
                Set<ActiveTask> packetTaskSet = new HashSet<>();
                if (ruleToTasks.containsKey(srcIp)) {
                    packetTaskSet.addAll(ruleToTasks.get(srcIp));
                }
                if (ruleToTasks.containsKey(dstIp)) {
                    packetTaskSet.addAll(ruleToTasks.get(dstIp));
                }

                //写入
                if (!packetTaskSet.isEmpty()) {
                    for (ActiveTask task : packetTaskSet) {
                        //判断任务是否达到采集上限
                        int sensorBytes = task.getSensorBytes();
                        if (sensorBytes >= Constant.MAX_TASK_SIZE) {
                            continue;
                        }

                        //判断当前文件是否达到上限
                        int tmpFileSize = task.getTmpFileSize();
                        if (tmpFileSize + packetSize > Constant.MAX_TMP_FILE_SIZE) {
                            //关闭dumper
                            if (task.dumper != null) {
                                task.dumper.close();
                                task.dumper = null;
                            }

                            if (task.dumper == null) {
                                int patFileCount = task.getPatFileCount();
                                task.setPatFileCount(++patFileCount);
                                String tmpFilename = task.getDirPath() + "PAT" + String.valueOf(patFileCount) + ".pcap";
                                task.setTmpFilename(tmpFilename);
                                task.setTmpFileSize(0);
                                task.dumper = handle.dumpOpen(tmpFilename);
                            }
                            task.dumper.dump(packet, handle.getTimestamp());
                            tmpFileSize = task.getTmpFileSize();
                            task.setTmpFileSize(tmpFileSize + packetSize);
                            task.setSensorBytes(sensorBytes + packetSize);
                        }
                    }

                }
            }
        } catch (PcapNativeException e) {
            LOG.debug("PcapNativeException...");
        } catch (NotOpenException e) {
            LOG.debug("NotOpenException...");
        } catch (IllegalRawDataException e) {
            LOG.debug("IllegalRawDataException...");
        } catch (ArrayIndexOutOfBoundsException e) {
            LOG.debug("ArrayIndexOutOfBoundsException...");
        } finally {
            //关闭dumper
            if (!ruleToTasks.isEmpty()) {
                for (Map.Entry<Long,Set> entry: ruleToTasks.entrySet()) {
                    Set<ActiveTask> set = entry.getValue();
                    for (ActiveTask t: set) {
                        if (t.dumper != null) {
                            t.dumper.close();
                            t.dumper = null;
                        }
                    }
                }
            }
            //关闭handle
            if (handle != null) {
                handle.close();
            }
            LOG.debug(pcapFileName + "分离结束...");

        }
    }

     /**
     * 任务强制停止后的处理
     * @param task
     */
    private void handlerForceTask(ActiveTask task) throws IOException{

        if (task == null)
            return;

        //合并merge文件
        PcapFileManager.mergePcapfiles(task, false);

        //处理
        task.setStatus(ActiveTaskStatus.FINISHED.getValue());
        ActiveTaskManager.updateActiveTaskStatusByTaskId(task.getId(), ActiveTaskStatus.FINISHED);
        LOG.debug("任务[id:" + task.getId() + "]停止成功.");
    }

    /**
     * 不断从数据库读取任务、PCAP文件
     */
    @Override
    public void run() {

        while(true) {
            //从数据库读取强制结束的任务

            Set<Integer> forcedTaskIdSet = ActiveTaskManager.getForceTaskIdSetFromDB();
            if (!forcedTaskIdSet.isEmpty()) {
                LOG.debug("共有" + forcedTaskIdSet.size() + "个任务待停止.");
                for (Iterator<Map.Entry<Long,Set>> iterator = ruleToTasks.entrySet().iterator(); iterator.hasNext(); ) {
                    Map.Entry<Long,Set> entry = iterator.next();
                    Set<ActiveTask> taskSet = entry.getValue();
                    for (Iterator<ActiveTask> taskIterator = taskSet.iterator(); taskIterator.hasNext(); ) {
                        ActiveTask task = taskIterator.next();
                        if (forcedTaskIdSet.contains(task.getId())) {
                            if (task.getStatus() == ActiveTaskStatus.FINISHED.getValue()) {
                                try {
                                    handlerForceTask(task);
                                } catch (IOException e) {
                                    LOG.debug("FORCE IOException...");
                                }
                            }
                            taskIterator.remove();
                        }
                    }
                    //若规则无任务映射，则删除
                    if (taskSet.isEmpty())
                        iterator.remove();
                }
            }

            pcapFileInfo =  PcapFileManager.getUnhandlerPcapFile();

            //从数据库中读取等待执行的任务,并创建存储目录
            List<ActiveTask> taskList = ActiveTaskManager.getTaskListfromDB(ActiveTaskStatus.WAIT_SENSOR);
            Utils.createDir(taskList);

            //build rule
            if (!taskList.isEmpty()) {
                for (ActiveTask task: taskList) {
                    buildRuleTotasks(task);
                }
            }


            if(pcapFileInfo != null) {
                //以文件为单位心跳
                if (!ruleToTasks.isEmpty()) {
                    //离线分离
                    splitPcapFile(pcapFileInfo);
                }
                //更新数据库中pcap文件信息
                PcapFileManager.updatePcapFileInfoFromDB(pcapFileInfo.getId());
                //删除系统中的pcap文件
                String filePath = pcapFileInfo.getFilepath();
                File f = new File(filePath);
                if (f != null)
                    f.delete();

                Set<ActiveTask> tasks = new HashSet<>();
                if (!ruleToTasks.isEmpty()) {
                    for (Map.Entry<Long,Set> entry: ruleToTasks.entrySet()) {
                        Set<ActiveTask> taskSet = entry.getValue();
                        if (!taskSet.isEmpty()) {
                            for (ActiveTask at: taskSet) {
                                if (at.getMegedPatFileCount() < at.getPatFileCount()) {
                                    tasks.add(at);
                                }
                            }
                        }
                    }
                }

                int fCount = tasks.size();
                LOG.debug("文件周期匹配任务个数: " + fCount);

                if (fCount > 0) {
                    CountDownLatch countDownLatch = new CountDownLatch(fCount);
                    for (ActiveTask task: tasks) {
                        ConcurrentDetection concurrentDetection = new ConcurrentDetection(task, countDownLatch);
                        executorService.execute(concurrentDetection);
                    }

                    try {
                        countDownLatch.await();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    //回送结果给chairs
                    if (!tasks.isEmpty()) {
                        for (ActiveTask task: tasks)
                            ActionHandler.returnResults(task);
                    }
                }

            }
        }
    }

    //main
    public static void main(String[] args) {

    }
}