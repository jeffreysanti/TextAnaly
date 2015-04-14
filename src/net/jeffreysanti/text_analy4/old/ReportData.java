/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.jeffreysanti.text_analy4;

/**
 *
 * @author jeffrey
 */
public abstract class ReportData {
    
    public ReportData(String lbl){
        label = lbl;
    }
    
    abstract public boolean isDatabaseEntry();
    abstract public int getDBID();
    abstract public String getDBIDStr();
    abstract public String getDBTable();
    
    abstract public String[] getDerrivativeDataNames();
    abstract public ReportData getDerrivativeData(String nm);
    
    abstract public int getElmCountX();
    abstract public int getElmCountY();
    abstract public ReportData getElm(int x, int y);
    
    
    public String label;
    
}
