package com.github.jonathanlalou.album2book

import groovy.util.logging.Log4j
import lombok.Getter
import lombok.Setter
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component

import javax.annotation.PostConstruct

@Component
@Getter
@Setter
@Log4j
class Album2Epub implements ApplicationRunner {

    @Value('${ebook.target.type}')
    String targetType
    @Value("#{'\${chapter.folders}'.split(',')}")
    List<String> folders
    @Value("#{'\${chapter.titles}'.split(',')}")
    List<String> titles

    @PostConstruct
    void postConstruct() {
        log.info("targetType: $targetType")
        log.info("folders: ${folders}")
        log.info("titles: ${titles}")
    }

    @Override
    void run(ApplicationArguments args) throws Exception {
        log.warn("Hello world")
    }
}
