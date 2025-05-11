package tf.bug.fishutils.japanese.jmdict;

import java.util.Map;
import java.util.Set;
import org.apache.lucene.document.*;
import org.apache.lucene.index.IndexWriter;

import java.io.IOException;
import tf.bug.fishutils.japanese.freqcc100.FreqCc100;

// TODO handle tags and reading exclusions
public final class JmdictLuceneVisitor extends JmdictVisitor {
    private final IndexWriter writer;
    private final FreqCc100 frequencyInfo;
    private final Map<String, Set<FreqCc100.Entry>> kebLookup;
    private Document document;
    private final StringBuilder senseColumnBuilder;
    private int senseIndex;

    public JmdictLuceneVisitor(IndexWriter writer, FreqCc100 frequencyInfo) {
        this.writer = writer;
        this.frequencyInfo = frequencyInfo;
        this.kebLookup = this.frequencyInfo.kebLookupMap();
        this.document = null;
        this.senseColumnBuilder = new StringBuilder();
        this.senseIndex = 0;
    }

    @Override
    public void enterJmdict() {
        // do nothing
    }

    @Override
    public void exitJmdict() {
        // do nothing
    }

    @Override
    public void enterEntry() {
        assert this.senseIndex == 0;
        this.document = new Document();
    }

    @Override
    public void exitEntry() {
        try {
            int frequency = this.frequencyInfo.entries().size();
            String[] kebs = this.document.getValues("keb");
            if(kebs.length != 0) {
                Set<String> rebs = Set.of(this.document.getValues("reb"));
                for(String keb : kebs) {
                    Set<FreqCc100.Entry> entries = this.kebLookup.get(keb);
                    if(entries == null) continue;

                    for(FreqCc100.Entry entry : entries) {
                        if(entry.reb() != null && !rebs.contains(entry.reb())) continue;

                        frequency = Math.min(entry.frequency(), frequency);
                    }
                }
            } else {
                String[] rebs = this.document.getValues("reb");
                for(String reb : rebs) {
                    // If an entry has no `keb`, it is Han-less in FreqCc100
                    Set<FreqCc100.Entry> entries = this.kebLookup.get(reb);

                    if(entries == null) continue;

                    for(FreqCc100.Entry entry : entries) {
                        frequency = Math.min(entry.frequency(), frequency);
                    }
                }
            }

            this.document.add(new NumericDocValuesField("frequency", frequency));
            this.senseIndex = 0;

            this.writer.addDocument(this.document);
            this.document = null;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void visitEntSeq(String entSeq) {
        // do nothing
    }

    @Override
    public void enterKEle() {
        // do nothing
    }

    @Override
    public void exitKEle() {
        // do nothing
    }

    @Override
    public void visitKeb(String keb) {
        this.document.add(new TextField("keb", keb, Field.Store.YES));
        this.document.add(new SortedNumericDocValuesField("keb_length", keb.codePointCount(0, keb.length())));
    }

    @Override
    public void visitKeInf(String keInf) {
        // do nothing
    }

    @Override
    public void visitKePri(String kePri) {
        // do nothing
    }

    @Override
    public void enterREle() {
        // do nothing
    }

    @Override
    public void exitREle() {
        // do nothing
    }

    @Override
    public void visitReb(String reb) {
        this.document.add(new TextField("reb", reb, Field.Store.YES));
        this.document.add(new SortedNumericDocValuesField("reb_length", reb.codePointCount(0, reb.length())));
    }

    @Override
    public void visitReNokanji(String reNokanji) {
        // do nothing
    }

    @Override
    public void visitReRestr(String reRestr) {
        // do nothing
    }

    @Override
    public void visitReInf(String reInf) {
        // do nothing
    }

    @Override
    public void visitRePri(String rePri) {
        // do nothing
    }

    @Override
    public void enterSense() {
        assert this.senseColumnBuilder.isEmpty();
    }

    @Override
    public void exitSense() {
        // Remove "; "
        this.senseColumnBuilder.setLength(this.senseColumnBuilder.length() - 2);
        this.document.add(new TextField("sense", this.senseColumnBuilder.toString(), Field.Store.YES));

        this.senseIndex++;
        this.senseColumnBuilder.setLength(0);
    }

    @Override
    public void visitStagk(String stagk) {
        // do nothing
    }

    @Override
    public void visitStagr(String stagr) {
        // do nothing
    }

    @Override
    public void visitXref(String xref) {
        this.document.add(new TextField("xref-%d".formatted(this.senseIndex), xref, Field.Store.YES));
    }

    @Override
    public void visitAnt(String ant) {
        // do nothing
    }

    @Override
    public void visitPos(String pos) {
        this.document.add(new TextField("pos-%d".formatted(this.senseIndex), pos, Field.Store.YES));
    }

    @Override
    public void visitField(String field) {
        // do nothing
    }

    @Override
    public void visitMisc(String misc) {
        // do nothing
    }

    @Override
    public void enterLsource(String lsLang, String lsType, String lsWasei) {
        // do nothing
    }

    @Override
    public void visitLsource(String lsource) {
        // do nothing
    }

    @Override
    public void exitLsource() {
        // do nothing
    }

    @Override
    public void visitDial(String dial) {
        // do nothing
    }

    @Override
    public void enterGloss(String glang, String gGend) {
        // do nothing
    }

    @Override
    public void visitGloss(String gloss) {
        this.document.add(new TextField("gloss", gloss, Field.Store.YES));
        this.senseColumnBuilder.append(gloss);
        this.senseColumnBuilder.append("; ");
    }

    @Override
    public void visitPri(String pri) {
        // TODO prioritize pri fields
        // Maybe add a pri column that gets searched before the gloss column?
        this.visitGloss(pri);
    }

    @Override
    public void exitGloss() {

    }

    @Override
    public void visitSInf(String sInf) {
        this.document.add(new TextField("s_inf-%d".formatted(this.senseIndex), sInf, Field.Store.YES));
    }
}
