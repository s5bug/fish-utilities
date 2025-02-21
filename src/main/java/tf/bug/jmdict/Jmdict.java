package tf.bug.jmdict;

import discord4j.store.api.util.Lazy;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.zip.GZIPInputStream;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;
import org.xml.sax.SAXException;

public final class Jmdict {
    private final HttpClient client;
    private final URI jmdictEGzUri;

    public Jmdict(HttpClient client, URI jmdictEGzUri) {
        this.client = client;
        this.jmdictEGzUri = jmdictEGzUri;
    }

    public static Directory downloadAndSaturateInMemoryStore(HttpClient client, URI jmdictEGzUri) throws IOException, ParserConfigurationException, InterruptedException, SAXException {
        Directory result = new ByteBuffersDirectory();
        Analyzer analyzer = Jmdict.getJmdictAnalyzer();
        IndexWriterConfig iwc = new IndexWriterConfig(analyzer);
        IndexWriter writer = new IndexWriter(result, iwc);

        Jmdict self = new Jmdict(client, jmdictEGzUri);
        self.saturate(writer);

        writer.close();

        return result;
    }

    private static final Lazy<Analyzer> JMDICT_ANALYZER = new Lazy<>(() -> {
        HashMap<String, Analyzer> analyzers = new HashMap<>();
        analyzers.put("keb", new JapaneseNormalAnalyzer());
        analyzers.put("reb", new JapaneseNormalAnalyzer());
        analyzers.put("gloss", new SimpleAnalyzer());
        analyzers.put("sense", new EnglishAnalyzer());

        return new PerFieldAnalyzerWrapper(new StandardAnalyzer(), analyzers);
    });

    public static Analyzer getJmdictAnalyzer() {
        return JMDICT_ANALYZER.get();
    }

    // TODO rethrow exceptions with domain-specific exceptions
    public void saturate(IndexWriter writer) throws IOException, InterruptedException, ParserConfigurationException, SAXException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(this.jmdictEGzUri)
                .method("GET", HttpRequest.BodyPublishers.noBody())
                .build();
        HttpResponse<InputStream> gz =
                this.client.send(request, _ -> HttpResponse.BodySubscribers.ofInputStream());
        InputStream decompress = new GZIPInputStream(gz.body());
        SAXParserFactory pf = SAXParserFactory.newInstance();
        // TODO check schema
        SAXParser p = pf.newSAXParser();
        p.parse(decompress, new JmdictVisitorSAXAdapter(new JmdictLuceneVisitor(writer)));
    }
}
