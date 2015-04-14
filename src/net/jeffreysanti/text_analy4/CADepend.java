/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.jeffreysanti.text_analy4;

import edu.stanford.nlp.dcoref.CorefChain;
import edu.stanford.nlp.dcoref.CorefChain.CorefMention;
import edu.stanford.nlp.dcoref.CorefCoreAnnotations.CorefChainAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreAnnotations.LemmaAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations.CollapsedCCProcessedDependenciesAnnotation;
import edu.stanford.nlp.semgraph.SemanticGraphEdge;
import edu.stanford.nlp.trees.GrammaticalRelation;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.IntPair;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author jeffrey
 */
public class CADepend implements ComponentAnalyzer {

    static final HashSet<String> lstAcceptedRelations = new HashSet(Arrays.asList(new String []{
        "acomp",
        "advmod",
        "agent",
        "appos",
        "amod",
        "conj",
        "dep",
        "discourse",
        "dobj",
        "infmod",
        "iobj",
        "nn",
        "npadvmod",
        "nsubj",
        "nsubjpass",
        "prep",
        "prepc",
        "prt",
        "tmod",
        "vmod",
        "xsubj",
        "xcomp"
    }));
    
    static final HashSet<String> lstStopDep = new HashSet(Arrays.asList(new String []{
        "at",
        "be",
        "can",
        "do",
        "go",
        "have",
        "above",
        "after",
        "again",
        "all",
        "also",
        "always",
        "any",
        "anyone",
        "anymore",
        "anything",
        "as",
        "at",
        "be",
        "can",
        "cause",
        "come",
        "do",
        "everything",
        "else",
        "even",
        "for",
        "go",
        "have",
        "he",
        "him",
        "she",
        "her",
        "how",
        "however",
        "just",
        "like",
        "look",
        "lot",
        "make",
        "most",
        "much",
        "now",
        "of",
        "only",
        "so",
        "something",
        "somewhat",
        "somewhere",
        "that",
        "this",
        "time",
        "their",
        "there",
        "to",
        "too",
        "very",
        "we",
        "what",
        "when",
        "where",
        "who",
        "while"
    }));
    
    
    public CADepend(Connection db, Section s) throws SQLException
    {
        c = db;
        sec = s;
        
        
        Statement stmt = c.createStatement();
        stmt.executeUpdate("CREATE TABLE IF NOT EXISTS entrel (eid1 INTEGER, eid2 INTEGER, sec INTEGER, " + 
                " sent INTEGER, reltype TEXT)");
        stmt.close();
    }
    
    private Connection c;
    private Section sec;
    
            
    @Override
    public void sentenceStart(int id) throws Exception {
    }

    @Override
    public void pushWord(String word, String tag, String stem, int stemId) throws Exception {
    }

    @Override
    public void pushNamedEntity(String word, int stemId, String ne, int wordIndex) throws Exception {

    }

    @Override
    public void sentenceEnd() throws Exception {
    }

    @Override
    public void sectionEnd() throws Exception {
        
        HashSet<EntitySystem.EntityRefLoc> L = new HashSet();
        
        // Search all coreferences
        Map<Integer, CorefChain> graph = sec.getDoc().get(CorefChainAnnotation.class);
        for (Map.Entry entry : graph.entrySet()) {
            CorefChain chain = (CorefChain) entry.getValue();

            for(CorefMention m : chain.getMentionsInTextualOrder()){
                //System.out.println("  ->  "+m.toString()+" ; MT:"+m.mentionType+" ; ANIM: "+m.animacy);
                AnalysisCore.getEntSys().extractCorefChain(chain, sec, L);
            }
        }
        //AnalysisCore.getEntSys().dumpData();
        
        PreparedStatement stmt = c.prepareStatement("INSERT INTO entrel (eid1, eid2, sec, sent, reltype) "+
                            " VALUES (?,?,?,?,?)");
        
        List<CoreMap> sentences = sec.getDoc().get(SentencesAnnotation.class);
        for(CoreMap sentence : sentences){
            SemanticGraph dependencies = sentence.get(CollapsedCCProcessedDependenciesAnnotation.class);
            for(SemanticGraphEdge edge : dependencies.getEdgeSet()){
                int word1;
                int word2;
                int sent = sentence.get(CoreAnnotations.SentenceIndexAnnotation.class);
                String t = edge.getRelation().getShortName().toLowerCase();
                
                if(!lstAcceptedRelations.contains(t))
                    continue;
                
                word1 = edge.getDependent().index()-1;
                word2 = edge.getGovernor().index()-1;
                String lemma1 = sentence.get(TokensAnnotation.class).get(word1).get(LemmaAnnotation.class);
                String lemma2 = sentence.get(TokensAnnotation.class).get(word2).get(LemmaAnnotation.class);
                
                EntitySystem.EntityRefLoc loc1 = AnalysisCore.getEntSys().findEntityLoc(sec, sent, word1, lemma1, L);
                EntitySystem.EntityRefLoc loc2 = AnalysisCore.getEntSys().findEntityLoc(sec, sent, word2, lemma2, L);
                
                if(loc1.e.primaryName.equals(loc2.e.primaryName))
                    continue;
                
                // If both words are basic, we should skip them
                if(lstStopDep.contains(loc1.e.primaryName.toLowerCase()) && lstStopDep.contains(loc2.e.primaryName.toLowerCase()))
                    continue;
                
                //System.out.println("REL: "+t+":: "+loc1.e.primaryName+" / "+loc2.e.primaryName);
                
                // now write to db
                stmt.setInt(1, Math.min(loc1.e.eid, loc2.e.eid));
                stmt.setInt(2, Math.max(loc1.e.eid, loc2.e.eid));
                stmt.setInt(3, loc1.sec);
                stmt.setInt(4, loc1.sent);
                stmt.setString(5, t);
                stmt.execute();
            }
        }
        
        
        
        
        
        
        /*for(Entity e : E.values())
        {
            System.out.println("COREF: "+e.name);
            boolean added = true; // iteratively build coreferences until no more added
            while(added){
                added = false;
                for(CorefChain chain : graph.values())
                {
                    boolean flagged = false;
                    for(WordPos w : e.pos){
                        if(chain.getMentionMap().containsKey(new IntPair(w.sentInt, w.word))){
                            flagged = true;
                            break;
                        }
                    }
                    if(flagged){ // This whole chain is a coreference :)
                        Map<IntPair, Set<CorefMention>> M = chain.getMentionMap();
                        WordPos w;
                        for(IntPair p : M.keySet()){
                            System.out.println("   C: "+p.elems()[0]+","+p.elems()[1] + "  /  "+ M.get(p).size());
                            for(CorefMention ment : M.get(p)){
                                System.out.println("      : "+ment.position.elems()[0]+","+ment.position.elems()[1]);
                            }
                            w = new WordPos(p.elems()[0], p.elems()[1]);
                            if(!e.pos.contains(w)){
                                e.pos.add(w);
                                added = true;
                            }
                        }
                    }
                }
            }
        }
        
        // Now find dependencies for the words
        HashSet<Relation> R = new HashSet();
        for(Entity e : E.values())
        {
            System.out.println("GRAPHOF: "+e.name);
            for(WordPos pos : e.pos){
                System.out.println("  -> : "+pos.sentInt+"."+pos.word);
                CoreMap sentence = sec.getDoc().get(SentencesAnnotation.class).get(pos.sentInt-1);
                SemanticGraph dependencies = sentence.get(CollapsedCCProcessedDependenciesAnnotation.class);
                for(SemanticGraphEdge edge : dependencies.getEdgeSet()){
                    String t = edge.getRelation().getShortName();
                    if(t.equals("agent") || t.equals("appos") || t.equals("dobj") || t.equals("nsubj") || t.equals("nsubjpass")){
                        // noun last dependency
                        if(edge.getDependent().index() != pos.word)
                            continue;
                        
                        System.out.println("    | > "+ 
                                sentence.get(TokensAnnotation.class).get(edge.getDependent().index()-1).get(CoreAnnotations.TextAnnotation.class) + " -> "+
                                sentence.get(TokensAnnotation.class).get(edge.getGovernor().index()-1).get(CoreAnnotations.TextAnnotation.class));
                        
                        int w = AnalysisCore.getRootId(sentence.get(TokensAnnotation.class).get(edge.getDependent().index()-1).get(LemmaAnnotation.class));
                        int wRel = AnalysisCore.getRootId(sentence.get(TokensAnnotation.class).get(edge.getGovernor().index()-1).get(LemmaAnnotation.class));
                        boolean done = false;
                        for(Relation r : R){
                            if(r.equals(new Relation(w, wRel, t))){
                                r.instCount++;
                                r.instSentHash.add(internalToSentId.get(pos.sentInt-1));
                                done = true;
                                break;
                            }
                        }
                        if(!done){
                            Relation r = new Relation(w, wRel, t);
                            r.instCount = 1;
                            r.instSentHash.add(internalToSentId.get(pos.sentInt-1));
                            R.add(r);
                        }
                    }else if(t.equals("amod")){
                        // noun first (govenor)
                        if(edge.getGovernor().index() != pos.word)
                            continue;
                        
                        System.out.println("    | > "+ 
                                sentence.get(TokensAnnotation.class).get(edge.getDependent().index()-1).get(CoreAnnotations.TextAnnotation.class) + " -> "+
                                sentence.get(TokensAnnotation.class).get(edge.getGovernor().index()-1).get(CoreAnnotations.TextAnnotation.class));
                        
                        int wRel = AnalysisCore.getRootId(sentence.get(TokensAnnotation.class).get(edge.getDependent().index()-1).get(LemmaAnnotation.class));
                        int w = AnalysisCore.getRootId(sentence.get(TokensAnnotation.class).get(edge.getGovernor().index()-1).get(LemmaAnnotation.class));
                        boolean done = false;
                        for(Relation r : R){
                            if(r.equals(new Relation(w, wRel, t))){
                                r.instCount++;
                                r.instSentHash.add(internalToSentId.get(pos.sentInt-1));
                                done = true;
                                break;
                            }
                        }
                        if(!done){
                            Relation r = new Relation(w, wRel, t);
                            r.instCount = 1;
                            r.instSentHash.add(internalToSentId.get(pos.sentInt-1));
                            R.add(r);
                        }
                    }
                }
            }
        }
        
        // fianally Write to DB
        PreparedStatement stmt;
        stmt = c.prepareStatement("INSERT INTO deps (word ,rel, type, section, cnt, shash) VALUES (?,?,?,?,?,?)");
        for(Relation r : R){
            stmt.setInt(1, r.word);
            stmt.setInt(2, r.relWord);
            stmt.setString(3, r.relType);
            stmt.setInt(4, sec.getId());
            stmt.setInt(5, r.instCount);
            
            String hash = "";
            for(int snum : r.instSentHash){
                hash += "|"+snum+";";
            }
            stmt.setString(6, hash);
            stmt.execute();
        }
        E.clear();
        internalToSentId.clear();
        stmt.close();
        */
    }

    @Override
    public void pushTagString(String tag) {
    }
    
}
