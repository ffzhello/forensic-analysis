package main.java.cn.edu.jslab6.manager;

import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;

import main.java.cn.edu.jslab6.entity.ActiveTask;
import main.java.cn.edu.jslab6.config.SystemConfig;
import main.java.cn.edu.jslab6.entity.ResponseResult;
import main.java.cn.edu.jslab6.entity.PcapResponseResult;
import main.java.cn.edu.jslab6.enums.ResponseAction;
import main.java.cn.edu.jslab6.utils.Utils;

/**
 * @author Andy
 * @date 18-6-17 上午7:20
 * @description
 */
public class ActionHandler {
    private static Logger LOG  = LoggerFactory.getLogger(ActionHandler.class);
    private SystemConfig systemConfig = new SystemConfig();
    private ResponseResultSender resultSender = null;

    //构造器，读取配置文件
    public ActionHandler() {
        try {
            systemConfig = new SystemConfig("./system.properties");
        }catch (IOException e) {
            e.printStackTrace();
        }
        resultSender = new ResponseResultSender(systemConfig.getTaskSendUrl());
    }

    //采集完成,返回结果，并做相应的离线检测
    public void inteactWithChairs(ActiveTask task) {
        if (task == null)
            return;
        //回送采包结果
        try {
            PcapResponseResult result = new PcapResponseResult();
            result.ticketid = String.valueOf(task.getTicketid());
            result.actionResult.put(ResponseAction.PcapCap, true);
            result.attach.firstpkttime = task.getFirstpkttime();
            result.attach.lastpkttime = task.getLastpkttime();

            int start = task.getMegedPatFileCount() + 1;
            int end = task.getPatFileCount();

            if (start <= end) {
                for (; start <= end; start ++) {
                    String file = task.getDirPath() + "PAT" + String.valueOf(start) + ".pcap";
                    byte[] b;
                    if ((b = Utils.readBinaryFile(file)) != null) {
                        result.files.fileContent = new String(b, StandardCharsets.ISO_8859_1);
                        // LOG.debug("Convert file {} to Latin-1 format, binary size = {}, string size = {} ", result.files.fileName, b.length, result.files.fileContent.length());
                        if (task.getUsername().equals("CHAIRS")) {
                            String pcapResult = new Gson().toJson(result);
                            try {
                                resultSender.send(pcapResult);
                                LOG.debug("Task[id: " + task.getId() + "] Return " + result.files.fileContent.length() + " bytes packets to CHAIRS success. ");
                            }catch (IOException e) {
                                e.printStackTrace();
                            }
                        } else {
                            //  LOG.debug("Finsh pcap capture, but response task are not from CHAIRS!");
                            LOG.debug("Task[id: " + task.getId() + "] Return " + result.files.fileContent.length() + " bytes packets to WHO success. " );
                        }
                    }
                }
            }else {
                result.files.fileContent = "";
                if (task.getUsername().equals("CHAIRS")) {
                    String pcapResult = new Gson().toJson(result);
                    try {
                        resultSender.send(pcapResult);
                        // LOG.debug("Task {} finish pcap capture, ID = {}", task.getTicketId(), task.getId());
                        LOG.debug("Task[id：" + task.getId() + "] Return 0 bytes packets to CHAIRS.");

                    }catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    //  LOG.debug("Finsh pcap capture, but response task are not from CHAIRS!");
                    LOG.debug("Task[id: " + task.getId() + "] Return 0 bytes packets to WHO.");
                }
            }

            //合并临时文件
            PcapFileManager.mergePcapfiles(task, true);
            int patCount = task.getPatFileCount();
            task.setMegedPatFileCount(patCount);

            String filename = task.getMegFilename();
            int megFileCount = task.getMegFileCount();
            String tmpDir = task.getDirPath() + "TMPDIR" + String.valueOf(megFileCount) + "/";
            if (!filename.equals("")) {
                LOG.debug("megfilename: {}", filename);
                File file = new File(tmpDir);
                file.mkdir();
            }

            //离线检测
            List<ResponseAction> actions = task.getActions();

            ResponseResult responseResult = new ResponseResult();
            responseResult.ticketid = String.valueOf(task.getTicketid());
            responseResult.actionResult.put(ResponseAction.SuricataDetect, false);
            responseResult.actionResult.put(ResponseAction.BroDetect, false);


            if (actions.contains(ResponseAction.SuricataDetect)) {
                if (doSuricataDetect(filename, tmpDir) == 0)
                    responseResult.actionResult.put(ResponseAction.SuricataDetect, true);
            }
            if (actions.contains(ResponseAction.BroDetect)) {
                if (doBroDetect(filename, tmpDir) == 0)
                    responseResult.actionResult.put(ResponseAction.BroDetect, true);
            }

            // Suricata检测和Bro检测至少有一个成功后，进行IDS融合警报的生成。
            if (responseResult.actionResult.get(ResponseAction.SuricataDetect) ||
                    responseResult.actionResult.get(ResponseAction.BroDetect)) {
                File simpleAlert = new File(tmpDir, "SimpleAlert" + task.getId() + ".txt");

                String outPath = simpleAlert.toString();

                responseResult.files.fileName = outPath;
                if (genSimpleAlert(tmpDir, outPath) == 0) {
                    responseResult.files.fileContent = Utils.readFileContent(outPath);
                }
            } else {
                responseResult.files.fileName = "";
                responseResult.files.fileContent = "";
            }

            if (task.getUsername().equals("CHAIRS")) {
                // 将IDS相关检测内容封装成ResponseResult格式，发往CHAIRS系统。
                String idsDetectResult = new Gson().toJson(responseResult);
                try {
                    resultSender.send(idsDetectResult);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                LOG.debug("Task[id：" + task.getId() +  "] Return IDS Results to CHAIRS success.");
            } else {
                // FIXME: 不是CHAIRS发送的案件，有需求的话，可以做一些额外的工作。
                LOG.debug("Task[id：" + task.getId() +  "] Return IDS Results to WHO success.");
            }
            //LOG.debug("Task {} finish IDSAction detect, ID = {}", task.getTicketId(), task.getId());

        }catch (InterruptedException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
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

    //test
    public static void main(String[] args) {
        /*ActiveTask task = new ActiveTask();
        task.setId(1);
        task.setTicketId(2);
        task.setFirstpkttime(1111);
        task.setLastpkttime(2222);
        task.setCurFilename("D:/0.pcap");

        task.setCurFilename("D:/1.pcap");

        task.setUsername("CHAIRS");

        ActionHandler ah = new ActionHandler();*/
    }
}