/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.jeffreysanti.text_analy4;

import edu.stanford.nlp.dcoref.CorefChain;
import edu.stanford.nlp.dcoref.CorefCoreAnnotations.CorefChainAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.LemmaAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.NamedEntityTagAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentenceIndexAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.ling.Sentence;
import edu.stanford.nlp.ling.TaggedWord;
import edu.stanford.nlp.ling.WordTag;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.process.CoreLabelTokenFactory;
import edu.stanford.nlp.process.DocumentPreprocessor;
import edu.stanford.nlp.process.Morphology;
import edu.stanford.nlp.process.PTBTokenizer;
import edu.stanford.nlp.process.TokenizerFactory;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations.CollapsedCCProcessedDependenciesAnnotation;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeCoreAnnotations.TreeAnnotation;
import edu.stanford.nlp.util.CoreMap;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author jeffrey
 */
public class SectionAnalyzer implements Runnable {

    
    public SectionAnalyzer(Section sec, Connection db)
    {        
        s = sec;
        A = new ArrayList();
        TagDir = new HashMap();
        try {
            c = db;           
            
            ComponentAnalyzer ca;
            
            ca = new CAWord(c, s);
            A.add(ca);
            
            ca = new CASentence(c, s);
            A.add(ca);
            
            ca = new CANGram(c, s);
            A.add(ca);
            
            ca = new CATag(c, s);
            A.add(ca);
            
            ca = new CADepend(c, s);
            A.add(ca);
        } catch (SQLException ex) {
            Logger.getLogger(SectionAnalyzer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    @Override
    public void run() {
        
        String txt = s.getText();
        
        // We need to make sure that all tags are not part of other words or punctuation (add spaces) and remove internal spaces
        if(s.isExtractingTags()){
            Matcher matcher = ptrn_global.matcher(txt);
            StringBuffer sb = new StringBuffer();
            while (matcher.find()) {
                int val = TagDir.size()*3+1;
                TagDir.put(val, "###"+matcher.group()+"###");
                matcher.appendReplacement(sb, " {&"+val+"*} ");
            }
            matcher.appendTail(sb);
            txt = sb.toString();
            
            matcher = ptrn_local.matcher(txt);
            sb = new StringBuffer();
            while (matcher.find()) {
                int val = TagDir.size()*3+1;
                TagDir.put(val, "^^^"+matcher.group()+"^^^");
                matcher.appendReplacement(sb, " {&"+val+"*} ");
            }
            matcher.appendTail(sb);
            txt = sb.toString();
        }
        
        
        // counter variables
        int uniqueWords = 0;
        int totalWords = 0;
        int lexWords = 0;
        int complexWords = 0;
        int sylCount = 0;
        
        Morphology morphology = new Morphology();
        Inquirer inq = new Inquirer();
        
        try {
            // Perform Analysis of Section :D
            
            
            // Run the Analysis
            Annotation document = new Annotation(txt);
            AnalysisCore.getNlpPipeline().annotate(document);
            System.out.println("Annotation Complete For: "+s.getId());
            List<CoreMap> sentences = document.get(SentencesAnnotation.class);
            
            s.setAnnotationDoc(document); // give components access to all info
            
            /*TokenizerFactory<CoreLabel> ptbTokenizerFactory = 
                            PTBTokenizer.factory(new CoreLabelTokenFactory(), "untokenizable=noneDelete");
            DocumentPreprocessor documentPreprocessor = new DocumentPreprocessor(rdr);
            documentPreprocessor.setTokenizerFactory(ptbTokenizerFactory);
            */
            
            
            // For All Sentences
            int sNum = 0;
            for(CoreMap sentence : sentences){
                
                // Split Words
                //ArrayList<TaggedWord> words = AnalysisCore.getTagger().tagSentence(sentence);//sentences.get(sent));
                //if(words.size() <= 0)
                //    continue;
                if(sentence.size() <= 0)
                    continue;
                
                if(sentence.size() == 5 &&
                        sentence.get(TokensAnnotation.class).get(0).get(TextAnnotation.class).equals("-LCB-") && 
                        sentence.get(TokensAnnotation.class).get(1).get(TextAnnotation.class).equals("&") && 
                        sentence.get(TokensAnnotation.class).get(2).get(PartOfSpeechAnnotation.class).equals("CD") && 
                        sentence.get(TokensAnnotation.class).get(3).get(TextAnnotation.class).equals("*") && 
                        sentence.get(TokensAnnotation.class).get(4).get(TextAnnotation.class).equals("-RCB-"))
                {
                    continue; // not really sure how this happened, but ambiguous
                }
                
                for(ComponentAnalyzer ca : A) ca.sentenceStart(sentence.get(SentenceIndexAnnotation.class));
                
                int tagStage = 0;
                
                // For Each Word
                for(int w=0; w<sentence.get(TokensAnnotation.class).size(); w++){
                    CoreLabel token = sentence.get(TokensAnnotation.class).get(w);
                    if(tagStage > 0){
                        tagStage --;
                        continue;
                    }
                    if(sentence.get(TokensAnnotation.class).size() > w+4 &&
                        sentence.get(TokensAnnotation.class).get(w).get(TextAnnotation.class).equals("-LCB-") && 
                        sentence.get(TokensAnnotation.class).get(w+1).get(TextAnnotation.class).equals("&") && 
                        sentence.get(TokensAnnotation.class).get(w+2).get(PartOfSpeechAnnotation.class).equals("CD") && 
                        sentence.get(TokensAnnotation.class).get(w+3).get(TextAnnotation.class).equals("*") && 
                        sentence.get(TokensAnnotation.class).get(w+4).get(TextAnnotation.class).equals("-RCB-"))
                    {
                        tagStage = 4; // skip four more times
                        try{
                            int tagKey = Integer.parseInt(sentence.get(TokensAnnotation.class).get(w+2).get(TextAnnotation.class));
                            for(ComponentAnalyzer ca : A) ca.pushTagString(TagDir.get(tagKey));
                        }catch(Exception e){
                            
                        }
                        continue;
                    }
                    
                    // Get Word
                    String word = token.get(TextAnnotation.class).toLowerCase();
                    String tag = token.get(PartOfSpeechAnnotation.class).toLowerCase();
                    String stem = token.get(LemmaAnnotation.class).toLowerCase();
                    String ne = token.get(NamedEntityTagAnnotation.class).toLowerCase();
                    
                    int stemId = AnalysisCore.getRootId(stem);
                    AnalysisCore.addWordToRoot(word, tag, stemId);
                    
                    for(ComponentAnalyzer ca : A) ca.pushWord(word, tag, stem, stemId);
                    if(!ne.equals("O")){
                        for(ComponentAnalyzer ca : A) ca.pushNamedEntity(word, stemId, ne, w);
                    }else{
                        // Could this be an important entity?
                        if(CAWord.shouldConsiderReference(tag)){
                            for(ComponentAnalyzer ca : A) ca.pushNamedEntity(word, stemId, "N/"+tag, w);
                        }
                    }
                    
                    // now deal with section specific stuff :)
                    if(CAWord.doSkipInCount(tag))
                        continue;
                    
                    totalWords ++;
                    
                    if(CAWord.isLexicalWord(tag))
                        lexWords ++;
                    
                    int curSyl = CAWord.getEnglishSyllableCount(word);
                    sylCount += curSyl;
                    if(curSyl >= 3){
                        complexWords ++;
                    }
                    
                    // now get inquirer section info :)
                    inq.loadWord(word, stem);               
                }
                
                Tree tree = sentence.get(TreeAnnotation.class);
                //tree.pennPrint();
                
                SemanticGraph dependencies = sentence.get(CollapsedCCProcessedDependenciesAnnotation.class);
                //dependencies.prettyPrint();
                
                
                for(ComponentAnalyzer ca : A) ca.sentenceEnd();
                sNum ++;
            }
            for(ComponentAnalyzer ca : A) ca.sectionEnd();
            
            
            // TODO: Get unique word count via wordist table
            Statement st = c.createStatement();
            ResultSet r = st.executeQuery("SELECT COUNT() FROM worddist WHERE section='"+s.getId()+"'");
            if(r.next())
                uniqueWords = r.getInt(1);
            
            // now we will write the information
            String qstr = "INSERT INTO secs (sec,scnt,wcnt,lextoks, unqwords,cwords,syls" +
                                inq.getSqlInsertNames() + ") VALUES (?,?,?,?,?,?,?"+inq.getSqlInsertValues()+")";
            PreparedStatement stmt = c.prepareStatement(qstr);
            stmt.setInt(1, s.getId());
            stmt.setInt(2, sNum);
            stmt.setInt(3, totalWords);
            stmt.setFloat(4, lexWords);
            stmt.setFloat(5, uniqueWords);
            stmt.setFloat(6, complexWords);
            stmt.setFloat(7, sylCount);
            stmt.execute();
            
            c.commit();
            
        } catch (Exception ex) {
            Logger.getLogger(SectionAnalyzer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public int getId(){
        return s.getId();
    }
    
    Section s;
    private Connection c;
    private ArrayList<ComponentAnalyzer> A;
    
    private static final Pattern ptrn_global = Pattern.compile("###(.{1,100})###", Pattern.CASE_INSENSITIVE);
    private static final Pattern ptrn_local = Pattern.compile("\\^\\^\\^(.{1,100})\\^\\^\\^", Pattern.CASE_INSENSITIVE);
    
    private HashMap<Integer, String> TagDir;
}
