package com.app.chao.chaoapp.net;

/**
 * Created by codeest on 16/8/28.
 */

public class VideoHttpResponse<T> {

    private boolean success;
    private String msg;
    private T result;

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public T getResult() {
        return result;
    }

    public void setResult(T result) {
        this.result = result;
    }
}
