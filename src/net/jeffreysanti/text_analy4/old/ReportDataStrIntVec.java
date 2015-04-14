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
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author jeffrey
 */
public class ReportDataStrIntVec extends ReportData {

    public static class StringIntPair{
        public StringIntPair(String s, int i){
            this.s = s;
            this.i = i;
        }
        public String s;
        public int i;
    }
    
    public ReportDataStrIntVec(String lbl, ViewerBase view, String tStr, String tInt) {
        super(lbl);
        vb = view;
        titleString = tStr;
        titleInt = tInt;
        id = -1;
        data = new ArrayList();
    }
    
    public void setDBID(String nm){
        id = vb.getDataSetID(nm);
        loadData();
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
        return "userdata_strintvec";
    }
    
    public static ArrayList<StringIntPair> getData(ViewerBase vb, int id){
        ArrayList<StringIntPair> ret = new ArrayList();
        try {
            String query = "SELECT vals,vali FROM userdata_strintvec WHERE id=? ";
            PreparedStatement stmt = vb.db.prepareStatement(query);
            stmt.setInt(1, id);
            ResultSet r = stmt.executeQuery();
            while(r.next()){
                ret.add(new StringIntPair(r.getString(1), r.getInt(2)));
            }
            r.close();
            stmt.close();
        } catch (SQLException ex) {
            Logger.getLogger(ViewerBase.class.getName()).log(Level.SEVERE, null, ex);
        }
        return ret;
    }
    
    public ArrayList<StringIntPair> getData(){
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
            PreparedStatement stmt = vb.db.prepareStatement("DELETE FROM userdata_strintvec WHERE id=?");
            stmt.setInt(1, id);
            stmt.executeUpdate();
            stmt.close();
            
            stmt = vb.db.prepareStatement("INSERT INTO userdata_strintvec (id, vals, vali) VALUES (?,?,?)");
            stmt.setInt(1, id);
            for(StringIntPair x : data){
                stmt.setString(2, x.s);
                stmt.setInt(3, x.i);
                stmt.executeUpdate();
            }
            stmt.close();
            
            vb.db.setAutoCommit(true);
        } catch (SQLException ex) {
            Logger.getLogger(ViewerBase.class.getName()).log(Level.SEVERE, null, ex);
        }
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
    
    
    protected ViewerBase vb;
    public String titleString, titleInt;
    protected int id;
    protected ArrayList<StringIntPair> data;
}
