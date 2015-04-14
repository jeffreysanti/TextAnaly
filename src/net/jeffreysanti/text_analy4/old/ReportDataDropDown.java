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
public class ReportDataDropDown extends ReportData {

    public ReportDataDropDown(String lbl, String[] options, String sel){
        super(lbl);
        selected = sel;
        ops = new String[options.length];
        System.arraycopy(options, 0, ops, 0, ops.length);
    }
    
    public String[] ops;
    public String selected;

    @Override
    public boolean isDatabaseEntry() {
        return false;
    }

    @Override
    public int getDBID() {
        return -1;
    }
    @Override
    public String getDBIDStr(){
        return "";
    }

    @Override
    public String getDBTable() {
        return "";
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
    
}
