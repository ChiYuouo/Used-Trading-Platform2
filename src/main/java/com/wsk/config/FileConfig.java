package com.wsk.config;

import cn.hutool.core.io.FileUtil;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

/**
 * @author sk
 * @date 2023/4/18
 */
@Configuration
@Getter
public class FileConfig {

    @Value("${my.file.save}")
    private String fileSave;
    @Value("${my.file.saveImage}")
    private String saveImage;
    @Value("${my.file.saveThumbnails}")
    private String saveThumbnails;

    @PostConstruct
    public void init() {
        FileUtil.mkdir(getImageDirPath());
        FileUtil.mkdir(getThumbnailsDirPath());
    }

    public String getImageDirPath() {
        return fileSave + ":" + saveImage;
    }

    public String getThumbnailsDirPath() {
        return fileSave + ":" + saveThumbnails;
    }
}
