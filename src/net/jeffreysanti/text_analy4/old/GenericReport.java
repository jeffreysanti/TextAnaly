/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.jeffreysanti.text_analy4;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author jeffrey
 */
public abstract class GenericReport {
    
    public GenericReport(ViewerBase base){
        O = new ArrayList();
        I = new ArrayList();
        vb = base;
    }
    
    abstract public GenericReport factory();
    
    public abstract String getName();
    public abstract void runReport();
    
    
    public ArrayList<ReportData> O;
    public ArrayList<ReportData> I;
    public ViewerBase vb;
    
}
