package de.yanwittmann.gallery;

import de.yanwittmann.gallery.media.MediaService;
import de.yanwittmann.gallery.media.config.ConfigField;
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
import org.springframework.web.bind.annotation.*;

import java.awt.*;
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

    @GetMapping("/media/page/count/{includeVideos}")
    public String getMedia(@PathVariable boolean includeVideos) {
        return new JSONObject().put("total", String.valueOf(mediaService.getPageCount(includeVideos))).toString();
    }

    @GetMapping("/media/page/{page}/{orderBy}/{asc}/{includeVideos}")
    public String getMedia(@PathVariable int page, @PathVariable String orderBy, @PathVariable boolean asc, @PathVariable boolean includeVideos) throws SQLException {
        return new JSONObject()
                .put("ids", new JSONArray(mediaService.getMediaIds(page, orderBy, asc, includeVideos).stream().map(Object::toString).collect(Collectors.toList())))
                .put("page", page)
                .put("total", mediaService.getPageCount(includeVideos))
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

    @GetMapping("/media/get/{id}/type")
    public String getMediaType(@PathVariable long id) {
        final File file = mediaService.getMediaFile(id);
        return new JSONObject()
                .put("type", file == null ? "unknown" : file.getName().endsWith(".mp4") ? "vid" : "img")
                .toString();
    }

    @GetMapping("/system/show-in-folder/{id}")
    public String getShowInEnclosingFolder(@PathVariable long id) {
        final File file = mediaService.getMediaFile(id);
        if (file == null) return new JSONObject().put("success", false).put("message", "File not found").toString();

        try {
            Desktop.getDesktop().open(file.getParentFile());
        } catch (IOException e) {
            return new JSONObject().put("success", false).put("message", "Failed to open folder: " + e.getMessage()).toString();
        }

        return new JSONObject().put("success", true).toString();
    }

    @GetMapping("/settings/get")
    public String getMediaThumb() {
        return new JSONObject()
                .put("settings", mediaService.getSettings().toJson())
                .toString();
    }

    @PostMapping("/settings/path/rescan")
    public String postSettingsPathRescan(@RequestBody String requestBody) {
        final JSONObject request = new JSONObject(requestBody);
        final String path = request.getString("path");
        if (path == null) {
            return new JSONObject().put("success", false).put("message", "Missing path").toString();
        }

        final File file = new File(path);

        mediaService.rescanMedia(file);
        return new JSONObject().put("success", true).toString();
    }

    @PostMapping("/settings/path/remove")
    public String postSettingsPathRemove(@RequestBody String requestBody) {
        final JSONObject request = new JSONObject(requestBody);
        final String path = request.getString("path");
        if (path == null) {
            return new JSONObject().put("success", false).put("message", "Missing path").toString();
        }

        final File file = new File(path);

        mediaService.removeMedia(file);
        return new JSONObject().put("success", true).toString();
    }

    @PostMapping("/settings/path/disable")
    public String postSettingsPathDisable(@RequestBody String requestBody) {
        final JSONObject request = new JSONObject(requestBody);
        final String path = request.getString("path");
        if (path == null) {
            return new JSONObject().put("success", false).put("message", "Missing path").toString();
        }

        final File file = new File(path);

        mediaService.disableMedia(file);
        return new JSONObject().put("success", true).toString();
    }

    @PostMapping("/settings/path/enable")
    public String postSettingsPathEnable(@RequestBody String requestBody) {
        final JSONObject request = new JSONObject(requestBody);
        final String path = request.getString("path");
        if (path == null) {
            return new JSONObject().put("success", false).put("message", "Missing path").toString();
        }

        final File file = new File(path);

        mediaService.enableMedia(file);
        return new JSONObject().put("success", true).toString();
    }

    @PostMapping("/settings/path/add")
    public String postSettingsPathAdd(@RequestBody String requestBody) {
        final JSONObject request = new JSONObject(requestBody);
        final String path = request.getString("path");
        if (path == null) {
            return new JSONObject().put("success", false).put("message", "Missing path").toString();
        }

        final File file = new File(path);

        mediaService.addMedia(file);
        return new JSONObject().put("success", true).toString();
    }

    @PostMapping("/settings/reindex-on-startup")
    public String postSettingsReindexOnStartup(@RequestBody String requestBody) {
        final JSONObject request = new JSONObject(requestBody);
        final boolean checked = request.getBoolean("checked");
        mediaService.getSettings().set(ConfigField.INDEX_ON_STARTUP, checked);
        return new JSONObject().put("success", true).toString();
    }

    private ResponseEntity<Resource> buildResponseEntityForFile(File file) throws IOException {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + file.getName());
        headers.add(HttpHeaders.CONTENT_TYPE, Files.probeContentType(file.toPath()));
        Resource resource = new FileSystemResource(file);
        return new ResponseEntity<>(resource, headers, HttpStatus.OK);
    }
}
