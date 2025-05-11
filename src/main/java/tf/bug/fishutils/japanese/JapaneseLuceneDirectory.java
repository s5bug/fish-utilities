package tf.bug.fishutils.japanese;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import discord4j.store.api.util.Lazy;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.zip.GZIPInputStream;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;
import org.xml.sax.SAXException;
import tf.bug.fishutils.japanese.freqcc100.FreqCc100;
import tf.bug.fishutils.japanese.jmdict.JmdictLuceneVisitor;
import tf.bug.fishutils.japanese.jmdict.JmdictVisitorSAXAdapter;

public final class JapaneseLuceneDirectory {
    private final InputStream jmdictEGz;
    private final InputStream freqCc100;

    public JapaneseLuceneDirectory(InputStream jmdictEGz, InputStream freqCc100) {
        this.jmdictEGz = jmdictEGz;
        this.freqCc100 = freqCc100;
    }

    public static Directory of(InputStream jmdictEGz, InputStream freqCc100) throws IOException, ParserConfigurationException, SAXException {
        Directory result = new ByteBuffersDirectory();
        Analyzer analyzer = JapaneseLuceneDirectory.getJmdictAnalyzer();
        IndexWriterConfig iwc = new IndexWriterConfig(analyzer);
        IndexWriter writer = new IndexWriter(result, iwc);

        JapaneseLuceneDirectory self = new JapaneseLuceneDirectory(jmdictEGz, freqCc100);
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
    public void saturate(IndexWriter writer) throws IOException, ParserConfigurationException, SAXException {
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule entryDeserializerModule = new SimpleModule(
                "FreqCc100EntryDeserializer",
                new Version(1, 0, 0, null, null, null)
        );
        entryDeserializerModule.addDeserializer(FreqCc100.Entry.class, new FreqCc100.Entry.Deserializer());
        mapper.registerModule(entryDeserializerModule);
        List<FreqCc100.Entry> frequencyInfo = mapper.readerForListOf(FreqCc100.Entry.class).readValue(this.freqCc100);

        InputStream jmDict = new GZIPInputStream(this.jmdictEGz);
        SAXParserFactory pf = SAXParserFactory.newInstance();
        // TODO check schema
        SAXParser p = pf.newSAXParser();
        p.parse(jmDict, new JmdictVisitorSAXAdapter(new JmdictLuceneVisitor(writer, new FreqCc100(frequencyInfo))));
    }
}
