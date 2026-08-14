package com.sky.handler;

import com.sky.constant.MessageConstant;
import com.sky.exception.BaseException;
import com.sky.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.sql.SQLIntegrityConstraintViolationException;

/**
 * 全局异常处理器，处理项目中抛出的业务异常
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * 捕获业务异常
     * @param ex
     * @return
     */
    @ExceptionHandler
    public Result exceptionHandler(BaseException ex){
        log.error("异常信息：{}", ex.getMessage());
        return Result.error(ex.getMessage());
    }

    /**
     *
     * @param sq 异常信息
     * @return 返回错误信息
     */
    @ExceptionHandler
    public Result<Object> employeeException(SQLIntegrityConstraintViolationException sq){
        // 记录异常日志，方便排查（原来没打日志，控制台才看不到）
        log.error("异常信息：{}", sq.getMessage());
        String message = sq.getMessage();
        // MySQL 唯一约束冲突的报错是 "Duplicate entry 'xxx' for key 'xxx'"
        if(message.contains("Duplicate entry")) {
            String[] mes = message.split(" ");
            String errorMes = mes[2] + MessageConstant.ACCOUNT_ALREADY_EXIT;
            return Result.error(errorMes);
        }
        else {
            return Result.error(MessageConstant.UNKNOWN_ERROR);
        }
    }
}
