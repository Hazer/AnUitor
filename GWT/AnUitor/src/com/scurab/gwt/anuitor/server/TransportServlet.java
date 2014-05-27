package com.scurab.gwt.anuitor.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import sun.net.www.http.HttpClient;

/**
 * This is simple fake servlet class to cross-site image loading
 * 
 * @author jbruchanov
 * 
 */
public class TransportServlet extends HttpServlet {

    /**
     * 
     */
    private static final long serialVersionUID = 1020491090626232287L;

    public String getContentURL(String path) {
        return String.format("http://%s%s", getDeviceAddress(), path);
    }

    public String getDeviceAddress() {
        return "10.0.1.56:8080";
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            String qs = req.getQueryString();            
            String newUrl = getContentURL(req.getRequestURI() + (qs == null || qs.length() == 0 ? "" : "?" + req.getQueryString()));
//            newUrl = "http://www.scurab.com/gtfo.png";
            System.out.println("NewURL:" + newUrl);
            URL url = new URL(newUrl);
            System.out.println("OpenConnection");
            HttpURLConnection urlc = (HttpURLConnection) url.openConnection();
            
            urlc.setDoInput(true);
            urlc.setDoOutput(true);
            System.out.println("connect");
            urlc.connect();            
            System.out.println("getInputStream");            
            InputStream is = urlc.getInputStream();
            System.out.println("getOutputStream");
            OutputStream os = resp.getOutputStream();

            byte[] buffer = new byte[1024];
            int len = is.read(buffer);
            while (len != -1) {
                os.write(buffer, 0, len);
                len = is.read(buffer);
            }
            is.close();
            os.close();            
        } catch (Exception e) {            
            e.printStackTrace();
        }
    }      
}


