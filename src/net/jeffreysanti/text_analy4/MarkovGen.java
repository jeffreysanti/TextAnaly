/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package net.jeffreysanti.text_analy4;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jblas.util.Random;
import org.languagetool.JLanguageTool;
import org.languagetool.language.English;
import org.languagetool.rules.RuleMatch;
import org.languagetool.tools.Tools;

/**
 *
 * @author jeffrey
 */
public class MarkovGen {
    
    public MarkovGen(Connection d){
        db = d;
        bigramProb = 0;
        Random.seed(System.currentTimeMillis());
        enabledSubstituer = false;
        langTool = null;
    }
    
    public MarkovGen(Connection d, double bprob){
        db = d;
        bigramProb = bprob;
        Random.seed(System.currentTimeMillis());
        enabledSubstituer = false;
        langTool = null;
    }
    
    public void enableWordSubstituer(double nouns, double adjs)
    {
        nounSubProb = nouns;
        adjSubProb = adjs;
        enabledSubstituer = true;
    }
    public void disableWordSubstituer()
    {
        enabledSubstituer = false;
    }
    public void enableGrammarVerifier(boolean attempCorrect){
        langTool = new JLanguageTool(new English());
        try {
            langTool.activateDefaultPatternRules();
        } catch (IOException ex) {
            Logger.getLogger(MarkovGen.class.getName()).log(Level.SEVERE, null, ex);
        }
        grammarAttemptCorrect = attempCorrect;
    }
    public void disableGrammarVerifier(){
        langTool = null;
    }
    
    public String handleSubs(String orig){
        if(!enabledSubstituer)
            return orig;
        try {
            PreparedStatement stmt = db.prepareStatement("SELECT pos FROM altwords WHERE word=?");
            stmt.setString(1, orig);
            ResultSet r = stmt.executeQuery();
            if(!r.next())
                return orig;
            
            boolean sub = false;
            String pos = r.getString(1);
            if(pos.equals("jj;"))
                sub = doSubAdj();
            if(pos.equals("nn;") || pos.equals("nns;"))
                sub = doSubNoun();
            
            r.close();
            stmt.close();
            
            if(!sub)
                return orig;
            
            // substitue word
            stmt = db.prepareStatement("SELECT word FROM altwords WHERE pos=? ORDER BY RANDOM() LIMIT 1");
            stmt.setString(1, pos);
            r = stmt.executeQuery();
            if(r.next()){
                String newWord = r.getString(1);
                //System.out.println("   "+orig+" -> "+newWord);
                orig = newWord;                
            }
            r.close();
            stmt.close();
            
        } catch (SQLException ex) {
            Logger.getLogger(MarkovGen.class.getName()).log(Level.SEVERE, null, ex);
        }
        return orig;
    }
    
    public ArrayList<String> fixContractions(ArrayList<String> chain)
    {
        ArrayList<String> ret = new ArrayList();
        int i = 0;
        while(i < chain.size()){
            if(chain.get(i).equals("is") && chain.size() > i+1 && chain.get(i+1).equals("n't")){
                ret.add("isn't");
                i += 2;
                continue;
            }
            if(chain.get(i).equals("do") && chain.size() > i+1 && chain.get(i+1).equals("n't")){
                ret.add("don't");
                i += 2;
                continue;
            }
            
            ret.add(chain.get(i));
            i++;
        }
        return ret;
    }
    
    public String arrayToString(ArrayList<String> arr){
        String s = "";
        arr = fixContractions(arr);
        
        int wordno = 0;
        for(String w : arr){
            if(w.isEmpty())
                continue;
            String word = handleSubs(w);
            if(word.equals("i"))
                word = "I";
            if(wordno == 0){
                word = word.toUpperCase().substring(0, 1) + word.substring(1);
            }
            if(wordno != 0 && word.charAt(0) != '\'' && !word.equals(".") && !word.equals("?") && !word.equals(";"))
                s += " ";

            s += word;
            wordno ++;
        }
        if(s.charAt(s.length()-1) != '.' && s.charAt(s.length()-1) != '?' && s.charAt(s.length()-1) != '!' &&
                s.charAt(s.length()-1) != ';' && s.charAt(s.length()-1) != ':'){
            s = s + ".";
        }
        return s;
    }
    
    private boolean doBiGram(){
        double dVal = Random.nextDouble();
        return bigramProb > dVal;
    }
    private boolean doSubNoun(){
        double dVal = Random.nextDouble();
        return nounSubProb > dVal;
    }
    private boolean doSubAdj(){
        double dVal = Random.nextDouble();
        return adjSubProb > dVal;
    }
    
    private boolean doesEnd(ArrayList<String> ret){
        for(String stop : CANGram.stopTags){
            if(ret.contains(stop)){
                return true;
            }
        }
        for(String stop : CANGram.stopTagsPostInclude){
            if(ret.contains(stop)){
                return true;
            }
        }
        return false;
    }
    
    public ArrayList<String> getSentenceStart(String init, int cnt){
        ArrayList<String> ret = new ArrayList();
        if(cnt > 20){
            return ret;
        }
        
        try {
            PreparedStatement stmt;
            boolean bigram = doBiGram();
            if(bigram){
                stmt = db.prepareStatement("SELECT word2 FROM biworddist WHERE word1=? ORDER BY RANDOM() LIMIT 1");
                if(init.isEmpty()){
                    stmt.setString(1, CANGram.VAL_SENT_START);
                }else{
                    stmt.setString(1, init);
                    ret.add(init);
                }
            }else{ // trigram
                stmt = db.prepareStatement("SELECT word2, word3 FROM triworddist WHERE word1=? ORDER BY RANDOM() LIMIT 1");
                if(init.isEmpty()){
                    stmt.setString(1, CANGram.VAL_SENT_START);
                }else{
                    stmt.setString(1, init);
                    ret.add(init);
                }
            }
            ResultSet r = stmt.executeQuery();
            
            if(!r.next()){
                r.close();
                stmt.close();
                
                ret.clear();
                return ret;
            }
            ret.add(r.getString(1));
            if(!bigram)
                ret.add(r.getString(2)); // third word as well
            
            r.close();
            stmt.close();
            
        } catch (SQLException ex) {
            Logger.getLogger(MarkovGen.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        // don't want 1 word sentences.
        if(doesEnd(ret)){
            ret = getSentenceStart(init, cnt+1);
        }
        return ret;
    }
    
    public void getNextWord_inner(ArrayList<String> chain, int cnt){
        boolean bigram = doBiGram();
        if(chain.size() < 2)
            bigram = true;
        
        String w1, w2;
        w2 = "";
        if(bigram)
            w1 = chain.get(chain.size()-1);
        else{
            w1 = chain.get(chain.size()-2);
            w2 = chain.get(chain.size()-1);
        }
        
        if(CANGram.stopTags.contains(w2) || CANGram.stopTagsPostInclude.contains(w2)){ // need to start new sentence (or return)
            return;
        }
        
        if(cnt > 60)
            return;
        
        try {
            PreparedStatement stmt;
            ResultSet r;
            if(bigram){
                stmt = db.prepareStatement("SELECT word2 FROM biworddist WHERE word1=? "+
                       "GROUP BY word1, word2 ORDER BY random()*SUM(cnt) LIMIT 1");
                stmt.setString(1, w1);
                r = stmt.executeQuery();
            }else{
                stmt = db.prepareStatement("SELECT word3 FROM triworddist WHERE word1=? AND word2=? "+
                       "GROUP BY word1, word2, word3 ORDER BY random()*SUM(cnt) LIMIT 1");
                stmt.setString(1, w1);
                stmt.setString(2, w2);
                r = stmt.executeQuery();
            }
            
            if(!r.next()){
                return;
            }
            
            String w3 = r.getString(1);
            
            chain.add(w3);
            getNextWord_inner(chain, cnt+1); // recurse
        } catch (SQLException ex) {
            Logger.getLogger(MarkovGen.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public String correctGrammar(String sent){
        try {
            List<RuleMatch> matches = langTool.check(sent);
            if(!matches.isEmpty()){
                // attempt a fix
                if(!grammarAttemptCorrect){
                    return "";
                }
                sent = Tools.correctTextFromMatches(sent, matches);
                
                // now recheck
                matches = langTool.check(sent);
                if(!matches.isEmpty()){
                    return "";
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(MarkovGen.class.getName()).log(Level.SEVERE, null, ex);
        }
        return sent;
    }
    
    public String finishSentence(ArrayList<String> chain){
        if(langTool == null){
            getNextWord_inner(chain, 0);
            return arrayToString(chain);
        }
        ArrayList<String> chain_cpy = (ArrayList<String>) chain.clone();
        for(int i=0; i<50; i++){
            chain.clear();
            chain.addAll(chain_cpy);
            getNextWord_inner(chain, 0);
            String sent_txt = correctGrammar(arrayToString(chain));
            if(!sent_txt.isEmpty())
                return sent_txt;
        }
        return "";
    }
    
    public void genText(){
        ArrayList<String> chain = getSentenceStart("", 0);
       
        System.out.println(finishSentence(chain));
    }
    
    public void genTextStarting(String first){
        first = first.toLowerCase();
        ArrayList<String> chain = getSentenceStart(first, 0);
        
        System.out.println(finishSentence(chain));
    }
    
    public void genTextStarting(String first, String second){
        first = first.toLowerCase();
        second = second.toLowerCase();
        
        ArrayList<String> chain = new ArrayList();
        chain.add(first);
        chain.add(second);
        
        System.out.println(finishSentence(chain));
    }
    
    public void genContaining(String word){
        genContaining(word, 500);
    }
    
    public void genContaining(String word, int maxAttempts){
        ArrayList<String> words = new ArrayList();
        try {
            PreparedStatement stmt = db.prepareStatement("SELECT word FROM altwords WHERE root=(SELECT root FROM altwords " + 
                    " WHERE word=?)");
            stmt.setString(1, word);
            ResultSet r = stmt.executeQuery();
            while(r.next()){
                words.add(r.getString(1));
            }
            r.close();
            stmt.close();
        } catch (SQLException ex) {
            Logger.getLogger(MarkovGen.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        if(words.isEmpty())
            return;
        
        for(int i=0; i<maxAttempts; i++){
            ArrayList<String> chain = getSentenceStart("", 0);
            String sent_txt = finishSentence(chain);
            for(String s : words){
                if(chain.contains(s)){ // we have a match
                    System.out.println(sent_txt);
                    return;
                }
            }
        }
    }
    
     
    private Connection db;
    
    double bigramProb;
    double nounSubProb;
    double adjSubProb;
    boolean enabledSubstituer;
    JLanguageTool langTool;
    boolean grammarAttemptCorrect;
    
}
