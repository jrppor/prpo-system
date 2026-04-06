package com.jirapat.prpo.config;

import java.util.Set;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.upload")
public class FileUploadProperties {

    private String directory;
    private long maxFileSize;
    private int maxFilesPerReference;
    private Set<String> allowedContentTypes;
}
