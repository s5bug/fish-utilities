package tf.bug.fishutils.japanese.jmdict;

// TODO generate this from DTD
public abstract class JmdictVisitor {

    public abstract void enterJmdict();
    public abstract void exitJmdict();

    public abstract void enterEntry();
    public abstract void exitEntry();

    public abstract void visitEntSeq(String entSeq);

    public abstract void enterKEle();
    public abstract void exitKEle();

    public abstract void visitKeb(String keb);
    public abstract void visitKeInf(String keInf);
    public abstract void visitKePri(String kePri);

    public abstract void enterREle();
    public abstract void exitREle();

    public abstract void visitReb(String reb);
    public abstract void visitReNokanji(String reNokanji);
    public abstract void visitReRestr(String reRestr);
    public abstract void visitReInf(String reInf);
    public abstract void visitRePri(String rePri);

    public abstract void enterSense();
    public abstract void exitSense();

    public abstract void visitStagk(String stagk);
    public abstract void visitStagr(String stagr);
    public abstract void visitXref(String xref);
    public abstract void visitAnt(String ant);
    public abstract void visitPos(String pos);
    public abstract void visitField(String field);
    public abstract void visitMisc(String misc);

    public abstract void enterLsource(String lsLang, String lsType, String lsWasei);
    public abstract void visitLsource(String lsource);
    public abstract void exitLsource();

    public abstract void visitDial(String dial);

    public abstract void enterGloss(String glang, String gGend);
    public abstract void visitGloss(String gloss);
    public abstract void visitPri(String pri);
    public abstract void exitGloss();

    public abstract void visitSInf(String sInf);

}
