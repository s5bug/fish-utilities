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
import org.apache.lucene.analysis.cjk.CJKAnalyzer;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;
import org.jetbrains.annotations.Nullable;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import reactor.util.Logger;
import reactor.util.Loggers;

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
        p.parse(decompress, new SAXHandler(writer));
    }

    // TODO handle tags and reading exclusions
    private static final class SAXHandler extends DefaultHandler {
        private final IndexWriter writer;
        @Nullable private Document inProgress;
        @Nullable private StringBuilder accumulator;
        private boolean inSense;
        private boolean inGloss;

        public SAXHandler(IndexWriter indexWriter) {
            this.writer = indexWriter;
            this.inProgress = null;
            this.accumulator = null;
            this.inSense = false;
            this.inGloss = false;
        }

        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
            if(this.accumulator != null) {
                if(!this.inSense || this.inGloss)
                    this.accumulator.append(ch, start, length);
            }
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            switch(qName) {
                // TODO reorder these looking at schema
                case "entry" -> {
                    // TODO throw an error if inProgress is non-null
                    this.inProgress = new Document();
                }
                case "keb" -> {
                    // TODO throw an error if StringBuilder is non-null
                    this.accumulator = new StringBuilder();
                }
                case "reb" -> {
                    // TODO throw an error if StringBuilder is non-null
                    this.accumulator = new StringBuilder();
                }
                case "sense" -> {
                    // TODO throw an error if StringBuilder is non-null
                    this.inSense = true;
                    this.accumulator = new StringBuilder();
                }
                case "gloss" -> {
                    this.inGloss = true;
                }
            }
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            switch(qName) {
                case "entry" -> {
                    try {
                        this.writer.addDocument(this.inProgress);
                        this.inProgress = null;
                    } catch (IOException e) {
                        // TODO should this be a different exception type?
                        throw new SAXException(e);
                    }
                }
                case "keb" -> {
                    this.inProgress.add(new TextField("keb", this.accumulator.toString(), Field.Store.YES));
                    this.accumulator = null;
                }
                case "reb" -> {
                    this.inProgress.add(new TextField("reb", this.accumulator.toString(), Field.Store.YES));
                    this.accumulator = null;
                }
                case "sense" -> {
                    // remove trailing "; "
                    this.accumulator.setLength(this.accumulator.length() - 2);
                    this.inProgress.add(new TextField("sense", this.accumulator.toString(), Field.Store.YES));
                    this.inSense = false;
                    this.accumulator = null;
                }
                case "gloss" -> {
                    this.accumulator.append("; ");
                    this.inGloss = false;
                }
            }
        }
    }
}
