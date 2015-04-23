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
import java.io.StringWriter;
import java.util.Enumeration;
import java.util.SortedSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JProgressBar;
import net.jxta.credential.AuthenticationCredential;
import net.jxta.credential.Credential;
import net.jxta.discovery.DiscoveryEvent;
import net.jxta.discovery.DiscoveryListener;
import net.jxta.discovery.DiscoveryService;
import net.jxta.document.MimeMediaType;
import net.jxta.document.StructuredDocument;
import net.jxta.document.StructuredTextDocument;
import net.jxta.exception.PeerGroupException;
import net.jxta.exception.ProtocolNotSupportedException;
import net.jxta.id.IDFactory;
import net.jxta.impl.membership.pse.PSEAuthenticatorEngine;
import net.jxta.impl.membership.pse.PSEMembershipService;
import net.jxta.impl.membership.pse.PSEMembershipService.PSEAuthenticatorEngineFactory;
import net.jxta.impl.membership.pse.StringAuthenticator;
import net.jxta.impl.protocol.PSEConfigAdv;
import net.jxta.impl.rendezvous.RendezVousServiceImpl;
import net.jxta.impl.rendezvous.rpv.PeerView;
import net.jxta.impl.rendezvous.rpv.PeerViewElement;
import net.jxta.impl.rendezvous.rpv.PeerViewEvent;
import net.jxta.impl.rendezvous.rpv.PeerViewListener;
import net.jxta.membership.MembershipService;
import net.jxta.peer.PeerID;
import net.jxta.peergroup.PeerGroup;
import net.jxta.peergroup.PeerGroupID;
import net.jxta.pipe.PipeService;
import net.jxta.platform.Module;
import net.jxta.protocol.DiscoveryResponseMsg;
import net.jxta.protocol.ModuleImplAdvertisement;
import net.jxta.protocol.PeerAdvertisement;
import net.jxta.protocol.PeerGroupAdvertisement;
import net.jxta.rendezvous.RendezVousService;
import net.jxta.rendezvous.RendezvousEvent;
import net.jxta.rendezvous.RendezvousListener;
import net.jxta.resolver.ResolverService;

/**
 *
 * @author usr
 */
public final class CustomGroup extends Thread implements DiscoveryListener, RendezvousListener, PeerViewListener {
   
    //Graphical Variables
    public boolean endOfSearch = false;
    //private JList usrList = null;
    //private ComboBoxModel pgList = null;
    
    //Specific group variable
//    private final int RDV_PROMOTION_TIMEOUT = 1000;
    private final static int TIMEOUT = 1000;
    private PeerGroup parentGroup = null,
            childGroup = null;
    
    //Group Services
    private DiscoveryService discoverySrvc = null;
    private RendezVousService rdvSrvc = null;
    private ResolverService resolverSrvc = null;
    private PipeService pipeSrvc = null;
    private PeerView peerView = null;
    private boolean addListener = false;
    private PeerGroupAdvertisement grpAdv = null;
    private PeerAdvertisement usrAdv = null;
    
    private PeerGroupID ID = null;
    private Chat chat = null;
    private Share share = null;
    //private Remora r = null;
    
    //Local data storage
//    public static DATA peerList = null;
    private UserModel peerList = new UserModel();
    //private DATA peerGroupList = null;
    
//    public CustomGroup(boolean search, PeerGroup parent, String name, String desc, boolean main) throws PeerGroupException{
//        this.parentGroup = parent;
//        this.discoverySrvc = parentGroup.getDiscoveryService();
//        this.resolverSrvc = parent.getResolverService();
//        
//        if(search && isKnown(name)){
//            childGroup = parentGroup.newGroup(grpAdv);
//        }else{
//            childGroup = createGroup(name,desc);
//        }
//        
//    }
//    
//    public CustomGroup(boolean search, PeerGroup parent, String name, String desc, String password, boolean main) throws PeerGroupException{
//        this.parentGroup = parent;
//        this.discoverySrvc = parentGroup.getDiscoveryService();
//        this.resolverSrvc = parentGroup.getResolverService();
//        
//        if(search && isKnown(name)){
//            childGroup = parentGroup.newGroup(grpAdv);
//        }
//        
//    }
//    
//    public CustomGroup(PeerGroup parent, PeerGroup child) throws PeerGroupException{
//        this.parentGroup = parent;
//        this.childGroup = child;
//        this.rdvSrvc = parentGroup.getRendezVousService();
//        this.discoverySrvc = parentGroup.getDiscoveryService();
//        this.grpAdv = child.getPeerGroupAdvertisement();
//        this.usrAdv = parent.getPeerAdvertisement();
//        this.resolverSrvc = parent.getResolverService();
//    }
    
    public CustomGroup(PeerGroup parent, String name, String desc, boolean publish, boolean mainGrp) throws PeerGroupException{
        this.parentGroup = parent;
        this.discoverySrvc = this.parentGroup.getDiscoveryService();
        this.resolverSrvc = this.parentGroup.getResolverService();
        
        //Check if we already know the group
        if(!isKnown(name)){
            if(mainGrp)
                this.childGroup = createGroup(name, desc, publish);
            else
                this.childGroup = createSubGroup(name, desc, publish);
        }else{
            this.childGroup = this.parentGroup.newGroup(this.grpAdv);
        }
        
    }
    
    @Override
    public void start(){
        joinPeerGroup(this.childGroup.getPeerID(),"myjxtaPassword");
    }
    
    private void displayLog(String s){
        if(Kb00m.GUI_MODE){
            GUI.addLogMsg(s);
        }else{
            System.out.println(s);
        }
    }

    private boolean isKnown(String name){
        displayLog("[######################################]");
        displayLog("[#]Searching for group: "+name);
            
        Enumeration adv = null;    //will create the group itself.   
        int count =1;
        displayLog("[+]Searching for "+name+" Advertisements.");
        //discoverySrvc.getRemoteAdvertisements(null,DiscoveryService.GROUP,"Name",name,10);
        while(count <= 3){
            try {
                displayLog("[+]Try Number: " + (count) + "/3");
                //Searching for Group Advertisements , first from local cach if not
                //found then search from remote peers
                adv = discoverySrvc.getLocalAdvertisements(DiscoveryService.GROUP,"Name",name);
                if((adv != null) && adv.hasMoreElements()){
                    displayLog("[+]Group "+name+" found in Local advertisement.");
                    grpAdv = (PeerGroupAdvertisement)adv.nextElement();
                    return true;
                }else{
                    displayLog("[-]No Group Found in Local advertisement.\n[+]Starting Remote Search...");
                    discoverySrvc.getRemoteAdvertisements(null,DiscoveryService.GROUP,"Name",name,10);
                    Thread.sleep(2*TIMEOUT);
                }
                //if group not found after couple of tries it will return false
                if((count == 3) && (adv == null || !adv.hasMoreElements())){
                    return false;
                }
                
            } catch (InterruptedException ex) {
                displayLog("[*]Interrupted Fatal Error while searching/creating group:" + ex.getMessage());
                
            } catch (IOException ex) {
                displayLog("[*]IOFatal Error while searching/creating group:" + ex.getMessage());
                
            }
            count++;
        
        }
        return false;
    }
    
    private PeerGroup createGroup(String name, String desc, boolean publish) //This method will Create main group
    {
        displayLog("[######################################]");
        displayLog("[#]Please wait during creation of the group "+name);
        displayLog("[======================================]");
        displayLog("[+]Creating New Group "+name+"...");
        
        try{
            //specifying advertisement for group and configure group, then publish it
            //Advertisement for remote peers
            //ModuleImplAdvertisement myMIA = netPeerGroup.getAllPurposePeerGroupImplAdvertisement();
            ModuleImplAdvertisement myMIA = Tools.createAllPurposePeerGroupWithPSEModuleImplAdv(name, desc);
            this.ID = IDFactory.newPeerGroupID(this.parentGroup.getPeerGroupID(), name.getBytes());
            
            this.childGroup = this.parentGroup.newGroup(this.ID,
                                        myMIA,
                                        name,
                                        desc,
                                        publish);
            
            // A simple check to see if connecting to the group worked
            if (Module.START_OK != this.childGroup.startApp(new String[0]))
            displayLog("[!]Cannot start "+name+" peergroup");

            //retrieving new group advertisements
            this.grpAdv = this.childGroup.getPeerGroupAdvertisement();
                            
            displayLog("[+]New Peer Group Successfully created :-)");
            displayLog("[+]Publishing new Group Advertisements.");
            displayLog("[+]Group Information:");
            displayLog("[======================================]");
            displayLog("[+]Group Name: " + grpAdv.getName());
            displayLog("[+]Group ID:" + grpAdv.getPeerGroupID().toString());
            displayLog("[+]Group Description: " + grpAdv.getDescription());
            displayLog("[+]Group Module ID: " + grpAdv.getModuleSpecID().toString());
            displayLog("[+]Advertisement Type: " + PeerGroupAdvertisement.getAdvertisementType());
            displayLog("[======================================]");
            
        }catch(Exception e){
            System.out.println("[*]Fatal Error while creating PeerGroup "+name+":" + e.getMessage());
            System.exit(-1);
        }
        
        //return the new created group
        return childGroup;
    }
    
    protected PeerGroup createSubGroup(String name, String desc, boolean publish) //This method will Create sub group
    {
        displayLog("[######################################]");
        displayLog("[#]Please wait during creation of the sub-group "+name);
        displayLog("[======================================]");
        displayLog("[+]Creating New Group "+name+"...");
        
        try{
            //specifying advertisement for group and configure group, then publish it
            //Advertisement for remote peers
//            ModuleImplAdvertisement myMIA = parentGroup.getAllPurposePeerGroupImplAdvertisement();
//            ModuleImplAdvertisement myMIA = Tools.createAllPurposePeerGroupImplAdv(name, desc);
            ModuleImplAdvertisement myMIA = Tools.createAllPurposePeerGroupWithPSEModuleImplAdv(name, desc);
            this.ID = IDFactory.newPeerGroupID(this.parentGroup.getPeerGroupID(), name.getBytes());
            
            
            this.childGroup = this.parentGroup.newGroup(this.ID,
                                        myMIA,
                                        name,
                                        desc,
                                        publish);
            
            // A simple check to see if connecting to the group worked
            if (Module.START_OK != this.childGroup.startApp(new String[0]))
            displayLog("[!]Cannot start "+name+" peergroup");

            //retrieving new group advertisements
            this.grpAdv = this.childGroup.getPeerGroupAdvertisement();
                            
            displayLog("[+]New Peer Group Successfully created :-)");
            displayLog("[+]Publishing new Group Advertisements.");
            displayLog("[+]Group Information:");
            displayLog("[======================================]");
            displayLog("[+]Group Name: " + this.grpAdv.getName());
            displayLog("[+]Group ID:" + this.grpAdv.getPeerGroupID().toString());
            displayLog("[+]Group Description: " + this.grpAdv.getDescription());
            displayLog("[+]Group Module ID: " + this.grpAdv.getModuleSpecID().toString());
            displayLog("[+]Advertisement Type: " + PeerGroupAdvertisement.getAdvertisementType());
            displayLog("[======================================]");
            
        }catch(Exception e){
            System.out.println("[*]Fatal Error while creating sub PeerGroup "+name+":" + e.getMessage());
            System.exit(-1);
        }
        
        //return the new created group
        return this.childGroup;
    }
    
    //Password authentication constructor
    protected void joinPeerGroup(PeerID pid, String pass) //This method will join to either found group or created group
    {
        displayLog("[######################################]");
        displayLog("[#]Joining Peer Group");
        displayLog("[======================================]");
        StructuredDocument creds = null;
        displayLog("[+]Joining into "+this.childGroup.getPeerGroupName()+" Group..");
        
//        try{
            //Athenticate and join to group
        AuthenticationCredential authCred = new AuthenticationCredential(this.parentGroup,"StringAuthentication",creds);
        MembershipService membership = this.childGroup.getMembershipService();
        displayLog("[+]Group membership implementation: "+membership.getClass().getName());
        try {
            displayLog("[+]Group default credential: "+membership.getDefaultCredential().toString());
        } catch (PeerGroupException ex) {
            displayLog("[!]PeerGroup Exception while retrieving default credential: "+ex.getMessage());
        }
        StringAuthenticator auth;
        try {
            auth = (StringAuthenticator) membership.apply(authCred);
            //auth.setAuth1_KeyStorePassword(MyJxtaPassword);
            auth.setAuth1_KeyStorePassword(pass);
            auth.setAuth2Identity(pid);
            //auth.setAuth3_IdentityPassword(PKeyPassword);
            auth.setAuth3_IdentityPassword(pass);
            
//            displayLog("[+]Authentication credential created "+auth.getCertificate(pass.toCharArray(), pid).getIssuerDN());
                if(auth.isReadyForJoin()){
                    Credential myCred;
                    try {
                        myCred = membership.join(auth);
                        if(myCred!=null){
                            displayLog("[===== Decrypted "+this.childGroup.getPeerGroupName()+" Details =====]");
                            StructuredTextDocument doc;
                            try {
                                doc = (StructuredTextDocument)myCred.getDocument(new MimeMediaType("text/plain"));
                                StringWriter out = new StringWriter();
                                doc.sendToWriter(out);

                                displayLog(out.toString());
                            } catch (Exception ex) {
                                displayLog("[!]Exception while viewing decrypted details from group "+this.childGroup.getPeerGroupName()+": " + ex.getMessage());
                            }


                        }else{
                            displayLog("[!]Error: "+this.childGroup.getPeerGroupName()+" Group Authentication unsuccesful");
                        }
                    } catch (PeerGroupException ex) {
                        displayLog("[!]PeerGroup exception while joining group "+this.childGroup.getPeerGroupName()+": " + ex.getMessage());
                    }
                    
                }
                else{
                    System.out.println("[!!]Fatal Error: Cannot be ready to join the Group "+this.childGroup.getPeerGroupName()+"! ");
    //                System.exit(-1);
                }
        } catch (PeerGroupException ex) {
            displayLog("[!]PeerGroup exception while applying to group "+this.childGroup.getPeerGroupName()+": " + ex.getMessage());
            System.out.println("[!]PeerGroup exception while applying to group "+this.childGroup.getPeerGroupName()+": " + ex.getMessage());
        } catch (ProtocolNotSupportedException ex) {
            displayLog("[!]Fatal Error protocol not supported to join group "+this.childGroup.getPeerGroupName()+": " + ex.getMessage());
            System.out.println("[!]Fatal Error protocol not supported to join group "+this.childGroup.getPeerGroupName()+": " + ex.getMessage());
        }
        
                    
//        }catch(Exception e){
//            System.out.println("[!]Fatal Error cannot join group "+this.childGroup.getPeerGroupName()+": " + e.getMessage());
////            System.exit(-1);
//        }
    }
    
//    protected void registerGraphics(JList userList){
//        this.usrList = userList;
//        //this.pgList = peerGroupList;
//        //this.pgList;
//    }
    
    //retrieve the necessary services in the newly created group
    protected void startGroupServices(){
        try {
            
            displayLog("[+]Obtaining Main Group Discovery Services.");
            this.discoverySrvc = childGroup.getDiscoveryService();
            this.discoverySrvc.addDiscoveryListener(this);
            
            displayLog("[+]Obtaining Main Group RendezVous Services.");
            this.rdvSrvc = childGroup.getRendezVousService();
            this.rdvSrvc.addListener(this);
            
            //r = new Remora(TcpPort,log);
            //this.rdvSrvc.setAutoStart(true,RDV_PROMOTION_TIMEOUT);
            
            
            //This method will discover Peers connected
//            displayLog("[+]Starting PeerView Services.");
//            RendezVousServiceImpl rdvSrvcImpl = new RendezVousServiceImpl();
//            this.peerView = new PeerView(childGroup,childGroup,rdvSrvcImpl,childGroup.getPeerGroupName()+"PEERVIEW");
//            this.peerView.addListener(this);
//            this.peerView.start();
//            this.peerView.seed();
                    
            
                
                
                    
            //If we are not Rendez-Vous for this group, let's announce ourself
            if(!this.rdvSrvc.isRendezVous()){ 
                //This method will discover Peers connected
                displayLog("[+]Starting PeerView Services."); 
                RendezVousServiceImpl rdvSrvcImpl = new RendezVousServiceImpl();
                this.peerView = new PeerView(childGroup,parentGroup,rdvSrvcImpl,childGroup.getPeerGroupName()+"PEERVIEW");
                this.peerView.addListener(this);
                this.peerView.start();
                
                    //Sending our own rdv advertisement
                    this.peerView.seed();
                    
//                    displayLog("[+]Discovering connected peers");
//                    this.peerView.getUpPeer();
//                    this.peerView.getDownPeer();
                    //r.registerSelf();
            }
            
            //Anounce the PeerGroup presence
            displayLog("[+]Publishing PeerGroup Advertisement.");
            discoverySrvc.publish(grpAdv,PeerGroup.DEFAULT_LIFETIME, PeerGroup.DEFAULT_EXPIRATION);
            discoverySrvc.remotePublish(grpAdv,PeerGroup.DEFAULT_LIFETIME);
            childGroup.publishGroup(Core.MAINGROUP_NAME, Core.MAINGROUP_DESC);
            
            //Publish the Peer in PeerGroup
            displayLog("[+]Publishing Peer Advertisement.");
            usrAdv = childGroup.getPeerAdvertisement();
            usrAdv.setName(Core.PeerName);
            usrAdv.setDescription("OnLine");
            discoverySrvc.publish(usrAdv,PeerGroup.DEFAULT_LIFETIME, PeerGroup.DEFAULT_EXPIRATION);
            discoverySrvc.remotePublish(usrAdv,PeerGroup.DEFAULT_LIFETIME);

            //All done, let's start the local data container
            displayLog("[======================================]");
//            peerList.setPeerGroup(childGroup);
//            peerList.addUser(usrAdv.getPeerID().toString(), usrAdv.getName(), "Online");
            peerList.addUser(usrAdv.getPeerID().toString(), "Me", "Online");
            //peerList.updateUserStatus(usrAdv);
            
            displayLog("[+]Peer Name : " + childGroup.getPeerName() + " is now "+childGroup.getPeerAdvertisement().getDescription()+" :-)");
            
        } catch (IOException ex) {
            displayLog("[!]IOError when registring input pipe\n" + ex.getMessage());
        }
    }

//    protected void updatePeerList(){
//        //Update the Peer List in GUI
//        //usrList.setListData(peerList.listUsers().toArray());
//        GUI.updatePeerList(peerList.listUsers());
//    }

    protected PeerGroupAdvertisement getGrpAdv(){
        return this.grpAdv;
    }
    
    protected boolean isRDVConnected(){
        //If the peer is a rdv return true
        if(this.rdvSrvc.isConnectedToRendezVous()) return true;
        
        return false;
    }
    
    protected void startPeersListing()
    {   //this method will start the listening process
        new Thread("PeersList "+this.childGroup.getPeerGroupName()+" Thread"){
            @Override
            public void run(){
                displayLog("[+]Start Listening Thread for Peer listing.");
                while(!endOfSearch){
                    try{
                        SortedSet<PeerViewElement> pList = peerView.getView();
                        for(PeerViewElement p : pList){
                            if(!peerList.isKnownUser(p.getPeerID()) && !p.getRdvAdvertisement().getName().equals(Core.PeerName)) peerList.addUser(p.getPeerID().toString(), p.getRdvAdvertisement().getName(), "OnLine");
                        }
                        discoverySrvc.getRemoteAdvertisements(null,DiscoveryService.PEER,"Name",null,999);
                        Thread.sleep(TIMEOUT);

                    }catch(Exception e){
                        displayLog("[-]Exception in Searching for Peers process!");
                    }
                }
                displayLog("[-]Listing Peers Stopped.");
            }
        
        };
    }
    
    protected UserModel getUserModel(){
        return this.peerList;
    }
    
    protected void stopPeersListing(){
        this.endOfSearch = true;
        this.peerView.stop();
        displayLog("[+]Peer listing sucessfully stopped !!\n");
    }
    
    protected PeerGroup getGroup(){
        //Return newly created PeerGroup
        return this.childGroup;
    }

    protected PeerGroup getParentGroup(){
        //Return newly created PeerGroup
        return this.parentGroup;
    }

    protected void addChat(){
        //Starting Chat Services, which includes chat input and Output.
        this.chat = new Chat(this.childGroup, this.peerList);
        this.chat.start();
        
    }
    
    protected void removeChat(){
        //Stopping Chat Services, which includes chat input and Output.
        this.chat.stopListening();
        displayLog("[+]Chat sucessfully stopped !!\n");
        
    }
    
    protected void addShare(File dir){
        //Starting Share Services
        this.share = new Share(this.childGroup, dir);
        this.share.start();
        
    }
    
    protected void removeShare(){
        //Stopping Share Services
        this.share.stopListening();
        displayLog("[+]Share sucessfully stopped !!\n");
    }
    
    protected void addSharedDirectory(String grp,File dir){
        //Share a directory in a group
        this.share.addSharedDirectory(dir,false);
    }
    
    protected void suspendSearch(String pattern){
        this.share.suspendSearch(pattern);
    }
    
    protected void resumeSearch(String pattern){
        this.share.resumeSearch(pattern);
    }
    
    protected void stopSearch(String pattern){
        this.share.stopSearch(pattern);
    }
    
    protected void suspendUpload(String threadName){
        this.share.suspendUpload(threadName);
    }
    
    protected void resumeUpload(String threadName){
        this.share.resumeUpload(threadName);
    }
    
    protected void seenDownload(){
        this.share.seenDownload();
    }
    
//    protected void refreshSharedFilesList(JTable sharedTable){
//        share.refreshSharedFilesList(sharedTable);
//    }
    
    //Update our status
    protected void updateUserStatus(String status){
        try {
            this.peerList.updateUserStatus("Me", status);
            //peerSrvc.notifyAll();
            this.discoverySrvc.flushAdvertisement(this.usrAdv);
            this.usrAdv.setDescription(status);
            displayLog("[+]Publishing Peer Status modification: "+status);
            this.discoverySrvc.publish(this.usrAdv,PeerGroup.DEFAULT_LIFETIME, PeerGroup.DEFAULT_EXPIRATION);
            this.discoverySrvc.remotePublish(this.usrAdv,PeerGroup.DEFAULT_LIFETIME);
            
//            if(Kb00m.GUI_MODE) updatePeerList();
            
        } catch (IOException ex) {
            displayLog("[!]IOException while publishing Peer Status Advertisement.\n"+ex.getMessage());
        }
                    
    }
    
    protected void sendChatMesg(String text){
        //Send chat messages
        this.chat.sendMsg(text);
    }
    
    protected void searchFile(String pattern, String type, boolean track){
        this.share.searchFile(pattern, type, track);
    }
    
    protected void makeTracker(SearchNode node){
        this.share.makeTracker(node);
    }
    
    protected void revertTracker(SearchNode node){
        this.share.revertTracker(node);
    }
    
//    protected void downloadFile(String context, String seeder, String fileName, String hash, String size){
//        this.share.downloadFile(context, seeder, fileName, hash, size);
//    }
    
    protected void downloadFile(String context, SearchNode node){
        this.share.downloadFile(context, node);
    }
    
//    protected List getPeerList(){
//        //Return the list of known Peers
//        return peerList.listUsers();
//    }
    
    protected SearchModel getSearchResultsModel(){
        return this.share.getSearchModel();
    }
    
    protected PartTransfertModel getDownloadModel(){
        return this.share.getDownloadModel();
    }
    
    protected PartTransfertModel getUploadModel(){
        return this.share.getUploadModel();
    }
    
    protected LibraryModel getLibraryModel(){
        return this.share.getLibraryModel();
    }
    
    protected LibraryModel getPrivateLibraryModel(){
        return this.share.getPrivateLibraryModel();
    }
    
    protected Share getShare(){
        return this.share;
    }
    
    @Override
    public void discoveryEvent(DiscoveryEvent paramDiscoveryEvent) {
        DiscoveryResponseMsg res = paramDiscoveryEvent.getResponse();
        PeerAdvertisement myAdv = null;
        Enumeration en = res.getAdvertisements();
        
        //Assigning new Peers to Collection and show them
        if(en != null){
            while(en.hasMoreElements()){
                try{
                    myAdv = (PeerAdvertisement) en.nextElement();
                    displayLog("[+]Received discovery advert for peer "+myAdv.getName());
                    //If the user is logging off
                    if(myAdv.getDescription().equals("logoff")){
                        displayLog("[!]User "+myAdv.getName()+" is leaving");
                        //Remove the user shares and user found has seeder
                        this.share.removeSeederShares(myAdv.getName());
                        this.share.removeSeederResults(myAdv.getName());
                        //Notify chat
                        this.chat.userLogOff(myAdv.getName());
                        //Remove the user
                        this.peerList.removeUserByName(myAdv.getName());
                        
                    }
                    //If the user is unknown and is not ourselves
                    else if(!this.peerList.isKnownUser(myAdv.getPeerID())){
                        displayLog("[+]Welcome to user "+myAdv.getName());
                        this.peerList.addUser(myAdv);
                    }
                    else{
                        displayLog("[+]Updating user "+myAdv.getName()+" to status "+myAdv.getDescription());
                        this.peerList.updateUserStatus(myAdv.getName(), myAdv.getDescription());
                    }
                    
                }catch(ClassCastException e){
                    //This is not a Peer advertisement, skipping it...
                }
            }
        
//            
//            //........//THE BIG KEY :
//            
//            //Readvertise the usrAdv to EDGE
//            //discoverySrvc.notifyAll();
//            //discoverySrvc.remotePublish(myAdv);
        }
    }

    @Override
    public void rendezvousEvent(RendezvousEvent event) {
           
        if ( event == null ) return;

        if ( event.getType() == RendezvousEvent.RDVCONNECT ) {
            displayLog("[+]Connection to RDV");
            GUI.infos("HAPPY SHARING ;) !!");
//            try {
//                discoverySrvc.publish(usrAdv,PeerGroup.DEFAULT_LIFETIME, PeerGroup.DEFAULT_EXPIRATION);
//                discoverySrvc.remotePublish(usrAdv,PeerGroup.DEFAULT_LIFETIME);
//                
//            } catch (IOException ex) {
//                Logger.getLogger(CustomGroup.class.getName()).log(Level.SEVERE, null, ex);
//            }
            discoverySrvc.getRemoteAdvertisements(event.getPeer(),DiscoveryService.PEER,"Name",null,999);
            
        } else if ( event.getType() == RendezvousEvent.RDVRECONNECT ) {
            displayLog("[+]Reconnection to RDV");
            discoverySrvc.getRemoteAdvertisements(event.getPeer(),DiscoveryService.PEER,"Name",null,999);
            
        } else if ( event.getType() == RendezvousEvent.CLIENTCONNECT ) {
            displayLog("[+]EDGE client connection");
        } else if ( event.getType() == RendezvousEvent.CLIENTRECONNECT ) {
            displayLog("[+]EDGE client reconnection");
        } else if ( event.getType() == RendezvousEvent.RDVDISCONNECT ) {
            displayLog("[+]Disconnection from RDV");
        } else if ( event.getType() == RendezvousEvent.RDVFAILED ) {
            displayLog("[+]Connection to RDV failed");
        } else if ( event.getType() == RendezvousEvent.CLIENTDISCONNECT ) {
            displayLog("[+]EDGE client disconnection from RDV");
            peerList.removeUserByName(event.getPeer());
        } else if ( event.getType() == RendezvousEvent.CLIENTFAILED ) {
            displayLog("[+]EDGE client connection to RDV failed");
        } else if ( event.getType() == RendezvousEvent.BECAMERDV ) {
            displayLog("[+]This peer became RDV");
        } else if ( event.getType() == RendezvousEvent.BECAMEEDGE ) {
            displayLog("[+]This peer became EDGE");
        }
        
        //If we are under graphical environement update the peer list
        //if(gui) updatePeerList();
    }

    @Override
    public void peerViewEvent(PeerViewEvent paramPeerViewEvent) {
        String userName = null;
        displayLog("[+]Received PeerView Event advert for peer "+paramPeerViewEvent.getPeerViewElement().getRdvAdvertisement().getName());
        
        //Check the event type
        //Adding a new peer
        if(paramPeerViewEvent.getType() == PeerViewEvent.ADD){
            //If User is unknown let's add it
            displayLog("[+]Adding peer");
            if(!this.peerList.isKnownUser(paramPeerViewEvent.getPeerViewElement().getRdvAdvertisement().getName())){ //If the user is unknown
                this.peerList.addUser(paramPeerViewEvent.getPeerViewElement().getRdvAdvertisement().getPeerID().toString(), paramPeerViewEvent.getPeerViewElement().getRdvAdvertisement().getName(), "OnLine");
            }
        }//Removing a new peer
        else if (paramPeerViewEvent.getType() == PeerViewEvent.REMOVE) {
            userName = paramPeerViewEvent.getPeerViewElement().getRdvAdvertisement().getName();
            displayLog("[!]User " + userName + " is leaving");

            //Remove the user shares and user found has seeder
            this.share.removeSeederShares(userName);
            this.share.removeSeederResults(userName);
            //Notify chat
            this.chat.userLogOff(userName);
            //Remove the user
            this.peerList.removeUserByName(userName);
            this.peerList.removeUserByName(userName);
        }
        //If fail event, exit process
    }
}