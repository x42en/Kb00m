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


import java.io.IOException;
import java.net.DatagramPacket;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import net.jxta.discovery.DiscoveryService;
import net.jxta.endpoint.Message;
import net.jxta.endpoint.MessageElement;
import net.jxta.endpoint.StringMessageElement;
import net.jxta.peergroup.PeerGroup;
import net.jxta.pipe.*;
import net.jxta.protocol.PipeAdvertisement;

/**
 *
 * @author usr
 */
public class Chat extends Thread implements PipeMsgListener, Runnable {

    //Constant
    private final static int TIMEOUT = 5 * 1000;
    
    //Static variable dealing with Status
    public final int OFFLINE_STATUS = 0;
    public final int ONLINE_STATUS = 1;
    public final int BUSY_STATUS = 2;
    public final int AWAY_STATUS = 3;
    public final int BACK_SOON_STATUS = 4;
    //Using Seed in order to create pipeID
    //Combinated with the PeerGroupID
    private String grpName = null;
    private DiscoveryService discoverySv;
    private PipeService pipeSv;
    private PeerGroup chatGrp;
    //private JTextArea outputChat;
    private OutputPipe propagatePipe = null;
    //private JxtaMulticastSocket propagateMultPipe = null;
    private InputPipe inPipe = null,
            inPipeM = null;
    private PipeAdvertisement propagatePipeAdv = null;
    private String sender = null;
    private int status;
    private String myPeerID;
    private String msg = "Hello";
    private UserModel peerList = null;
    public static final String SEPARATOR = " says: ";
    
    public Chat(PeerGroup p, UserModel pLst) {
        
        chatGrp = p;
        grpName = chatGrp.getPeerGroupName();
        peerList = pLst;
        displayLog("[+]Retrieving Chat Advertisement.");
        discoverySv = chatGrp.getDiscoveryService();
        pipeSv = chatGrp.getPipeService();
        
        displayLog("[+]Retrieving peer informations.");
        sender = chatGrp.getPeerName();
        myPeerID = chatGrp.getPeerID().toString();
             
    }
    
    @Override
    public void run(){
        startListening();
        try {
            discoverySv.publish(inPipeM.getAdvertisement());
            discoverySv.remotePublish(inPipeM.getAdvertisement());
            discoverySv.publish(inPipe.getAdvertisement());
            discoverySv.remotePublish(inPipe.getAdvertisement());
        } catch (IOException ex) {
            displayLog("[!]IOException while publishing Chat pipe advertisement!!");
        }
        
        displayLog("[+]Chat sucessfully started !!");
        if(Kb00m.GUI_MODE){
            GUI.addChatMsg("  -- Welcome in the "+grpName+" world --\n");
        }else{
            displayLog("  -- Welcome in the "+grpName+" world --\n");
        }
    }
    
    private void displayLog(String s){
        if(Kb00m.GUI_MODE){
            GUI.addLogMsg(s);
        }else{
            System.out.println("\n"+s);
        }
    }

    public void startListening() //This method will start listening for incoming messages thro created pipe
    {
        propagatePipeAdv = Tools.getPipeAdvertisement(chatGrp.getPeerGroupID(),(grpName+"M:CHAT"), true);
        displayLog("[+]Start Listening for Incoming Messages.");
        try {
            inPipe = pipeSv.createInputPipe(Tools.getPipeAdvertisement(chatGrp.getPeerGroupID(),(grpName+"CHAT"), false),this);
            displayLog("[+]Creation successful of the unicast chat listening inputPipe with ID: "+inPipe.getPipeID());
            inPipeM = pipeSv.createInputPipe(propagatePipeAdv, this);
            displayLog("[+]Creation successful of the multicast chat listening inputPipe with ID: "+inPipeM.getPipeID());
            //propagateMultPipe = new JxtaMulticastSocket(chatGrp, propagatePipeAdv);
            //displayLog("[+]Creation successful of the multicast socket chat listening Pipe with ID: "+propagateMultPipe);
        } catch (IOException ioe) {
            displayLog("[!]IOException when registring input pipe\n" + ioe.getMessage());
        }
        if (inPipe == null) {
            displayLog("[!]Failure in Opening Chat Input Pipe :-(");
            System.exit(-1);
        }
        
    }
    
    public void userLogOff(String user){
        if(Kb00m.GUI_MODE){
            GUI.userLogoff(user);
        }else{
            displayLog("<"+user+"> has left the group");
        }
    }

    
    public void stopListening() //This method will stop input pipe
    {
        if(Kb00m.GUI_MODE){
            GUI.addChatMsg("  -- You left "+grpName+" world --\n");
        }else{
            displayLog("  -- You left "+grpName+" world --\n");
        }
        inPipe.close();
        inPipeM.close();
        //propagateMultPipe.close();
        displayLog("[-]Input Pipe Closed for Incomming Message.");
    }

   
    @Override
    public void pipeMsgEvent(PipeMsgEvent msgEvent) {
        Message msg = null;

        String msgPeerID = null;
        String msgSender = null;
        String msgPeerStatus = null;
        String msgText = null;
        String msgTime = null;
        try {
            msg = msgEvent.getMessage();
            MessageElement senderElement = msg.getMessageElement("peerName");
            msgSender = senderElement.toString();
            MessageElement senderIDElement = msg.getMessageElement("peerID");
            msgPeerID = senderIDElement.toString();
            MessageElement senderStatusElement = msg.getMessageElement("peerStatus");
            msgPeerStatus = senderStatusElement.toString();
            MessageElement textElement = msg.getMessageElement("chatMessage");
            msgText = textElement.toString();
            MessageElement timeElement = msg.getMessageElement("Time");
            msgTime = timeElement.toString();
        } catch (Exception e) {
            displayLog("[+]Error has receiving message\n" + e.getMessage());
            return;
        }
        
        if (msgSender != null && msgText != null && msgTime != null) {
            if(msgSender.equals(chatGrp.getPeerName())) msgSender = "Me";
            if(!peerList.isKnownUser(msgSender))
                peerList.addUser(msgPeerID,msgSender);
            peerList.updateUserStatus(msgSender, msgPeerStatus);
            
            String s = "[" + msgSender + " @ " + msgTime + "] " + msgText + "\n";
            if(Kb00m.GUI_MODE){
                GUI.addChatMsg(s);
            }else System.out.println("\n[+]CHAT MESSAGE FROM ROOM "+grpName+"\n[+]"+s);
        }
    }

    
    public void sendMsg(String txt) {
        
        msg = txt;
        
        DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
        Date date = new Date();
        String myTime = dateFormat.format(date).toString();
        
        Message myMessage = new Message();
        //adding timestap and peers details also messages to XML tag and send them 
        StringMessageElement sme = new StringMessageElement("peerName", sender, null);
        StringMessageElement sme1 = new StringMessageElement("peerID", myPeerID, null);
        StringMessageElement sme2 = new StringMessageElement("chatMessage", msg, null);
        StringMessageElement sme3 = new StringMessageElement("peerStatus", peerList.getUserStatus("Me"), null);
        StringMessageElement sme4 = new StringMessageElement("Time", myTime, null);

        myMessage.addMessageElement(sme);
        myMessage.addMessageElement(sme1);
        myMessage.addMessageElement(sme2);
        myMessage.addMessageElement(sme3);
        myMessage.addMessageElement(sme4);
        
        DatagramPacket mdpkt = new DatagramPacket(msg.getBytes(),msg.length());
        try {
            displayLog("[+]Creating Chat OutPutPipe");
            propagatePipe = pipeSv.createOutputPipe(Tools.getPipeAdvertisement(chatGrp.getPeerGroupID(),grpName+"M:CHAT", true), TIMEOUT);
            
            boolean send = propagatePipe.send(myMessage);
            //propagateMultPipe.send(mdpkt);
            
            if(send){
                displayLog("[+]Message sent: " + msg + "\n[+]To Group: " + grpName + "\n[+] with ID: " + chatGrp.getPeerGroupID().toString());
                
            }else displayLog("[!]Impossible to send the message");
            propagatePipe.close();
            
        } catch (IOException ioe) {
            displayLog("[!]IOError has sending message <" + txt + ">\n"+ioe.getMessage());
            
        }
        
    }

   public String getPresenceInfo() {
        switch (status) {
            case ONLINE_STATUS:
                return "OnLine";
            case AWAY_STATUS:
                return "Away";
            case BUSY_STATUS:
                return "Busy";
            case BACK_SOON_STATUS:
                return "BackSoon";
            case OFFLINE_STATUS:
                return "OffLine";
        }
        return null;
    }
    
    public int getPresenceIndex() {
        return status;
    }

    public String getLocalPeerID() {
        return chatGrp.getPeerID().toString();
    }

}
