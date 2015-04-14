/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.jeffreysanti.text_analy4;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 *
 * @author jeffrey
 */
public class CANGram implements ComponentAnalyzer {

    // do not include in n-gram & reset
    public static final List<String> stopTags = Arrays.asList(
        "-LRB-",
        "-RRB-",
        "''",
        "\"\"",
        "``");
    
    // include in n-gram, then reset (assure they are at end)
    public static final List<String> stopTagsPostInclude = Arrays.asList(
        ".",
        ":",
        "?");
    
    // skip over these
    public static final List<String> ignoreTags = Arrays.asList(
        ",",
        ";",
        "POS");

    @Override
    public void pushTagString(String tag) {
    }

    @Override
    public void pushNamedEntity(String word, int stemId, String ne, int wordIndex) throws Exception {
    }
    
    public class BiGram{
        public BiGram(String r1, String r2){
            root1 = r1;
            root2 = r2;
        }
        public String root1, root2;

        @Override
        public boolean equals(Object arg) {
            if(!(arg instanceof BiGram))
                return false;
            BiGram b = (BiGram)arg;
            return (b.root1.equals(root1) && b.root2.equals(root2));
        }

        @Override
        public int hashCode() {
            int hash = 3;
            hash = 47 * hash + (this.root1 != null ? this.root1.hashCode() : 0);
            hash = 47 * hash + (this.root2 != null ? this.root2.hashCode() : 0);
            return hash;
        }
    }
    
    public class TriGram{
        public TriGram(String r1, String r2, String r3){
            root1 = r1;
            root2 = r2;
            root3 = r3;
        }
        public String root1, root2, root3;

        @Override
        public boolean equals(Object arg) {
            if(!(arg instanceof TriGram))
                return false;
            TriGram t = (TriGram)arg;
            return (t.root1.equals(root1) && t.root2.equals(root2) && t.root3.equals(root3));
        }

        @Override
        public int hashCode() {
            int hash = 5;
            hash = 53 * hash + (this.root1 != null ? this.root1.hashCode() : 0);
            hash = 53 * hash + (this.root2 != null ? this.root2.hashCode() : 0);
            hash = 53 * hash + (this.root3 != null ? this.root3.hashCode() : 0);
            return hash;
        }
    }
    
    public CANGram(Connection db, Section s) throws SQLException
    {
        c = db;
        sec = s;
        Statement stmt = c.createStatement();
        stmt.executeUpdate("CREATE TABLE IF NOT EXISTS biworddist (word1 TEXT NOT NULL, word2 TEXT NOT NULL, section INTEGER, cnt INTEGER)");
        stmt.close();

        stmt = c.createStatement();
        stmt.executeUpdate("CREATE TABLE IF NOT EXISTS triworddist (word1 TEXT NOT NULL, word2 TEXT NOT NULL, word3 TEXT NOT NULL, section INTEGER, cnt INTEGER)");
        stmt.close();
    }
    
    private Connection c;
    private Section sec;
    
    public static final String VAL_UNDEFINED = "{UNDEF}";
    public static final String VAL_SENT_START = "{START}";
    
    
    private BiGram B = new BiGram(VAL_UNDEFINED, VAL_UNDEFINED);
    private TriGram T = new TriGram(VAL_UNDEFINED, VAL_UNDEFINED, VAL_UNDEFINED);
    
    private HashMap<BiGram, Integer> BL = new HashMap();
    private HashMap<TriGram, Integer> TL = new HashMap();
    
    
    
    
    @Override
    public void sentenceStart(int id) throws Exception {
        B = new BiGram(VAL_SENT_START, VAL_UNDEFINED);
        T = new TriGram(VAL_SENT_START, VAL_UNDEFINED, VAL_UNDEFINED);
    }

    private void pushBiGram(){
        if(BL.containsKey(B)){
            BL.put(B, BL.get(B)+1);
        }else{
            BL.put(B, 1);
        }
    }
    private void pushTriGram(){
        if(TL.containsKey(T)){
            TL.put(T, TL.get(T)+1);
        }else{
            TL.put(T, 1);
        }
    }
    
    private void acceptWord(String wrd)
    {
        if(B.root2.equals(VAL_UNDEFINED)){
            B = new BiGram(B.root1, wrd);
            pushBiGram();
        }else{
            B = new BiGram(B.root2, wrd);
            pushBiGram();
        }
        if(T.root2.equals(VAL_UNDEFINED)){
            T = new TriGram(T.root1, wrd, VAL_UNDEFINED);
        }else if(T.root3.equals(VAL_UNDEFINED)){
            T = new TriGram(T.root1, T.root2, wrd);
            pushTriGram();
        }else{
            T = new TriGram(T.root2, T.root3, wrd);
            pushTriGram();
        }
    }
    
    @Override
    public void pushWord(String word, String tag, String stem, int stemId) throws Exception {
        if(stopTags.contains(tag)){ // ie: quotes or parens -> equiv to new sentence w/o full stop
            sentenceStart(-1);
            return;
        }
        if(ignoreTags.contains(tag))
            return;
        
        if(stopTagsPostInclude.contains(tag)){
            acceptWord(word);
            sentenceStart(-1);
            return;
        }
        acceptWord(word);
    }

    @Override
    public void sentenceEnd() throws Exception {
        sentenceStart(-1);
    }

    @Override
    public void sectionEnd() throws Exception {
        // populate db with bi/tri grams
        PreparedStatement sbi = c.prepareStatement("INSERT INTO biworddist (word1,word2,section,cnt) VALUES (?,?,?,?)");
        PreparedStatement stri = c.prepareStatement("INSERT INTO triworddist (word1,word2,word3,section,cnt) VALUES (?,?,?,?,?)");
        System.out.println("Section:"+sec.getId()+":::: "+BL.keySet().size()+"/"+TL.keySet().size());
        for(BiGram b : BL.keySet()){
            sbi.setString(1, b.root1);
            sbi.setString(2, b.root2);
            sbi.setInt(3, sec.getId());
            sbi.setInt(4, BL.get(b));
            sbi.execute();
        }
        for(TriGram t : TL.keySet()){
            stri.setString(1, t.root1);
            stri.setString(2, t.root2);
            stri.setString(3, t.root3);
            stri.setInt(4, sec.getId());
            stri.setInt(5, TL.get(t));
            stri.execute();
        }
        TL.clear();
        BL.clear();
        sbi.close();
        stri.close();
    }
    
}
