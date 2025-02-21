package tf.bug.jmdict;

import org.apache.lucene.document.*;
import org.apache.lucene.index.IndexWriter;

import java.io.IOException;

// TODO handle tags and reading exclusions
final class JmdictLuceneVisitor extends JmdictVisitor {
    private final IndexWriter writer;
    private Document document;
    private final StringBuilder senseColumnBuilder;

    public JmdictLuceneVisitor(IndexWriter writer) {
        this.writer = writer;
        this.document = null;
        this.senseColumnBuilder = new StringBuilder();
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
        this.document = new Document();
    }

    @Override
    public void exitEntry() {
        try {
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
        // do nothing
    }

    @Override
    public void visitAnt(String ant) {
        // do nothing
    }

    @Override
    public void visitPos(String pos) {
        // do nothing
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

    }
}
