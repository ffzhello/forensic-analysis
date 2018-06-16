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
    String ticketid;

    @SerializedName("responseresult")
    Map<ResponseAction, Boolean> actionResult = new HashMap<ResponseAction, Boolean>();

    ResponseFiles files = new ResponseFiles();

    class ResponseFiles {
        @SerializedName("filename")
        String fileName;
        @SerializedName("filecontent")
        String fileContent;
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


