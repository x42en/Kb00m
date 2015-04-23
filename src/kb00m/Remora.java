package kb00m;

/*
 * © 2013 - Certiwise Software Services (www.certiwise.com)
 * 
 * KB00m code is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public 
 * License as published by the Free Software Foundation; either 
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This code is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public 
 * License along with this program; if not, write to the Free 
 * Software Foundation, Inc., 59 Temple Place - Suite 330, Boston, 
 * MA  02111-1307, USA.
 * 
 * Author      : Ben Mz
 * Contact Mail: bmz at certiwise dot com
 * Softwares   : JXTA Version 2.7, JDK Version 1.6.0_05, NetBeans IDE Version 7.1.1, BouncyCastle Version 1.47
 * 
 */

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.net.MalformedURLException;
import java.security.cert.Certificate;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLPeerUnverifiedException;


public class Remora implements Runnable {
    
    private Thread t = null;
    private Integer tcpPort = 0;
    private String ID = null;
    private boolean accessible = true;
    private boolean registered = false;
    private boolean runFlag = false;
    private boolean suspendFlag = false;
    
    protected Remora(int port){
        tcpPort = port;
//        displayLog("[+]Digging holes... ;)");
//                
//                //Finding address other than localhost
//                Enumeration e = null;
//                try {
//                    e = NetworkInterface.getNetworkInterfaces();
//                } catch (SocketException ex) {
//                    displayLog("[!]SocketException with remora "+ex.getMessage());
//                }
//                InetAddress i = null;
//                InetAddress finalIP = null;
//                while(e.hasMoreElements())
//                {
//                    NetworkInterface n=(NetworkInterface) e.nextElement();
//                    Enumeration ee = n.getInetAddresses();
//                    while(ee.hasMoreElements())
//                    {
//                        i = (InetAddress) ee.nextElement();
//                        //Skip localhost and ipv6 address
//                        if(!i.getHostAddress().startsWith("127.") && !i.getHostAddress().contains(":"))
//                            finalIP = i;
//                    }
//                    
//                }
//                
//                //Open socket on ip address
//                Socket socket = null;
//                PrintWriter out = null;
//                InetAddress addr;
//                try {
//                    addr = InetAddress.getByName("www.certiwise.com");
//                    displayLog("[+]Connecting remora to "+addr.getHostAddress()+" with IP: "+finalIP.getHostAddress());
//                    try {
//                        socket = new Socket(addr, 80, finalIP, tcpPort);
//                        out = new PrintWriter(socket.getOutputStream(), true);
//                        
//                        // send request
//                        out.print("GET register.php HTTP/1.1\r\n\r\n");
//                        out.flush();
//                        BufferedReader in = null;
//                        try {
//                            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
//                            int tryNum = 0;
//                            while(tryNum < 3){
//                                if(in.readLine() != null){
//                                    displayLog("[+]Communication with world now allowed");
//                                    accessible = true;
//                                    break;
//                                }
//                                else{
//                                    tryNum++;
//                                }
//                            }
//                            if(tryNum >= 3)
//                                displayLog("[!]Communication with world not available");
//
//
//                        } catch (IOException ex) {
//                            displayLog("[!]IOException while remora read server answer stream... "+ex.getMessage());
//                        }
//                        try {
//                            in.close();
//                            out.close();
//                            socket = null;
//                        } catch (IOException ex) {
//                            displayLog("[!]IOException while remora close stream... "+ex.getMessage());
//                        }
//                    } catch (IOException ex) {
//                        displayLog("[!]IOException while remora open socket... "+ex.getMessage());
//                    }
//                } catch (UnknownHostException ex) {
//                    displayLog("[!]UnknownHostException while starting remora... "+ex.getMessage());
//                }
    }
    
    private void displayLog(String s){
        if(Kb00m.GUI_MODE){
            GUI.addLogMsg(s);
        }else{
            System.out.println(s);
        }
    }
    
    public void start() {
        t = new Thread(this, "REMORA");
        runFlag = true;
        t.start();
            
            
    }
    
    protected void setID(String peerID){
        ID = peerID;
    }
    
    protected void registerSelf(){
        
        displayLog("[+]Trying to register public RDV status... ");
        
        try {
            // Construct data
            String data = URLEncoder.encode("a", "UTF-8") + "=" + URLEncoder.encode("HELLO", "UTF-8");
            data += "&" + URLEncoder.encode("id", "UTF-8") + "=" + URLEncoder.encode(ID, "UTF-8");
            data += "&" + URLEncoder.encode("port", "UTF-8") + "=" + URLEncoder.encode(tcpPort.toString(), "UTF-8");

            // Send data
            URL url = new URL(Kb00m.APP_NODES);
            
            displayLog("[+]Connecting to: "+url.toString());
            URLConnection conn = url.openConnection();
            conn.setDoOutput(true);
            
            OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
            wr.write(data);
            wr.flush();
            
            // Get the response
            BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line;
            
            while ((line = rd.readLine()) != null) {
                if(line.equals("OK")){
                    registered = true;
                    displayLog("[+]Peer accessibility registered for the next 20min");
                }else
                    displayLog("[!]Something went wrong when trying to publish peer!!\n[!]Please check any NAT issue, you're probably not reachable from the outside...\n[!]Receive: "+line);
            }
            wr.close();
            rd.close();
        } catch (Exception e) {
            displayLog("[-]Unable to register ourself "+e.getMessage());
        }
    }
    
    protected void updateSelf(){
        try {
            // Construct data
            String data = URLEncoder.encode("a", "UTF-8") + "=" + URLEncoder.encode("PING", "UTF-8");
            data += "&" + URLEncoder.encode("id", "UTF-8") + "=" + URLEncoder.encode(ID, "UTF-8");
            data += "&" + URLEncoder.encode("port", "UTF-8") + "=" + URLEncoder.encode(tcpPort.toString(), "UTF-8");

            // Send data
            URL url = new URL(Kb00m.APP_NODES);
            URLConnection conn = url.openConnection();
            conn.setDoOutput(true);
            OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
            wr.write(data);
            wr.flush();
            
            // Get the response
            BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line;
            while ((line = rd.readLine()) != null) {
                if(line.equals("OK")){
                    registered = true;
                    displayLog("[+]Peer accessibility registered for the next 20min");
                }else
                    displayLog("[!]Something went wrong when trying to update peer!!\n[!]Please check any NAT issue, you're probably not reachable from the outside...\n[!]Receive: "+line);
            }
            wr.close();
            rd.close();
        } catch (Exception e) {
            displayLog("[-]Unable to re-register ourself "+e.getMessage());
        }
    }
    
    protected void unRegisterSelf(){
        
        displayLog("[+]Unregistering public RDV status... ");
        
        try {
            // Construct data
            String data = URLEncoder.encode("a", "UTF-8") + "=" + URLEncoder.encode("BYE", "UTF-8");
            data += "&" + URLEncoder.encode("id", "UTF-8") + "=" + URLEncoder.encode(ID, "UTF-8");
            data += "&" + URLEncoder.encode("port", "UTF-8") + "=" + URLEncoder.encode(tcpPort.toString(), "UTF-8");

            // Send data
            URL url = new URL(Kb00m.APP_NODES);
            URLConnection conn = url.openConnection();
            conn.setDoOutput(true);
            OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
            wr.write(data);
            wr.flush();
            displayLog("[+]Connecting to: "+url.toString());
            
            // Get the response
            BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line;
            while ((line = rd.readLine()) != null) {
                displayLog(line);
            }
            wr.close();
            rd.close();
        } catch (Exception e) {
            displayLog("[-]Unable to unregister ourself");
        }
        registered = false;
    }
    
    protected boolean isRegistered(){
        return registered;
    }
    
    protected boolean isConnected(){
        return accessible;
    }
    
    void mysuspend() {
        displayLog("[!]Remora thread "+t.getName() + " suspended.");
        suspendFlag = true;
    }

    synchronized void myresume() {
        displayLog("[+]Resuming remora thread "+t.getName() + ".");
        suspendFlag = false;
        notify();
    }

    synchronized void mystop() {
        displayLog("[!]Remora thread "+t.getName() + " stopped.");
        //Before closing unregister ourselfves if needed
        if(registered)
             unRegisterSelf();
        suspendFlag = false;
        runFlag = false;
        notify();
    }

    @Override
    public void run() {
        
//        try {
            
                
                    
//        } catch (IOException ex) {
//            displayLog("[-]Unable to exit to server "+ex.getMessage());
//        }
        registerSelf();
        while(runFlag){
            try {
                //Wait for 20min
                Thread.sleep(20*60*1000);
                //Try new annoucement if accessible
                if(accessible && registered)
                    updateSelf();
                //If suspend is asked
                synchronized (this) {
                    while (suspendFlag) {
                        wait();
                    }
                }
            } catch (InterruptedException ex) {
                displayLog("[!]Error while remora sleep... "+ex.getMessage());
            }
        }
    }
}
