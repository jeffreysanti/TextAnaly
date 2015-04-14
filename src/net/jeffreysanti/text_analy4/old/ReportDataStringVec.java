/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.jeffreysanti.text_analy4;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author jeffrey
 */
public class ReportDataStringVec extends ReportData {
    
    public ReportDataStringVec(String lbl, ViewerBase view) {
        super(lbl);
        vb = view;
        data = new ArrayList();
        id = -1;
    }
    
    public static HashSet<String> textListToHashSet(String lst){
        HashSet<String> data = new HashSet();
        lst = lst.replaceAll("\n", " ");
        lst = lst.replaceAll(",", " ");
        lst = lst.replaceAll(";", " ");
        for(String s : lst.split(" ")){
            s = s.trim().toLowerCase();
            if(!s.isEmpty())
                data.add(s);
        }
        return data;
    }
    
    public void setDBID(String nm){
        id = vb.getDataSetID(nm);
        loadData();
    }
    
    public String getRawStringData(){
        String ret = "";
        for(String s : data){
            ret += s + "\n";
        }
        return ret;
    }

    @Override
    public boolean isDatabaseEntry() {
        return true;
    }

    @Override
    public int getDBID() {
        return id;
    }
    public String getDBIDStr(){
        return vb.getDataSetName(id);
    }

    @Override
    public String getDBTable() {
        return "userdata_strvec";
    }   
    
    public static ArrayList<String> getData(ViewerBase vb, int id){
        ArrayList<String> data = new ArrayList();
        try {
            String query = "SELECT val FROM userdata_strvec WHERE id=?";
            PreparedStatement stmt = vb.db.prepareStatement(query);
            stmt.setInt(1, id);
            ResultSet r = stmt.executeQuery();
            while(r.next()){
                data.add(r.getString(1).toLowerCase());
            }
            r.close();
            stmt.close();
        } catch (SQLException ex) {
            Logger.getLogger(ViewerBase.class.getName()).log(Level.SEVERE, null, ex);
        }
        return data;
    }
    
    protected void loadData(){
        data = getData(vb, id);
    }
    
    protected void flushData(){
        if(id < 0)
            return;
        try {
            vb.db.setAutoCommit(false);
            
            // clear data
            PreparedStatement stmt = vb.db.prepareStatement("DELETE FROM userdata_strvec WHERE id=?");
            stmt.setInt(1, id);
            stmt.executeUpdate();
            stmt.close();
            
            stmt = vb.db.prepareStatement("INSERT INTO userdata_strvec (id, val) VALUES (?,?)");
            stmt.setInt(1, id);
            for(String s : data){
                stmt.setString(2, s);
                stmt.executeUpdate();
            }
            stmt.close();
            
            vb.db.setAutoCommit(true);
        } catch (SQLException ex) {
            Logger.getLogger(ViewerBase.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public void appendData(HashSet<String> dta){
        dta.addAll(data);
        flushData();
    }
    public void removeData(HashSet<String> dta){
        HashSet<String> dtaOld = new HashSet(data);
        dtaOld.removeAll(dta);
        data = new ArrayList(dtaOld);
        flushData();
    }
    public void appendDataFrom(String nm){
        HashSet<String> dta = new HashSet();
        int id2 = vb.getDataSetID(nm);
        
        dta.addAll(ReportDataStringVec.getData(vb, id2)); // from string vectors
        //for(ReportDataStrIntVec.StringIntPair x : getStrIntVec(id2, false, true, true)){ // from string data inside str-int pair vec
        //    dta.add(x.s);
        //}
        
        appendData(dta);
    }
    public void removeDataFrom(int id, String nm){
        HashSet<String> dta = new HashSet();
        int id2 = vb.getDataSetID(nm);
        
        dta.addAll(ReportDataStringVec.getData(vb, id2)); // from string vectors
        //for(ReportDataStrIntVec.StringIntPair x : getStrIntVec(id2, false, true, true)){ // from string data inside str-int pair vec
        //    dta.add(x.s);
        //}
        
        removeData(dta);
    }

    
    @Override
    public String[] getDerrivativeDataNames() {
        return new String [] {};
    }
    @Override
    public ReportData getDerrivativeData(String nm) {
        return null;
    }
    @Override
    public int getElmCountX() {
        return 0;
    }
    @Override
    public int getElmCountY() {
        return 0;
    }
    @Override
    public ReportData getElm(int x, int y) {
        return null;
    }
    
    private int id;
    private ViewerBase vb;
    private ArrayList<String> data;
}
