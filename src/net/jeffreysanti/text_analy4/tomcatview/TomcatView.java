/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.jeffreysanti.text_analy4.tomcatview;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.jeffreysanti.text_analy4.GenericReport;
import net.jeffreysanti.text_analy4.ViewerBase;
import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.startup.Tomcat;

/**
 *
 * @author jeffrey
 */

@WebServlet(
        name = "MyServlet",
        urlPatterns = {"/hello"}
    )
public class TomcatView extends ViewerBase {

    protected Tomcat tc = null;
    

    public TomcatView(String dbFile) {
        super(dbFile);
        
        /*tc = new Tomcat();
        tc.setPort(8080);
        
        Context ctx = tc.addContext("/", new File(".").getAbsolutePath());
        Tomcat.addServlet(ctx, "index", new HttpServlet() {
            @Override
            protected void service(HttpServletRequest req, HttpServletResponse resp)
                    throws ServletException, IOException {
                resp.setContentType("text/html;charset=UTF-8");
                Writer w = resp.getWriter();
                for(String r : getReportNames()){
                    w.write("<a href=\"/report?report="+r+"\">"+r+"</a><br/>\n");
                }
                w.write("---------<br />");
                w.write("<a href=\"/data?\">Data Manager</a><br/>\n");
                w.flush();
            }
        });
        ctx.addServletMapping("/", "index");
        
        Tomcat.addServlet(ctx, "report", new HttpServlet() {
            @Override
            protected void service(HttpServletRequest req, HttpServletResponse resp)
                    throws ServletException, IOException {
                String repName = req.getParameter("report");
                GenericReport rep = getReport(repName);
                TomcatReportView trv = new TomcatReportView(rep);
                trv.service(req, resp);
            }
        });
        ctx.addServletMapping("/report/*", "report");
        
        Tomcat.addServlet(ctx, "data", new HttpServlet() {
            @Override
            protected void service(HttpServletRequest req, HttpServletResponse resp)
                    throws ServletException, IOException {
                TomcatDataView tdv = new TomcatDataView(TomcatView.this);
                tdv.service(req, resp);
            }
        });
        ctx.addServletMapping("/data/*", "data");
        
        
        try {
            tc.start();
        } catch (LifecycleException ex) {
            Logger.getLogger(TomcatView.class.getName()).log(Level.SEVERE, null, ex);
        }
        tc.getServer().await();*/
    }

}
