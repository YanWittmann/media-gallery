package de.yanwittmann.gallery;

import de.yanwittmann.gallery.media.MediaService;
import de.yanwittmann.gallery.media.db.MediaTable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.sql.SQLException;

@SpringBootApplication
@RestController
public class MainController {

    public static final Logger LOG = LoggerFactory.getLogger(MainController.class);

    private final MediaService mediaService;

    private static int instance_count = 0;

    public MainController() {
        LOG.info("Starting media service");
        try {
            this.mediaService = new MediaService();
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize media service: " + e.getMessage(), e);
        }
    }

    @GetMapping("/")
    public String index() {
        return "Hello World!";
    }

    @GetMapping("/test")
    public String test() throws SQLException {
        MediaTable mediaTable = new MediaTable();

        System.out.println(mediaTable.getAll());

        return "Test";
    }

    /*@GetMapping("/media")
    public MediaPage getMedia(int start, int limit) {
        return mediaService.getMedia(start, limit);
    }

    @GetMapping("/media/{id}")
    public Media getMedia(@PathVariable String id) {
        return mediaService.getMedia(id);
    }

    @GetMapping("/media/{id}/file")
    public StreamingResponseBody getMediaFile(@PathVariable String id, HttpServletResponse response) throws IOException {
        Media media = mediaService.getMedia(id);
        Path path = Paths.get(media.getPath());
        response.setContentType(Files.probeContentType(path));
        return outputStream -> Files.copy(path, outputStream);
    }*/
}
