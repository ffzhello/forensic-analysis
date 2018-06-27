package main.java.cn.edu.jslab6.manager;

import main.java.cn.edu.jslab6.entity.ActiveTask;
import main.java.cn.edu.jslab6.entity.ResponseResult;
import main.java.cn.edu.jslab6.enums.ResponseAction;
import main.java.cn.edu.jslab6.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * @author Andy
 * @date 18-6-27 下午6:59
 * @description
 */
public class ConcurrentDetection implements Runnable{

    private static Logger LOG = LoggerFactory.getLogger(ConcurrentDetection.class);
    private ActiveTask activeTask  = null;
    private CountDownLatch countDownLatch = null;

    public ConcurrentDetection(ActiveTask activeTask, CountDownLatch countDownLatch) {
        this.activeTask = activeTask;
        this.countDownLatch = countDownLatch;
    }

    @Override
    public void run() {
        if (activeTask == null || countDownLatch == null)
            return;

        try {
            //合并临时文件
            PcapFileManager.mergePcapfiles(activeTask, true);

            String filename = activeTask.getMegFilename();
            int megFileCount = activeTask.getMegFileCount();
            String tmpDir = activeTask.getDirPath() + "TMPDIR" + String.valueOf(megFileCount) + "/";

            if (!filename.equals("")) {
                File file = new File(tmpDir);
                file.mkdir();
            }

            //离线检测
            List<ResponseAction> actions = activeTask.getActions();

            activeTask.responseResult = new ResponseResult();
            activeTask.responseResult.ticketid = String.valueOf(activeTask.getTicketid());
            activeTask.responseResult.actionResult.put(ResponseAction.SuricataDetect, false);
            activeTask.responseResult.actionResult.put(ResponseAction.BroDetect, false);

            if (actions.contains(ResponseAction.SuricataDetect)) {
                if (doSuricataDetect(filename, tmpDir) == 0)
                    activeTask.responseResult.actionResult.put(ResponseAction.SuricataDetect, true);
            }

            if (actions.contains(ResponseAction.BroDetect)) {
                if (doBroDetect(filename, tmpDir) == 0)
                    activeTask.responseResult.actionResult.put(ResponseAction.BroDetect, true);
            }

            //Suricata和Bro至少一个成功后，进行IDS警告融合
            if (activeTask.responseResult.actionResult.get(ResponseAction.SuricataDetect) ||
                    activeTask.responseResult.actionResult.get(ResponseAction.BroDetect)) {
                File simpleAlert = new File(tmpDir,"SimpleAlert" + activeTask.getId() + ".txt");

                String outPath = simpleAlert.toString();

                activeTask.responseResult.files.fileName = outPath;
                if (genSimpleAlert(tmpDir, outPath) == 0) {
                    activeTask.responseResult.files.fileContent = Utils.readFileContent(outPath);
                }
            }else {
                activeTask.responseResult.files.fileName = "";
                activeTask.responseResult.files.fileContent = "";
            }
        } catch (IOException e) {
            LOG.debug("IOException...");
        } catch (InterruptedException e) {
            LOG.debug("InterruptedException...");
        } finally {
            LOG.debug("任务[id:" + activeTask.getId() + "]报文检测完成.");
            countDownLatch.countDown();
        }
    }

    /**
     * Offline Suricata detect mode.
     * Suricata的离线检测结果将保存在filepath内的suricata_detect子目录中
     * @param pcapPath: 离线报文保存的绝对路径
     * @throws InterruptedException
     * @return 返回0代表Suricata检测成功，否则检测失败。
     */
    private int doSuricataDetect(String pcapPath, String dir) throws InterruptedException, IOException {
        if (pcapPath == null || pcapPath.equals(""))  {
            LOG.error("No pcap file, Suricata Detect would return false.");
            return -1;
        }

        if (!pcapPath.endsWith(".pcap")) {
            LOG.error("{} is not a pcap file.", pcapPath);
            return -1;
        }

        File pcapFile = new File(pcapPath);
        String dirPath ;
        if (dir != null)
            dirPath = dir;
        else
            dirPath = pcapFile.getParent();
        File alertOutDir = new File(dirPath, "suricata_detect");

        alertOutDir.mkdir();
        // TODO: 调用系统命令来执行suricata检测命令.
        // 命令如: suricata -r pcapPath -c /etc/suricata/suricata.yaml -l alertOutDir
        String cmd = "suricata -r " + pcapPath + " -c /etc/suricata/suricata.yaml -l " + alertOutDir;
        LOG.debug(cmd);
        Process process = Runtime.getRuntime().exec(cmd);
        // 等待suricata检测结束
        process.waitFor();

        // FIXME: 可将子程序中的标准错误输出到错误日志中。
        if (process.exitValue() != 0) {
            LOG.error("Suricata detect failed!");
            return -1;
        }

        LOG.debug("Finish suricata detect.");
        return 0;
    }

    /**
     * Offline Bro detect mode.
     *   Bro的离线检测结果将保存在filePath内的bro_detect子目录中。
     * @param pcapPath: 离线报文保存的绝对路径
     * @return 返回0代表Bro检测成功，否则检测失败。
     * @throws InterruptedException
     * @throws IOException
     */
    private int doBroDetect(String pcapPath, String dir) throws InterruptedException, IOException {
        //Thread.sleep(300);
        if (pcapPath == null || pcapPath.equals(""))  {
            LOG.error("No pcap file, Bro detect would return false.");
            return -1;
        }

        if (!pcapPath.endsWith(".pcap")) {
            LOG.error("{} is not a pcap file.", pcapPath);
            return -1;
        }

        File pcapFile = new File(pcapPath);
        String dirPath;
        if(dir != null)
            dirPath = dir;
        else
            dirPath = pcapFile.getParent();

        File alertOutDir = new File(dirPath, "bro_detect");
        alertOutDir.mkdir();

        String cmd = "bro -r " + pcapPath;
        // bro离线检测时，会在当前目录下生成相关检测结果，所以运行程序时，需要设置其working directory值。
        Process process = Runtime.getRuntime().exec(cmd, null, alertOutDir);
        // 等待Bro检测结果
        process.waitFor();

        // FIXME: 可将子程序中的标准错误输出到错误日志中。
        if (process.exitValue() != 0) {
            LOG.error("Bro detect failed!");
            return -1;
        }

        LOG.debug("Finish bro detect.");
        return 0;
    }

    /**
     * 综合Suricata检测和Bro检测得到的日志文件生成警报日志文件
     * FIXME: 现阶段调用python脚本完成警报的转换。脚本主要完成：
     *        1. Suricata eve.json to simple alert.
     *        2. Bro weired.log to simple alert.
     *        3. simple alert将被放在{@param parentDir}内的simple_alert.txt中。
     * @param parentDir:
     * @return 返回0,代表生成IDS融合警报成功(警报将被放 {@param outAlertFile}中).否则生成融合警报失败。
     * @throws InterruptedException
     */
    private int genSimpleAlert(String parentDir, String outAlertFile) throws InterruptedException, IOException {
        //String ALERT_CONVERTER_PATH = "E:\\AlertConverter\\AlertConverter.py";
        // 调用脚本完成IDS融合警报生成, 脚本需要添加到java -cp选项内
        String cmd = "python AlertConverter.py -i " + parentDir + " -o " + outAlertFile;
        //String cmd = "python " + ALERT_CONVERTER_PATH + " -i " + parentDir + " -o " + outAlertFile;
        LOG.debug(cmd);
        Process process = Runtime.getRuntime().exec(cmd);

        //等待融合警报的生成
        //程序执行到这里，跑不动了
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        while ((reader.readLine()) != null) {
            //
        }

        process.waitFor();

        if (process.exitValue() != 0) {
            LOG.error("Generate Simple Alert failed!");
            return -1;
        }
        LOG.debug("Finish simple alert convert.");
        return 0;
    }

    public void forcedTaskDetect(ActiveTask task) {
        if (task == null)
            return;

        String filename = task.getFilename();

        //离线检测
        List<ResponseAction> actions = task.getActions();
        boolean flag = false;

        try {
            if (actions.contains(ResponseAction.SuricataDetect)) {
                if (doSuricataDetect(filename, null) == 0)
                    flag = true;
            }
            if (actions.contains(ResponseAction.BroDetect)) {
                if (doBroDetect(filename, null) == 0)
                    flag = true;
            }
            if (flag == true) {
                String pDir = new File(filename).getParent();

                File simpleAlert = new File(pDir,"SimpleAlert" + task.getId() + ".txt");

                String outPath = simpleAlert.toString();
                genSimpleAlert(pDir, outPath);
            }
        } catch (InterruptedException e) {
            LOG.debug("任务[id: " + task.getId() + "]离线检测失败...");
            e.printStackTrace();
        }catch (IOException e) {
            LOG.debug("任务[id: " + task.getId() + "]离线检测失败...");
            e.printStackTrace();
        }
    }
}
