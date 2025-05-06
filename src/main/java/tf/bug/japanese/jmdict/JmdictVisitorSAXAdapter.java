package tf.bug.japanese.jmdict;

import org.jetbrains.annotations.Contract;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

// TODO generate this from DTD
public final class JmdictVisitorSAXAdapter extends DefaultHandler {
    private final JmdictVisitor visitor;
    private final StringBuilder accumulator;
    private State state;

    public JmdictVisitorSAXAdapter(JmdictVisitor visitor) {
        this.visitor = visitor;
        this.accumulator = new StringBuilder();
        this.state = null;
    }

    public static enum State {
        ROOT,

        JMDICT,

        ENTRY,

        ENT_SEQ,
        K_ELE,
        R_ELE,
        SENSE,

        KEB,
        KE_INF,
        KE_PRI,

        REB,
        RE_NOKANJI,
        RE_RESTR,
        RE_INF,
        RE_PRI,

        STAGK,
        STAGR,
        XREF,
        ANT,
        POS,
        FIELD,
        MISC,
        LSOURCE,
        DIAL,
        GLOSS,

        PRI,
        S_INF,

        // Transitioned to GLOSS from PRI
        // Accumulator should be empty on GLOSS exit
        GLOSS_FROM_PRI,
    }

    @Contract("_, _ -> fail")
    private static <T> T elemPanic(State state, String qName) {
        throw new IllegalStateException("Unexpected element %s in state %s".formatted(qName, state));
    }

    @Override
    public void startDocument() throws SAXException {
        this.state = State.ROOT;
    }

    @Override
    public void endDocument() throws SAXException {
        assert this.state == State.ROOT;
        this.state = null;
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        this.accumulator.append(ch, start, length);
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        switch (this.state) {
            case ROOT -> {
                if (!"JMdict".equals(qName)) elemPanic(this.state, qName);
                this.state = State.JMDICT;
                this.visitor.enterJmdict();
            }
            case JMDICT -> {
                if (!"entry".equals(qName)) elemPanic(this.state, qName);
                this.state = State.ENTRY;
                this.visitor.enterEntry();
            }
            case ENTRY -> {
                switch (qName) {
                    case "ent_seq" -> this.state = State.ENT_SEQ;
                    case "k_ele" -> {
                        this.state = State.K_ELE;
                        this.visitor.enterKEle();
                    }
                    case "r_ele" -> {
                        this.state = State.R_ELE;
                        this.visitor.enterREle();
                    }
                    case "sense" -> {
                        this.state = State.SENSE;
                        this.visitor.enterSense();
                    }
                    default -> elemPanic(this.state, qName);
                }
            }
            case K_ELE -> {
                switch (qName) {
                    case "keb" -> {
                        assert this.accumulator.isEmpty();
                        this.state = State.KEB;
                    }
                    case "ke_inf" -> {
                        assert this.accumulator.isEmpty();
                        this.state = State.KE_INF;
                    }
                    case "ke_pri" -> {
                        assert this.accumulator.isEmpty();
                        this.state = State.KE_PRI;
                    }
                    default -> elemPanic(this.state, qName);
                }
            }
            case R_ELE -> {
                switch (qName) {
                    case "reb" -> {
                        assert this.accumulator.isEmpty();
                        this.state = State.REB;
                    }
                    case "re_nokanji" -> {
                        assert this.accumulator.isEmpty();
                        this.state = State.RE_NOKANJI;
                    }
                    case "re_restr" -> {
                        assert this.accumulator.isEmpty();
                        this.state = State.RE_RESTR;
                    }
                    case "re_inf" -> {
                        assert this.accumulator.isEmpty();
                        this.state = State.RE_INF;
                    }
                    case "re_pri" -> {
                        assert this.accumulator.isEmpty();
                        this.state = State.RE_PRI;
                    }
                    default -> elemPanic(this.state, qName);
                }
            }
            case SENSE -> {
                switch (qName) {
                    case "stagk" -> {
                        assert this.accumulator.isEmpty();
                        this.state = State.STAGK;
                    }
                    case "stagr" -> {
                        assert this.accumulator.isEmpty();
                        this.state = State.STAGR;
                    }
                    case "xref" -> {
                        assert this.accumulator.isEmpty();
                        this.state = State.XREF;
                    }
                    case "ant" -> {
                        assert this.accumulator.isEmpty();
                        this.state = State.ANT;
                    }
                    case "pos" -> {
                        assert this.accumulator.isEmpty();
                        this.state = State.POS;
                    }
                    case "field" -> {
                        assert this.accumulator.isEmpty();
                        this.state = State.FIELD;
                    }
                    case "misc" -> {
                        assert this.accumulator.isEmpty();
                        this.state = State.MISC;
                    }
                    case "lsource" -> {
                        assert this.accumulator.isEmpty();
                        this.state = State.LSOURCE;
                        String lang = attributes.getValue("xml:lang");
                        String type = attributes.getValue("ls_type");
                        String wasei = attributes.getValue("ls_wasei");
                        this.visitor.enterLsource(lang, type, wasei);
                    }
                    case "dial" -> {
                        assert this.accumulator.isEmpty();
                        this.state = State.DIAL;
                    }
                    case "gloss" -> {
                        assert this.accumulator.isEmpty();
                        this.state = State.GLOSS;
                        String lang = attributes.getValue("xml:lang");
                        String gend = attributes.getValue("g_gend");
                        this.visitor.enterGloss(lang, gend);
                    }
                    case "s_inf" -> {
                        assert this.accumulator.isEmpty();
                        this.state = State.S_INF;
                    }
                    default -> elemPanic(this.state, qName);
                }
            }
            case GLOSS -> {
                if (!"pri".equals(qName)) elemPanic(this.state, qName);
                assert this.accumulator.isEmpty();
                this.state = State.PRI;
            }
            default -> elemPanic(this.state, qName);
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        switch (this.state) {
            case JMDICT -> {
                if(!"JMdict".equals(qName)) elemPanic(this.state, qName);
                this.state = State.ROOT;
                this.visitor.exitJmdict();
            }
            case ENTRY -> {
                if(!"entry".equals(qName)) elemPanic(this.state, qName);
                this.state = State.JMDICT;
                this.visitor.exitEntry();
            }
            case ENT_SEQ -> {
                if(!"ent_seq".equals(qName)) elemPanic(this.state, qName);
                this.state = State.ENTRY;
                this.visitor.visitEntSeq(this.accumulator.toString());
                this.accumulator.setLength(0);
            }
            case K_ELE -> {
                if(!"k_ele".equals(qName)) elemPanic(this.state, qName);
                this.state = State.ENTRY;
                this.visitor.exitKEle();
            }
            case KEB -> {
                if(!"keb".equals(qName)) elemPanic(this.state, qName);
                this.state = State.K_ELE;
                this.visitor.visitKeb(this.accumulator.toString());
                this.accumulator.setLength(0);
            }
            case KE_INF -> {
                if(!"ke_inf".equals(qName)) elemPanic(this.state, qName);
                this.state = State.K_ELE;
                this.visitor.visitKeInf(this.accumulator.toString());
                this.accumulator.setLength(0);
            }
            case KE_PRI -> {
                if(!"ke_pri".equals(qName)) elemPanic(this.state, qName);
                this.state = State.K_ELE;
                this.visitor.visitKePri(this.accumulator.toString());
                this.accumulator.setLength(0);
            }
            case R_ELE -> {
                if(!"r_ele".equals(qName)) elemPanic(this.state, qName);
                this.state = State.ENTRY;
                this.visitor.exitREle();
            }
            case REB -> {
                if(!"reb".equals(qName)) elemPanic(this.state, qName);
                this.state = State.R_ELE;
                this.visitor.visitReb(this.accumulator.toString());
                this.accumulator.setLength(0);
            }
            case RE_NOKANJI -> {
                if(!"re_nokanji".equals(qName)) elemPanic(this.state, qName);
                this.state = State.R_ELE;
                this.visitor.visitReNokanji(this.accumulator.toString());
                this.accumulator.setLength(0);
            }
            case RE_RESTR -> {
                if(!"re_restr".equals(qName)) elemPanic(this.state, qName);
                this.state = State.R_ELE;
                this.visitor.visitReRestr(this.accumulator.toString());
                this.accumulator.setLength(0);
            }
            case RE_INF -> {
                if(!"re_inf".equals(qName)) elemPanic(this.state, qName);
                this.state = State.R_ELE;
                this.visitor.visitReInf(this.accumulator.toString());
                this.accumulator.setLength(0);
            }
            case RE_PRI -> {
                if(!"re_pri".equals(qName)) elemPanic(this.state, qName);
                this.state = State.R_ELE;
                this.visitor.visitRePri(this.accumulator.toString());
                this.accumulator.setLength(0);
            }
            case SENSE -> {
                if(!"sense".equals(qName)) elemPanic(this.state, qName);
                this.state = State.ENTRY;
                this.visitor.exitSense();
            }
            case STAGK -> {
                if(!"stagk".equals(qName)) elemPanic(this.state, qName);
                this.state = State.SENSE;
                this.visitor.visitStagk(this.accumulator.toString());
                this.accumulator.setLength(0);
            }
            case STAGR -> {
                if(!"stagr".equals(qName)) elemPanic(this.state, qName);
                this.state = State.SENSE;
                this.visitor.visitStagr(this.accumulator.toString());
                this.accumulator.setLength(0);
            }
            case XREF -> {
                if(!"xref".equals(qName)) elemPanic(this.state, qName);
                this.state = State.SENSE;
                this.visitor.visitXref(this.accumulator.toString());
                this.accumulator.setLength(0);
            }
            case ANT -> {
                if(!"ant".equals(qName)) elemPanic(this.state, qName);
                this.state = State.SENSE;
                this.visitor.visitAnt(this.accumulator.toString());
                this.accumulator.setLength(0);
            }
            case POS -> {
                if(!"pos".equals(qName)) elemPanic(this.state, qName);
                this.state = State.SENSE;
                this.visitor.visitPos(this.accumulator.toString());
                this.accumulator.setLength(0);
            }
            case FIELD -> {
                if(!"field".equals(qName)) elemPanic(this.state, qName);
                this.state = State.SENSE;
                this.visitor.visitField(this.accumulator.toString());
                this.accumulator.setLength(0);
            }
            case MISC -> {
                if(!"misc".equals(qName)) elemPanic(this.state, qName);
                this.state = State.SENSE;
                this.visitor.visitMisc(this.accumulator.toString());
                this.accumulator.setLength(0);
            }
            case LSOURCE -> {
                if(!"lsource".equals(qName)) elemPanic(this.state, qName);
                this.state = State.SENSE;
                this.visitor.visitLsource(this.accumulator.toString());
                this.accumulator.setLength(0);
                this.visitor.exitLsource();
            }
            case DIAL -> {
                if(!"dial".equals(qName)) elemPanic(this.state, qName);
                this.state = State.SENSE;
                this.visitor.visitDial(this.accumulator.toString());
                this.accumulator.setLength(0);
            }
            case GLOSS -> {
                if(!"gloss".equals(qName)) elemPanic(this.state, qName);
                this.state = State.SENSE;
                this.visitor.visitGloss(this.accumulator.toString());
                this.accumulator.setLength(0);
                this.visitor.exitGloss();
            }
            case GLOSS_FROM_PRI -> {
                if(!"gloss".equals(qName)) elemPanic(this.state, qName);
                this.state = State.SENSE;
                this.visitor.exitGloss();
            }
            case PRI -> {
                if(!"pri".equals(qName)) elemPanic(this.state, qName);
                this.state = State.GLOSS_FROM_PRI;
                this.visitor.visitPri(this.accumulator.toString());
                this.accumulator.setLength(0);
            }
            case S_INF -> {
                if(!"s_inf".equals(qName)) elemPanic(this.state, qName);
                this.state = State.SENSE;
                this.visitor.visitSInf(this.accumulator.toString());
                this.accumulator.setLength(0);
            }
            default -> elemPanic(this.state, qName);
        }
    }
}
