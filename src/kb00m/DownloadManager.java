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

import java.awt.Toolkit;
import java.io.*;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.jxta.discovery.DiscoveryService;
import net.jxta.endpoint.Message;
import net.jxta.endpoint.MessageElement;
import net.jxta.endpoint.StringMessageElement;
import net.jxta.peergroup.PeerGroup;
import net.jxta.pipe.*;
import net.jxta.protocol.PipeAdvertisement;
import net.jxta.socket.JxtaServerSocket;
import net.jxta.socket.JxtaSocket;


public class DownloadManager extends Thread implements PipeMsgListener, Runnable {

    private DiscoveryService discoService = null;
    private PipeService pipeSrvc;
    private PipeAdvertisement propagatePipeAdv = null;
    private PeerGroup group = null;
    private String groupName = null;
    private OutputPipe propagatePipe = null;
    private InputPipe inPipe = null,
            inPipeM = null;
    private final static int TIMEOUT = 5 * 1000;
    private File myDownloadedPath = null;
    private LibraryModel library = null;
    private LibraryModel privateLibrary = null;
    private PartTransfertModel downloadList = null;
    private PartTransfertModel uploadList = null;
    private Integer nbUnotifiedDownload = 0;
    private HashMap<String, DownloaderThread> downloadThreadList = new HashMap();
    private HashMap<String, UploadThread> uploadThread = new HashMap();
    private HashMap<String, ArrayList<String>> seederDic = new HashMap();
    private HashMap<String, List<Part>> filePartDic = new HashMap();
    HashMap<String, ArrayList<Part>> warRoom = new HashMap<String, ArrayList<Part>>();
    private final static long BLOCK_SIZE = 4096;
    protected static final int BUFFER_SIZE = 4096;
    protected static final long MIN_DOWNLOAD_SIZE = BLOCK_SIZE * 1000;
    private final static int NUM_THREAD = 1;
    private final static int NUM_RETRY_CONNECTION = 3;
    private final static int MIN_CLIENT = 1;
    private final static int MAX_CLIENT = 10;

    public DownloadManager(PeerGroup grp, File downloadDirectory, LibraryModel pLibrary, LibraryModel gLibrary) {
        //Initialize global variable
        myDownloadedPath = downloadDirectory;
        group = grp;
        groupName = group.getPeerGroupName();
        library = gLibrary;
        privateLibrary = pLibrary;
        uploadList = new PartTransfertModel();
        downloadList = new PartTransfertModel();


        displayLog("[+]Retrieve the Download Discovery Service");
        discoService = group.getDiscoveryService();
        pipeSrvc = group.getPipeService();

    }

    private void displayLog(String s) {
        if (Kb00m.GUI_MODE) {
            GUI.addLogMsg(s);
        } else {
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
            displayLog("[!]IOException while publishing Download pipe advertisement!!");
        }

        displayLog("[+]Download service sucessfully started !!\n");
    }

    //This method will start listening for incoming messages throught created pipe
    public void startListening() {
        propagatePipeAdv = Tools.getPipeAdvertisement(group.getPeerGroupID(), (groupName + "M:DOWNLOAD"), true);

        displayLog("[+]Start Listening for Download service Incoming Messages.");
        try {
            inPipe = pipeSrvc.createInputPipe(Tools.getPipeAdvertisement(group.getPeerGroupID(), (group.getPeerName() + ":DOWNLOAD"), false), this);
            displayLog("[+]Creation successful of the unicast download listening inputPipe with ID: " + inPipe.getPipeID());
            inPipeM = pipeSrvc.createInputPipe(propagatePipeAdv, this);
            displayLog("[+]Creation successful of the multicast download listening inputPipe with ID: " + inPipeM.getPipeID());
            //propagateMultPipe = new JxtaMulticastSocket(group, propagatePipeAdv);
            //displayLog("[+]Creation successful of the multicast socket share listening Pipe with ID: "+propagateMultPipe);
        } catch (IOException ioe) {
            displayLog("[!]IOException when registring download input pipe\n" + ioe.getMessage());
        }
        if (inPipe == null) {
            displayLog("[!]Failure in Opening Download Input Pipe :-(");
            System.exit(-1);
        }

    }

    protected PartTransfertModel getUploadModel() {
        return this.uploadList;
    }

    protected PartTransfertModel getDownloadModel() {
        return this.downloadList;
    }

    protected void seenDownload() {
        if (this.nbUnotifiedDownload > 0) {
            this.nbUnotifiedDownload = 0;
        }
        if (Kb00m.OS_NAME.contains("mac")) {
            com.apple.eawt.Application.getApplication().setDockIconBadge(null);
        }
    }

    protected int getNbNewDownload() {
        return this.nbUnotifiedDownload;
    }

    protected void addDownload(String context, SearchNode node) {
        LibraryNode tmp = this.library.isInLibrary(node.getHash());
        if (tmp != null) {
            //The rFile already exists in our shared directory
            displayLog("[!]The requested file " + node.getName() + " is already in your directory\n[!]At: " + tmp.getPath());
            return;
        }

        //If the rFile doesn't exists... the usual case ;)
        if (downloadThreadList.get(node.getHash()) == null) {
            downloadThreadList.put(node.getHash(), new DownloaderThread(context, node));
            downloadThreadList.get(node.getHash()).start();
        } else {
            displayLog("[!]This file is already being downloaded, please check your peering table");
        }

        //Removing ourselves from thread list has download is finished
//        downloadThreadList.remove(node.getHash());

    }

    //Remove a seeder from all occurence it can be found
    protected void removeSeeder(String seeder) {
        for (String h : seederDic.keySet()) {
            seederDic.get(h).remove(seeder);
        }
    }

    private void uploadFile(final String pipeId, final String dest, final String hash, final File file, Part p) {
        displayLog("[+]Starting upload thread of " + file.getName() + " part #" + p.getNumPart().toString() + "\n[+]To dest:" + dest);
        ShareEntry s = new ShareEntry(dest, file.getName() + " part#" + p.getNumPart().toString(), file.length(), hash);
        this.uploadList.addtransfert(dest, s);
        this.uploadList.updateTransferStatus(file.getName() + " part#" + p.getNumPart().toString(), "Uploading");

//        if(Kb00m.GUI_MODE)
//                GUI.updateUploadTable();

        UploadThread upload = new UploadThread(file.getName() + dest, dest, hash, file, p);
        this.uploadThread.put(file.getName() + "#" + p.getNumPart().toString() + "@" + dest, upload);
        upload.start();


    }

    @Override
    public void pipeMsgEvent(PipeMsgEvent pme) {
        Message msg = null;

        String msgType = null;
        String msgSender = null;
        String msgSenderID = null;
        String msgFileName = null;
        String msgFilePart = null;
        String msgFileStart = null;
        String msgFileEnd = null;
        String msgFileCategory = null;
        String msgFileHash = null;
        String msgPipeID = null;
        String msgStatus = null;
        String msgTime = null;

        try {

            //Parsing the message in order to find the good values ;)
            msg = pme.getMessage();
            MessageElement typeElement = msg.getMessageElement("Type");
            msgType = typeElement.toString();
            MessageElement senderElement = msg.getMessageElement("PeerName");
            msgSender = senderElement.toString();
            MessageElement senderIDElement = msg.getMessageElement("SessionID");
            msgSenderID = senderIDElement.toString();
            MessageElement fileNameElement = msg.getMessageElement("FileName");
            if (fileNameElement != null) {
                msgFileName = fileNameElement.toString();
            }
            MessageElement filePartElement = msg.getMessageElement("FilePart");
            if (filePartElement != null) {
                msgFilePart = filePartElement.toString();
            }
            MessageElement fileStartElement = msg.getMessageElement("FileStart");
            if (fileStartElement != null) {
                msgFileStart = fileStartElement.toString();
            }
            MessageElement fileEndElement = msg.getMessageElement("FileEnd");
            if (fileEndElement != null) {
                msgFileEnd = fileEndElement.toString();
            }
            MessageElement fileCatElement = msg.getMessageElement("FileCategory");
            if (fileCatElement != null) {
                msgFileCategory = fileCatElement.toString();
            }
            MessageElement fileHashElement = msg.getMessageElement("FileHash");
            if (fileHashElement != null) {
                msgFileHash = fileHashElement.toString();
            }
            MessageElement pipeIDElement = msg.getMessageElement("PipeID");
            if (pipeIDElement != null) {
                msgPipeID = pipeIDElement.toString();
            }
            MessageElement statusElement = msg.getMessageElement("Status");
            if (statusElement != null) {
                msgStatus = statusElement.toString();
            }
            MessageElement timeElement = msg.getMessageElement("Time");
            if (timeElement != null) {
                msgTime = timeElement.toString();
            }

        } catch (Exception e) {
            displayLog("[!]Error while parsing message received\n" + e.getMessage());
            return;
        }

        //Don't deal with our own messages or strange ones without sender
        if (msgSender == null || msgSender.equals(group.getPeerName())) {
            return;
        }

        //Check if the Type is defined
        if (msgType != null) {
            // If we are receiving a notification about an open pipe
            if (msgType.startsWith("DOWNLOAD:") && msgPipeID != null && msgFileName != null && msgFileHash != null && msgTime != null && msgFileStart != null && msgFileEnd != null) {
                String[] typeTMP = msgType.split(":");
                String groupContext = typeTMP[1];

                String tmpFilePath = null;
//                try {
                displayLog("[+]We've receive a listening Pipe notification from " + msgSender + "\n[+]He wants " + msgFileName + " in context: " + groupContext);
                //If we are searching throught the open group
                if (groupContext.equalsIgnoreCase(group.getPeerGroupName())) {
                    displayLog("[+]Searching group " + group.getPeerGroupName() + " library...");
                    LibraryNode tmpNode = this.library.isInLibrary(msgFileHash);
                    if (tmpNode != null) {
                        displayLog("[+]File found in our group library\n[+]File at: " + tmpNode.getPath());
                        tmpFilePath = tmpNode.getPath();
                    } else {
                        displayLog("[!]We received a listening Pipe notification for a file we don't have... weird -> aborting !!");
                        return;
                    }

                } //If we are requesting a private shared rFile
                else if (groupContext.equals("PRIVATE")) {
                    displayLog("[+]Searching private library...");
                    List<LibraryNode> tmpList = this.privateLibrary.searchInLibrary(msgFileName, msgFileCategory);
                    tmpFilePath = tmpList.get(0).getPath();
                    displayLog("[+]File found in our private library\n[+]File at: " + tmpFilePath);
                } else {
                    //If we are searching in a different group just break and return;
                    return;
                }
                //If the result found is still valid on the computer (rFile exists)
                File tmp = new File(tmpFilePath);
                if (tmp.exists() && msgStatus.equals("READY")) {
                    displayLog("[+]Upload " + tmpFilePath + " part#" + msgFilePart + " to " + msgPipeID);
                    displayLog("[+]Upload from offset " + msgFileStart + " to " + msgFileEnd);
                    uploadFile(msgPipeID, msgSender, msgFileHash, tmp, new Part(new Integer(msgFilePart), new Long(msgFileStart), new Long(msgFileEnd)));
                } else {
                    displayLog("[!]Error: The file requested has been found in library (" + tmpFilePath + "), but doesn't exists anymore !!");
                }
//                } catch (IOException ex) {
//                    displayLog("[!]IOError has opening "+msgFileName+" to upload\n"+ex.getMessage());
//                }
            } else if (msgType.startsWith("SEEK:") && msgSender != null && msgSenderID != null && msgFileHash != null && msgTime != null) {
                String[] typeTMP = msgType.split(":");
                String groupContext = typeTMP[1];

                String tmpFilePath = null;
                displayLog("[+]Seeder discovery from " + msgSender + " in context: " + groupContext);
                if (groupContext.equalsIgnoreCase(group.getPeerGroupName())) {
                    displayLog("[+]Searching group " + group.getPeerGroupName() + " library...");
                    LibraryNode tmpNode = this.library.isInLibrary(msgFileHash);
                    if (tmpNode != null) {
                        displayLog("[+]File found in our public group library\n[+]File at: " + tmpNode.getPath());
                        tmpFilePath = tmpNode.getPath();
                    } else {
                        displayLog("[!]We received a listening Pipe notification for a file we don't have... weird -> aborting !!");
                        return;
                    }

                } //If we are requesting a private shared rFile
                else if (groupContext.equals("PRIVATE")) {
                    displayLog("[+]Searching private library...");
                    LibraryNode tmpNode = this.privateLibrary.isInLibrary(msgFileHash);
                    if (tmpNode != null) {
                        displayLog("[+]File found in our private group library\n[+]File at: " + tmpNode.getPath());
                        tmpFilePath = tmpNode.getPath();
                    } else {
                        displayLog("[!]We received a listening Pipe notification for a file we don't have... weird -> aborting !!");
                        return;
                    }
                } else {
                    //If we are searching in a different group just break and return;
                    return;
                }
                File tmp = new File(tmpFilePath);
                if (tmp.exists()) {
                    displayLog("[+]Send seed answer " + tmp.getName() + " to " + msgSender);
                    sendSeederAnswer(groupContext, msgSenderID, msgSender, msgFileHash);
                } else {
                    displayLog("[!]Error: The file requested has been found in library (" + tmpFilePath + "), but doesn't exists anymore !!");
                }
            } else if (msgType.startsWith("SEEDER:") && msgSender != null && msgSenderID != null && msgFileHash != null && msgTime != null) {
                String[] typeTMP = msgType.split(":");
                String groupContext = typeTMP[1];

                //If the answer is not coming from one of our request just abort
                if (!msgSenderID.equals(group.getPeerID().toString())) {
                    return;
                }

                displayLog("[+]Seeder found: " + msgSender + " for hash " + msgFileHash + " in context: " + groupContext);
                //Add the seeder to our dictionnary
                this.seederDic.get(msgFileHash).add(msgSender);
            } //If the Type is unknown we display the error in log
            else {
                displayLog("[!]Error: unknown type Download message received\n[FOUND]: " + msg.toString());
            }
        } //If the Type has not been defined we display an error in log
        else {
            displayLog("[!]Error: invalid Download message received\n" + msg.toString());
        }

    }

    private void sendSeederAnswer(String context, String peerID, String peer, String hash) {
        DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
        Date date = new Date();
        String myTime = dateFormat.format(date).toString();

        Message myMessage = new Message();
        //adding timestap and peers details also messages to XML tag and send them 
        StringMessageElement sme = new StringMessageElement("PeerName", group.getPeerName(), null);
        StringMessageElement sme1 = new StringMessageElement("SessionID", peerID, null);
        StringMessageElement sme2 = new StringMessageElement("FileHash", hash, null);
        StringMessageElement sme3 = new StringMessageElement("Type", "SEEDER:" + context, null);
        StringMessageElement sme4 = new StringMessageElement("Time", myTime, null);

        myMessage.addMessageElement(sme);
        myMessage.addMessageElement(sme1);
        myMessage.addMessageElement(sme2);
        myMessage.addMessageElement(sme3);
        myMessage.addMessageElement(sme4);

        try {
            displayLog("[+]Creating seed answer OutPutPipe notification");
            propagatePipe = pipeSrvc.createOutputPipe(Tools.getPipeAdvertisement(group.getPeerGroupID(), peer + ":DOWNLOAD", false), TIMEOUT);

            boolean send = propagatePipe.send(myMessage);
            if (send) {
                displayLog("[+]Seed OutPutPipe answer sent to Peer: " + peer);

            } else {
                displayLog("[!]Impossible to send the share OutPutPipe");
            }
            propagatePipe.close();

        } catch (IOException ioe) {
            displayLog("[!]IOError has sending seed OutPutPipe answer <" + hash + "> to " + peer + "\n" + ioe.getMessage());

        }
    }

    //Thread allowing seeder discovery for a specific hash
    class SeekerThread implements Runnable {

        String remoteHash = null;
        Message seekMsg = null;
        boolean shouldRun = false;
        boolean suspendFlag = false;
        Thread t = null;

        SeekerThread(String context, String hash) {
            remoteHash = hash;

            DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
            Date date = new Date();
            String myTime = dateFormat.format(date).toString();


            seekMsg = new Message();
            //adding timestap and peers details also messages to XML tag and send them 
            StringMessageElement sme = new StringMessageElement("PeerName", group.getPeerName(), null);
            StringMessageElement sme1 = new StringMessageElement("SessionID", group.getPeerID().toString(), null);
            StringMessageElement sme2 = new StringMessageElement("FileHash", remoteHash, null);
            StringMessageElement sme3 = new StringMessageElement("Type", "SEEK:" + context, null);
            StringMessageElement sme4 = new StringMessageElement("Time", myTime, null);

            seekMsg.addMessageElement(sme);
            seekMsg.addMessageElement(sme1);
            seekMsg.addMessageElement(sme2);
            seekMsg.addMessageElement(sme3);
            seekMsg.addMessageElement(sme4);


        }

        public void start() {
            shouldRun = true;
            t = new Thread(this, "SeederSeek:" + remoteHash);
            t.start();
        }

        @Override
        public void run() {
//            synchronized (this) {
            while (shouldRun) {
                while (seederDic.get(remoteHash).size() < MIN_CLIENT) {
                    displayLog("[+]Sending hash seeder seek message");
                    try {
                        try {
                            propagatePipe = pipeSrvc.createOutputPipe(propagatePipeAdv, TIMEOUT);
                            boolean send = propagatePipe.send(seekMsg);
                            if (send) {
                                displayLog("[+]Seeker OutPutPipe status sent");

                            } else {
                                displayLog("[!]Impossible to send the seek OutPutPipe");
                            }
                            propagatePipe.close();
                        } catch (IOException ex) {
                            displayLog("[!]IOException while creating output pipe in SeekerThread " + ex.getMessage());
                        }
                        //Wait until sending hash seeker again
                        t.sleep(5000);
                    } catch (InterruptedException ex) {
                        displayLog("[!]Interrupted seeker Thread for hash " + remoteHash + " with error: " + ex.getMessage());
                    }
                }
                try {
                    //Wait until checking seeder count again
                    t.sleep(5000);
                } catch (InterruptedException ex) {
                    displayLog("[!]Interrupted seeker Thread seeder looking\n[+]For hash " + remoteHash + " with error: " + ex.getMessage());
                }
                synchronized (this) {
                    while (suspendFlag) {
                        try {
                            wait();
                        } catch (InterruptedException ex) {
                            displayLog("[!]Interrupted seeker Thread seeder sleeping\n[+]For hash " + remoteHash + " with error: " + ex.getMessage());
                        }
                    }
                }
            }
            displayLog("[+]Seeder seeker ended gracefully");
//            }

        }
        
        protected boolean isSearching(){
            return !suspendFlag;
        }
        
        protected void mysuspend() {
            displayLog("[!]Seeker Thread " + t.getName() + " suspended.");
            suspendFlag = true;
        }

        protected synchronized void myresume() {
            displayLog("[+]Resuming " + t.getName() + " seeker Thread.");
            suspendFlag = false;
            notify();
        }

        protected synchronized void mystop() {
            shouldRun = false;
            notify();
        }
        
    }

    //Thread able to dispatch download parts beetween seeder found for a hash
    class DownloaderThread extends Thread implements Runnable {

        SearchNode node = null;
        String context = null;
        boolean shouldRun = true;
        boolean dispatchDone = false;
        Thread t = null;
        SeekerThread sT = null;
        HashMap<String, SimpleServer> srvList = new HashMap<String, SimpleServer>();
        ArrayList<PipeAdvertisement> socketAdvList = new ArrayList<PipeAdvertisement>();

        DownloaderThread(String c, SearchNode n) {
            context = c;
            node = n;

            //Declare new entry in our seeder dictionnary if it is not existing
            if (!seederDic.containsKey(node.getHash())) {
                seederDic.put(node.getHash(), new ArrayList<String>());
            }

            //Add global download to download list
            downloadList.addtransfert(node.getSeeder(), node.getNode());

            //Create a new entry for this file Hash
            if (!filePartDic.containsKey(node.getHash())) {
                filePartDic.put(node.getHash(), new ArrayList<Part>());
            }

            int i = 1;
            Part p = null;
            //If the file size if les than minimum download size
            if (node.getSize() <= MIN_DOWNLOAD_SIZE) {
                p = new Part(i, new Long(0), node.getSize());
                filePartDic.get(node.getHash()).add(p);
//            downloadList.addpart(node.getName(), p.getNumPart(), new ShareEntry(node.getSeeder(),node.getName()+" part#"+p.getNumPart().toString(),node.getSize(),node.getHash()));
            } //If the file is bigger than min download size, construct parts
            else {
                // start/end Byte for each part
                long startByte = 0;
                long endByte = MIN_DOWNLOAD_SIZE - 1;
                p = new Part(i, startByte, endByte);
                filePartDic.get(node.getHash()).add(p);
//            downloadList.addpart(node.getName(), p.getNumPart(), new ShareEntry(node.getSeeder(),node.getName()+" part#"+p.getNumPart().toString(),node.getSize(),node.getHash()));
                i++;
                while (endByte < node.getSize()) {
                    startByte = endByte + 1;
                    endByte += MIN_DOWNLOAD_SIZE;
                    //If the addition exceed total size, modify it
                    if (endByte > node.getSize()) {
                        endByte = node.getSize();
                    }
                    p = new Part(i, startByte, endByte);
                    //If filePartDic doesn't contain this part, just add it
                    if (!filePartDic.get(node.getHash()).contains(p)) {
                        filePartDic.get(node.getHash()).add(p);
                    }
//                downloadList.addpart(node.getName(), p.getNumPart(), new ShareEntry(node.getSeeder(),node.getName()+" part#"+p.getNumPart().toString(),node.getSize(),node.getHash()));
                    i++;
                }
            }

            displayLog("[+]File: " + node.getName() + " split in " + (i - 1) + " parts");


//        displayLog("[+]Creating fake file: "+node.getName());
//        try {
//               RandomAccessFile f = new RandomAccessFile(myDownloadedPath.getCanonicalPath() + Kb00m.OS_FILE_SEPARATOR + node.getName(), "rw");
//               f.setLength(node.getSize());
//               f.close();
//          } catch (IOException e) {
//               displayLog("[!]IOError while creating fake file "+node.getName()+" "+e.getMessage());
//          }

            downloadList.updateTransferStatus(node.getName(), "Searching seeder...");
//            if (Kb00m.GUI_MODE) {
//                GUI.updateDownloadTable();
//            }

            //Start a seeder seeker thread
            sT = new SeekerThread(context, n.getHash());
            sT.start();

        }

        public void start() {
            t = new Thread(this, "DownloadManager:" + node.getHash());
            t.start();
        }

        @Override
        public void run() {

            try {

                File tmp = new File(myDownloadedPath.getCanonicalPath() + Kb00m.OS_FILE_SEPARATOR + node.getName());

//                synchronized (this) {
                while (shouldRun) {
                    if (seederDic.get(node.getHash()).size() < MIN_CLIENT) {
                        downloadList.updateTransferStatus(node.getName(), "Searching seeder...");
                        try {
                            //Wait 1sec before checking seeder count again
                            displayLog("[+]Searching seeders for file " + node.getName());
                            Thread.sleep(1000);
                        } catch (InterruptedException ex) {
                            displayLog("[!]Interrupted downloaderThread waiting for seeders with error: " + ex.getMessage());
                        }
                    } else if (!dispatchDone) {
//                        else{   
                        downloadList.updateTransferStatus(node.getName(), "Downloading...");

                        int numSeed = 1;
                        //For each seeder found for a specific hash
                        for (String seeder : seederDic.get(node.getHash())) {

                            //If we have exceeded the number of seeder, suspend seeder seeker thread
                            if (numSeed >= MAX_CLIENT) {
                                sT.mysuspend();
                            }else if(!sT.isSearching()){
                                sT.myresume();
                            }

                            //If seeder not registred yet, add him to warRoom
                            if (!warRoom.containsKey(seeder)) {
                                warRoom.put(seeder, new ArrayList<Part>());
                            }

                            //Retrieving available thread for this node
                            int avail = NUM_THREAD - warRoom.get(seeder).size();

                            //If the seeder is fully working leave him working
                            if (avail <= 0) {
                                break;
                            }

                            //We start specific number of thread for each seeder
                            for (int i = 0; i < avail; i++) {
                                //Find next available part to download
                                Part p = findNextPart(node.getHash());

                                //If nothing has been found just exit (probably no more part ?)
                                if (p == null) {
                                    break;
                                }

                                downloadList.addpart(node.getName(), p.getNumPart(), new ShareEntry(node.getSeeder(), node.getName() + " part#" + p.getNumPart().toString(), node.getSize(), node.getHash()));
                                downloadList.updatePartStatus(node.getName(), p.getNumPart(), "Downloading");
                                downloadList.updatePartProgress(node.getName(), p.getNumPart(), 0);

                                //Adding part to seeder warRoom
                                warRoom.get(seeder).add(p);

                                //Trying to start listen pipe, if not just skip to net iteration
                                srvList.put(node.getHash() + ":PART:" + p.getNumPart().toString() + "@" + seeder, new SimpleServer(this, node.getHash() + ":PART:" + p.getNumPart().toString() + "@" + seeder, seeder, node.getName(), node.getHash(), p));
                                srvList.get(node.getHash() + ":PART:" + p.getNumPart().toString() + "@" + seeder).start();
//                                    try {
//                                        srvList.get(node.getHash()+":PART:"+p.getNumPart().toString()+"@" + seeder).join();
//                                    } catch (InterruptedException ex) {
//                                        Logger.getLogger(DownloadManager.class.getName()).log(Level.SEVERE, null, ex);
//                                    }
                            }
                            numSeed++;
                        }



                        for (SimpleServer srv : srvList.values()) {
                            try {
                                srv.waitFinish();
                            } catch (InterruptedException ex) {
                                displayLog("[!]Interrupted simple server Thread with error: " + ex.getMessage());
                            }
                        }
                        dispatchDone = true;


                    }//Wait a notification before next pass
                    else {
//                            synchronized (this) {
//                                while(dispatchDone){
//                                    try {
//                                        wait();
//                                    } catch (InterruptedException ex) {
//                                        Logger.getLogger(DownloadManager.class.getName()).log(Level.SEVERE, null, ex);
//                                    }
//                                }
//                            }
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException ex) {
                            displayLog("[!]Interrupted downloaderThread waiting with error: " + ex.getMessage());
                        }
                        dispatchDone = false;
                    }

                    //If no more part for this hash are needed to download force exit
                    if (filePartDic.get(node.getHash()).isEmpty()) {
                        shouldRun = false;
                    }

//                        else{
////                            for(Part p : filePartDic.get(node.getHash()))
////                                System.out.println("Part#"+p.getNumPart()+" remaining with status:"+p.getStatus()+"\n");
//                        }

                }
//                }

                //Stopping seeder search
                sT.mystop();

                //Closing open socket //freeing ressources
                for (SimpleServer s : srvList.values()) {
                    if (s.isActive()) {
                        s.mystop();
                    }
                }
                srvList.clear();
                warRoom.clear();
                filePartDic.clear();

                //Announce to user that a download is finished
                Toolkit.getDefaultToolkit().beep();
                displayLog("[+]File " + node.getName() + " successfully downloaded");

                //Adding the new download to our library
                library.addFile(library.getRoot(), tmp);
                downloadList.updateTransferStatus(node.getName(), "Completed");
                nbUnotifiedDownload++;
                displayLog("[+]File " + tmp.getName() + " added to your own library");

                if (Kb00m.GUI_MODE) {
                    GUI.updateDownloadTable();
                }

                //If we are running on a mac, add a cute number on dock icon... niiiice !! :D
                if (Kb00m.OS_NAME.indexOf("mac") >= 0) {
                    com.apple.eawt.Application application = com.apple.eawt.Application.getApplication();
                    application.setDockIconBadge(nbUnotifiedDownload.toString());
                }

            } catch (IOException ex) {
                displayLog("[!]IOException while creating destination file " + ex.getMessage());
            }

        }

        private Part findNextPart(String hash) {
            for (Part p : filePartDic.get(hash)) {
                if (p.getStatus().equals("init") || p.getStatus().equals("error") || p.getStatus().equals("expired")) {
                    p.setStatus("ready");
                    return p;
                }
            }

            return null;
        }

        synchronized void partTried() {
            dispatchDone = false;
            notify();
        }

        synchronized void mystop() {
            shouldRun = false;
            notify();
        }
    }

    public class SimpleServer extends Thread implements Runnable, Serializable {

        private boolean runFlag = false;
        private volatile Thread t = null;
        private DownloaderThread parent = null;
        
        private Part filePart = null;
        private String seeder = null;
        private String toFile = null;
        private String tName = null;
        private String fHash = null;
        
        private Integer progress = 0;
        private Long maxSize = new Long(0);
        private Long TTIMEOUT = new Long(1000);
        private int tryConnect = 0;
        private long fileStart = 0;
        
        private PipeAdvertisement serverSocketAdv = null;
        private JxtaServerSocket myJXTAServerSocket = null;
        private Socket mySocket = null;

        public SimpleServer(DownloaderThread pT, String threadName, String sdr, String destFile, String destHash, Part p) {
            this.parent = pT;
            this.seeder = sdr;
            this.tName = threadName;
            this.toFile = destFile;
            this.filePart = p;
            this.fHash = destHash;
        }

        @Override
        public void start() {
            t = new Thread(this, this.tName);
            t.start();
        }

        public void waitFinish() throws InterruptedException {
            t.join();
        }

        public boolean isActive() {
            return (Thread.currentThread() == t);
        }

        @Override
        public void run() {

            // Creating the JXTA download socket server
            for (int i = 0; i < NUM_RETRY_CONNECTION; i++) {
                //If connection is made just exit
                if (startListeningPipe(this.toFile)) {
                    break;
                }
            }

        }

        private boolean startListeningPipe(String toReceive) {

            int backLog = 50;
            try {

                this.myJXTAServerSocket = new JxtaServerSocket(group, Tools.getPipeAdvertisement(group.getPeerGroupID(), group.getPeerName() + ":" + this.fHash + ":PART:" + this.filePart.getNumPart().toString(), false), backLog, TIMEOUT);
                this.serverSocketAdv = this.myJXTAServerSocket.getPipeAdv();

                discoService.publish(this.serverSocketAdv);
                discoService.remotePublish(this.serverSocketAdv);
            } catch (IOException ioex) {
                displayLog("[!]IOError has publishing server socket for file " + this.toFile + " part:" + this.filePart.getNumPart().toString() + " \n" + ioex.getMessage());
            }
            displayLog("[+]Waiting " + this.seeder + "'s data, for receiving " + toReceive + " part# " + this.filePart.getNumPart().toString() + "...");
            
            //If sending the notification advert generate error, just skip
            if(!sendPipeListeningNotification(this.seeder, this.toFile, this.filePart, this.serverSocketAdv.getPipeID().toString(), this.fHash, "READY")){
                closeSockets("error");
                return false;
            }
            try {
                
                //Block until a connection is made
                this.mySocket = this.myJXTAServerSocket.accept();
                
                    displayLog("[+]Socket connection established with " + this.seeder);
                    //Starting Server thread
                    File file = null;

                    try {
                        file = new File(myDownloadedPath.getCanonicalPath() + Kb00m.OS_FILE_SEPARATOR + this.toFile);
                    } catch (IOException ex) {
                        displayLog("[!]IOException when opening file " + this.toFile + " to write " + ex.getMessage());
                    }
                    
                    // start getting the data
                        this.filePart.setStatus("downloading");
                        InputStream in = null;
                        

                    try {
                        // Creating the JXTA download socket server
                        in = this.mySocket.getInputStream();
                        displayLog("[+]Download in progress of file " + this.toFile + " part#" + this.filePart.getNumPart() + "...");
                        toFile(in, file, this.filePart.getStart(), this.filePart.getEnd(), BUFFER_SIZE);
                        
                        //Closing InputStream if it has been able to start
                        in.close();
                        
                        displayLog("[+]Part#" + this.filePart.getNumPart() + " of file " + this.toFile + " successfully downloaded !!");

                        //Update part listing
                        this.filePart.setStatus("completed");
                        filePartDic.get(this.fHash).remove(this.filePart);

                        //Update warRoom
                        warRoom.get(this.seeder).remove(this.filePart);
                        downloadList.updateTransferProgress(this.toFile, downloadList.getTransfert(this.fHash).getProgress() + this.filePart.getSize().intValue());

                    } catch (SocketTimeoutException soex) {
                        displayLog("[!]Socket TimeOut while receiving " + this.toFile + " part#" + this.filePart.getNumPart().toString() + "\n[!]" + soex.getMessage());
                        closeSockets("expired");
                        return false;
                    } catch (IOException ex) {
                        displayLog("[!]IOException in ConnectionHandler while downloading " + this.toFile + " part #" + this.filePart.getNumPart() + "\n[!]" + ex.getMessage());
                        closeSockets("error");
                        return false;
                    } 
//                    finally{
////                        displayLog("[+]Closing server Thread");
                        
//
//                    }

                

            } catch (SocketTimeoutException soex) {
                displayLog("[!]SocketTimeoutException while trying to receive " + this.toFile + " part#" + this.filePart.getNumPart().toString() + "\n[!]" + soex.getMessage());
                closeSockets("expired");
                return false;
            } catch (IOException ioex) {
                displayLog("[!]IOError has opening socket for transfering file " + toReceive + " \n" + ioex.getMessage());
                closeSockets("error");
                return false;
            }

            closeSockets("completed");
            return true;
        }
        
        private void toFile(InputStream ins, RandomAccessFile fc, long end, int buf_size) throws
                java.io.FileNotFoundException,
                java.io.IOException {

            //Position pointer in file and lock region
//            fc.position(this.fileStart);
//            FileLock lock = fc.lock();

            byte[] buffer = new byte[ buf_size ];
//            ByteBuffer buff = ByteBuffer.allocate(buf_size);
//            byte[] buffer = buff.array();
            int len_read = 0;

            while ((this.fileStart + buf_size <= end) && ((len_read = ins.read(buffer,0,buf_size)) != -1)) {
                this.fileStart += len_read;

                if (len_read <= 0) {
                    break;
                }

//                buff.put(buffer);
                try {
                    writeFile(fc,buffer);
                } catch (Exception e) {
                    displayLog("[!]Error start:" + this.fileStart + "   end:" + end + "   read:" + len_read + "   buffer:" + buf_size + "  error:" + e.getMessage());
                }
//                buff.clear();
            }

            //Release lock as no more treatment has to be done
//            lock.release();

            if (this.fileStart < end) {
                toFile(ins, fc, end, buf_size / 2);
            }

        }
        
        private void writeFile(RandomAccessFile file, byte[] bytes) {
            try {
                boolean written = false;
                do {
                try {
                    // Lock it!
                    FileLock lock = file.getChannel().lock();
                    try {
                    // Write the bytes.
                    file.write(bytes);
                    written = true;
                    } finally {
                    // Release the lock.
                    lock.release();
                    }
                } catch ( OverlappingFileLockException ofle ) {
                    try {
                    // Wait a bit
                    Thread.sleep(0);
                    } catch (InterruptedException ex) {
                        throw new InterruptedIOException ("Interrupted waiting for a file lock.");
                    }
                }
                } while (!written);
            } catch (IOException ex) {
                displayLog("[!]Failed to lock "+ ex.getMessage());
            }
        }

        private void toFile(InputStream ins, File file, Long start, Long end, int buf_size) throws
                java.io.FileNotFoundException,
                java.io.IOException {

            //Open the output file and seek to the start location
            RandomAccessFile raf = new RandomAccessFile(file, "rw");

            //Increase file length if necessary
            if (end > file.length()) {
                raf.setLength(end);
            }
//            raf.close();

            this.fileStart = start;
            raf.seek(this.fileStart);
//            FileChannel chan1 = new FileOutputStream(file).getChannel();
//            FileChannel fc = raf.getChannel();
            
            //Write into file
            toFile(ins, raf, end, buf_size);
            
            //Close pointer
//            fc.close();
//            chan1.close();
            raf.close();
        }

        private void closeSockets(String status) {
//            displayLog("[+]Closing SOCKETS !!!");
            warRoom.get(this.seeder).remove(this.filePart);
            this.filePart.setStatus(status);

            try {
                discoService.flushAdvertisement(this.serverSocketAdv);
            } catch (IOException ex) {
                displayLog("[!]IOException while flushing socket advertisement " + ex.getLocalizedMessage());
            }

            try {
                if (this.mySocket != null) {
                    this.mySocket.close();
                }

                this.myJXTAServerSocket.close();

            } catch (IOException ex) {
                displayLog("[!]IOException while closing socket with error " + ex.getLocalizedMessage());
            }

//            this.parent.partTried();
        }

        private boolean sendPipeListeningNotification(String dest, String fileName, Part part, String pipeID, String checksum, String status) {
            boolean send = false;
            
            DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
            Date date = new Date();
            String myTime = dateFormat.format(date).toString();

            Message myMessage = new Message();
            //adding timestap and peers details also messages to XML tag and send them 
            StringMessageElement sme = new StringMessageElement("PeerName", group.getPeerName(), null);
            StringMessageElement sme1 = new StringMessageElement("SessionID", group.getPeerID().toString(), null);
            StringMessageElement sme2 = new StringMessageElement("Status", status, null);
            StringMessageElement sme3 = new StringMessageElement("FileName", fileName, null);
            StringMessageElement sme4 = new StringMessageElement("FilePart", part.getNumPart().toString(), null);
            StringMessageElement sme5 = new StringMessageElement("FileStart", part.getStart().toString(), null);
            StringMessageElement sme6 = new StringMessageElement("FileEnd", part.getEnd().toString(), null);
            StringMessageElement sme7 = new StringMessageElement("FileHash", checksum, null);
            StringMessageElement sme8 = new StringMessageElement("PipeID", pipeID, null);
            StringMessageElement sme9 = new StringMessageElement("Type", "DOWNLOAD:" + group.getPeerGroupName(), null);
            StringMessageElement sme10 = new StringMessageElement("Time", myTime, null);

            myMessage.addMessageElement(sme);
            myMessage.addMessageElement(sme1);
            myMessage.addMessageElement(sme2);
            myMessage.addMessageElement(sme3);
            myMessage.addMessageElement(sme4);
            myMessage.addMessageElement(sme5);
            myMessage.addMessageElement(sme6);
            myMessage.addMessageElement(sme7);
            myMessage.addMessageElement(sme8);
            myMessage.addMessageElement(sme9);
            myMessage.addMessageElement(sme10);

            try {
                displayLog("[+]Creating download OutPutPipe status notification");
                propagatePipe = pipeSrvc.createOutputPipe(Tools.getPipeAdvertisement(group.getPeerGroupID(), dest + ":DOWNLOAD", false), TIMEOUT);

                send = propagatePipe.send(myMessage);
                if (send) {
                    displayLog("[+]Download OutPutPipe status sent: " + status + "\n[+]To Peer: " + dest + "\n[+]with PipeID: " + pipeID);
                } else {
                    displayLog("[!]Impossible to send the download OutPutPipe");
                }
                propagatePipe.close();

            } catch (IOException ioe) {
                displayLog("[!]IOError has sending download OutPutPipe status <" + status + "> to " + dest + "\n" + ioe.getMessage());
                send = false;
            }
            
            return send;

        }

        synchronized void mystop() {
            this.runFlag = false;
            notify();
        }
    }


    class UploadThread implements Runnable, Serializable {

        private String tName; // name of thread
        private String rPeer;
        private String rHash;
        private File rFile;
        private Part rFilePart;
        private Thread t;
        private boolean suspendFlag;
        private boolean fileSent = false;
//        private FileInputStream fileInputStream = null;
        private JxtaSocket socket = null;
        private OutputStream os = null;
        private long startFile = 0;
//        private ObjectOutputStream oos = null;

        UploadThread(String threadname, String dest, String h, File f, Part p) {
            this.tName = threadname;
            this.rPeer = dest;
            this.suspendFlag = false;
            this.rHash = h;
            this.rFile = f;
            this.rFilePart = p;
            try {
                this.socket = new JxtaSocket(group, Tools.getPipeAdvertisement(group.getPeerGroupID(), dest + ":" + h + ":PART:" + p.getNumPart().toString(), false));
                this.os = this.socket.getOutputStream();
            } catch (IOException ex) {
                displayLog("[!]IOException has uploading part#" + p.getNumPart().toString() + " of file " + rFile.getName() + "\n" + ex.getMessage());
            }
        }

        public void start() {
            t = new Thread(this, tName);
            t.start();
            try {
                t.join();
            } catch (InterruptedException ex) {
                Logger.getLogger(DownloadManager.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        public void run() {

            try {
                // start pumping in the data
                try {

                    // open the output rFile and seek to the start location
                    displayLog("[+]Uploading file " + this.rFile.getName() + " part#" + this.rFilePart.getNumPart());
                    toStream(this.os, this.rFile, this.rFilePart.getStart(), this.rFilePart.getEnd(), BUFFER_SIZE);
                    this.fileSent = true;

                } catch (IOException ex) {
                    displayLog("[!]IOException while creating the socket to send " + this.rFile.getName() + "part#" + this.rFilePart.getNumPart().toString() + "\n" + ex.getMessage());
                }
                synchronized (this) {
                    while (this.suspendFlag) {
                        wait();
                    }
                }

            } catch (InterruptedException e) {
                displayLog("[!]Transfert " + this.tName + " interrupted.");
            }

            try {
                displayLog("[+]closing socket transfer for file " + this.rFile.getName() + " part#" + this.rFilePart.getNumPart().toString());
                this.os.close();
                this.socket.close();
            } catch (IOException ex) {
                displayLog("[!]IOException while closing the socket to send " + this.rFile.getName() + "\n" + ex.getMessage());
            }

            if (this.fileSent) {
                displayLog("[+]File " + this.rFile.getName() + " part#" + this.rFilePart.getNumPart().toString() + " has been uploaded sucessfully!!");
                uploadList.updateTransferStatus(this.rFile.getName() + " part#" + this.rFilePart.getNumPart().toString(), "Upload completed");
            }

//            if(Kb00m.GUI_MODE)
//                GUI.updateUploadTable();
        }

        private void toStream(OutputStream os, File file, Long start, Long end, int buf_size)
                throws java.io.FileNotFoundException,
                java.io.IOException {
            // open the output file and seek to the start location
            RandomAccessFile raf = new RandomAccessFile(file, "r");

            this.startFile = start;
            raf.seek(this.startFile);
//            FileChannel chan1 = new FileInputStream(file).getChannel();
//            FileChannel fc = raf.getChannel();

            toStream(os, raf , end, buf_size);
            
//            fc.close();
//            chan1.close();
            raf.close();
        }

        private void toStream(OutputStream os, RandomAccessFile fc, Long end, int buf_size)
                throws java.io.FileNotFoundException,
                java.io.IOException {


//            fc.position(this.startFile);
//            FileLock lock = fc.lock(this.startFile, end, false);

            byte[] buff = new byte[ buf_size ];
//            ByteBuffer buffer = ByteBuffer.allocate(buf_size);
            int numRead = 0;

            while ((this.startFile + buf_size <= end) && ((numRead = fc.read(buff)) != -1)) {
//                  buffer.put(buff);
                os.write(buff);
                this.startFile += numRead;
//                buffer.clear();
            }
            os.flush();

//            lock.release();

            //If less than a buffer_size is remaining, just pass it to ourselves
            if (this.startFile < end) {
                toStream(os, fc, end, buf_size / 2);
            }
        }
        
        protected void mysuspend() {
            displayLog("[!]Transfert " + tName + " suspended.");
            this.suspendFlag = true;
        }

        protected synchronized void myresume() {
            displayLog("[!]Resuming " + tName + " transfer.");
            this.suspendFlag = false;
            notify();
        }

        protected boolean isUploading() {
            return !this.suspendFlag;
        }
    }

    protected void suspendUpload(String threadName) {
        this.uploadThread.get(threadName).mysuspend();
    }

    protected void resumeUpload(String threadName) {
        this.uploadThread.get(threadName).myresume();
    }
}

class Part {

    private Integer numPart = 0;
    private Long start = new Long(0);
    private Long end = new Long(0);
    private String status = "init";

    Part(int nPart, Long s, Long e) {
        this.numPart = nPart;
        this.start = s;
        this.end = e;
    }

    protected void setStatus(String s) {
        this.status = s;
    }

    protected String getStatus() {
        return this.status;
    }

    protected Long getStart() {
        return this.start;
    }

    protected Long getEnd() {
        return this.end;
    }

    protected Long getSize() {
        return this.end - this.start;
    }

    protected Integer getNumPart() {
        return numPart;
    }
}
