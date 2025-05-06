package tf.bug;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Paths;

public class UriReader {

    private final HttpClient httpClient;

    public UriReader(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public InputStream open(URI uri) throws IOException, InterruptedException {
        switch(uri.getScheme()) {
            case "http", "https" -> {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(uri)
                        .method("GET", HttpRequest.BodyPublishers.noBody())
                        .build();
                HttpResponse<InputStream> response =
                        this.httpClient.send(request, _ -> HttpResponse.BodySubscribers.ofInputStream());
                return response.body();
            }
            case "file" -> {
                return Files.newInputStream(Paths.get(uri));
            }
            default -> {
                throw new UnsupportedOperationException("Unsupported URI scheme: " + uri.getScheme());
            }
        }
    }
}
