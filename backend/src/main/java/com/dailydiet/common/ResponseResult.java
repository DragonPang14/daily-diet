package com.dailydiet.common;

import lombok.Data;

@Data
public class ResponseResult<T> {

    private int code;
    private String msg;
    private T data;

    public static <T> ResponseResult<T> success(T data) {
        ResponseResult<T> r = new ResponseResult<>();
        r.setCode(0);
        r.setMsg("ok");
        r.setData(data);
        return r;
    }

    public static <T> ResponseResult<T> success() {
        return success(null);
    }

    public static <T> ResponseResult<T> error(int code, String msg) {
        ResponseResult<T> r = new ResponseResult<>();
        r.setCode(code);
        r.setMsg(msg);
        return r;
    }

    public static <T> ResponseResult<T> error(String msg) {
        return error(500, msg);
    }
}
