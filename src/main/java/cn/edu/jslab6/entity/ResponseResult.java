package main.java.cn.edu.jslab6.entity;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import java.util.HashMap;
import java.util.Map;

import main.java.cn.edu.jslab6.enums.ResponseAction;

/**
 * @author Andy
 * @date 18-6-16 下午6:01
 * @description
 */
public class ResponseResult {
    public String ticketid;

    @SerializedName("responseresult")
    public Map<ResponseAction, Boolean> actionResult = new HashMap<ResponseAction, Boolean>();

    public ResponseFiles files = new ResponseFiles();

    public class ResponseFiles {
        @SerializedName("filename")
        public String fileName;
        @SerializedName("filecontent")
        public String fileContent;
    }


    public static void main(String[] args) {
        ResponseResult result = new ResponseResult();
        result.ticketid = String.valueOf(123456);
        result.actionResult.put(ResponseAction.PcapCap, false);
        result.actionResult.put(ResponseAction.SuricataDetect, false);
        result.actionResult.put(ResponseAction.BroDetect, false);
        //    result.files.fileName = "";
        //    result.files.fileContent = "";

        System.out.println(new Gson().toJson(result));
    }

}


