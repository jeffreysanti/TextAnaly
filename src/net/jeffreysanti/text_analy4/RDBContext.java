/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.jeffreysanti.text_analy4;

import java.io.File;
import java.sql.Array;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author jeffrey
 */
public class RDBContext {
    
    public RDBContext(String fl)
    {
        try {
            Class.forName("org.sqlite.JDBC");
            
            File f = new File(fl);
            if(!f.exists()){
                System.out.println("File Specified Does Not Exist!");
                return;
            }
            
            db = DriverManager.getConnection("jdbc:sqlite:"+fl);
            
            // Verify Database correct format
            PreparedStatement stmt = db.prepareStatement("SELECT COUNT() FROM secs");
            ResultSet r = stmt.executeQuery();
            if(!r.next())
                throw new SQLException();
            
            if(r.getInt(1) < 1)
                throw new SQLException();
            
        } catch (SQLException ex) {
            System.out.println("SQLite Error Occured: Is DB Valid?");
            db = null;
            return;
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(RDBContext.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public Connection getDB(){
        return db;
    }
    
    public boolean isOkay()
    {
        return (db != null);
    }
    
    public int rawQuery(String in)
    {
        try{
            Statement stmt = db.createStatement();
            ResultSet r = stmt.executeQuery(in);
            int i = processResults(r);
            stmt.close();
            return i;
        } catch (SQLException ex) {
            System.out.println("Invalid Query: Entered: See Below");
            System.out.println(ex.toString());
            return 0;
        }
    }
    
    private int processResults(ResultSet r) throws SQLException
    {
        dtaSet.clear();
        
        ArrayList<String> masterEnt = new ArrayList();
        for(int i=1; i<=r.getMetaData().getColumnCount(); i++){
            masterEnt.add(r.getMetaData().getColumnName(i));
        }
        dataSetColNames = masterEnt.toArray(new String[masterEnt.size()]);
        
        while(r.next()){
            ArrayList<Object> entry = new ArrayList();
            int colCount = r.getMetaData().getColumnCount();
            for(int i=1; i<=colCount; i++){
                if(i-1 >= dataSetColNames.length)
                    continue;
                if(r.getMetaData().getColumnTypeName(i).equalsIgnoreCase("text"))
                    entry.add(r.getString(i));
                else if(r.getMetaData().getColumnTypeName(i).equalsIgnoreCase("integer"))
                    entry.add(r.getInt(i));
                else if(r.getMetaData().getColumnTypeName(i).equalsIgnoreCase("float"))
                    entry.add(r.getFloat(i));
                else if(r.getMetaData().getColumnTypeName(i).equalsIgnoreCase("null"))
                    entry.add("");
                else{
                    System.out.println("WARNING: Unkown Type of SQL Column:"+r.getMetaData().getColumnTypeName(i));
                    entry.add(r.getString(i));
                }
            }
            if(colCount < dataSetColNames.length){ // Assure equal length of rows
                for(int i=colCount; i< dataSetColNames.length; i++){
                    entry.add("");
                }
            }
            dtaSet.add(entry.toArray(new Object[entry.size()]));
        }
        return dtaSet.size();
    }
    
    public String[] getTitleRow()
    {
        Float i = 4.5f;
        
        return dataSetColNames;
        
    }
    public Object[] getRow(int rowNum)
    {
        return dtaSet.get(rowNum);
    }
    
    
    private Connection db=null;
    private ArrayList<Object[]> dtaSet = new ArrayList();
    private String[] dataSetColNames = new String[0];
    
    
    // NOW SOME GOOD STUFF
    
    public int sentiment(boolean incSent, int[] secs)
    {
        String[] attr = Inquirer.getAttrNames();
        
        String q = "SELECT ";
        if(incSent){
            q += "sec, sent";
            for(String a : attr){
                q += ", "+a+"/wcnt";
            }
            q += " FROM slst ";
            if(secs.length > 0){
                q += " WHERE sec IN (";
                for(int i=0; i<secs.length; i++){
                    if(i>0)
                        q += ",";
                    q += secs[i];
                }
                q += ") ";
            }
            q += "ORDER BY sec ASC, sent ASC ";
        }else{
             q += "sec";
            for(String a : attr){
                q += ", "+a+"/wcnt";
            }
            q += " FROM secs ";
            if(secs.length > 0){
                q += " WHERE sec IN (";
                for(int i=0; i<secs.length; i++){
                    if(i>0)
                        q += ",";
                    q += secs[i];
                }
                q += ") ";
            }
            q += "ORDER BY sec ASC ";
        }
        return rawQuery(q);
    }
    
    public int sentenceSentiment(String[] pairs)
    {
        String[] attr = Inquirer.getAttrNames();
        
        String q = "SELECT ";
        q += "sec, sent";
        for(String a : attr){
            q += ", "+a+"/wcnt";
        }
        q += " FROM slst ";
        q += " WHERE ";
        int i=0;
        for(String s : pairs){
            if(!s.contains(":"))
                continue;
            try{
                int sec = Integer.parseInt(s.substring(0, s.indexOf(":")));
                int sent = Integer.parseInt(s.substring(s.indexOf(":")+1));
                if(i > 0)
                    q += " OR ";
                q += "(sec="+sec+" AND sent="+sent+")";
                i ++;
            }catch(NumberFormatException e){
                continue;
            }
        }
        if(i == 0){
            return 0;
        }
        
        q += "ORDER BY sec ASC, sent ASC ";
        return rawQuery(q);
    }
    
    protected HashSet<Integer> getRoots(String [] words) throws SQLException{
        PreparedStatement stmt = db.prepareStatement("SELECT root FROM altwords WHERE word=?");
        HashSet<Integer> lst = new HashSet();
        for(String w:words){
            stmt.setString(1, w.toLowerCase());
            ResultSet r = stmt.executeQuery();
            if(r.next()){
                lst.add(r.getInt(1));
            }
        }
        return lst;
    }
    
    public int alternateWords(String [] words) throws SQLException
    {
        HashSet<Integer> roots = getRoots(words);
        String qsMarks = "";
        for(int i=0; i<roots.size(); i++){
            if(i>0)
                qsMarks += ",";
            qsMarks += "?";
        }
        
        PreparedStatement stmt = db.prepareStatement("SELECT word FROM altwords WHERE "
                + " root IN ("+qsMarks+") GROUP BY word ORDER BY word ");
        int x = 1;
        for(int i : roots){
            stmt.setInt(x, i);
            x ++;
        }
        processResults(stmt.executeQuery());
        stmt.close();
        return dtaSet.size();
    }
    
    public int wordLocation(String [] words, int [] secs, boolean containsAll) throws SQLException
    {
        HashSet<Integer> roots = getRoots(words);
        String qsMarks = "";
        for(int i=0; i<roots.size(); i++){
            if(i>0){
                if(containsAll)
                    qsMarks += "AND";
                else
                    qsMarks += "OR";
            }
            qsMarks += " rootlist LIKE ? ";
        }
        if(qsMarks.length() == 0)
            return 0;
        
        String secClause = "";
        if(secs.length > 0){
            secClause = " AND section IN (";
            for(int i=0; i<secs.length; i++){
                if(i>0)
                    secClause += ",";
                secClause += Integer.toString(secs[i]);
            }
            secClause += ") ";
        }
        
        PreparedStatement stmt = db.prepareStatement("SELECT sec, sent FROM slst WHERE (" + qsMarks + ")" + secClause
                + "ORDER BY sec ASC, sent ASC ");
        
        int x = 1;
        for(int i : roots){
            stmt.setString(x, "%|"+i+";%");
            x ++;
        }
        processResults(stmt.executeQuery());
        stmt.close();
        return dtaSet.size();
    }
    
    public int wordDist(String [] words, boolean agregate) throws SQLException
    {
        HashSet<Integer> roots = getRoots(words);
        String qsMarks = "";
        for(int i=0; i<roots.size(); i++){
            if(i>0)
                qsMarks += ",";
            qsMarks += "?";
        }
        
        PreparedStatement stmt = db.prepareStatement("SELECT section, SUM(cnt) AS cnt FROM worddist WHERE "
                + " word IN ("+qsMarks+") GROUP BY section ORDER BY section ");
        int x = 1;
        for(int i : roots){
            stmt.setInt(x, i);
            x ++;
        }
        processResults(stmt.executeQuery());
        stmt.close();
        
        // Normalize Results so that all sections included :)
        stmt = db.prepareStatement("SELECT sec FROM secs ORDER BY sec");
        ResultSet r = stmt.executeQuery();
        int resId = 0;
        while(r.next()){
            int secNo = r.getInt(1);
            if(resId >= dtaSet.size()){
                Integer[] set = {secNo,0};
                dtaSet.add(set);
            }else{
                if((Integer)(dtaSet.get(resId)[0]) != secNo){
                    Integer[] set = {secNo,0};
                    dtaSet.add(resId, set);
                }
            }
            resId ++;
        }
        
        if(agregate)
        {
            int runningTotal = 0;
            for(int i=0; i<dtaSet.size(); i++){
                runningTotal += (Integer)(dtaSet.get(i)[1]);
                dtaSet.get(i)[1] = new Integer(runningTotal);
            }
        }
        return dtaSet.size();
    }
    
    public int wordsByCount_StopList(String [] stopList, int[] sections) throws SQLException{
        PreparedStatement stmt;
        HashSet<Integer> roots = getRoots(stopList);
        String qsMarks = "";
        for(int i=0; i<roots.size(); i++){
            if(i>0)
                qsMarks += ",";
            qsMarks += "?";
        }
        
        int x = 1;
        if(sections.length == 0){
            stmt = db.prepareStatement("SELECT roots.root, SUM(worddist.cnt) FROM worddist JOIN roots ON "
                + " worddist.word=roots.rid WHERE worddist.word NOT IN ("+qsMarks
                + ") GROUP BY worddist.word ORDER BY SUM(worddist.cnt) DESC");
        }else{
            String qsMarks2 = "";
            for(int i=0; i<sections.length; i++){
                if(i>0)
                    qsMarks2 += ",";
                qsMarks2 += "?";
            }
            stmt = db.prepareStatement("SELECT roots.root, SUM(worddist.cnt) FROM worddist JOIN roots ON "
                + " worddist.word=roots.rid WHERE worddist.section IN ("+qsMarks2+") AND worddist.word NOT IN ("+qsMarks
                + ")  GROUP BY worddist.word ORDER BY SUM(worddist.cnt) DESC");
            for(int i : sections){
                stmt.setInt(x, i);
                x ++;
            }
        }
        for(int i : roots){
            stmt.setInt(x, i);
            x ++;
        }
        processResults(stmt.executeQuery());
        stmt.close();
        return dtaSet.size();
    }
    public int wordsByCount_ByList(String [] stopList, int[] sections) throws SQLException{
        PreparedStatement stmt;
        HashSet<Integer> roots = getRoots(stopList);
        String qsMarks = "";
        for(int i=0; i<roots.size(); i++){
            if(i>0)
                qsMarks += ",";
            qsMarks += "?";
        }
        
        int x = 1;
        if(sections.length == 0){
            stmt = db.prepareStatement("SELECT roots.root, SUM(worddist.cnt) FROM worddist JOIN roots ON "
                + " worddist.word=roots.rid WHERE worddist.word IN ("+qsMarks
                + ") GROUP BY worddist.word ORDER BY SUM(worddist.cnt) DESC");
        }else{
            String qsMarks2 = "";
            for(int i=0; i<sections.length; i++){
                if(i>0)
                    qsMarks2 += ",";
                qsMarks2 += "?";
            }
            stmt = db.prepareStatement("SELECT roots.root, SUM(worddist.cnt) FROM worddist JOIN roots ON "
                + " worddist.word=roots.rid WHERE worddist.section IN ("+qsMarks2+") AND worddist.word IN ("+qsMarks
                + ") GROUP BY worddist.word ORDER BY SUM(worddist.cnt) DESC");
            for(int i : sections){
                stmt.setInt(x, i);
                x ++;
            }
        }
        for(int i : roots){
            stmt.setInt(x, i);
            x ++;
        }
        processResults(stmt.executeQuery());
        stmt.close();
        return dtaSet.size();
    }
    
    public int sectionData(int[] secs) throws SQLException{
        PreparedStatement stmt;
        if(secs.length == 0){
            stmt = db.prepareStatement("SELECT sec,scnt,wcnt,lextoks,unqwords,cwords,syls FROM secs ORDER BY sec ASC");
        }else{
            String qsMarks = "";
            for(int i=0; i<secs.length; i++){
                if(i>0)
                    qsMarks += ",";
                qsMarks += "?";
            }
            stmt = db.prepareStatement("SELECT sec,scnt,wcnt,lextoks,unqwords,cwords,syls FROM secs WHERE sec IN ( "
                    + qsMarks + " ) ORDER BY sec ASC");
            
            int x = 1;
            for(int i : secs){
                stmt.setInt(x, i);
                x ++;
            }
        }
        processResults(stmt.executeQuery());
        stmt.close();
        return dtaSet.size();
    }
    
    public int sentenceData(int sec, boolean showText, int[] sents) throws SQLException{
        PreparedStatement stmt;
        if(sents.length == 0){
            if(showText)
                stmt = db.prepareStatement("SELECT sent,wcnt,lextokans,unqwords,complexwrds,syl,txt FROM slst WHERE sec=? "
                        + " ORDER BY sent ASC");
            else
                stmt = db.prepareStatement("SELECT sent,wcnt,lextokans,unqwords,complexwrds,syl FROM slst WHERE sec=? "
                        + " ORDER BY sent ASC");
        }else{
            String qsMarks = "";
            for(int i=0; i<sents.length; i++){
                if(i>0)
                    qsMarks += ",";
                qsMarks += "?";
            }
            if(showText)
                stmt = db.prepareStatement("SELECT sent,wcnt,lextokans,unqwords,complexwrds,syl,txt FROM slst WHERE sent IN ("
                        + qsMarks+") AND  sec=? ORDER BY sent ASC");
            else
                stmt = db.prepareStatement("SELECT sent,wcnt,lextokans,unqwords,complexwrds,syl FROM slst WHERE sent IN ("
                        + qsMarks+") AND  sec=? ORDER BY sent ASC");
            
            int x = 1;
            for(int i : sents){
                stmt.setInt(x, i);
                x ++;
            }
        }
        stmt.setInt(sents.length+1, sec);
        processResults(stmt.executeQuery());
        stmt.close();
        return dtaSet.size();
    }
    
    public int sentenceDataChoose(String [] slst) throws SQLException{
        PreparedStatement stmt;
        
        String cond = " WHERE ";
        int i=0;
        for(String s : slst){
            if(!s.contains(":"))
                continue;
            try{
                int sec = Integer.parseInt(s.substring(0, s.indexOf(":")));
                int sent = Integer.parseInt(s.substring(s.indexOf(":")+1));
                if(i > 0)
                    cond += " OR ";
                cond += "(sec="+sec+" AND sent="+sent+")";
                i ++;
            }catch(NumberFormatException e){
                continue;
            }
        }
        if(i == 0){
            return 0;
        }
        
        stmt = db.prepareStatement("SELECT sec,sent,wcnt,lextokans,unqwords,complexwrds,syl,txt FROM slst "
                        +cond+ " ORDER BY sec ASC, sent ASC");
        
        processResults(stmt.executeQuery());
        stmt.close();
        return dtaSet.size();
    }
    
    public int sentenceDataAll() throws SQLException{
        PreparedStatement stmt;
        stmt = db.prepareStatement("SELECT sec,sent,wcnt,lextokans,unqwords,complexwrds,syl FROM slst "
                        + " ORDER BY sec ASC, sent ASC");
        
        processResults(stmt.executeQuery());
        stmt.close();
        return dtaSet.size();
    }
    
    private HashSet<Integer> getEntityIDsBySearch(String [] words) throws SQLException{
        PreparedStatement stmt;
        HashSet<Integer> eids = new HashSet();
        String qsMarks = "(";
        for(int i=0; i<words.length; i++){
            if(i>0)
                qsMarks += "OR";
            qsMarks += " name LIKE ? ";
        }
        qsMarks += ")";
        
        stmt = db.prepareStatement("SELECT eid FROM ents WHERE "+qsMarks);
        
        int x = 1;
        for(String s : words){
            stmt.setString(x, ""+s+"");
            x++;
        }
        
        ResultSet r = stmt.executeQuery();
        while(r.next()){
            eids.add(r.getInt("eid"));
        }
        return eids;
    }
    
    public int relationsSummary(String[] words, int[] secs) throws SQLException{
        PreparedStatement stmt;
        HashSet<Integer> eids = getEntityIDsBySearch(words);
        
        String qsMarks = "";
        for(int i=0; i<eids.size(); i++){
            if(i>0)
                qsMarks += ",";
            qsMarks += "?";
        }
        
        String secClause = "";
        if(secs.length > 0){
            secClause = " AND sec IN (";
            for(int i=0; i<secs.length; i++){
                if(i>0)
                    secClause += ",";
                secClause += Integer.toString(secs[i]);
            }
            secClause += ") ";
        }
        stmt = db.prepareStatement("SELECT COUNT(),(SELECT name FROM ents WHERE eid=eid1),"
                        + " (SELECT name FROM ents WHERE eid=eid2) FROM entrel WHERE eid1 IN ("+qsMarks+") "
                        + " OR eid2 IN (" +qsMarks+ ")  "+secClause + "GROUP BY eid1,eid2 ORDER BY COUNT() DESC");
        int x = 1;
        for(int y=0; y<2; y++){
            for(int i : eids){
                stmt.setInt(x, i);
                x++;
            }
        }
        processResults(stmt.executeQuery());
        stmt.close();
        
        dataSetColNames = new String[3];
        dataSetColNames[0] = "count";
        dataSetColNames[1] = "entity1";
        dataSetColNames[2] = "entity2";
        return dtaSet.size();
    }
    
    
    public int relations(String[] words1, String[] words2, int[] secs) throws SQLException{
        PreparedStatement stmt;
        HashSet<Integer> eids1 = getEntityIDsBySearch(words1);
        HashSet<Integer> eids2 = getEntityIDsBySearch(words2);
        String qsMarks1 = "";
        for(int i=0; i<eids1.size(); i++){
            if(i>0)
                qsMarks1 += ",";
            qsMarks1 += "?";
        }
        String qsMarks2 = "";
        for(int i=0; i<eids2.size(); i++){
            if(i>0)
                qsMarks2 += ",";
            qsMarks2 += "?";
        }
        String secClause = "";
        if(secs.length > 0){
            secClause = " AND section IN (";
            for(int i=0; i<secs.length; i++){
                if(i>0)
                    secClause += ",";
                secClause += Integer.toString(secs[i]);
            }
            secClause += ") ";
        }
        
        stmt = db.prepareStatement("SELECT sec, sent, (SELECT name FROM ents WHERE eid=eid1),"
                        + " (SELECT name FROM ents WHERE eid=eid2), reltype FROM entrel WHERE "
                        + " (eid1 IN ("+qsMarks1+") OR eid1 IN ("+qsMarks2+")) AND "
                        + " (eid2 IN ("+qsMarks1+") OR eid2 IN ("+qsMarks2+")) "+secClause
                        + " ORDER BY sec ASC, sent ASC");
        int x = 1;
        for(int y=0; y<2; y++){ // repeat twice
            for(int i : eids1){
                stmt.setInt(x, i);
                x++;
            }
            for(int i : eids2){
                stmt.setInt(x, i);
                x++;
            }
        }
        
        processResults(stmt.executeQuery());
        stmt.close();

        dataSetColNames = new String[5];
        dataSetColNames[0] = "sec";
        dataSetColNames[1] = "sent";
        dataSetColNames[2] = "entity1";
        dataSetColNames[3] = "entity2";
        dataSetColNames[4] = "type";
        
        return dtaSet.size();
    }
    
    public int sectionsWithTag(String tag, int [] secs) throws SQLException{
        PreparedStatement stmt;
        if(secs.length == 0){
            stmt = db.prepareStatement("SELECT section, sent,val FROM tags WHERE name LIKE ? ORDER BY SECTION ASC, sent ASC");
            stmt.setString(1, tag);
        }else{
            String qsMarks = "";
            for(int i=0; i<secs.length; i++){
                if(i>0)
                    qsMarks += ",";
                qsMarks += "?";
            }
            stmt = db.prepareStatement("SELECT section, sent,val FROM tags WHERE name LIKE ? AND section IN ( "+qsMarks+
                            " ) ORDER BY SECTION ASC, sent ASC");
            
            stmt.setString(1, tag);
            int x = 2;
            for(int i : secs){
                stmt.setInt(x, i);
                x ++;
            }
        }
        processResults(stmt.executeQuery());
        stmt.close();
        return dtaSet.size();
    }
    
}
