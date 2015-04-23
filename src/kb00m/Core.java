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

//This class is for initializing Peer and launch it into Default JXTA network and use its

import java.io.*;
import java.net.ServerSocket;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.EncryptedPrivateKeyInfo;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import net.jxta.discovery.DiscoveryEvent;
import net.jxta.discovery.DiscoveryListener;
import net.jxta.discovery.DiscoveryService;
import net.jxta.document.AdvertisementFactory;
import net.jxta.document.Element;
import net.jxta.document.MimeMediaType;
import net.jxta.endpoint.Message;
import net.jxta.endpoint.MessageElement;
import net.jxta.exception.PeerGroupException;
import net.jxta.id.ID;
import net.jxta.id.IDFactory;
import net.jxta.impl.id.UUID.ModuleSpecID;
import net.jxta.impl.membership.pse.FileKeyStoreManager;
import net.jxta.impl.membership.pse.PSEConfig;
import net.jxta.impl.membership.pse.PSEMembershipService;
import net.jxta.impl.membership.pse.PSEUtils;
import net.jxta.impl.peergroup.StdPeerGroupParamAdv;
import net.jxta.peer.PeerID;
import net.jxta.peergroup.PeerGroup;
import net.jxta.peergroup.PeerGroupID;
import net.jxta.pipe.InputPipe;
import net.jxta.pipe.PipeMsgEvent;
import net.jxta.pipe.PipeMsgListener;
import net.jxta.pipe.PipeService;
import net.jxta.platform.NetworkConfigurator;
import net.jxta.platform.NetworkManager;
import net.jxta.protocol.*;
import net.jxta.socket.JxtaMulticastSocket;

//Services to start global group and app.


public class Core implements DiscoveryListener, PipeMsgListener {
    
    private boolean endOfSearch = false;
    
//    private JFrame mainFrame = null;
//    private JComboBox peerGroupList = null;
//    private JList peerList = null;
    private static JTextArea chatOut = null;
    private static JTable activeDownload = null;
    
    //Static attributes
    //Wait Period in (sec) to get a RDV connection until we switch ourselves to RDV
    private final static int TIMEOUT = 5 * 1000;
    protected static final String SPLIT_CHAR = ";";
    protected static final String MAINGROUP_NAME = "B00m";
    private static final String MAINSERVICE_NAME = MAINGROUP_NAME+"Service";
    protected static final String MAINGROUP_DESC = MAINGROUP_NAME+" World Public P2P Group";
    protected static final String MAINGROUP_ID = "urn:jxta:uuid-425A5C703CD5454F9C03938A0D65BD5002";
    //The ID that our custom peer group will use. We use a hardcoded id so that
    //all instances use the same value. This ID was generated using the
    // <tt>newpgrp -s</tt> JXSE Shell command.
    protected final static PeerGroupID MAINPEERGROUP_ID = PeerGroupID.create(URI.create(MAINGROUP_ID));
    
    //Internal dynamic configuration
    private NetworkManager.ConfigMode B00mConfigMode = null;
    private static int TcpPort = 0;
    private static int HttpPort = 0;
    private static int UserLevel = 0;
    private String TimeStamp = null;
    protected static String PeerName = null;
    private PeerID b00mPeerID = null;
    private String b00mPeerIDString = null;
    private File CertificateDirectory = null;
    private File ConfigurationFile = null;
    private File KeystoreFile = null;
    protected static File sharedPath = null;
    private String MyJxtaPassword = null;
    private String PKeyPassword = null;
    private KeyStore MyKeyStore = null;
    private X509Certificate PSEX509Certificate = null;
    private File CertificateFile = null;
    private PrivateKey PSEPrivateKey = null;
    
    
    //Main Group variables
    private PeerGroup netPeerGroup = null,
            b00mGroup = null;
    private CustomGroup b00mCustomGroup = null;
    
    //JXTA Services variables
    private DiscoveryService b00mDiscoverySrvc = null;
    private PipeService b00mPipeSrvc = null;
    private PeerGroupAdvertisement B00mGRPAdv = null;
    private PeerAdvertisement B00mUSRAdv = null;
    private JxtaMulticastSocket propagateMultPipe = null;
    private InputPipe inPipe = null,
            inPipeM = null,
            inPipePrivate = null,
            inPipePrivateM = null;;
    private PipeAdvertisement propagatePipeAdv = null;
    private NetworkManager MyNetworkManager = null ;
    private NetworkConfigurator MyNetworkConfigurator = null;
    
    //Databases
//    private DATA knownGroups = new DATA(Kb00m.GUI_MODE);
//    private DATA knownPeers = new DATA(Kb00m.GUI_MODE);
    private SearchModel searchResults = null;
    private LibraryModel library = null;
    private LibraryModel privateLibrary = null;
    private UserModel peerList = null;
    private PeerGroupModel groupList = null;
    private PGroup contextPeerGroup = null;
    private CustomGroup contextGroup = null;
    
    private Remora r = null;
    
    //Core Constructor
    public Core(String nickName) {
        System.setProperty("net.jxta.endpoint.WireFormatMessageFactory.CBJX_DISABLE", "true");
        Core.PeerName = nickName;
        this.groupList = new PeerGroupModel();
        PGroup def = this.groupList.addCustomGroup("-- Select a Group --");
        this.groupList.setSelectedItem(def);
    }
    
    private void displayLog(String s){
        if(Kb00m.GUI_MODE){
            GUI.addLogMsg(s);
        }else{
            System.out.println(s);
        }
    }
    
    //Start main Process in GUI mode
//    protected void launchB00m(JTextArea chatOutput, JTable shareOutput, JTable downloadOuput){
//        //register the GUI handle
//        //this.peerGroupList = groupList;
//        //this.peerList = userList;
//        Core.chatOut = chatOutput;
//        Core.shareOut = shareOutput;
//        Core.activeDownload = downloadOuput;
//        launchB00m();
//    }

    //Start main Process
    protected void launchB00m(){
        
        //Validate user rights
        checkLocalLevel();
        
        //Generate ID
        setPeerName();
        
        //Check if configuration file exists (contains share path / remoteName)
        createDirectory();
        
        //Check if certificate exists / Generate keystore if not
        configureKeystore(this.b00mPeerID.getPeerGroupID().toString());
        
        //Start network
        B00mStart();
    }
    
    protected void B00mStart(){
        
        this.MyJxtaPassword = this.b00mPeerID.getPeerGroupID().toString();
        
        //Configure JXTA NetworkManager
        configureNetwork();
        
        
        try {
            //Launch JXTA
            MyNetworkManager.startNetwork();
        } catch (PeerGroupException ex) {
            displayLog("[!]PeerGroup Exception, NetPeerGroup unable to start.\n"+ex.getMessage());
        } catch (IOException ex) {
            displayLog("[!]IOException, NetPeerGroup unable to start.\n"+ex.getMessage());
        }
        
        //Register the information accessibles from the netPeerGroup contextPeerGroup
        retrieveGlobalGroupInfos();
        
        try {
            //PeerGroup Creation
            
//            this.b00mCustomGroup = new CustomGroup(true, this.netPeerGroup, this.MAINGROUP_NAME ,this.MAINGROUP_DESC, true);
            this.b00mCustomGroup = new CustomGroup(this.netPeerGroup, this.MAINGROUP_NAME ,this.MAINGROUP_DESC, true, true);
            this.b00mCustomGroup.joinPeerGroup(this.b00mPeerID, this.b00mPeerID.getPeerGroupID().toString());
            
            //If we are on a graphical contextPeerGroup register it in PeerGroup Custom
            //if(Kb00m.GUI_MODE) b00mCustomGroup.registerGraphics(peerList);
            
            //Start the PeerGroup services
            this.b00mCustomGroup.startGroupServices();
            this.b00mCustomGroup.startPeersListing();
            
            //Retrieve PeerGroup
            this.b00mGroup = this.b00mCustomGroup.getGroup();
            this.contextGroup = this.b00mCustomGroup;
            
            //Adding the main peer Group to the list of known Group is a minimum !!
            this.contextPeerGroup = this.groupList.addCustomGroup(this.b00mGroup.getPeerGroupAdvertisement());
            this.contextPeerGroup.setPeerGroup(this.b00mGroup);
            
            //Position the group created has the selected one
            this.groupList.setSelectedItem(this.contextPeerGroup);
            
            //Retrieve b00mCustomGroup DiscoveryService
            this.b00mDiscoverySrvc = this.b00mGroup.getDiscoveryService();
            this.b00mDiscoverySrvc.addDiscoveryListener(this);
            
            this.b00mPipeSrvc = this.b00mGroup.getPipeService();
            this.inPipePrivate = this.b00mPipeSrvc.createInputPipe(Tools.getPipeAdvertisement(b00mGroup.getPeerGroupID(), this.PeerName, false),this);
            displayLog("[+]Creation successful of the unicast private listing inputPipe with ID: "+inPipePrivate.getPipeID());
            
            //Publishing the pipe
            this.b00mDiscoverySrvc.publish(inPipePrivate.getAdvertisement());
            this.b00mDiscoverySrvc.remotePublish(inPipePrivate.getAdvertisement());
            
            //Start the PeerGroups Listing
            startPeerGroupsListing();
            
            //Publishing our peerGroup
            this.b00mDiscoverySrvc.publish(this.b00mGroup.getPeerGroupAdvertisement());
            this.b00mDiscoverySrvc.remotePublish(this.b00mGroup.getPeerGroupAdvertisement());
            
//            if(Kb00m.GUI_MODE) b00mCustomGroup.updatePeerList();
            
            //Starting the Remora object
            r = new Remora(TcpPort);
            r.setID(b00mPeerIDString);
            r.start();
            
            
        } catch (PeerGroupException ex) {
            displayLog("[!]PeerGroup Exception, "+MAINGROUP_NAME+" unable to start.\n"+ex.getMessage());
        } catch (IOException ioEx) {
            displayLog("[!]IOException, "+MAINGROUP_NAME+" unable to create private Pipe.\n"+ioEx.getMessage());
        }    
            //Search if global group exists, either create it
    //        searchGlobalGroup();
    //        
    //        //Create listening interface on Unicast & Multicast
    //        startListening();
    //        
    //        try {
    //            b00mDiscoverySrvc.publish(inPipe.getAdvertisement());
    //            b00mDiscoverySrvc.remotePublish(inPipe.getAdvertisement());
    //            b00mDiscoverySrvc.publish(inPipeM.getAdvertisement());
    //            b00mDiscoverySrvc.remotePublish(inPipeM.getAdvertisement());
    //            b00mDiscoverySrvc.publish(inPipePrivate.getAdvertisement());
    //            b00mDiscoverySrvc.remotePublish(inPipePrivate.getAdvertisement());
    //            b00mDiscoverySrvc.publish(inPipePrivateM.getAdvertisement());
    //            b00mDiscoverySrvc.remotePublish(inPipePrivateM.getAdvertisement());
    //            
    //        } catch (IOException ex) {
    //            displayLog("[!]IOException while publishing Core pipe advertisement!!");
    //        }
        
        
    }

    protected void B00mStop() {
//        if(r.isRegistered()){
//            r.unRegisterSelf();
//        }
        //stopping remora Thread
        r.mystop();
        if (this.isConnected()) {
            System.out.println("[-]Announcing we're leaving");
            this.contextGroup.updateUserStatus("logoff");
            System.out.println("[-]Stopping b00m Network");
            b00mCustomGroup.removeShare();
            b00mCustomGroup.removeChat();
            
            MyNetworkManager.stopNetwork();
            //Stop the searching thread
            endOfSearch = false;
            //Erase the known peers
            peerList.clearUsers();
            
            System.out.println("[-]b00m Network is NO MORE connected.");
        }
        
    }
    
    protected boolean isConnected(){
        return MyNetworkManager.isStarted();
    }
    
    protected void startServices(File shareDir){
        
        displayLog("[+]Adding Share Service to "+this.contextGroup.getGroup().getPeerGroupName()+" group");
        this.contextGroup.addShare(shareDir);
        displayLog("[+]Adding Chat Service to "+this.contextGroup.getGroup().getPeerGroupName()+" group");
        this.contextGroup.addChat();
        //displayLog("[+]Adding Page Service to the Main group");
        //b00m.addPage(pageOut, log);
        
        //Setting the global share Directory variable
        Core.sharedPath = shareDir;
        
        //Retrieve the data model generated
        this.peerList = this.contextGroup.getUserModel();
        this.searchResults = this.contextGroup.getSearchResultsModel();
        this.library = this.contextGroup.getLibraryModel();
        this.privateLibrary = this.contextGroup.getPrivateLibraryModel();
            
//        r.setID(b00mGroup.getPeerID().toString());
//        if(b00mGroup.isRendezvous() && r.isConnected()) r.registerSelf();
    }
    
//    protected void startServicesCLI(File shareDir){
//        
//        displayLog("[+]Adding Share Service to the Main group");
//        b00mCustomGroup.addShare(shareDir);
//        displayLog("[+]Adding Chat Service to the Main group");
//        b00mCustomGroup.addChat();
//        
//        //Setting the global share Directory variable
//        Core.sharedPath = shareDir;
//        
//        //Starting the Remora object
//        r = new Remora(b00mGroup.getPeerID().toString(),TcpPort);
//        if(b00mGroup.isRendezvous() && !r.isRegistered()) r.registerSelf();
//    }
    
    
    private void checkLocalLevel() {
        
        
        displayLog("[######################################]");
        displayLog("[#]Step 0 : Checking your rights");
        //Try open a root-level port
        ServerSocket tmp;
        try {
            tmp = new ServerSocket(443);
            if (tmp != null) {
                displayLog("[+]HELLO, You're running " + Kb00m.APP_NAME + " at root level, Thanks !!");
                TcpPort = 443;
                HttpPort = 80;
                tmp.close();
                UserLevel = 2;
                B00mConfigMode = NetworkManager.ConfigMode.RENDEZVOUS_RELAY;
            }
        } catch (Exception e) {
            //if fail try to open standard port
            try {
                tmp = new ServerSocket(1337);
                if (tmp != null) {
                    displayLog("[+]HELLO, You're running " + Kb00m.APP_NAME + " non-root");
                    TcpPort = 1337;
                    HttpPort = 1338;
                    tmp.close();
                    UserLevel = 1;
                    B00mConfigMode = NetworkManager.ConfigMode.RENDEZVOUS_RELAY;
                }
            } catch (IOException ex) {
                try {
                    tmp = new ServerSocket(9666);
                    if (tmp != null) {
                        displayLog("[+]HELLO, You're running " + Kb00m.APP_NAME + " non-root and you're not alone !!");
                        //Randomizing port in order to let anyone access network.. or not: pray !! ;)
                        TcpPort = 9000 + new Random().nextInt(100);
                        HttpPort = TcpPort+1;
                        tmp.close();
                        UserLevel = 0;
                        B00mConfigMode = NetworkManager.ConfigMode.EDGE;
                    }
                } catch (IOException ex1) {
                    //if still fail exit app... no need to start as there is no network available !!
                    System.out.println("[!!]SORRY, Network seems to be unavailable... Aborting");
                    System.out.println(ex.getStackTrace());
                    System.exit(1);
                }
            }
        }
        
    }
    
    private void setPeerName() {
        
        displayLog("[######################################]");
        displayLog("[#]Step 1 : Setting the PeerName value");
        //Test if a valid certificate is present
        File certFile = new File(CertificateDirectory, MAINGROUP_NAME+".crt");
        if (certFile.exists() && certFile.isFile()) {
            displayLog("[+]Certificate file Found.");
            //b00mConfig.setCertificate(null);            
        } else {
            //If no certificate available
            displayLog("[!]Certificate file NOT Found...\n[+]Will ask for a 1h trial one.");


            //generate UUID based on
            //Windows -> TIMESTAMP + MotherboardID + 4 RandomDigit
            //Others -> TIMESTAMP + MOBILE + 10 RandomDigit
            Random randGen = new Random();
//            java.util.Date date = new java.util.Date();
//            Calendar cal = new GregorianCalendar();
//            Integer year = cal.get(Calendar.YEAR);
//            Integer month = cal.get(Calendar.MONTH);
//            Integer day = cal.get(Calendar.DAY_OF_MONTH);
//            Integer hour = cal.get(Calendar.HOUR_OF_DAY);
//            Integer min = cal.get(Calendar.MINUTE);
//            Integer sec = cal.get(Calendar.SECOND);
            
//            Core.PeerName = String.valueOf(UUID.randomUUID());
            String seed = String.valueOf(UUID.randomUUID());
            Core.PeerName = Core.PeerName+"-"+seed.substring(1, 10);
//            
//            if (System.getProperty("os.name").toLowerCase().indexOf("win") >= 0) {
//                
//                PeerName = day.toString() + hour.toString() + min.toString() + sec.toString() + new Integer(randGen.nextInt(1000)).toString();
//
//            } else {
//                PeerName = day.toString() + hour.toString() + min.toString() + sec.toString() + new Integer(randGen.nextInt(1000)).toString();
//                //############################################
//            }

        }
        CertificateDirectory = new File(new File("." + MAINGROUP_NAME), "./certificates");
        String theSeed = MAINGROUP_ID + Core.PeerName;
        b00mPeerID = (PeerID) IDFactory.newPeerID(PeerGroupID.defaultNetPeerGroupID, theSeed.getBytes());
        b00mPeerIDString = b00mPeerID.toString();

        
        displayLog("[+]Welcome Aboard!! You're register as " + Core.PeerName + " ;)");
        
    }
    
    public void createDirectory() {
        displayLog("[######################################]");
        displayLog("[#]Step 2 : Setting the PID + directories creation if needed");
        CertificateDirectory = new File(new File("." + MAINGROUP_NAME), "./certificates");
        CertificateFile = new File(CertificateDirectory, b00mPeerID + ".crt");
        ConfigurationFile = new File(new File("." + MAINGROUP_NAME), PeerName);
        KeystoreFile = new File(ConfigurationFile, "keystore");

        if (!CertificateDirectory.exists()) {
            CertificateDirectory.mkdirs();
        }
        if (!ConfigurationFile.exists()) {
            ConfigurationFile.mkdirs();
        }

    }

    public void configureKeystore(String pass) {

        displayLog("[######################################]");
        displayLog("[#]Step 3 : Configuring the KeyStore");
        try {
            FileKeyStoreManager fksm = new FileKeyStoreManager((String) null, "KS Provider", KeystoreFile);
            //PKeyPassword = MyJxtaPassword+Name;
            PKeyPassword = pass;

            if (!fksm.isInitialized()) {
                displayLog("[+]Keystore is NOT initialized\n[+]Creating it...");
                fksm.createKeyStore(pass.toCharArray());

                X509Certificate cert = null;

                PSEUtils.IssuerInfo ForPSE = PSEUtils.genCert(PeerName, null);
                PSEX509Certificate = ForPSE.cert;
                PSEPrivateKey = ForPSE.issuerPkey;
                
                MyKeyStore = fksm.loadKeyStore(pass.toCharArray());

                X509Certificate[] Temp = {PSEX509Certificate};
                MyKeyStore.setKeyEntry(b00mPeerIDString, PSEPrivateKey, PKeyPassword.toCharArray(), Temp);

                fksm.saveKeyStore(MyKeyStore, pass.toCharArray());
                
                //X509Certificate aliceCertificate = alicePSEConfig.getTrustedCertificate(alicePeerID);
                //X509Certificate bobCertificate = bobPSEConfig.getTrustedCertificate(bobPeerID);
            }

            displayLog("[+]Keystore is initialized");
            MyKeyStore = fksm.loadKeyStore(pass.toCharArray());
            PSEX509Certificate = (X509Certificate) MyKeyStore.getCertificate(b00mPeerIDString);
            PSEPrivateKey = (PrivateKey) MyKeyStore.getKey(b00mPeerIDString, PKeyPassword.toCharArray());


        } catch (IOException e) {
            displayLog("[!]IOException, unable to load KeyStore.\n"+e.getMessage());
        } catch (NoSuchProviderException e) {
            displayLog("[!]No such Provider Exception while loading KeyStore.\n"+e.getMessage());
        } catch (NoSuchAlgorithmException e) {
            displayLog("[!]No such Algorithm Exception while loading KeyStore.\n"+e.getMessage());
        } catch (UnrecoverableKeyException e) {
            displayLog("[!]Unrecoverable key Exception while loading KeyStore. Double check your password please.\n"+e.getMessage());
        } catch (KeyStoreException e) {
            displayLog("[!]Keystore Exception.\n"+e.getMessage());
        }


    }
//    
//    public void configureAuth(){
//        
//        String PSE_SAMPLE_GROUP_ROOT_CERT_BASE64 = 
//            "MIICGTCCAYKgAwIBAgIBATANBgkqhkiG9w0BAQUFADBTMRUwEwYDVQQKEwx3d3cuanh0YS5vcmcx" + 
//            "GzAZBgNVBAMTElBTRV9TYW1wbGVfUm9vdC1DQTEdMBsGA1UECxMURERFMkNGQ0MyMjVBNDM0OUQx" + 
//            "QTAwHhcNMDUwNjE5MDIyODM4WhcNMTUwNjE5MDIyODM4WjBTMRUwEwYDVQQKEwx3d3cuanh0YS5v" + 
//            "cmcxGzAZBgNVBAMTElBTRV9TYW1wbGVfUm9vdC1DQTEdMBsGA1UECxMURERFMkNGQ0MyMjVBNDM0" +
//            "OUQxQTAwgZ4wDQYJKoZIhvcNAQEBBQADgYwAMIGIAoGAdVgeJotJWEEfh/NtusfI8cAIMAq7WxXA" + 
//            "ZsPIOYnybHPXFNmCTozs/KW0dx01zI6kfHwO1qYXmR/djJAFhr3VhFdUp8y1wDCf6DT63vFOi47t" + 
//            "6TC1yywjZe59VIAxhDt0B8XJnkEbsEl+uO95ec6/U6dYI1vrtWU4ORdSYz615XMCAwEAATANBgkq" + 
//            "hkiG9w0BAQUFAAOBgQBRJXLRyIGHvw3GJC3lYwQUDwRSm6vaPKPlCA5Axfwy+jPuStldhuPYOvxz" + 
//            "a3NxQ/iBlzTGwoVzgxzArM6oLRvtAAvvkQl8z6Lu+NF2ugMs6XfuzRKqrBvSjNaSYM83E51niga2" + 
//            "3UGc4Brbn3RCTPRADykVhWxgiCADNGVBIBUAMw==";
//        String PSE_SAMPLE_GROUP_ROOT_ENCRYPTED_KEY_BASE64 = 
//            "MIICoTAbBgkqhkiG9w0BBQMwDgQIPPpnqsvvaS0CAicQBIICgJAYTLxQfaUMFL08DnrO/tAZioTu" + 
//            "TlUnt32h3n9nE/L0UM8u7Q9elq2YwBNN72LD6ODzZKPmS/PnUl0NnE1AOnLVuMUgl1OBXgmUtC4P" + 
//            "jfgA+En7S0YEmgZN42ceqMpcKGDiBNdr0ebGD9SVy4/XkTLrNcEcHqrhyC6JkSOAo2EKL9OkS6gR" + 
//            "bVp59JSEiAruDvAZnz3XTjlXJYchZGcMVNfCDJVEMCgCsaKkr1Pf5JAfj1kKBJbazwlvqVrU0eI7" + 
//            "nPXTdTNVUaZLA7ucbUialef2/osefm5oB00DVkgIkUQSjesVM+THKu3UxIFe+3yTbUsI3zDja+DK" + 
//            "36l+UBmCLwFSOzJ1HAzP2qj1yvE/crEsvZMr9QrfNp7acfZQCgJZWFBG0wkdkvpTC0SBbzD6TqdW" + 
//            "hbGq8rca4KDkI4HeVoB3yBnMDm52NOtvh2uTKHul7Zz+3GTjXTIT7B4WcdiKmYo5hzdAidHzrWHV" +
//            "eTmBnda34kM4o0uX1rQjWe3pfpp7rKG/zRDMUsqaZhK0k3t8IiNZroMnH39wz/kiRWgh+LBZOmi6" + 
//            "vG4LeaNDom6+o1tH4lHFXh0uCOSjOOKvX91BaptgXXLuFpny1ZMPnSkWzZA20nCJgNB1+S5RLQGg" + 
//            "jObczNUFtI8c/nSlbn339fN9G9/EpGaQuoMqxoSWwVnMnfmBnYlq2LehZ3UC3DgSaxRI9XN/F2Ul" + 
//            "ako4dwiccGcMsGHB/eKHQU/Csk9E19GGghwC2L7Tb2zIx01Ctd2yecpK3clhvN35xR5cvtnKKLtA" + 
//            "KSi8v6rCLDJ0cPa88QfIHRk+M5ZTDP5QN4A0uFKnsWtMI/xjA9tK4VsMEMtxqjBFem8=";
//        
//        X509Certificate PSE_SAMPLE_GROUP_ROOT_CERT;
//        EncryptedPrivateKeyInfo PSE_SAMPLE_GROUP_ROOT_ENCRYPTED_KEY;
//        
//        /* Initialize some static final variables */
//        try {
//            // Initialize the Root certificate.
//            byte[] cert_der = PSEUtils.base64Decode(new StringReader(PSE_SAMPLE_GROUP_ROOT_CERT_BASE64));
//
//            CertificateFactory cf = CertificateFactory.getInstance("X509");
//
//            // Initialize the Root private key.
//            PSE_SAMPLE_GROUP_ROOT_CERT = (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(cert_der));
//
//            byte[] key_der = PSEUtils.base64Decode(new StringReader(PSE_SAMPLE_GROUP_ROOT_ENCRYPTED_KEY_BASE64));
//
//            PSE_SAMPLE_GROUP_ROOT_ENCRYPTED_KEY = new EncryptedPrivateKeyInfo(key_der);
//        } catch (IOException failure) {
//            IllegalStateException failed = new IllegalStateException("Could not read certificate or key.");
//
//        } catch (CertificateException failure) {
//            IllegalStateException failed = new IllegalStateException("Could not process certificate.");
//
//        }
//    
//    }
//    
//    static ModuleImplAdvertisement build_psegroup_impl_adv(PeerGroup base) {
//        ModuleImplAdvertisement newGroupImpl;
//        PeerGroupID PSE_SAMPLE_PGID = (PeerGroupID) ID.create(
//            URI.create("urn:jxta:uuid-6E0C1C2781794A2F983EA4D2ACB758E602"));
//        ModuleSpecID PSE_SAMPLE_MSID = (ModuleSpecID) ID.create(
//            URI.create("urn:jxta:uuid-DEADBEEFDEAFBABAFEEDBABE0000000133BF5414AC624CC8AD3AF6AEC2C8264306"));
//        
//        try {
//            newGroupImpl = base.getAllPurposePeerGroupImplAdvertisement();
//        } catch (Exception unlikely) {
//            // getAllPurposePeerGroupImplAdvertisement() doesn't really throw expections.
//            throw new IllegalStateException("Could not get All Purpose Peer Group Impl Advertisement.");
//        }
//
//        newGroupImpl.setModuleSpecID(PSE_SAMPLE_MSID);
//        newGroupImpl.setDescription("PSE Sample Peer Group Implementation");
//
//        // FIXME bondolo Use something else to edit the params.
//        StdPeerGroupParamAdv params = new StdPeerGroupParamAdv(newGroupImpl.getParam());
//
//        Map services = params.getServices();
//
//        ModuleImplAdvertisement aModuleAdv = (ModuleImplAdvertisement) services.get(PeerGroup.membershipClassID);
//
//        services.remove(PeerGroup.membershipClassID);
//
//        ModuleImplAdvertisement implAdv = (ModuleImplAdvertisement) AdvertisementFactory.newAdvertisement(
//                ModuleImplAdvertisement.getAdvertisementType());
//
//        implAdv.setModuleSpecID(PSEMembershipService.pseMembershipSpecID);
//        implAdv.setCompat(aModuleAdv.getCompat());
//        implAdv.setCode(PSEMembershipService.class.getName());
//        implAdv.setUri(aModuleAdv.getUri());
//        implAdv.setProvider(aModuleAdv.getProvider());
//        implAdv.setDescription("PSE Membership Service");
//
//        // Add our selected membership service to the peer group service as the
//        // group's default membership service.
//        services.put(PeerGroup.membershipClassID, implAdv);
//
//        // Save the group impl parameters
//        newGroupImpl.setParam((Element) params.getDocument(MimeMediaType.XMLUTF8));
//
//        return newGroupImpl;
//    }
    
    public void configureNetwork() {

        displayLog("[######################################]");
        displayLog("[#]Step 4 : Configuring and starting the network manager");
        try {
            MyNetworkManager = new NetworkManager(B00mConfigMode, PeerName, ConfigurationFile.toURI());
            
            MyNetworkConfigurator = MyNetworkManager.getConfigurator();

            MyNetworkConfigurator.setHome(ConfigurationFile);
           
            //### TODO -> GET RDVIP NODES FROM A GOOGLE IP
            //  wget --no-check-certificates https://sites.google.com/feeds/content/zworld.co/infra?path=/home
            // see: Tools.DownloadFile(url);
            //parse the html retrieve : IPs are on the format:
            // <div dir="ltr">IP1
            //<div>IP2</div>
            //</div>
            MyNetworkConfigurator.clearRendezvousSeeds();
            MyNetworkConfigurator.clearRelaySeeds();
            
            MyNetworkConfigurator.addRdvSeedingURI(Kb00m.APP_NODES);
            MyNetworkConfigurator.addRelaySeedingURI(Kb00m.APP_NODES);
            

            MyNetworkConfigurator.setTcpEnabled(true);
            MyNetworkConfigurator.setHttpEnabled(false);
            MyNetworkConfigurator.setHttp2Enabled(false);
            MyNetworkConfigurator.setUseMulticast(true);

            MyNetworkConfigurator.setHttp2Incoming(true);
            MyNetworkConfigurator.setHttp2Outgoing(true);
            MyNetworkConfigurator.setHttp2Port(HttpPort);

            MyNetworkConfigurator.setTcpIncoming(true);
            MyNetworkConfigurator.setTcpOutgoing(true);
            MyNetworkConfigurator.setTcpPort(TcpPort);

            MyNetworkConfigurator.setMulticastPort(1664);
            MyNetworkConfigurator.setMulticastAddress("224.5.1.8");
            //MyNetworkConfigurator.setMulticastInterface("224.0.0.1");

            MyNetworkConfigurator.setPeerID(b00mPeerID);
            MyNetworkConfigurator.setName(PeerName);
            MyNetworkConfigurator.setDescription("OnLine");

            MyNetworkConfigurator.setKeyStoreLocation(KeystoreFile.toURI());
            MyNetworkConfigurator.setPassword(MyJxtaPassword);
            MyNetworkConfigurator.setCertificate(PSEX509Certificate);
            MyNetworkConfigurator.setPrivateKey(PSEPrivateKey);


        } catch (IOException e) {
            displayLog("[!]IOException, unable to create/load JXTA configuration file.\n"+e.getMessage());
        }

    }
    
    public void retrieveGlobalGroupInfos() {
        displayLog("[######################################]");
        displayLog("[#]Step 5 : Getting info on the peer group + adding trusted certificates");
        netPeerGroup = MyNetworkManager.getNetPeerGroup();
        b00mDiscoverySrvc = netPeerGroup.getDiscoveryService();
        
        try {
            PSEMembershipService pms = (PSEMembershipService) netPeerGroup.getMembershipService();
            PSEConfig pcfg = pms.getPSEConfig();
            CertificateFactory cf = CertificateFactory.getInstance("X.509");

            if (!CertificateFile.exists()) {
                FileOutputStream cfos = new FileOutputStream(CertificateFile);
                cfos.write(pcfg.getTrustedCertificate(b00mPeerID).getEncoded());
                cfos.close();
            }

            File[] CRTfiles = CertificateDirectory.listFiles(new Tools().new CRTFilter());
            int i = CRTfiles.length;

            while (i > 0) {
                i--;

                //PeerID loadedPeer = (PeerID) IDFactory.fromURI(URI.create("jxta:uuid-" + Tools.getFilenameWithoutExtension(CRTfiles[i].getName())));
                PeerID loadedPeer = (PeerID) IDFactory.fromURI(URI.create(Tools.getFilenameWithoutExtension(CRTfiles[i].getName())));
                if (!loadedPeer.equals(b00mPeerID)) {
                    displayLog("[+]Adding PeerID to trusted list : " + Tools.getFilenameWithoutExtension(CRTfiles[i].getName()));

                    ByteArrayInputStream bais = new ByteArrayInputStream(Tools.getBytesFromFile(CRTfiles[i]).toByteArray());
                    X509Certificate loadedCertificate = (X509Certificate) cf.generateCertificate(bais);

                    pcfg.setTrustedCertificate(loadedPeer, loadedCertificate);
                }
            }

        } catch (CertificateException e) {
            displayLog("[!]Certificate Exception while retrieving global group infos.\n"+e.getMessage());
        } catch (IOException e) {
            displayLog("[!]IOException while retrieving certificate global group.\n"+e.getMessage());
        } catch (KeyStoreException e) {
            displayLog("[!]Keystore Exception while setting trusted certificate of global group.\n"+e.getMessage());
        } catch (URISyntaxException e) {
            displayLog("[!]URI syntax Exception while loading found peerID.\n"+e.getMessage());
        }

    }
    

    public void startPeerGroupsListing()
    {   //this method will start this Thread
        //Start the listening process
        new Thread("PeerGroupsList "+this.b00mGroup.getPeerGroupName()+" Thread"){
            @Override
            public void run(){
                displayLog("[+]Start Listening Thread for PeerGroup listing.");
                while(!endOfSearch){
                    try{
                        b00mDiscoverySrvc.getRemoteAdvertisements(null,DiscoveryService.GROUP,"Name",null,999);
                        Thread.sleep(TIMEOUT);

                    }catch(Exception e){
                        displayLog("[-]Exception in Searching for PeerGroups process!");
                    }
                }
                displayLog("[-]Listing PeerGroups Stopped.");
            }
        
        };
    }
    
    //This method will return NickName
    protected String getNickName() {
        return Core.PeerName;
    }
    
    protected String getStatus(){
        return this.peerList.getUserStatus(Core.PeerName);
    }
    
    protected UserModel getUserModel(){
        return this.peerList;
    }
    
    protected void changeGroupContext(String groupName){
        this.contextGroup = this.groupList.getGroupByName(groupName);
    }
    
    protected PartTransfertModel getUploadModel(String context){
//        if(context.equals(MAINGROUP_NAME)){
//            //If we are sending message in the main contextPeerGroup
//            return this.b00mCustomGroup.getUploadModel();
//        }else{
            //If we are sending message from a different contextPeerGroup
            return this.contextGroup.getUploadModel();
//        }
    }
    
    protected PartTransfertModel getDownloadModel(String context){
//        if(context.equals(MAINGROUP_NAME)){
//            //If we are sending message in the main contextPeerGroup
//            return this.b00mCustomGroup.getDownloadModel();
//        }else{
            //If we are sending message from a different contextPeerGroup
            return this.contextGroup.getDownloadModel();
//        }
    }
    protected SearchModel getSearchResults(){
        return this.contextGroup.getSearchResultsModel();
    }

    protected LibraryModel getLibrary(String context){
//        if(context.equals(MAINGROUP_NAME)){
//            //If we are sending message in the main contextPeerGroup
//            return this.b00mCustomGroup.getLibraryModel();
//        }else{
            //If we are sending message from a different contextPeerGroup
            return this.contextGroup.getLibraryModel();
//        }
    }

    protected LibraryModel getPrivateLibrary(){
        return this.contextGroup.getPrivateLibraryModel();
    }
    
    //This accessor will return group model
    protected PeerGroupModel getGroupModel() 
    {
        return this.groupList;
    }
    
    //This accessor will return group model
    protected void addPeerGroupInModel(PeerGroupAdvertisement adv) 
    {
        this.groupList.addCustomGroup(adv);
    }
    
    //This accessor will return main group
    protected CustomGroup getB00mGroup() 
    {
        return this.b00mCustomGroup;
    }

    //This accessor will return custom group
    protected CustomGroup getB00mGroup(String customGroup) 
    {
        return this.groupList.getGroupByName(customGroup);
    }

    //This method will update the peer status
    protected void updateUserStatus(String context, String status){
//        if(context.equals(MAINGROUP_NAME)){
//            //If we are sending message in the main contextPeerGroup
//            this.b00mCustomGroup.updateUserStatus(status);
//        }else{
            //If we are sending message from a different contextPeerGroup
            this.contextGroup.updateUserStatus(status);
//        }
    }
    
    //This method will notify that one user is going away
    protected void userLogoff(String userName){
//        this.b00mCustomGroup.updateUserStatus("logoff");
        for(PGroup context : this.groupList.listGroups()){
            context.getGroup().updateUserStatus("logoff");
        }
    }
    
    protected void suspendUpload(String context, String threadName){
//        if(context.equals(MAINGROUP_NAME)){
//            //If we are sending message in the main contextPeerGroup
//            this.b00mCustomGroup.suspendUpload(threadName);
//        }else{
            //If we are sending message from a different contextPeerGroup
            this.contextGroup.suspendUpload(threadName);
//        }
    }
    
    protected void resumeUpload(String context, String threadName){
//        if(context.equals(MAINGROUP_NAME)){
//            //If we are sending message in the main contextPeerGroup
//            this.b00mCustomGroup.resumeUpload(threadName);
//        }else{
            //If we are sending message from a different contextPeerGroup
            this.contextGroup.resumeUpload(threadName);
//        }
    }
    
    protected void seenDownload(String context){
//        if(context.equals(MAINGROUP_NAME)){
//            //If we are sending message in the main contextPeerGroup
//            this.b00mCustomGroup.seenDownload();
//        }else{
            //If we are sending message from a different contextPeerGroup
            this.contextGroup.seenDownload();
//        }
    }
    
    //Allow the listing of Peers through CLI
    protected String listUsersCli(){
        String resp = "########## List of connected Peers #############\n";
        List<String> tmp = this.peerList.listUsers();
        Integer i = 1;
        for(String usr : tmp){
            resp += "User #"+i.toString()+": "+ usr + "\n";
            i++;
        }
        
        return resp;
    }
    
    //Allow the listing of Peer Groups through CLI
    protected String listGroupsCli(){
        String resp = "########## List of known Groups #############\n";
        List<PGroup> tmp = this.groupList.getGroups();
        for(int i = 0; i<tmp.size(); i++){
            PGroup grp = tmp.get(i);
            resp += "Group #"+grp.toString()+": "+ grp.getName() + "("+grp.getDesc()+")\n";
            i++;
        }
        
        return resp;
    }
    
    //Allow the listing of public Shares through CLI
    protected String listSharesCli(){
        return listSharesCli(MAINGROUP_NAME);
    }
    
    //Allow the listing of specific group Shares through CLI
    protected String listSharesCli(String grp){
        String resp = "########## List of shares in group "+grp+" #############\n";
        List<LibraryNode> tmp = this.library.getShares();
        Integer i = 1;
        for(LibraryNode share : tmp){
            resp += "Share #"+i.toString()+": "+ share.getName() + "\n";
            i++;
        }
        
        return resp;
    }
    
//    protected void registerFrame(JFrame main){
//        this.mainFrame = main;
//    }
    
//    protected List listGlobalUsers(){
//        return b00mCustomGroup.getPeerList();
//    }
    
    protected List listGroups(){
        return this.groupList.listGroups();
    }
    
//    protected void createCustomPeerGroup(String peerGroupName, String peerGroupDesc, String peerGroupAuth, String peerGroupPass, JProgressBar pbar, JLabel loadLabel){
////        int nbGroups = 0;
////        
////        //Positioning the number of peerGroups
////        if(customGRP == null){
////            nbGroups = 0;
////        }else{
////            nbGroups = customGRP.length;
////        }
////        
//        //Starting the process
//        pbar.setValue(10);
//        pbar.setEnabled(true);
//        
//        knownGroups.addCustomGroup(peerGroupName, peerGroupDesc, null);
//        knownGroups.getCustomGroupByName(peerGroupName).setGroup(new CustomGroup(b00mGroup, peerGroupName, peerGroupDesc, pbar, loadLabel));
//        
//        loadLabel.setText("Custom Group "+peerGroupName+" is now created !!");
//        if(Kb00m.GUI_MODE) updatePeerGroupList();
//    }
    
    protected void addChatService(String pgName,JTextArea chatOutput){
        this.contextGroup.addChat();
    }

    protected void addShareService(String pgName, JTable shareOutput){
        this.contextGroup.addShare(sharedPath);
//        knownGroups.getGroupByName(pgName).refreshSharedFilesList(shareOutput);
    }

//    protected void downloadFile(String context, String seeder, String fileName, String hash, String size){
////        if(context.equals(MAINGROUP_NAME)){
////            //If we are sending message in the main contextPeerGroup
////            this.b00mCustomGroup.downloadFile(MAINGROUP_NAME, seeder, fileName, hash, size, part);
////        }else{
//            //If we are sending message from a different contextPeerGroup
//            this.contextGroup.downloadFile(context, seeder, fileName, hash, size);
////        }
//    }
    
    protected void downloadFile(String context, SearchNode node){
//        if(context.equals(MAINGROUP_NAME)){
//            //If we are sending message in the main contextPeerGroup
//            this.b00mCustomGroup.downloadFile(MAINGROUP_NAME, node, part);
//        }else{
            //If we are sending message from a different contextPeerGroup
            this.contextGroup.downloadFile(context, node);
//        }
    }
    
    protected void sendChatMsg(String context,String text){
//        if(context.equals(MAINGROUP_NAME)){
//            //If we are sending message in the main contextPeerGroup
//            this.b00mCustomGroup.sendChatMesg(text);
//        }else{
            //If we are sending message from a different contextPeerGroup
            this.contextGroup.sendChatMesg(text);
//        }
        //knownGroups.getGroupByName(contextPeerGroup).sendChatMesg(text);
    }
    
    protected void searchFile(String context, String pattern, String category, boolean track){
//        if(context.equals(MAINGROUP_NAME)){
//            //If we are sending message in the main contextPeerGroup
//            this.b00mCustomGroup.searchFile(pattern,category);
//        }else{
            //If we are sending message from a different contextPeerGroup
            this.contextGroup.searchFile(pattern,category, track);
//        }
    }

//    protected void refreshSharedFilesList (String contextShared, JTable mySharedTable){
//        if(contextShared.equals(MAINGROUP_NAME)){
//            b00mCustomGroup.refreshSharedFilesList(mySharedTable);
//        }else{
//            knownGroups.getGroupByName(contextShared).refreshSharedFilesList(mySharedTable);
//        }
//    }

    protected void shareDirectoryInGroup(String context,File shareDir){
//        if(context.equals(MAINGROUP_NAME)){
//            this.b00mCustomGroup.addSharedDirectory(context,shareDir);
//        }else{
            this.contextGroup.addSharedDirectory(context,shareDir);
//        }
    }
    
    protected void suspendSearch(String context, String pattern){
//        if(context.equals(MAINGROUP_NAME)){
//            //If we are sending message in the main contextPeerGroup
//            this.b00mCustomGroup.suspendSearch(pattern);
//        }else{
            //If we are sending message from a different contextPeerGroup
            this.contextGroup.suspendSearch(pattern);
//        }
    }
    
    protected void stopSearch(String context, String pattern){
//        if(context.equals(MAINGROUP_NAME)){
//            //If we are sending message in the main contextPeerGroup
//            this.b00mCustomGroup.stopSearch(pattern);
//        }else{
            //If we are sending message from a different contextPeerGroup
            this.contextGroup.stopSearch(pattern);
//        }
    }
    
    protected void resumeSearch(String context, String pattern){
//        if(context.equals(MAINGROUP_NAME)){
//            //If we are sending message in the main contextPeerGroup
//            this.b00mCustomGroup.resumeSearch(pattern);
//        }else{
            //If we are sending message from a different contextPeerGroup
            this.contextGroup.resumeSearch(pattern);
//        }
    }
    
    protected void makeTracker(SearchNode node){
        this.contextGroup.makeTracker(node);
    }
    
    protected void revertTracker(SearchNode node){
        this.contextGroup.revertTracker(node);
    }

//    private void updatePeerGroupList(){
//        //Update the Peer List in GUI
//        peerGroupList.addItem(new DefaultComboBoxModel(knownGroups.listGroupsName().toArray()));
//    }

    //React on discoveryEvent dealing with peerGroup adv
    @Override
    public void discoveryEvent(DiscoveryEvent event)
    {
        DiscoveryResponseMsg res = event.getResponse();
        PeerGroupAdvertisement myAdv = null;
        Enumeration en = res.getAdvertisements();
        
        //Assigning new Custom groups to Collection and show them
        if(en != null){
            while(en.hasMoreElements()){
                try{
                    //Test if the discovery is a group one
                    myAdv = (PeerGroupAdvertisement) en.nextElement();
                    
                    //If the group is already known just skip it
//                    if(this.knownGroups.isKnownGroup(myAdv.getPeerGroupID())) return;
                    if(this.groupList.isKnownGroup(myAdv.getPeerGroupID())) return;
                    
                    //If the group is unknown, add it
                    displayLog("[+]New group discovered: "+myAdv.getName());
                    displayLog("[+]Group Desc: "+myAdv.getDescription());
                    displayLog("[+]Group ID: "+myAdv.getPeerGroupID());
                    this.groupList.addCustomGroup(myAdv);
//                    this.knownGroups.addCustomGroup(myAdv.getName(), myAdv.getDescription(), myAdv.getPeerGroupID().toString());                    
                    
                }catch(ClassCastException e){
                    //This is not a PeerGroup advertisement skipping it
                }
            }
            //If we are under graphical contextPeerGroup, add the group Name to list
//            if(Kb00m.GUI_MODE) GUI.addPeerGroup(myAdv.getName());
                //updatePeerGroupList();
        }
    }


    //this listener will respond to incoming messages and show them in Designated area
    @Override
    public void pipeMsgEvent(PipeMsgEvent ev)
    {
        String TestPass = null;
        displayLog("[+] Direct Message Received...");
        // We received a message
        Message myMessage = null;
        try{
            myMessage = ev.getMessage();
            if(myMessage == null){
                displayLog("[-] Direct Message received was null... :(");
                return;
            }
            //Assigning values to wanted Tages
            MessageElement me = myMessage.getMessageElement("PeerName");
            MessageElement me1 = myMessage.getMessageElement("SessionID");
            MessageElement me2 = myMessage.getMessageElement("Message");
            MessageElement me3 = myMessage.getMessageElement("FileName");
            MessageElement me4 = myMessage.getMessageElement("FileSize");
            MessageElement me5 = myMessage.getMessageElement("FileHash");
            MessageElement me6 = myMessage.getMessageElement("Password");
            MessageElement me7 = myMessage.getMessageElement("Seed");
            MessageElement me8 = myMessage.getMessageElement("Type");
            MessageElement me9 = myMessage.getMessageElement("Time");

            String remoteName = me.toString();
//            displayLog("[+] RemotePeer: "+remoteName);
            String sessionID = me1.toString();
//            displayLog("[+] SessionID: "+sessionID);
            String msgContent = me2.toString();
//            displayLog("[+] message: "+msgContent);
            String sentTime = me9.toString();
//            displayLog("[+] Time: "+sentTime);
            String fileName = me3.toString();
//            displayLog("[+] file: "+fileName);
            String fileSize = me4.toString();
//            displayLog("[+] fileSize: "+fileSize);
            String fileHash = me5.toString();
//            displayLog("[+] fileHash: "+fileHash);
            String passwd = me6.toString();
//            displayLog("[+] passwd: "+passwd);
            String rseed = me7.toString();
//            displayLog("[+] seed: "+rseed);
            String msgType = me8.toString();
//            displayLog("[+] Type: "+msgType);
            
            //Don't deal with our own messages or strange ones without sender
            if(remoteName != null && remoteName.equals(PeerName)) return;

            //Check if the Type is defined
            if(msgType!=null){
                
                //If the message require a password
                if(passwd!=null && !passwd.equals("EMPTY")){
                    boolean asking = true;
                        int count = 0;
                        while(asking){
                            if(Kb00m.GUI_MODE){//If we're GUI compliant
                                TestPass = JOptionPane.showInputDialog(null,
                                                "YOU HAVE RECEIVED A PROTECTED MESSAGE FROM "+remoteName+"\nPlease enter the password used to decrypt it \t",
                                                "Password Confirmation",
                                                JOptionPane.INFORMATION_MESSAGE);

                            }else{//If we're CLI-mode
                                displayLog("\n#####################################\n"+
                                        "[##]YOU HAVE RECEIVED A PROTECTED MESSAGE FROM "+remoteName+"\n[##]Please enter the password used to decrypt it (try #"+(count+1)+"/5:");
                                Scanner LineInput=new Scanner(System.in);
                                TestPass = LineInput.nextLine();
                            }
                            try {
                                //We allow 5 tries... wich is quite sufficient
                                if(Tools.getHash(TestPass).equals(passwd)){
                                    displayLog("[+]PASSWORD FOUND!!");
                                    asking=false;
                                }
                                else if(count==4){
                                    if(Kb00m.GUI_MODE) JOptionPane.showMessageDialog(null, "Maximum try excedeed, sorry...","PASSWORD ERROR",  JOptionPane.ERROR_MESSAGE);
                                    displayLog("[!]Maximum tries exceeded for typing password: refusing delivery of message from "+remoteName);
                                    return;
                                }else{
                                    if(Kb00m.GUI_MODE) JOptionPane.showMessageDialog(null, "Incorrect password, try again #"+(count+1)+"/5","PASSWORD ERROR", JOptionPane.ERROR_MESSAGE);
                                    displayLog("[-]Incorrect password try #"+(count+1)+"/5");
                                }
                                count++;
                            } catch (Exception ex) {
                                Logger.getLogger(Core.class.getName()).log(Level.WARNING, null, ex);
                            }
                        }

                        try {
                            String[] remoteSeed = rseed.split(SPLIT_CHAR);
                            msgContent = Tools.decryptAES(msgContent,TestPass,Base64.decode(remoteSeed[0]), Base64.decode(remoteSeed[1]));
                            
                            //Test if message is empty
                            if(msgContent.equals("EMPTY")) msgContent = "";
                            displayLog("[!!]Msg decrypted : "+msgContent);
                            //fileName = Tools.decryptAES(fileName,TestPass,Base64.decode(remoteSeed[0]), Base64.decode(remoteSeed[1]));
                            
                            if(Kb00m.GUI_MODE){
                                JPrivateMessage pm = new JPrivateMessage(b00mCustomGroup.getShare(),b00mGroup,remoteName,fileName,fileSize,fileHash,msgContent,activeDownload);
//                                pm.displayMessage(false);
                            }
                            
                        } catch (Exception ex) {
                            displayLog(ex.getMessage());
                        }

                    }else{ //If the message is NOT password protected
                        if(Kb00m.GUI_MODE){
                            JPrivateMessage pm = new JPrivateMessage(b00mCustomGroup.getShare(),b00mGroup,remoteName,fileName,fileSize,fileHash,msgContent,activeDownload);
//                            pm.displayMessage(false);
                        }else{//We're not under a graphical contextPeerGroup
                            JPrivateMessage pm = new JPrivateMessage(b00mCustomGroup.getShare(),b00mGroup,remoteName,fileName,fileSize,fileHash,msgContent,null);
//                            pm.displayMessage(false);
                        }
                    }
                    
                    
                    displayLog("####################################\n"+
                                "##\tNEW MESSAGE\t##\n"+
                                "#####################################");
                    displayLog("[##]From: [" + remoteName+ "@" + sentTime +"]");
                    displayLog("[##]Message: " + msgContent);
                    displayLog("[##]File send privatly: ("+fileName+")");
                    displayLog("[##]FileSize: ("+fileSize+")");
                    displayLog("[##]FileHash: ("+fileHash+")");
                    displayLog("######################################");
                }

    
        }catch(Exception e){
            displayLog("[!]Exception happened when trying to parse Private Message element!"+e.getMessage());
        }
    }
//    
//    @Override
//    public void rendezvousEvent(RendezvousEvent event) {
//           
//        if ( event == null ) return;
//
//        if ( event.getType() == RendezvousEvent.RDVCONNECT ) {
//            displayLog("[+]Connection to RDV");
//        } else if ( event.getType() == RendezvousEvent.RDVRECONNECT ) {
//            displayLog("[+]Reconnection to RDV");
//        } else if ( event.getType() == RendezvousEvent.CLIENTCONNECT ) {
//            displayLog("[+]EDGE client connection");
//        } else if ( event.getType() == RendezvousEvent.CLIENTRECONNECT ) {
//            displayLog("[+]EDGE client reconnection");
//        } else if ( event.getType() == RendezvousEvent.RDVDISCONNECT ) {
//            displayLog("[+]Disconnection from RDV");
//        } else if ( event.getType() == RendezvousEvent.RDVFAILED ) {
//            displayLog("[+]Connection to RDV failed");
//        } else if ( event.getType() == RendezvousEvent.CLIENTDISCONNECT ) {
//            displayLog("[+]EDGE client disconnection from RDV");
//        } else if ( event.getType() == RendezvousEvent.CLIENTFAILED ) {
//            displayLog("[+]EDGE client connection to RDV failed");
//        } else if ( event.getType() == RendezvousEvent.BECAMERDV ) {
//            displayLog("[+]This peer became RDV");
//        } else if ( event.getType() == RendezvousEvent.BECAMEEDGE ) {
//            displayLog("[+]This peer became EDGE");
//        }
//    }
//
//    @Override
//    public void peerViewEvent(PeerViewEvent paramPeerViewEvent) {
//        displayLog("[+]Received PeerView Event advert for peer "+paramPeerViewEvent.getPeerViewElement().getRdvAdvertisement().getName());
//                    
//        if(paramPeerViewEvent.getType() == PeerViewEvent.ADD){
//            displayLog("[+]Adding peer");
//            knownPeers.addUser(paramPeerViewEvent.getPeerViewElement().getRdvAdvertisement().getName(), paramPeerViewEvent.getPeerViewElement().getPeerID().toString());
//            knownPeers.updateUserStatus(paramPeerViewEvent.getPeerViewElement().getRdvAdvertisement().getName(), "Online");
//            
//        }else if(paramPeerViewEvent.getType() == PeerViewEvent.REMOVE){
//            displayLog("[+]Deleting peer");
//            knownPeers.removeUserByName(paramPeerViewEvent.getPeerViewElement().getRdvAdvertisement().getName());
//        }
//        
//        //discoverySrvc.getRemoteAdvertisements(null,DiscoveryService.PEER,null,null,999);
//        //listModel.clear();
//        if(gui) updatePeerList();
//        //Thread.sleep(3*TIMEOUT);
//    }
}
