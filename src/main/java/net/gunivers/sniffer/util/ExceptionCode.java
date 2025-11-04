package net.gunivers.sniffer.util;

public enum ExceptionCode {
    REFLECT_INVOKE_EXCEPTION(0),
    NULL_POINTER_EXCEPTION(1);

    private final int code;

    ExceptionCode(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
