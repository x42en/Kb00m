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
 * Author      : benoit Malchrowicz
 * Contact Mail: bmz at certiwise dot com
 * Softwares   : JXTA Version 2.7, JDK Version 1.6.0_05, NetBeans IDE Version 7.1.1, BouncyCastle Version 1.47
 * 
 */

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.jxta.content.ContentService;
import net.jxta.discovery.DiscoveryEvent;
import net.jxta.discovery.DiscoveryListener;
import net.jxta.discovery.DiscoveryService;
import net.jxta.endpoint.Message;
import net.jxta.endpoint.MessageElement;
import net.jxta.endpoint.StringMessageElement;
import net.jxta.peergroup.PeerGroup;
import net.jxta.pipe.*;
import net.jxta.protocol.ContentShareAdvertisement;
import net.jxta.protocol.DiscoveryResponseMsg;
import net.jxta.protocol.PipeAdvertisement;


public final class Share extends Thread implements PipeMsgListener, Runnable, DiscoveryListener {

    protected static final String SHARES_FILENAME = "."+Core.MAINGROUP_NAME+".shares";
    private final static int TIMEOUT = 5 * 1000;
    
//    private ImageIcon Mp3Icon = null;
//    private ImageIcon VideoIcon = null;
//    private ImageIcon OthersIcon = null;
    //Defining Class Variables
//    private JTable searchResults = null;
//    private JTable activeTransfer = null;
//    private DefaultTableModel TableModelResult = null;
//    private DefaultTableModel TableModelActive = null;
    private File mySharedPath = null;
    //private File cachedLibrary = null;
    //private List<ContentShare> shares = null;
    //private Map<String, List> sharedLibrary = new HashMap();
    //private Map<String, String> cachedSharedLibrary = new HashMap();
    
    //Using the default sharing service... thanks to the jxta Team !! ;)
    private ContentService contentService = null;
    private DiscoveryService discoService=null;
    private PipeAdvertisement propagatePipeAdv = null;
    private PipeService pipeSrvc;
    private PeerGroup group = null;
    private String groupName = null;
    private OutputPipe propagatePipe = null;
//    private JxtaMulticastSocket propagateMultPipe = null;
    private InputPipe inPipe = null,
            inPipeM = null;
//    private HashMap<String,String> searchSession = new HashMap();
    //private boolean listening = true;
    
//    private static DATA Library = null;
    private LibraryModel library = null;
    private LibraryModel privateLibrary = null;
    private HashMap<String,LibraryModel> groupLibrary = new HashMap<String,LibraryModel>();
    private SearchModel searchResults = null;
    private ArrayList<String> trackList = null;
    
    static final String CHAR_ENCODING = "UTF-8";                 //character encoding
    
    private boolean isStarted = false;
    private HashMap<String, SearchThread> searchThread = new HashMap();
    
    private DownloadManager dManager = null;
    
    /**
     * Constructor.
     *
     * @param toServe rFile to serve
     * @param id ContentID to use when serving the toServer rFile, or
     *  {@code null} to generate a random ContentID
     * @param waitForRendezvous true to wait for rdv connection, false otherwise
     */
    public Share(PeerGroup grp, File sharedDirectory) {

        //Initialize global variable
        mySharedPath = sharedDirectory;
        group = grp;
        groupName = group.getPeerGroupName();
        
        //Creating the shared library
        library = new LibraryModel(group, sharedDirectory);
        privateLibrary = new LibraryModel(group, new File(sharedDirectory,".private"));
        searchResults = new SearchModel();
        trackList = new ArrayList<String>();
        
        //Instantiate the download manager object
        dManager = new DownloadManager(group, sharedDirectory, privateLibrary, library);
        dManager.start();
        
        displayLog("[+]======= Add Default Shared Contents =======");
        addSharedDirectory(sharedDirectory,true);
        
        isStarted = true;
        
        displayLog("[+]Retrieve the Share Services");
        contentService = group.getContentService();
        discoService = group.getDiscoveryService();
        pipeSrvc = group.getPipeService();
        
    }

    private void displayLog(String s){
        if(Kb00m.GUI_MODE){
            GUI.addLogMsg(s);
        }else{
            System.out.println(s);
        }
    }

    
    @Override
    public void run() {
         //Start the pipe allowing us to communicate with the share service
        startListening();

        //Publishing the pipes created
        try {
            discoService.publish(inPipeM.getAdvertisement());
            discoService.remotePublish(inPipeM.getAdvertisement());
            discoService.publish(inPipe.getAdvertisement());
            discoService.remotePublish(inPipe.getAdvertisement());
        } catch (IOException ex) {
            displayLog("[!]IOException while publishing Share pipe advertisement!!");
        }

        displayLog("[+]Share sucessfully started !!\n");
    }
    
    protected void addSharedDirectory(File f, boolean defNode){
        //If rFile specified does not exists just skip
        if(!f.exists())
            return;
        //If a single rFile is specified just add it
        if(f.isFile()){
            //Create a node with this rFile and his parent
            LibraryNode dir = new LibraryNode(this.group, library.getRoot(), f.getParentFile(), Kb00m.DELETE);
            LibraryNode res = new LibraryNode(this.group, dir, f, Kb00m.DELETE);
            if (!(f.getName().equals(".b00m.shares")) && !(f.getName().startsWith("."))) {
                    try {
                        //Sharing Files and checksum localy in a catalog
                        LibraryNode tmp = library.addFile(res, f);
                        displayLog("[+]Sharing a new file in Group "+groupName);
                        displayLog("[.]    Name     : " + tmp.getName());
                        displayLog("[.]    Checksum : " + tmp.getHash().substring(0,15) +"..."+ tmp.getHash().substring((tmp.getHash().length()-15)));
                        displayLog("[.]    Type     : " + tmp.getType());
                        displayLog("[.]    Category : " + tmp.getCategory());
                    } catch (Exception ex) {
                        displayLog("[!]Error while adding file "+f.getName()+" to share.\n[!]" + ex.getMessage());
                    }
                }
            return;
        }
        //If the rFile selected already exists, just escape 
        if(library.libContains(f))
            return;
        
        LibraryNode libNode = null;
        //If it's not the default directory add it to list
        if(defNode){
            libNode = library.getRoot().getParent();
        }else{
            libNode = library.addDirectory(f);
        }
        
        ShareDirThread s;
        try {
            s = new ShareDirThread(this.group, f.getCanonicalPath(),libNode);
            s.start();
        } catch (IOException ex) {
            displayLog("[!]Error while parsing thread of "+f.getName());
        }
        
    }
    
    class ShareDirThread implements Runnable {

        private String tname = null;
        private PeerGroup group = null;
        private Thread t = null;
        private LibraryNode dir = null;
        private Map<String, ArrayList<LibraryNode>> directoryMapping= new HashMap<String, ArrayList<LibraryNode>>();
        private Map<String, String> filesMapping = new HashMap<String, String>();
        private long parsedSize = 0;
        
        ShareDirThread(PeerGroup grp, String threadName, LibraryNode d) {
            tname = threadName;
            group = grp;
            dir = d;
        }
        
        public void start() {
            t = new Thread(this, tname);
            t.start();
        }
        
        @Override
        public void run() {
            
            displayLog("[+]Parsing directory " + t.getName());
            this.directoryMapping.put(dir.getPath(), new ArrayList<LibraryNode>());
            shareDirectoryInGroup(dir);
            library.addAllFiles(directoryMapping,filesMapping);
            displayLog("[+]All Content are Successfully Shared :-)");
            dir.setStatus("");
        }
        
        private void shareDirectoryInGroup(LibraryNode dirNode) {
        
            LibEntry dirTmp = dirNode.getNode();
            
            //sharing all files in specified parentNode
            File[] list = dirTmp.listFiles();
            for(int i =0;i < list.length;i++) {
                //If rFile found is a rFile
                if (list[i].isFile()) {
                    shareFileInGroup(dirNode,list[i]);
                }
                //If this is a parentNode
                else if(list[i].isDirectory()){
                    //Parsing sub-directories...
                    LibraryNode parentDir = new LibraryNode(this.group, dirNode,list[i],false);
                    this.directoryMapping.put(parentDir.getPath(), new ArrayList<LibraryNode>());
                    this.directoryMapping.get(dirNode.getPath()).add(parentDir);
                    shareDirectoryInGroup(parentDir);
                }
            }

            //Library.storeLibrary(mySharedPath+System.getProperty("rFile.separator")+SHARES_FILENAME);
        }

        private void shareFileInGroup(LibraryNode dirNode, File child){
            //Avoid dealing with empty files (Mac symLinks for ex)
            if(child.length() <= 0)
                return;
            
            LibraryNode res = new LibraryNode(this.group, dirNode, child, false);
            
            //We avoid sharing ".b00m.shares" and all files begining with a "."
            if (!(child.getName().equals(".b00m.shares")) && !(child.getName().startsWith("."))) {
                    try {
                        
                        if(res != null){
                            //If the rFile doesn't exists in the dirMap
                            if(this.filesMapping.get(res.getHash()) == null){
                                //Add the rFile has a child of his directory
                                this.directoryMapping.get(dirNode.getPath()).add(res);
                                //Register the share in Share dictionnary
                                this.filesMapping.put(res.getHash(), res.getPath());
                                parsedSize += child.length();
                                displayLog("[+]Sharing a new file in Group "+groupName);
                                displayLog("[.]    Name     : " + res.getName());
                                displayLog("[.]    Checksum : " + res.getHash().substring(0,15) +"..."+ res.getHash().substring((res.getHash().length()-15)));
                                displayLog("[.]    Type     : " + res.getType());
                                displayLog("[.]    Category : " + res.getCategory());
                                
                                //If we have parsed more than 100elements or greater than 1Go of data add them to library
                                if(this.filesMapping.size() > 100 || parsedSize > 1000000000){
                                    //Sharing Files and checksum localy in a catalog
                                    library.addAllFiles(directoryMapping,filesMapping);
                                    
                                    //reinitialize environnment
                                    parsedSize = 0;
                                    filesMapping.clear();
                                    
                                    //Update table in case of GUI
                                    if(Kb00m.GUI_MODE)
                                        GUI.updateShareTable();
                                }
                                
                            }else{
                                displayLog("[!]Share "+ res.getName() +" already exists");
                            }
                        }
                        else{
                                displayLog("[!]Null exception while parsing directory "+dirNode.getName());
                            }
                        
                    } catch (Exception ex) {
                        displayLog("[!]Error while adding file "+child.getName()+" to share.\n[!]" + ex.getMessage());
                    }
                }
        }
    }
    

    //This method will start listening for incoming messages throught created pipe
    public void startListening() 
    {
        propagatePipeAdv = Tools.getPipeAdvertisement(group.getPeerGroupID(),(groupName+"M:SHARE"), true);
        
        displayLog("[+]Start Listening for Share service Incoming Messages.");
        try {
            inPipe = pipeSrvc.createInputPipe(Tools.getPipeAdvertisement(group.getPeerGroupID(),(group.getPeerName()+":SHARE"), false),this);
            displayLog("[+]Creation successful of the unicast share listening inputPipe with ID: "+inPipe.getPipeID());
            inPipeM = pipeSrvc.createInputPipe(propagatePipeAdv, this);
            displayLog("[+]Creation successful of the multicast share listening inputPipe with ID: "+inPipeM.getPipeID());
            //propagateMultPipe = new JxtaMulticastSocket(group, propagatePipeAdv);
            //displayLog("[+]Creation successful of the multicast socket share listening Pipe with ID: "+propagateMultPipe);
        } catch (IOException ioe) {
            displayLog("[!]IOException when registring share input pipe\n" + ioe.getMessage());
        }
        if (inPipe == null) {
            displayLog("[!]Failure in Opening Share Input Pipe :-(");
            System.exit(-1);
        }
        
    }
    
    public void searchFile(String pattern, String fCat, boolean track) {
        
//        searchResults = result;
        
        
        //If the search is already going on just skip
        if(this.searchThread.containsKey(fCat+":"+pattern))
            return;
        
        DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
        Date date = new Date();
        String myTime = dateFormat.format(date).toString();
        
        
        
        Message myMessage = new Message();
        //adding timestap and peers details also messages to XML tag and send them 
        StringMessageElement sme = new StringMessageElement("PeerName", group.getPeerName(), null);
        StringMessageElement sme1 = new StringMessageElement("SessionID", group.getPeerID().toString()+myTime, null);
        StringMessageElement sme2 = new StringMessageElement("SearchPattern", pattern, null);
        StringMessageElement sme3 = new StringMessageElement("Type", "SEARCH", null);
        StringMessageElement sme4 = new StringMessageElement("Time", myTime, null);
        StringMessageElement sme5 = new StringMessageElement("FileCategory", fCat, null);
        
        myMessage.addMessageElement(sme);
        myMessage.addMessageElement(sme1);
        myMessage.addMessageElement(sme2);
        myMessage.addMessageElement(sme3);
        myMessage.addMessageElement(sme4);
        myMessage.addMessageElement(sme5);
        
        //DatagramPacket mdpkt = new DatagramPacket(pattern.getBytes(),pattern.length());
//        try {
        if(track){
            displayLog("[+]Creating Tracking search OutPutPipe");
            SearchThread search = new SearchThread(fCat+":"+pattern, myMessage, new Long(10000));
            search.start();
            this.searchThread.put(fCat+":"+pattern, search);
//            this.searchSession.put(pattern,group.getPeerID().toString()+myTime);
            
            this.searchResults.addSearch(fCat+":"+pattern);
//            TrackThread search = new TrackThread(fCat+":"+pattern, myMessage);
//            search.start();
//            this.searchThread.put(fCat+":"+pattern, search);
////            this.searchSession.put(pattern,group.getPeerID().toString()+myTime);
//            
//            this.searchResults.addSearch(fCat+":"+pattern);
        }else{
        
            displayLog("[+]Creating Share search OutPutPipe");
            SearchThread search = new SearchThread(fCat+":"+pattern, myMessage, new Long(10000));
            search.start();
            this.searchThread.put(fCat+":"+pattern, search);
//            this.searchSession.put(pattern,group.getPeerID().toString()+myTime);
            
            this.searchResults.addSearch(fCat+":"+pattern);
        }
        
        
        
    }

    protected void makeTracker(SearchNode node){
        displayLog("[+]Tracking search "+node.getPattern());
        this.trackList.add(node.getPattern());
    }
    
    protected void revertTracker(SearchNode node){
        displayLog("[+]Stop tracking search "+node.getPattern());
        this.trackList.remove(this.trackList.indexOf(node.getPattern()));
    }
    
    protected void downloadFile(String context, SearchNode node){
        //Add a download to download manager
        dManager.addDownload(context, node);
        
        //If we are in graphical context, update table
        if(Kb00m.GUI_MODE)
            GUI.updateDownloadTable();
    }
    
    //If we have found an entry for the rFile requested we send back infos about it
    private void sendSearchAnswer(String sender, String senderID, String pattern, LibraryNode share){
        //Shares share = Library.getShareByName(groupName, shareName);
        //String [] result = {msgSender,"Type","File Name" , "Size Bytes","CheckSum(SHA-256)"};

        DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
        Date date = new Date();
        String myTime = dateFormat.format(date).toString();
        
        Message myMessage = new Message();
        //adding timestap and peers details also messages to XML tag and send them 
        StringMessageElement sme = new StringMessageElement("PeerName", group.getPeerName(), null);
        StringMessageElement sme1 = new StringMessageElement("SessionID", senderID, null);
        StringMessageElement sme2 = new StringMessageElement("SearchPattern", pattern, null);
        StringMessageElement sme3 = new StringMessageElement("FileName", share.getName(), null);
        StringMessageElement sme4 = new StringMessageElement("FileCategory", share.getCategory(), null);
        StringMessageElement sme5 = new StringMessageElement("FileHash", share.getHash(), null);
        StringMessageElement sme6 = new StringMessageElement("FileSize", share.getSize().toString(), null);
        StringMessageElement sme7 = new StringMessageElement("Type", "SEARCH:RESULT", null);
        StringMessageElement sme8 = new StringMessageElement("Time", myTime, null);

        myMessage.addMessageElement(sme);
        myMessage.addMessageElement(sme1);
        myMessage.addMessageElement(sme2);
        myMessage.addMessageElement(sme3);
        myMessage.addMessageElement(sme4);
        myMessage.addMessageElement(sme5);
        myMessage.addMessageElement(sme6);
        myMessage.addMessageElement(sme7);
        myMessage.addMessageElement(sme8);
        
        //String msg = "ANSWER:"+share.getName()+":";
        
        //DatagramPacket mdpkt = new DatagramPacket(msg.getBytes(),msg.length());
        try {
            displayLog("[+]Creating Share answer OutPutPipe");
            propagatePipe = pipeSrvc.createOutputPipe(Tools.getPipeAdvertisement(group.getPeerGroupID(),sender+":SHARE", false), TIMEOUT);
            
            boolean send = propagatePipe.send(myMessage);
            if(send){
                displayLog("[+]Answer sent: " + share.getName() + "\n[+]To Peer: " + sender + "\n[+]With ID: " + group.getPeerGroupID().toString());
                
            }else displayLog("[!]Impossible to send the search answer");
            propagatePipe.close();
            
        } catch (IOException ioe) {
            displayLog("[!]IOError has sending search answer <" + share.getName() + ">\n"+ioe.getMessage());
            
        }
        
    }
    
    class SearchThread implements Runnable {

        private Message searchMsg = null;
        private String tname = null;
        private Thread t = null;
        private boolean suspendFlag = false;
        private boolean runFlag = true;
        private Long TTIMEOUT = new Long(1000);
        
        SearchThread(String threadName, Message m, Long time) {
            tname = threadName;
            searchMsg = m;
            suspendFlag = false;
            if(time>TTIMEOUT)
                TTIMEOUT = time;
        }
        
        public void start() {
            t = new Thread(this, tname);
            t.start();
        }
        
        @Override
        public void run() {
            
            while (runFlag) {
                try {
                    try {
                        // start searching the data in whole group
                        propagatePipe = pipeSrvc.createOutputPipe(propagatePipeAdv, TIMEOUT);
                        boolean send = propagatePipe.send(searchMsg);
                        //propagateMultPipe.send(mdpkt);

                        if (send) {
                            displayLog("[+]Request sent: " + searchMsg.getMessageElement("SearchPattern") + "\n[+]To Group: " + groupName + "\n[+] with ID: " + group.getPeerGroupID().toString());
                            
                        } else {
                            displayLog("[!]Impossible to send the search request");
                        }
                        
                        propagatePipe.close();
                    } catch (IOException ex) {
                        Logger.getLogger(Share.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    Thread.sleep(TTIMEOUT);
                    synchronized (this) {
                        while (suspendFlag) {
                            wait();
                        }
                    }
                } catch (InterruptedException e) {
                    displayLog("[!]Search thread " + t.getName() + " interrupted.");
                }
            }
            
            
            
            displayLog("[+]closing search thread " + t.getName());
        }
        
        void mysuspend() {
            displayLog("[!]Search thread "+t.getName() + " suspended.");
            suspendFlag = true;
        }

        synchronized void myresume() {
            displayLog("[+]Resuming search thread "+t.getName() + ".");
            suspendFlag = false;
            notify();
        }
        
        synchronized void mystop() {
            displayLog("[!]Search thread "+t.getName() + " stopped.");
            suspendFlag = false;
            runFlag = false;
            notify();
        }
        
        protected boolean isSearching(){
            return !suspendFlag;
        }
    }
 
    protected void suspendUpload(String threadName){
        this.dManager.suspendUpload(threadName);
    }
    
    protected void resumeUpload(String threadName){
        this.dManager.resumeUpload(threadName);
    }
    
    protected void suspendSearch(String threadName){
        SearchThread s = this.searchThread.get(threadName);
        if(s != null && s.t.isAlive() && s.isSearching()){
            s.mysuspend();
        }
        else{
            displayLog("[!]"+threadName + " search thread not found.");
        }
    }
    
    protected void resumeSearch(String threadName){
        SearchThread s = this.searchThread.get(threadName);
        if(s != null && s.t.isAlive()){
            s.myresume();
        }
        else{
            displayLog("[!]"+threadName + " search thread not found.");
        }
    }
    
    protected void stopSearch(String threadName){
        SearchThread s = this.searchThread.get(threadName);
        if(s != null && s.t.isAlive()){
            s.mystop();
        }
        else{
            displayLog("[!]"+threadName + " search thread not found.");
        }
        this.searchThread.remove(threadName);
    }
        
    //Remove all occurences of user's shares found
    protected void removeSeederShares(String user){
        this.searchResults.removeSeederShares(user);
    }
    
    //Remove all occurences of user as seeder found
    protected void removeSeederResults(String user){
        this.dManager.removeSeeder(user);
    }
    
    protected PartTransfertModel getUploadModel(){
        return this.dManager.getUploadModel();
    }
    
    protected PartTransfertModel getDownloadModel(){
        return this.dManager.getDownloadModel();
    }
    
    protected SearchModel getSearchModel(){
        return this.searchResults;
    }
    
    protected LibraryModel getLibraryModel(){
        return this.library;
    }
    
    protected LibraryModel getPrivateLibraryModel(){
        return this.privateLibrary;
    }
    
    protected void seenDownload(){
        this.dManager.seenDownload();
    }
    
    protected int getNbNewDownload(){
        return this.dManager.getNbNewDownload();
    }
    
    protected void stopListening() //This method will stop input pipe
    {
        inPipe.close();
        inPipeM.close();
//        propagateMultPipe.close();
        displayLog("[-]Input Pipe Closed for Incomming Share Message.\n");
    }
    
    
    //Receiving a message: either a search message, or a download status
    @Override
    public void pipeMsgEvent(PipeMsgEvent msgEvent) {
        Message msg = null;

        String msgType = null;
        String msgSender = null;
        String msgSenderID = null;
        String msgPattern = null;
        String msgFileName = null;
        String msgFileCategory = null;
        String msgFileHash = null;
        String msgFileSize = null;
        String msgPipeID = null;
        String msgStatus = null;
        String msgTime = null;
        
        try {
            
            //Parsing the message in order to find the good values ;)
            msg = msgEvent.getMessage();
            MessageElement typeElement = msg.getMessageElement("Type");
            msgType = typeElement.toString();
            MessageElement senderElement = msg.getMessageElement("PeerName");
            msgSender = senderElement.toString();
            MessageElement senderIDElement = msg.getMessageElement("SessionID");
            msgSenderID = senderIDElement.toString();
            MessageElement searchElement = msg.getMessageElement("SearchPattern");
            if(searchElement!=null) msgPattern = searchElement.toString();
            MessageElement fileNameElement = msg.getMessageElement("FileName");
            if(fileNameElement!=null) msgFileName = fileNameElement.toString();
            MessageElement fileCatElement = msg.getMessageElement("FileCategory");
            if(fileCatElement!=null) msgFileCategory = fileCatElement.toString();
            MessageElement fileHashElement = msg.getMessageElement("FileHash");
            if(fileHashElement!=null) msgFileHash = fileHashElement.toString();
            MessageElement fileSizeElement = msg.getMessageElement("FileSize");
            if(fileSizeElement!=null) msgFileSize = fileSizeElement.toString();
            MessageElement pipeIDElement = msg.getMessageElement("PipeID");
            if(pipeIDElement!=null) msgPipeID = pipeIDElement.toString();
            MessageElement statusElement = msg.getMessageElement("Status");
            if(statusElement!=null) msgStatus = statusElement.toString();
            MessageElement timeElement = msg.getMessageElement("Time");
            if(timeElement!=null) msgTime = timeElement.toString();
            
        } catch (Exception e) {
            displayLog("[!]Error while parsing message received\n" + e.getMessage());
            return;
        }
        
        //Don't deal with our own messages or strange ones without sender
        if(msgSender == null || msgSender.equals(group.getPeerName())) return;

        //Check if the Type is defined
        if(msgType!=null){
            //If we are receiving a search request
            if(msgType.equals("SEARCH") && msgSenderID != null && msgPattern != null && msgTime != null){
//                try {
                    if(msgPattern.length() < Kb00m.PATTERN_LEN){
                        displayLog("[!]Pattern too small, DoS possiblity, aborting.");
                        return;
                    }else{
                        displayLog("[+]Peer "+msgSender+" is looking for "+msgPattern + " of type: "+msgFileCategory);
                        //Searching localy if we had this rFile
                        List<LibraryNode> results = this.library.searchInLibrary(msgPattern, msgFileCategory);
                        for(LibraryNode result : results){
                            //If we found a local result for this search, send back a message telling it
                            displayLog("[.]Found local share: "+result.getName());
                            if(result!=null) sendSearchAnswer(msgSender,msgSenderID,msgPattern,result);
                        }
                    }
//                } catch (IOException ex) {
//                    displayLog("[!]IOError has searching share "+msgPattern+"\n" + ex.getMessage());
//                }
            }
            //If we are receiving a result from a search in progress
            else if (msgType.equals("SEARCH:RESULT") && msgSenderID != null && msgFileName != null && msgPattern != null && msgFileHash != null && msgTime != null){
                if(this.searchThread.containsKey(msgFileCategory+":"+msgPattern) && this.searchThread.get(msgFileCategory+":"+msgPattern).isSearching()){
                    displayLog("[+]Search "+msgFileCategory+":"+msgPattern+" has a result from "+msgSender+"\n[+]He is sharing "+msgFileName);
                        SearchNode parentSearch = this.searchResults.getSearchNode(msgFileCategory+":"+msgPattern);
                        ShareEntry shareFound = new ShareEntry(msgSender, msgFileName , new Long(msgFileSize), msgFileHash);
//                        SearchNode res = new SearchNode("", shareFound, null,Kb00m.FOLLOW);
                        //If the search exists but the results hasn't been received yet
                        if((parentSearch != null && shareFound != null)){
                            if(!this.searchResults.isInSearchList(parentSearch)){
                                displayLog("[!]We are not searching this file... aborting");
                                return;
                            }
                            if(this.searchResults.isInShareResults(parentSearch, shareFound)){
                                displayLog("[!]This share is already known");
                            }
                            else{
                                displayLog("[+]Adding to search node "+parentSearch.getPattern());
                                this.searchResults.addShare(parentSearch, shareFound);
                                //Reload share table if we are in graphical context
                                if(Kb00m.GUI_MODE) updateSearchTable();
                            }
                        }
                }
                else if(this.searchThread.containsKey("All:"+msgPattern) && this.searchThread.get("All:"+msgPattern).isSearching()){
                        displayLog("[+]Search All:"+msgPattern+" has a result from "+msgSender+"\n[+]He is sharing "+msgFileName);
                        SearchNode parentSearch = this.searchResults.getSearchNode("All:"+msgPattern);
                        ShareEntry shareFound = new ShareEntry(msgSender, msgFileName , new Long(msgFileSize), msgFileHash);
//                        SearchNode res = new SearchNode("", shareFound, null,Kb00m.FOLLOW);
                        //If the search exists but the results hasn't been received yet
                        if((parentSearch != null && shareFound != null)){
                            if(!this.searchResults.isInSearchList(parentSearch)){
                                displayLog("[!]We are not searching this file... aborting");
                                return;
                            }
                            if(this.searchResults.isInShareResults(parentSearch, shareFound)){
                                displayLog("[!]This share is already known");
                            }
                            else{
                                displayLog("[+]Adding to search node "+parentSearch.getPattern());
                                SearchNode tmp = this.searchResults.addShare(parentSearch, shareFound);
                                //If this search is a tracking search
                                if(this.trackList.contains(parentSearch.getPattern())){
                                    displayLog("[+]This search is a tracker... downloading result !!");
                                    //We automatically download the file
                                    downloadFile(this.groupName,tmp);
                                    //And update his status
                                    this.searchResults.setSearchStatus(tmp, Kb00m.DOWNLOAD);
                                }
                                
                                //Reload share table if we are in graphical context
                                if(Kb00m.GUI_MODE) updateSearchTable();
                            }
                        }
                }else{
                    displayLog("[!]No search node found locally, aborting");
                }
            }
            
            //If the Type is unknown we display the error in log
            else{
                displayLog("[!]Error: unknown type Search message received\n[FOUND]: " + msg.toString());
            }
        }
        //If the Type has not been defined we display an error in log
        else{
            displayLog("[!]Error: invalid Pipe message received\n" + msg.toString());
        }
 
    }
    
    
    //Listener for monitoring Content and search in Group
    @Override 
    public void discoveryEvent(DiscoveryEvent event)
    {
        DiscoveryResponseMsg res = event.getResponse();
//        String rFile = "unknown";
//        String rHash = "init";
//        
//        boolean isInList = false;
        ContentShareAdvertisement contentAdv = (ContentShareAdvertisement) res.getAdvertisements();
        if(contentAdv != null){
            displayLog("[+][Content Received]: " + contentAdv.getContentAdvertisement());
//            if(this.library.isInLibrary())
            
            //file = contentAdv.getContentID();
            //hash = contentAdv.getDescription();
        }
    }
    
    private static void updateSearchTable(){
        GUI.updateSearchTable();
    }
  
}
