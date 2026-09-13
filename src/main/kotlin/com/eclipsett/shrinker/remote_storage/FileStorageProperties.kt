package com.eclipsett.shrinker.remote_storage

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties("app.file-storage")
@Component
class FileStorageProperties(val uploadBucket: String = "files")
