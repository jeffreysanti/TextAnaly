/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.jeffreysanti.text_analy4;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author jeffrey
 */
public class ReportWordCounts extends GenericReport {

    @Override
    public String getName() {
        return "Word Count Report";
    }
    
    public ReportWordCounts(ViewerBase base){
        super(base);
    }

    @Override
    public void runReport() {
        boolean blacklist = true;
        ReportDataStringVec inputList = null;
        String sortOrder = "DESC";
        String sortBy = "SUM(worddist.cnt)";
        for(ReportData d : I){
            if(d.label.equals("Filter Method")){
                if(((ReportDataDropDown)d).selected.equals("Whitelist"))
                    blacklist = false;
            }
            if(d.label.equals("Words")){
                inputList = ((ReportDataStringVec)d);
            }
            if(d.label.equals("Sort Order")){
                sortOrder = ((ReportDataDropDown)d).selected;
            }
            if(d.label.equals("Sort By")){
                sortBy = ((ReportDataDropDown)d).selected.equals("Count") ? "SUM(worddist.cnt)" : "roots.root";
            }
        }
        
        String outputDataName = vb.newUserData("rootcountvec");
        int outDataSetID = vb.getDataSetID(outputDataName);
        
        String query = "INSERT INTO userdata_strintvec (id, vals, vali) "
                + " SELECT ?, roots.root, SUM(worddist.cnt) FROM worddist"
                + " JOIN roots ON worddist.word=roots.rid "
                + " WHERE worddist.word ";
        
        if(blacklist)
            query += " NOT ";
        query +=  " IN (SELECT root FROM altwords "
                + "       WHERE word IN (SELECT val FROM userdata_strvec WHERE id=?)) "
                + " GROUP BY worddist.word ORDER BY "+sortBy+" "+sortOrder;
        PreparedStatement stmt;
        try {
            stmt = vb.db.prepareStatement("DELETE FROM userdata_strintvec WHERE id=?");
            stmt.setInt(1, outDataSetID);
            stmt.executeUpdate();
            stmt.close();
            
            stmt = vb.db.prepareStatement(query);
            stmt.setInt(1, outDataSetID);
            stmt.setInt(2, inputList.getDBID());
            stmt.executeUpdate();
            stmt.close();
        } catch (SQLException ex) {
            Logger.getLogger(ReportWordCounts.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        ReportDataStrIntVec res = new ReportDataRootCountVec("Results", vb);
        res.setDBID(outputDataName);
        O.add(res);
    }

    @Override
    public GenericReport factory() {
        
        ReportWordCounts report = new ReportWordCounts(vb);
        
        report.I.add(new ReportDataDropDown("Filter Method", new String []{"Blacklist", "Whitelist"}, "Blacklist"));
        report.I.add(new ReportDataStringVec("Words", vb));
        report.I.add(new ReportDataDropDown("Sort By", new String []{"Root Word", "Count"}, "Count"));
        report.I.add(new ReportDataDropDown("Sort Order", new String []{"DESC", "ASC"}, "Desc"));
        
        return report;
    }

    
    
}
