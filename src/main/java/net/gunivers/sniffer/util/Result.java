package net.gunivers.sniffer.util;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

@SuppressWarnings({"unused", "UnusedReturnValue"})
public class Result<T> {
    private final boolean success;
    private final T data;
    private final String error;
    private final ExceptionCode code;

    // 私有构造器
    private Result(boolean success, T data, String error, ExceptionCode errorCode) {
        this.success = success;
        this.data = data;
        this.error = error;
        this.code = errorCode;
    }

    public static <T> Result<T> success(T data) {
        return new Result<>(true, data, null, null);
    }

    public static <T> Result<T> success() {
        return new Result<>(true, null, null, null);
    }

    public static <T> Result<T> failure(String error) {
        return new Result<>(false, null, error, null);
    }

    public static <T> Result<T> failure(String error, ExceptionCode errorCode) {
        return new Result<>(false, null, error, errorCode);
    }

    public boolean isSuccess() {
        return success;
    }

    public boolean isFailure() {
        return !success;
    }

    // 数据获取
    public T getData() {
        if (!success) {
            throw new IllegalStateException("Cannot get data from failed result: " + error);
        }
        return data;
    }

    public T getDataOrElse(T defaultValue) {
        return success ? data : defaultValue;
    }

    // 错误信息
    public String getError() {
        return error;
    }

    public ExceptionCode getErrorCode() {
        return code;
    }

    // 函数式支持
    public <U> Result<U> map(Function<T, U> mapper) {
        if (isFailure()) {
            return Result.failure(error, code);
        }
        try {
            return Result.success(mapper.apply(data));
        } catch (Exception e) {
            return Result.failure("Mapping failed: " + e.getMessage());
        }
    }

    public <U> Result<U> flatMap(Function<T, Result<U>> mapper) {
        if (isFailure()) {
            return Result.failure(error, code);
        }
        try {
            return mapper.apply(data);
        } catch (Exception e) {
            return Result.failure("Flat mapping failed: " + e.getMessage());
        }
    }

    public Result<T> onSuccess(Consumer<T> action) {
        if (isSuccess()) {
            action.accept(data);
        }
        return this;
    }

    public Result<T> onFailure(Consumer<String> action) {
        if (isFailure()) {
            action.accept(error);
        }
        return this;
    }

    public Optional<T> toOptional() {
        return success ? Optional.of(data) : Optional.empty();
    }
}
