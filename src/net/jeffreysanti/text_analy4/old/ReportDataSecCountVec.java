/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.jeffreysanti.text_analy4;

import java.util.HashSet;

/**
 *
 * @author jeffrey
 */
public class ReportDataSecCountVec extends ReportDataIntIntVec {

    public ReportDataSecCountVec(String lbl, ViewerBase view) {
        super(lbl, view, "Section", "Count");
    }
    
    /*@Override
    public String[] getDerrivativeDataNames() {
        return new String [] {"Section List"};
    }
    @Override
    public ReportData getDerrivativeData(String nm) {
        if(nm.equals("Section List")){
            ReportDataRootVec r = new ReportDataRootVec(vb);
            r.setDBID(vb.newUserData("rootvec"));
            HashSet<String> append = new HashSet();
            for(ReportDataStrIntVec.StringIntPair x : data){
                append.add(x.s);
            }
            r.appendData(append);
            return r;
        }
        return null;
    }*/
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
    
}
