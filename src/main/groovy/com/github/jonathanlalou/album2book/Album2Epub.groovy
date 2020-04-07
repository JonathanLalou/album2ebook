package com.github.jonathanlalou.album2book

import groovy.util.logging.Log4j
import lombok.Getter
import lombok.Setter
import org.apache.commons.io.FilenameUtils
import org.apache.commons.lang3.StringUtils
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component

import javax.annotation.PostConstruct
import java.nio.file.Files
import java.nio.file.Path

@Component
@Getter
@Setter
@Log4j
class Album2Epub implements ApplicationRunner {

    @Value('${work.folder}')
    String workFolder
    @Value('${source.folder}')
    String sourceFolder
    @Value('${ebook.target.type}')
    String targetType
    @Value("#{'\${chapter.folders}'.split(',')}")
    List<String> folders
    @Value("#{'\${chapter.titles}'.split(',')}")
    List<String> titles

    @PostConstruct
    void postConstruct() {
        log.info("sourceFolder: $sourceFolder")
        log.info("targetType: $targetType")
        log.info("folders: ${folders}")
        log.info("titles: ${titles}")
    }

    @Override
    void run(ApplicationArguments args) throws Exception {
        final File work = new File(workFolder)
        if (work.exists()) {
            work.deleteDir()
        }
        work.mkdirs()
        copyResource("epub/mimetype", "mimetype")
        copyResource("epub/container.xml", "/META-INF/container.xml")
        copyResource("epub/styles.css", "/OEBPS/Styles/styles.css")

        // Load template for XHTML files embedding images
        String imageTemplateXhtml = this.getClass().getResource('/epub/image-template.xhtml').getText('UTF-8')

        for (int i = 0; i < folders.size(); i++) {
            log.info("Chapter: $i *** ${folders[i]} *** ${titles[i]}")
            File folder = new File(sourceFolder + folders[i])
            if (folder.exists()) {
                // Create folder for images
                Files.createDirectories(Path.of("${work}/OEBPS/Images/${folders[i]}/"))
                // Create folder for XHTML files
                Files.createDirectories(Path.of("${work}/OEBPS/Text/${folders[i]}/"))
                final List<File> files = Arrays.asList(folder.listFiles())
                for (File file : files) {
                    log.info(file)
                    // Copy image
                    Files.copy(file.toPath(), Path.of("${work}/OEBPS/Images/${folders[i]}/${file.getName()}"))
                    new File("${work}/OEBPS/Text/${folders[i]}/${FilenameUtils.removeExtension(file.getName())}.xhtml")
                            .write(
                                    StringUtils.replace(
                                            imageTemplateXhtml,
                                            '${image}',
                                            "${folders[i]}/${file.getName()}"
                                    )
                            )
                }
            }
        }
    }

    def Path copyResource(String sourceFile, String targetFile) {
        Path targetPath = Path.of("${workFolder}/" + targetFile)
        Files.createDirectories(targetPath.getParent());
        Files.copy(Path.of("./src/main/resources/" + sourceFile), targetPath)
    }
}
