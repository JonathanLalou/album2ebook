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

    @Value('${dc.identifier.scheme}')
    DcIdentifierScheme dcIdentifierScheme
    @Value('${dc.language}')
    String dcLanguage
    @Value('${dc.creator.aut}')
    String dcCreatorAut
    @Value('${dc.contributor.trl}')
    String dcContributorTrl
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
        log.info("dcIdentifierScheme: ${dcIdentifierScheme}")
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
        // Load template for XHTML files embedding images
        String contentOpf = this.getClass().getResource('/epub/content.opf').getText('UTF-8')

        StringBuffer items = new StringBuffer()
        StringBuffer itemrefs = new StringBuffer()

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
                    def targetImageRelativePath = "Images/${folders[i]}/${file.name}"
                    Files.copy(file.toPath(), Path.of("${work}/OEBPS/" + targetImageRelativePath))
                    def xhtmlFileName = "${FilenameUtils.removeExtension(file.name)}.xhtml"
                    def targetXhtmlRelativePath = "Text/${folders[i]}/${xhtmlFileName}"
                    new File("${work}/OEBPS/" + targetXhtmlRelativePath)
                            .write(
                                    StringUtils.replace(
                                            imageTemplateXhtml,
                                            '${image}',
                                            "${folders[i]}/${file.name}"
                                    )
                            )
                    String mediaType = retrieveMediaType(file)
                    items << "        <item id=\"${file.name}\" href=\"${targetImageRelativePath}\" media-type=\"${mediaType}\"/>\n"
                    items << "        <item id=\"${xhtmlFileName}\" href=\"${targetXhtmlRelativePath}\" media-type=\"application/xhtml+xml\"/>\n"
                    itemrefs << "        <itemref idref=\"${xhtmlFileName}\"/>\n"
                    // TODO feed toc.ncx
                }
            }
        }
        def targetImageRelativePath = "OEBPS/content.opf"
        contentOpf = StringUtils.replaceEach(
                contentOpf,
                ['${dcCreatorAut}', '${dcLanguage}', '${dcContributorTrl}', '${items}', '${itemrefs}'] as String[],
                [dcCreatorAut, dcLanguage, dcContributorTrl, items.toString(), itemrefs.toString()] as String[]
        )
        new File("${work}/OEBPS/content.opf").write(contentOpf)

    }

    String retrieveMediaType(File file) {
        switch (FilenameUtils.getExtension(file.name)) {
            case "jpg":
            case "jpeg":
                return "image/jpeg"
            case "png":
                return "image/png"
        }
        ""
    }

    def Path copyResource(String sourceFile, String targetFile) {
        Path targetPath = Path.of("${workFolder}/" + targetFile)
        Files.createDirectories(targetPath.getParent());
        Files.copy(Path.of("./src/main/resources/" + sourceFile), targetPath)
    }
}
