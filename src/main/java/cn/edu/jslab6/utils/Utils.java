package main.java.cn.edu.jslab6.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.*;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import main.java.cn.edu.jslab6.entity.ActiveTask;

/**
 * @author Andy
 * @date 18-6-16 下午5:03
 * @description
 */
public class Utils {
    private static final Logger LOG = LoggerFactory.getLogger(Utils.class);

    public static String readFileContent(String filename) {
        StringBuilder sb = new StringBuilder();
        LOG.debug("For Debug: filename = {}", filename);
        int lineCount = 0;
        try {
            BufferedReader input = new BufferedReader(new FileReader(filename));
            String s;
            try {
                while ((s = input.readLine()) != null) {
                    sb.append(s).append('\n');
                    lineCount++;
                }
            } finally {
                input.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        LOG.debug("For debug: file has {} lines", lineCount);
        return sb.toString();
    }

    public static byte[] readBinaryFile(String filename) {
        try {
            BufferedInputStream bf = new BufferedInputStream(
                    new FileInputStream(filename));
            LOG.debug("begin to read file {} to binary", filename);
            try {
                int count = bf.available();
                byte[] data = new byte[count];
                int num = bf.read(data);
                //LOG.debug("read {} bytes, and orig bf.available() = {} bytes.", num, count);
                return data;
            } finally {
                bf.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 拼接两个byte[].
     * @param lhs
     * @param rhs
     * @return
     */
    public static byte[] concatBytes(byte[] lhs, byte[]rhs) {
        byte[] ret = new byte[lhs.length + rhs.length];
        System.arraycopy(lhs, 0, ret, 0, lhs.length);
        System.arraycopy(rhs, 0, ret, lhs.length, rhs.length);
        return ret;
    }

    /**
     * 获取指定目录中(非递归)的所有pcap文件的绝对路径。
     *
     * @param rootdir: 保存pcap文件的目录
     * @return
     */
    public static Iterable<String> getPcapPaths(String rootdir) {
        File path = new File(rootdir);
        String[] pcapPaths;

        pcapPaths = path.list(new FilenameFilter() {
            private Pattern pattern = Pattern.compile(".*\\.pcap");

            public boolean accept(File dir, String name) {
                return pattern.matcher(name).matches();
            }
        });
        Arrays.sort(pcapPaths, String.CASE_INSENSITIVE_ORDER);

        ArrayList<String> pathList = new ArrayList<String>();
        for (String pcapPath : pcapPaths) {
            pathList.add(Paths.get(rootdir, pcapPath).toString());
        }
        return pathList;
    }

    public static  String listToString(List<String> theList, String sep) {
        StringBuilder sb  = new StringBuilder();
        boolean flag = false;
        for (String t : theList) {
            if (flag) {
                sb.append(sep);
            } else {
                flag = true;
            }
            sb.append(t);
        }
        return sb.toString();
    }

    private static byte[] getFilesContent(List<String> fileList) {
        byte[] totalPcap = null;
        boolean first = true;
        for (String file: fileList) {
            // 第一个报文,保存所有的报文字节，后面的pcap文件需去除pcap header(24个字节)进行拼接
            if(first) {
                totalPcap = Utils.readBinaryFile(file);
                first = false;
            }else {
                byte[] pcapcontent = Utils.readBinaryFile(file);
                if (pcapcontent.length > 24)
                    totalPcap = Utils.concatBytes(totalPcap, Arrays.copyOfRange(pcapcontent, 24, pcapcontent.length));
            }
            //删除本地周期临时文件
            File f = new File(file);
            if (f.exists()) {
                LOG.debug("del: " + f.getName());
                f.delete();
            }
        }
        return totalPcap;
    }

    //判断文件是否存在
    public static boolean isFileExists(String filename) {
        boolean exists = false;
        if (filename != null && !filename.isEmpty()){
            File file = new File(filename);
            if(file.exists() && file.isFile())
                exists = true;
        }
        return exists;
    }

    //创建目录
    public static void createDir(ActiveTask task) {
        if (task == null)
            return;

        //任务采集、分析结果保存路径
        String parentDir = task.getDirPath();
        //创建目录
        File dir = new File(parentDir);
        if (dir.exists() && dir.isDirectory())
            deleteDir(dir);

        dir.mkdir();
    }

    //创建目录
    public static void createDir(List<ActiveTask> taskList) {
        if (taskList == null || taskList.isEmpty())
            return;

        for(ActiveTask t: taskList) {
            //任务采集、分析结果保存路径
            String parentDir = t.getDirPath();
            //创建目录
            File dir = new File(parentDir);
            if (dir.exists() && dir.isDirectory())
                deleteDir(dir);

            dir.mkdir();
        }
    }

    /**
     * 递归删除目录下的所有文件及子目录下所有文件
     * @param dir 将要删除的文件目录
     * @return boolean Returns "true" if all deletions were successful.
     *                 If a deletion fails, the method stops attempting to
     *                 delete and returns "false".
     */
    public static boolean deleteDir(File dir) {
        if (dir.isDirectory()) {
            String[] children = dir.list();
            //递归删除目录中的子目录下
            for (int i = 0; i < children.length; i++) {
                boolean success = deleteDir(new File(dir, children[i]));
                if (!success) {
                    return false;
                }
            }
        }
        // 目录此时为空，可以删除
        return dir.delete();
    }

    public static void main(String[] args) {
        File file = new File("/home/andy/1.txt");
        deleteDir(file);

    }
}
