/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.jeffreysanti.text_analy4;

import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author jeffrey
 * 
 * Keeps Track of all resources in the program
 * 
 */
public class AnalysisCore {
    
    public static void initialize(String dic_pos, String dic_parse, String dic_ner, String dic_inq, String dbFl){
        
        try{
            Class.forName("org.sqlite.JDBC");
            db = DriverManager.getConnection("jdbc:sqlite:"+dbFl);
            
            //tagger = new MaxentTagger(dic);
            
            Properties props = new Properties();
            props.put("annotators", "tokenize, ssplit, pos, lemma, ner, parse, dcoref");
            props.put("pos.model", dic_pos);
            props.put("parse.model", dic_parse);
            props.put("ner.model", dic_ner);
            pipeline = new StanfordCoreNLP(props);
            
            Inquirer.initialize(dic_inq);
            //Inquirer.createInqDb();
            
            
            Statement stmt = db.createStatement();
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS secs (sec INTEGER NOT NULL PRIMARY KEY, scnt INTEGER, wcnt INTEGER,"
                    + "lextoks INTEGER, unqwords INTEGER, cwords INTEGER, syls INTEGER"
                    + Inquirer.getSqlCreateAddition()+" )");
            stmt.close();
            
            stmt = db.createStatement();
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS roots (rid INTEGER NOT NULL PRIMARY KEY, root STRING NOT NULL)");
            stmt.close();
            stmt = db.createStatement();
            stmt.executeUpdate("CREATE UNIQUE INDEX IF NOT EXISTS rootid ON roots (rid)");
            stmt.close();

            // word equivilance table
            stmt = db.createStatement();
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS altwords (word TEXT NOT NULL PRIMARY KEY, root INTEGER NOT NULL, pos TEXT)");
            stmt.close();
            stmt = db.createStatement();
            stmt.executeUpdate("CREATE UNIQUE INDEX IF NOT EXISTS wordid ON altwords (word)");
            stmt.close();
            
            readRootInfoFromDB();
            
            ESys = new EntitySystem(db);
            
            
        } catch (Exception ex) {
            Logger.getLogger(AnalysisCore.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public static void endAnalysis(){
        try {
            db.setAutoCommit(false);
            AnalysisCore.writeRootWordInfoToDB();
            db.setAutoCommit(true);
        } catch (SQLException ex) {
            Logger.getLogger(Analyzer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public static Connection getDB(){
        return db;
    }
    
    // For viewing data in R
    public static void initalizeNoAnalysisRun(String dic_inq){
        try {
            Class.forName("org.sqlite.JDBC");
            Inquirer.initialize(dic_inq);
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(AnalysisCore.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public static int getNextSectionID(){
        int ret = 1;
        try {
            Statement stmt = db.createStatement();
            ResultSet r = stmt.executeQuery("SELECT sec FROM secs ORDER BY sec DESC LIMIT 1");
            if(r.next()){
                ret = r.getInt("sec") + 1;
            }
            stmt.close();
        } catch (SQLException ex) {
            Logger.getLogger(AnalysisCore.class.getName()).log(Level.SEVERE, null, ex);
        }
        return ret;
    }
    
    public static StanfordCoreNLP getNlpPipeline(){
        return pipeline;
    }
    
    public static EntitySystem getEntSys(){
        return ESys;
    }
    
    synchronized static int getRootId(String root)
    {
        if(rootToRid.containsKey(root))
            return rootToRid.get(root);
        int tryID = rootToRid.size()+1;
        while(ridToRoot.containsKey(tryID)){
            tryID ++;
        }
        rootToRid.put(root, tryID);
        ridToRoot.put(tryID, root);
        return tryID;
    }
    
    synchronized static void addWordToRoot(String word, String pos, int rid)
    {
        if(wordToRid.containsKey(word)){
            RootPOSConnection con = wordToRid.get(word);
            con.pos.add(pos);
        }else{
            RootPOSConnection con = new RootPOSConnection();
            con.rid = rid;
            con.pos.add(pos);
            wordToRid.put(word, con);
        }
    }
    
    private static void readRootInfoFromDB() throws SQLException{
        Statement stmt = db.createStatement();
        ResultSet r = stmt.executeQuery("SELECT * FROM roots");
        while(r.next()){
            rootToRid.put(r.getString("root"), r.getInt("rid"));
            ridToRoot.put(r.getInt("rid"), r.getString("root"));
        }
        stmt.close();
        
        stmt = db.createStatement();
        r = stmt.executeQuery("SELECT * FROM altwords");
        while(r.next()){
            RootPOSConnection rpc = new RootPOSConnection();
            rpc.rid = r.getInt("root");
            String [] pos = r.getString("pos").split(";");
            for(String p : pos)
                rpc.pos.add(p.replaceAll(";", ""));
            wordToRid.put(r.getString("word"), rpc);
        }
        stmt.close();
    }
    
    public static void writeRootWordInfoToDB() throws SQLException
    {
        Statement stmt = db.createStatement();
        stmt.executeUpdate("DELETE FROM roots");
        stmt.executeUpdate("DELETE FROM altwords");
        stmt.close();
        PreparedStatement pst;
        for(String root : rootToRid.keySet()){
            pst = db.prepareStatement("INSERT INTO roots (rid, root) VALUES (?,?)");
            pst.setInt(1, rootToRid.get(root));
            pst.setString(2, root);
            pst.execute();
            pst.close();
        }
        for(String word : wordToRid.keySet()){
            RootPOSConnection rconn = wordToRid.get(word);
            String pos = "";
            for(String p:rconn.pos){
                pos += p + ";";
            }
            pst = db.prepareStatement("INSERT INTO altwords (word,root,pos) VALUES(?,?,?)");
            pst.setString(1, word);
            pst.setInt(2, rconn.rid);
            pst.setString(3, pos);
            pst.execute();
            pst.close();
        }
    }

    
    private static class RootPOSConnection{
        public int rid;
        public HashSet<String> pos = new HashSet();
    }
    
    
    private static ConcurrentHashMap<String, Integer> rootToRid = new ConcurrentHashMap();
    private static ConcurrentHashMap<Integer, String> ridToRoot = new ConcurrentHashMap();
    private static ConcurrentHashMap<String, RootPOSConnection> wordToRid = new ConcurrentHashMap();
    private static EntitySystem ESys = null;
    private static StanfordCoreNLP pipeline;
    private static Connection db;
    
}
