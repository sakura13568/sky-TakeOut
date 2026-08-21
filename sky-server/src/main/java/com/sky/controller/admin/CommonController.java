package com.sky.controller.admin;

import com.sky.result.Result;
import com.sky.utils.AliOssUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/**
 * 通用类管理
 */
@RestController
@RequestMapping("/admin/common")
@Slf4j
@Api(tags = "通用操作")
public class CommonController {
    @Autowired
    private AliOssUtil aliOssUtil;
    @PostMapping("/upload")
    @ApiOperation("文件上传")
    public Result<String> fillUpload(MultipartFile file){
        String originalName = file.getOriginalFilename();
        String extention = originalName.substring(originalName.lastIndexOf("."));
        String uuid = UUID.randomUUID().toString();
        String fileName = uuid + extention;
    try{
        String filePath = aliOssUtil.upload(file.getBytes(),fileName);
        return Result.success(filePath);
        }catch (Exception e){
            return Result.error(e.getMessage());
        }
    }

}
