package main.java.cn.edu.jslab6.enums;

/**
 * @author Andy
 * @date 18-6-16 上午11:45
 * @description 采集任务采集状态
 */
public enum ActiveTaskStatus {
    WAIT_SENSOR("等待采集",0),
    SENSORING("正在采集",1),
    FORCE("强制结束",2),
    FINISHED("完成采集",3);

    private String status;
    private Integer value;

    private ActiveTaskStatus(String status, Integer value) {
        this.status = status;
        this.value = value;
    }

    public String getKey() {
        return status;
    }

    public Integer getValue() {
        return value;
    }
}
