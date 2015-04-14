/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.jeffreysanti.text_analy4.tomcatview;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.jeffreysanti.text_analy4.GenericReport;
import net.jeffreysanti.text_analy4.ReportData;
import net.jeffreysanti.text_analy4.ReportDataDropDown;
import net.jeffreysanti.text_analy4.ReportDataIntIntVec;
import net.jeffreysanti.text_analy4.ReportDataIntIntVec.IntIntPair;
import net.jeffreysanti.text_analy4.ReportDataStrIntVec;
import net.jeffreysanti.text_analy4.ReportDataStrIntVec.StringIntPair;
import net.jeffreysanti.text_analy4.ReportDataStringVec;

/**
 *
 * @author jeffrey
 */
public class TomcatReportView {
    
    private GenericReport r;
    
    public TomcatReportView(GenericReport rep){
        r = rep;
    }
    
    public void service(HttpServletRequest req, HttpServletResponse resp)
    {
        try {
            boolean submit = false;
            if(req.getParameterMap().containsKey("submit")){
                submit = true;
            }
            String urlOut = req.getRequestURL() + "?";
            urlOut += "report="+req.getParameter("report") + "&";
            String urlFormOut = urlOut;
            
            resp.setContentType("text/html;charset=UTF-8");
            resp.getWriter().println("<h1>"+r.getName()+"</h1>\n");
            resp.getWriter().println("<form method=\"post\" action=\""+urlFormOut+"\">\n");
            resp.getWriter().println("<table><tr><td></td><td></td></tr>\n");
            for(ReportData d : r.I){
                resp.getWriter().println("<tr><td>"+d.label+"</td><td>\n");
                if(d instanceof ReportDataDropDown){
                    ReportDataDropDown tmp = (ReportDataDropDown)d;
                    if(req.getParameterMap().containsKey(tmp.label)){
                        tmp.selected = req.getParameter(tmp.label);
                    }
                    resp.getWriter().println("<select name=\""+d.label+"\">");
                    for(String op : tmp.ops){
                        if(op.equals(tmp.selected))
                            resp.getWriter().println("<option selected=\"1\" value=\""+op+"\">"+op+"</option>");
                        else
                            resp.getWriter().println("<option value=\""+op+"\">"+op+"</option>");
                    }
                    resp.getWriter().println("</select>");
                    //urlOut += tmp.label + "=" + tmp.selected + "&";
                }else if(d instanceof ReportDataStringVec){
                    ReportDataStringVec tmp = (ReportDataStringVec)d;
                    if(req.getParameterMap().containsKey(tmp.label)){
                        tmp.setDBID(req.getParameter(tmp.label));
                    }
                    resp.getWriter().println("<input type=\"text\" name=\""+d.label+"\" value=\""+tmp.getDBIDStr()
                            +"\" />\n");
                }else{
                    resp.getWriter().println("???");
                    System.err.println("Unknown ReportData Class: "+d.getClass().toString());
                }
                resp.getWriter().println("</td></tr>\n");
            }
            resp.getWriter().println("</table>\n");
            resp.getWriter().println("<input type=\"submit\" name=\"submit\" value=\"Go\" />\n");
            resp.getWriter().println("</form>\n");
            
            if(submit){
                r.runReport();
            }
            
            // now show output
            
            String table_strintvec = "";
            /*
            String sortOrder = (req.getParameterMap().containsKey("sortorder") && req.getParameter("sortorder").equals("desc"))
                    ? "desc" : "asc";
            String sortBy = (req.getParameterMap().containsKey("sortby") && req.getParameter("sortby").equals("int"))
                    ? "int" : "str";
            */
            for(ReportData d : r.O){
                if(d instanceof ReportDataStrIntVec){
                    ReportDataStrIntVec tmp = (ReportDataStrIntVec)d;
                    
                    table_strintvec += "<table>\n";
                    table_strintvec += "<tr><th>"+tmp.titleString+"</th><th>"+tmp.titleInt+"</th></tr>\n";
                    for(StringIntPair ent : tmp.getData()){
                        table_strintvec += "<tr><td>"+ent.s+"</td><td>"+ent.i+"</td></tr>\n";
                    }
                    table_strintvec += "</table>\n";
                }if(d instanceof ReportDataIntIntVec){
                    ReportDataIntIntVec tmp = (ReportDataIntIntVec)d;
                    table_strintvec += "<table>\n";
                    table_strintvec += "<tr><th>"+tmp.title1+"</th><th>"+tmp.title2+"</th></tr>\n";
                    for(IntIntPair ent : tmp.getData()){
                        table_strintvec += "<tr><td>"+ent.i1+"</td><td>"+ent.i2+"</td></tr>\n";
                    }
                    table_strintvec += "</table>\n";
                }else{
                    resp.getWriter().println("???");
                    System.err.println("Unknown ReportData Class: "+d.getClass().toString());
                }
                
                // now output data
                if(!table_strintvec.isEmpty()){
                    // first sort functions
                   
                    
                    /*if(sortBy.equals("str"))
                        resp.getWriter().println("Sorting By: <a href=\""+urlOut+"sortorder="+sortOrder+"&sortby=int&\">String</a>");
                    else
                        resp.getWriter().println("Sorting By: <a href=\""+urlOut+"sortorder="+sortOrder+"&sortby=str&\">Value</a>");
                    
                    if(sortOrder.equals("asc"))
                        resp.getWriter().println("Sort Order: <a href=\""+urlOut+"sortby="+sortBy+"&sortorder=desc&\">Ascending</a>");
                    else
                        resp.getWriter().println("Sort Order: <a href=\""+urlOut+"sortby="+sortBy+"&sortorder=asc&\">Decending</a>");
                    resp.getWriter().println("<br />");*/
                    resp.getWriter().println(table_strintvec);
                }
                 
                
            }
            
            
        } catch (IOException ex) {
            Logger.getLogger(TomcatReportView.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
