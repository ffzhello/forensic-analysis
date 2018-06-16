package main.java.cn.edu.jslab6.manager;

import java.io.File;
import java.io.IOException;
import java.util.*;

import main.java.cn.edu.jslab6.utils.Utils;
import org.pcap4j.core.*;
import org.pcap4j.packet.IllegalRawDataException;
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
    private List<ActiveTask> activeTaskList = new ArrayList<ActiveTask>();   //待处理任务
    private Set<Integer> forcedTaskIdSet = new HashSet<Integer>();
    private PcapFileInfo pcapFileInfo = null;

    public PcapFileOfflineSplit() {
        //初始化
        initActivetaskList();
    }

    /**
     * 初始化系统停掉前的采集任务
     */
    private void initActivetaskList() {
        activeTaskList = ActiveTaskManager.getTaskListfromDB(ActiveTaskStatus.SENSORING);
        activeTaskList.addAll(ActiveTaskManager.getTaskListfromDB(ActiveTaskStatus.FORCE));

        if (!activeTaskList.isEmpty()) {
            for (ActiveTask task: activeTaskList) {

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
    private void splitPcapFile(PcapFileInfo pcapFile) throws PcapNativeException, NotOpenException, IllegalRawDataException, ArrayIndexOutOfBoundsException {
        if (pcapFile == null)
            return;

        String pcapFileName = pcapFile.getFilepath();
        if (pcapFileName == null || pcapFileName.equals(""))
            return;

        LOG.debug(pcapFileName + "分离开始.");

        PcapHandle handle = Pcaps.openOffline(pcapFileName);
        Packet packet ;
        //限定每个文件的大小为5M
        final int MAX_TMPFILE_SIZE = 2*1024*1024;

        //处理离线文件中每个报文
        Iterator<ActiveTask> iterator ;
        ActiveTask task ;

        while ((packet = handle.getNextPacket()) != null) {
            int packetSize = packet.length();
            iterator = activeTaskList.iterator();
            while (iterator.hasNext()) {
                task = iterator.next();
                if (PacketMatchManager.isMatch(handle, packet, task)) {
                    //判断当前文件是否达到容量上限
                    int tmpFileSize = task.getTmpFileSize();
                    if (tmpFileSize + packetSize > MAX_TMPFILE_SIZE) {
                        //关闭dumper
                        if (task.dumper != null) {
                            task.dumper.close();
                            task.dumper = null;
                        }
                    }
                    if (task.dumper == null) {
                        int patFileCount = task.getPatFileCount();
                        task.setPatFileCount(++ patFileCount);
                        String tmpFilename = task.getDirPath() + "PAT" + String .valueOf(patFileCount) + ".pcap";
                        task.setTmpFilename(tmpFilename);
                        task.setTmpFileSize(0);
                        task.dumper = handle.dumpOpen(tmpFilename);
                    }
                    task.dumper.dump(packet, handle.getTimestamp());
                    tmpFileSize = task.getTmpFileSize();
                    task.setTmpFileSize(tmpFileSize + packetSize);
                    int count = task.getNumPkts();
                    task.setNumPkts(++count);
                    long pktTime = handle.getTimestamp().getTime()/1000;
                    if (count == 1)
                        task.setFirstpkttime(pktTime);
                    task.setLastpkttime(pktTime);
                }
            }
        }
        //关闭dumper
        if(!(activeTaskList.isEmpty())) {
            for(ActiveTask t: activeTaskList) {
                if(t.dumper != null) {
                    t.dumper.close();
                    t.dumper = null;
                }
            }
        }
        //关闭handle
        handle.close();
        LOG.debug(pcapFileName + "分离结束...");
    }

    /**
     * 重置采集任务
     * @param task
     */
    private void resetActivetask(ActiveTask task) {
        //更新采集任务,采集报文数
        ActiveTaskManager.updateActiveTaskByTask(task);

        //初始化内存中任务
        long time = System.currentTimeMillis()/1000;
        task.setLastInteractTime(time);
        task.setTmpFilename("");
        task.setTmpFileSize(0);
        task.setMegFilename("");

    }

    /**
     * @param activeTask
     * @throws IOException
     */
    private void interactWithChairs(ActiveTask activeTask) throws IOException{
        if(activeTask == null)
            return;

        //离线IDS
        ActionHandler actionHandler = new ActionHandler();
        if (actionHandler != null)
            actionHandler.inteactWithChairs(activeTask);
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

        //离线IDS
        ActionHandler ah = new ActionHandler();
        ah.forcedTaskDetect(task);

        //处理
        ActiveTaskManager.updateActiveTaskStatusByTaskId(task.getId(), ActiveTaskStatus.FINISHED);
    }

    /**
     * 不断从数据库读取任务、PCAP文件
     */
    @Override
    public void run() {

        while(true) {
            //从数据库读取强制结束的任务
            forcedTaskIdSet = ActiveTaskManager.getForceTaskIdSetFromDB();
            if (!forcedTaskIdSet.isEmpty()) {
                if(!activeTaskList.isEmpty()) {
                    Iterator<ActiveTask> it = activeTaskList.iterator();
                    while (it.hasNext()) {
                        ActiveTask at = it.next();
                        //去除内存中需要强制停止的任务
                        if (forcedTaskIdSet.contains(at.getId())) {
                            try {
                                handlerForceTask(at);
                                LOG.debug("任务[id: " + at.getId()  + "]强制停止成功.");
                            }catch (IOException e) {
                                LOG.debug("任务[id: " + at.getId()  + "]强制停止失败...");
                            }
                            forcedTaskIdSet.remove(at.getId());
                            it.remove();
                        }
                    }
                }
                //通过任务ID更新数据库
                for (Integer in: forcedTaskIdSet) {
                    ActiveTaskManager.updateActiveTaskStatusByTaskId(in, ActiveTaskStatus.FINISHED);
                }
            }

            /*
            //从数据库中取新的pcap文件
            List<PcapFileInfo> fileList = PcapFileManager.getUnhanlderedPcapFilefromDB();
            pcapFileList.addAll(fileList);
            */

            pcapFileInfo =  PcapFileManager.getUnhandlerPcapFile();

            //从数据库中读取等待执行的任务,并创建存储目录
            List<ActiveTask> taskList = ActiveTaskManager.getTaskListfromDB(ActiveTaskStatus.WAIT_SENSOR);
            Utils.createDir(taskList);
            activeTaskList.addAll(taskList);

            if(pcapFileInfo != null) {
                //以文件为单位心跳
                if (!(activeTaskList.isEmpty())) {
                    try {
                        //离线分离
                        splitPcapFile(pcapFileInfo);
                    } catch (PcapNativeException e) {
                        LOG.debug("pcap native exception...");
                        e.printStackTrace();
                    } catch (NotOpenException e) {
                        LOG.debug("not open exception...");
                        e.printStackTrace();
                    } catch (IllegalRawDataException e) {
                        LOG.debug("illegal raw data exception...");
                        e.printStackTrace();
                    }catch (ArrayIndexOutOfBoundsException e) {
                        LOG.debug("array index out of bounds exception...");
                        e.printStackTrace();
                    } finally {
                        /*
                        //删除系统中的pcap文件
                        String filePath = file.getFilepath();
                        File f = new File(filePath);
                        if (f != null)
                            f.delete();
                        */
                    }
                }
                //更新数据库中pcap文件信息
                PcapFileManager.updatePcapFileInfoFromDB(pcapFileInfo.getId());
                pcapFileInfo = null;

            }
            //心跳反应
            if (!activeTaskList.isEmpty()) {
                for (ActiveTask at: activeTaskList) {
                    if (at.getMegedPatFileCount() < at.getPatFileCount()) {
                        try {
                            interactWithChairs(at);
                            resetActivetask(at);
                            LOG.debug("文件周期：任务[id: " + at.getId() + "]心跳成功.");
                        } catch (IOException e) {
                            LOG.debug("文件周期：任务[id: " + at.getId() + "]心跳失败......");
                        }
                    }else {
                        //以时间为周期心跳
                        long currentTime = System.currentTimeMillis()/1000;
                        if (currentTime - at.getLastInteractTime() > 300) {
                            try {
                                interactWithChairs(at);
                                //心跳反应完成，任务采集重置
                                resetActivetask(at);
                                LOG.debug("时间周期：任务[id: " + at.getId() + "]心跳成功.");
                            }catch (IOException e) {
                                LOG.debug("时间周期：任务[id: " + at.getId() + "]心跳失败......");
                            }
                        }
                    }
                }
            }
        }
    }

    //main
    public static void main(String[] args) {

    }
}