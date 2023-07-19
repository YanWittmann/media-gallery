package de.yanwittmann.gallery;

import de.yanwittmann.gallery.media.MediaService;
import de.yanwittmann.gallery.util.ImageUtil;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.SQLException;
import java.util.stream.Collectors;

@SpringBootApplication
@RestController
public class MainController {

    public static final Logger LOG = LoggerFactory.getLogger(MainController.class);

    private final MediaService mediaService;

    public MainController() {
        LOG.info("Starting media service");
        try {
            this.mediaService = new MediaService();
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize media service: " + e.getMessage(), e);
        }

        ImageUtil.initializeThumbnailCleanup(MediaGalleryConfig.getThumbsDir());
    }

    @GetMapping("/media/page/count")
    public String getMedia() {
        return new JSONObject().put("total", String.valueOf(mediaService.getPageCount())).toString();
    }

    @GetMapping("/media/page/{page}")
    public String getMedia(@PathVariable int page) throws SQLException {
        return new JSONObject()
                .put("ids", new JSONArray(mediaService.getMediaIds(page).stream().map(Object::toString).collect(Collectors.toList())))
                .put("page", page)
                .put("total", mediaService.getPageCount())
                .toString();
    }

    @GetMapping("/media/get/{id}/full")
    public ResponseEntity<Resource> getMediaFull(@PathVariable long id) throws IOException {
        final File file = mediaService.getMediaFile(id);
        if (file == null) return new ResponseEntity<>(HttpStatus.NOT_FOUND);

        return buildResponseEntityForFile(file);
    }

    @GetMapping("/media/get/{id}/thumb/{size}")
    public ResponseEntity<Resource> getMediaThumb(@PathVariable long id, @PathVariable int size) throws IOException {
        final File file = mediaService.getMediaFile(id);
        if (file == null) return new ResponseEntity<>(HttpStatus.NOT_FOUND);

        File thumbnailFile = ImageUtil.createThumbnail(file, new File(MediaGalleryConfig.getThumbsDir(), size + "-" + MediaService.hash(file.getAbsolutePath()) + ".png"), size);

        return buildResponseEntityForFile(thumbnailFile);
    }

    private ResponseEntity<Resource> buildResponseEntityForFile(File file) throws IOException {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + file.getName());
        headers.add(HttpHeaders.CONTENT_TYPE, Files.probeContentType(file.toPath()));
        Resource resource = new FileSystemResource(file);
        return new ResponseEntity<>(resource, headers, HttpStatus.OK);
    }
}
