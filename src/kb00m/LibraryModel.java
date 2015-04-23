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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.Security;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.ImageIcon;
import net.jxta.content.Content;
import net.jxta.content.ContentID;
import net.jxta.impl.content.srdisocket.SRDIContentShare;
import net.jxta.impl.content.srdisocket.SRDISocketContentProvider;
import net.jxta.peergroup.PeerGroup;
import net.jxta.protocol.ContentAdvertisement;
import net.jxta.protocol.PipeAdvertisement;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.encoders.Hex;
import org.jdesktop.swingx.treetable.AbstractMutableTreeTableNode;
import org.jdesktop.swingx.treetable.AbstractTreeTableModel;
import org.jdesktop.swingx.treetable.TreeTableModel;
import org.jdesktop.swingx.treetable.TreeTableNode;


final class LibraryModel extends AbstractTreeTableModel implements TreeTableModel {

    private List<LibraryNode> rootDirList = new ArrayList<LibraryNode>();
    private HashMap<String, ArrayList<LibraryNode>> dirMap = new HashMap<String, ArrayList<LibraryNode>>();
    private HashMap<String, String> shareDic = new HashMap<String,String>();
    private LibraryNode root_ = null;
    private PeerGroup groupContext = null;
    
    protected LibraryModel(PeerGroup group, File dirFile) {
        this.groupContext = group;
        this.root_ = new LibraryNode(this.groupContext, dirFile);
        LibraryNode rootParent = addDirectory(dirFile);
        this.root_.setParent(rootParent);
    }
    
    protected LibraryNode addDirectory(File dir){
        LibraryNode dirNode = null;
        //Create a directory root Node
//        if(defNode)
//            dirNode = new LibraryNode(this.root_, dir);
//        else
            dirNode = new LibraryNode(this.groupContext, this.root_, dir, true);
        //Add the node to the root directory list
        this.rootDirList.add(dirNode);
        //Add this directory to the global dir Map
        this.dirMap.put(dir.getPath(), new ArrayList<LibraryNode>());

        return dirNode;
    }
    
    protected LibraryNode addSubDirectory(LibraryNode parent, File dir){
        
        LibraryNode dirNode = new LibraryNode(this.groupContext, parent,dir, false);
        //Add the sub directory to the directory Map
        this.dirMap.put(dir.getPath(), new ArrayList<LibraryNode>());
        //Add the directory has a child of his parent
        this.dirMap.get(parent.getPath()).add(dirNode);
        
        return dirNode;
    }

    protected LibraryNode addFile(LibraryNode parent, File fil) {
        //Create a node with this file and his parent
        LibraryNode res = new LibraryNode(this.groupContext, parent, fil, false);
        
        if(res != null){
            //If the file doesn't exists in the dirMap
            if(this.shareDic.get(res.getHash()) == null){
                //Add the file has a child of his directory
                this.dirMap.get(parent.getPath()).add(res);
                //Register the share in Share dictionnary
                this.shareDic.put(res.getHash(), res.getPath());
            }
        }
        
        return res;
            
    }
    
    protected void addAllFiles(Map directory, Map files){
        this.dirMap.putAll(directory);
        this.shareDic.putAll(files);
    }
    
    //Remove file everywhere defined
    protected void removeFile(LibraryNode node) {
        //Remove file from his parent node
        this.dirMap.get(node.getParent().getPath()).remove(node);
        //Remove file from shared Dictionnary
        this.shareDic.remove(node.getHash());
    }
    
    protected void removeDirectory(LibraryNode node) {
        //If the directory is a root one remove it from rootList
        if(this.rootDirList.contains(node)){
            this.rootDirList.remove(node);
        }
        //Remove all child of this directory
        List<LibraryNode> childs = this.dirMap.get(node.getDirectory());
        for(LibraryNode cNode : childs){
            this.shareDic.remove(cNode.getHash());
        }
        this.dirMap.remove(node.getDirectory());
        //Remove this directory from his parent
        this.dirMap.get(node.getParent().getPath()).remove(node);
    }
    
//    public void removeDirectory(String dirName) {
//        if(this.dirMap.containsKey(dirName)){
//            for(LibraryNode node : rootDirList){
//                if(node.getDirectory().equals(dirName))
//                    this.rootDirList.remove(node);
//            }
//            this.dirMap.remove(dirName);
//        }
//    }
    
    //Return all the library node matching a specific name/category
    protected List<LibraryNode> searchInLibrary(String name, String category){
        List<LibraryNode> sharesFound = new ArrayList<LibraryNode>();
        
//        for(LibraryNode dir : rootDirList){
//            List<LibraryNode> tmpShares = dirMap.get(dir.getDirectory());
//                for(LibraryNode share : tmpShares){
//                    if(share.getName().contains(name) && (type.equalsIgnoreCase("ALL") || share.getType().equals(type))){
//                        sharesFound.add(share);
//                    }
//                }
//        }
        
        
            
            for(List<LibraryNode> shareList : dirMap.values()){
                        for(LibraryNode share : shareList){
                            String lowerName = share.getName().toLowerCase();
                            if(lowerName.contains(name.toLowerCase()) && (category.equalsIgnoreCase("All") || share.getCategory().equals(category))){
                                //Only add response if they are not already found somewhere else
                                if(!sharesFound.contains(share))
                                    sharesFound.add(share);
                            }
                        }
                    }
        
        
        return sharesFound;
    }
    
    //Check if a hash is in library
    protected LibraryNode isInLibrary(String hash){
//            LibraryNode sharesFound = null;
//        
//            List<String> hashFound = new ArrayList<String>();
//            
//            //Parse the share dictionnary to find shares containing searchpattern
//            for(String key : shareDic.keySet()){
//                if(key.equals(hash)){
//                    hashFound.add(hash);
//                }
//                
//            }
            
            //If the pattern is found somewhere in our dictionnary we return the libraryNode
            for(List<LibraryNode> sharesList : dirMap.values()){
                        for(LibraryNode share : sharesList){
                            String h = share.getHash();
                            //If a corresponding share is found return it immediatly
                            if(h != null && h.equals(hash))
                                return share;
                        }
                            
                    }
        
        
        return null;
    }
    
    //Check if a directory exists in library
    protected boolean libContains(File dir){
        return (this.rootDirList.contains(new LibraryNode(this.groupContext, this.root_, dir, true))) ? true : false;
    }
    
//    protected List<LibraryNode> searchInPublicLibrary(String share, String type, String category) throws IOException{
//        return searchInGroupLibrary(Core.MAINGROUP_NAME,share,type,category);
//    }
//    
//    protected List<LibraryNode> searchInPrivateLibrary(String share, String type, String category) throws IOException{
//        return searchInGroupLibrary("PRIVATE",share,type,category);
//    }
    
    protected List<LibraryNode> getShares(){
        List<LibraryNode> sharesFound = new ArrayList<LibraryNode>();
        for(LibraryNode dir : rootDirList){
            List<LibraryNode> tmpShares = dirMap.get(dir.getDirectory());
                for(LibraryNode share : tmpShares){
                    sharesFound.add(share);
                }
        }
        
        return sharesFound;
    }
    
    public HashMap getMap(){
        return this.dirMap;
    }
    
    @Override
    public LibraryNode getRoot() {
        return this.root_;
    }

    @Override
    public int getColumnCount() {
        return 7;
    }
    
    
    @Override
    public Object getValueAt(Object node, int column) {
        LibraryNode treenode = (LibraryNode) node;
        
        //Allow auto-remove of user deleted files
        if(!treenode.getNode().exists()){
            removeFile(treenode);
            return null;
        }
        
        switch (column) {
            case 0:
                return treenode.getNode().isFile() ?  "" : treenode.getDirectory();
            case 1:
                return treenode.getType();
            case 2:
                return treenode.getNode().isFile() ?  treenode.getCategory() : "";
            case 3:
                return treenode.getNode().isFile() ?  treenode.getName() : treenode.getStatus();
            case 4:
                if(treenode.getNode().isFile()){
                    DecimalFormat df = new DecimalFormat("###.##");
                    Float size = new Float(treenode.getNode().length());
                    if(treenode.getSize() == null  || size <= 0)
                        return "";
                    else if(size > 1000000000)
                        return df.format(size/1000000000)+"Go";
                    else if(size > 1000000)
                        return df.format(size/1000000)+"Mo";
                    else if(size > 1000)
                        return df.format(size/1000)+"ko";
                    else
                        df = new DecimalFormat("###");
                        return df.format(size)+"o";
                    }else{
                        return null;
                }
                    
            case 5:
                return treenode.getHash();
            case 6:
                if(!treenode.getPath().equals(this.root_.getPath())){
                    if(Kb00m.GUI_MODE)
                        return  new ImageIcon(getClass().getResource("/Picts/Small/delete.png"));
                }
                return "";
            default:
                return "Unknown";
        }
    }

    @Override
    public Object getChild(Object o, int i) {
        LibraryNode node = (LibraryNode) o;
        if (o.equals(this.root_)) {
            return this.rootDirList.get(i);
        } else if (this.dirMap.containsKey(node.getDirectory())) {
            if(i < this.dirMap.get(node.getDirectory()).size())
                return this.dirMap.get(node.getDirectory()).get(i);
            else
                return null;
        } else {
            return this.root_;
        }
    }

    @Override
    public int getChildCount(Object o) {
        LibraryNode node = (LibraryNode) o;
        if (o == null) {
            return 0;
        } else if (o.equals(this.root_)) {
            return this.rootDirList.size();
        } else if (this.dirMap.containsKey(node.getDirectory())) {
            return this.dirMap.get(node.getDirectory()).size();
        } else {
            return 0;
        }
    }

    @Override
    public int getIndexOfChild(Object o, Object o1) {
        LibraryNode node = (LibraryNode) o;
        LibraryNode node1 = (LibraryNode) o1;
        if (o == null || o1 == null) {
            return -1;
        } else if (o.equals(this.root_)) {
            return this.rootDirList.indexOf(node1);
        } else if (this.dirMap.containsKey(node.getDirectory())) {
            return this.dirMap.get(node.getDirectory()).indexOf(node1);
        } else {
            return 0;
        }
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        Class clazz = String.class;
        switch (columnIndex) {
            case 4:
                clazz = Long.class;
                break;
            case 6:
                clazz = ButtonColumn.class;
                break;
        }
        return clazz;
    }

    @Override
    public String getColumnName(int column) {
        switch (column) {
            case 0:
                return "Shared Directory";
            case 1:
                return "Type";
            case 2:
                return "Category";
            case 3:
                return "Name";
            case 4:
                return "Size";
            case 5:
                return "Hash";
            case 6:
                return "Action";

            default:
                return "Unknown";
        }
    }

    @Override
    public void setValueAt(Object child, Object parent, int column){
        LibraryNode dirNode = (LibraryNode)parent;
        LibraryNode childNode = (LibraryNode) child;
        String dirName = dirNode.getPath();
        switch (column) {
            case 1:
                this.dirMap.get(dirName).get(this.dirMap.get(dirName).indexOf((LibraryNode)child)).setType(childNode.getType());
            case 2:
                this.dirMap.get(dirName).get(this.dirMap.get(dirName).indexOf((LibraryNode)child)).setCategory(childNode.getCategory());
            case 6:
                this.dirMap.get(dirName).get(this.dirMap.get(dirName).indexOf((LibraryNode)child)).setAction(childNode.getAction());

            default:
                break;
        }
    }
}



class LibraryNode extends AbstractMutableTreeTableNode implements TreeTableNode {

    private LibraryNode parentNode = null;
    private LibEntry child = null;
    private Object action = null;
    private String status = "";
    
    protected LibraryNode(PeerGroup grp, File node) {
        this.parentNode = null;
        this.child = new LibEntry(grp, node);
        this.action = null;
        this.status = null;
    }
    
    protected LibraryNode(PeerGroup grp, LibraryNode dir, File node, boolean rootNode) {
        this.parentNode = dir;
        this.child = new LibEntry(grp, node);
        this.action = Kb00m.DELETE;
        if(rootNode) this.status = "Please wait, parsing directory...";
    }
    
    protected LibraryNode(PeerGroup grp, LibraryNode dir, File node, String categ) {
        this.parentNode = dir;
        this.child = new LibEntry(grp, node);
        this.child.setCategory(categ);
        this.action = Kb00m.DELETE;
    }
    
    protected String getName() {
        return this.child.getName();
    }

    protected String getDirectory() {
        String path = null;
        LibEntry node = null;
        try {
            path = this.child.getCanonicalPath();
        } catch (IOException ex) {
            System.out.println("Error in retrieving parent node path");
        }
        
        return path;
    }

    protected LibEntry getNode() {
        return this.child;
    }
    
    protected String getCategory() {
        return this.child.getCategory();
    }

    protected String getType() {
        return this.child.getType();
    }

    protected String getHash() {
        return this.child.getHash();
    }

    protected String getPath() {
        return this.child.getPath();
    }

    protected Long getSize() {
        return this.child.length();
    }
    
    protected String getStatus() {
        return this.status;
    }
    
    protected Object getAction() {
        return this.action;
    }
    
    protected void setCategory(String cat) {
        this.child.setCategory(cat);
    }
    
    protected void setType(String t) {
        this.child.setType(t);
    }

    protected void setStatus(String s) {
        this.status = s;
    }
    
    protected void setAction(Object a) {
        this.action = a;
    }
    
    protected void setParent(LibraryNode node) {
        this.parentNode = node;
    }
    
    @Override
    public LibraryNode getParent() {
        if(this.parentNode == null)
            return this;
        else
            return this.parentNode;
    }

    
    @Override
    public String toString() {
        if (this.child.getName() != null) {
            return Kb00m.FILE+": " + getType() + ", " + getCategory() + " ," + getName() + " ," + getSize() + " ," + getHash();
        } else if (this.parentNode != null) {
            return Kb00m.DIRECTORY+": " + this.parentNode;
        } else {
            return "Root node";
        }
    }

    @Override
    public Object getValueAt(int i) {
        switch (i) {
            case 0:
                return getDirectory();
            case 1:
                return getType();
            case 2:
                return getCategory();
            case 3:
                return getName();
            case 4:
                return getSize();
            case 5:
                return getHash();
            case 6:
                return getAction();
            default:
                return "Unknown";
        }
    }

    @Override
    public int getColumnCount() {
        return 7;
    }
    
}

class LibEntry extends File {

    private String hash = null;
    private String type = Kb00m.FILE;
    private ContentID ID = null;
    private Content content = null;
    private ContentAdv contentAdv = null;
    private String category = null;
    private File node = null;
    
    protected LibEntry(PeerGroup grp, File entry) {
        super(entry, "");
        
          if(entry.isFile()){
                try {
                    this.hash = getChecksum(entry.getCanonicalPath());
                } catch (Exception ex) {
                    Logger.getLogger(LibEntry.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            else{
                this.hash = null;
            }
          //static final String DEFAULT_CONTENT_ID =
//        "urn:jxta:uuid-59616261646162614E50472050325033901EA80A652D476C9D1089545CEDE7B007"; -> urn:jxta:uuid-+66cararcters
            this.node = entry;
            String shareName = node.getName();
            if(entry.isDirectory()){
                this.type = Kb00m.DIRECTORY;
            }
            else if (shareName.endsWith(".mp3") || shareName.endsWith(".ogg") || shareName.endsWith(".wmv")) {
                this.category = "Music";

            } else if (shareName.endsWith(".avi") || shareName.endsWith(".mpg") || shareName.endsWith(".m4v")
                    || shareName.endsWith(".mpeg") || shareName.endsWith(".wmv") || shareName.endsWith(".mov")) {
                this.category = "Video";

            } else if (shareName.endsWith(".gif") || shareName.endsWith(".jpg") || shareName.endsWith(".jpeg")
                    || shareName.endsWith(".png") || shareName.endsWith(".jpg")) {
                this.category = "Picture";

            } else if (shareName.endsWith(".txt") || shareName.endsWith(".doc") || shareName.endsWith(".docx")) {
                this.category = "Text";

            } else if (shareName.endsWith(".exe") || shareName.endsWith(".sh") || shareName.endsWith(".bin")) {
                this.category = "Executable";

            } else if (shareName.endsWith(".iso") || shareName.endsWith(".dmg")) {
                this.category = "DiskImage";

            } else {
                this.category = "Other";

            }
            
            //Construct the content only if this is not a directory or the root node
//            if(this.node != null && this.hash != null){
//                this.ID = IDFactory.newContentID(grp.getPeerGroupID(), false, this.hash.getBytes());
//                this.contentAdv = new ContentAdv(this.ID, Core.PeerName);
//                this.contentAdv.setName(shareName);
//                this.contentAdv.setCategory(this.category);
//                this.contentAdv.setHash(this.hash);
//                Long size = this.node.length();
//                this.contentAdv.setSize(size.toString());
//                FileDocument fileDoc = new FileDocument(this.node, MimeMediaType.AOS);
//                this.content = new Content(this.ID, null, fileDoc);
//                List<ContentShare> shares = grp.getContentService().shareContent(content);
//                
//            }
            

    }

    protected File getFile() {
        return this.node;
    }

    protected String getHash() {
        return this.hash;
    }

    protected ContentID getID() {
        return this.ID;
    }

    protected String getCategory() {
        return this.category;
    }

    protected Content getContent() {
        return this.content;
    }

    protected ContentAdv getContentAdvertisement() {
        return this.contentAdv;
    }

    protected String getType() {
        return this.type;
    }

    protected void setContent(Content content) {
        this.content = content;
    }

    protected void setCategory(String cat) {
        this.category = cat;
    }

    protected void setType(String t) {
        this.type = t;
    }

    protected void setID(ContentID uri) {
        this.ID = uri;
    }
    
    private static byte[] createChecksum(String filename) throws
            Exception {
        InputStream fis = new FileInputStream(filename);

        byte[] buffer = new byte[1024];
        int numRead;
        Security.addProvider(new BouncyCastleProvider());

        MessageDigest complete = MessageDigest.getInstance("SHA-256", "BC");

        do {
            numRead = fis.read(buffer);
            if (numRead > 0) {
                complete.update(buffer, 0, numRead);
            }
        } while (numRead != -1);
        fis.close();
        return Hex.encode(complete.digest());
    }

    private static String getChecksum(String filename) throws Exception {
        byte[] b = createChecksum(filename);
        String result = "";
        for (int i = 0; i < b.length; i++) {
            result +=
                    Integer.toString((b[i] & 0xff) + 0x100, 16).substring(1);
        }
        return result;
    }
}





class MyTransferts extends SRDIContentShare{
    
    protected static final int DEFAULT_SOURCE_LOCATION_INTERVAL = 15;

    protected static final int DEFAULT_DISCOVERY_THRESHOLD = 10;

    protected static final int DEFAULT_MAX_STALLS = 3;

    protected static final boolean DEFAULT_ENABLE_LOCAL = true;

    protected static final boolean DEFAULT_ENABLE_REMOTE = true;

    public MyTransferts(SRDISocketContentProvider origin, Content content, PipeAdvertisement pipeAdv){
        super(origin,content,pipeAdv);
        
    }
    
}

class ContentAdv extends ContentAdvertisement {
            
            HashMap<String,String> content = null;
            String[] fields = {"ID","Seeder","Name", "Category", "Hash", "Size"};
            
            ContentAdv(ContentID id, String seed){
                super();
                content = new HashMap();
                content.put("Seeder", seed);
                content.put("ID", id.toString());
            }
            
            @Override
            public String[] getIndexFields() {
                return fields;
            }
            
            protected void setName(String value){
                content.put("Name", value);
            }
            
            protected void setCategory(String value){
                content.put("Category", value);
            }
            protected void setHash(String value){
                content.put("Hash", value);
            }
            protected void setSize(String value){
                content.put("Size", value);
            }
    }


//final class LibraryTest extends JFrame {
//
//    private ImageIcon Mp3Icon = null;
//    private ImageIcon VideoIcon = null;
//    private ImageIcon OthersIcon = null;
//    
//    private JTabbedPane tabs = new JTabbedPane();
//    private LibraryModel treeTableModel = null;
//    private JXTreeTable treeTable = null;
//    
//    public LibraryTest() {
//        super("B00M Library test");
//        
//        Mp3Icon = new javax.swing.ImageIcon(getClass().getResource("/Picts/Small/mp3Icon.png"));
//        VideoIcon = new javax.swing.ImageIcon(getClass().getResource("/Picts/Small/videoIcon.png"));
//        OthersIcon = new javax.swing.ImageIcon(getClass().getResource("/Picts/Small/otherfilesIcon.png"));
//        
//        treeTableModel = new LibraryModel(new File("/Users/usr/jxta"));
//        shareDirectoryInGroup(treeTableModel.getRoot());
//        
//        LibraryNode node2 = treeTableModel.addDirectory(new File("/Users/usr/jxta2"),false);
//        shareDirectoryInGroup(node2);
//        
//        // Build the tree table panel
//        treeTable =  new JXTreeTable((TreeTableModel)treeTableModel);
//        JPanel treeTablePanel = new JPanel(new BorderLayout());
//        treeTablePanel.add(new JScrollPane(treeTable));
//        tabs.addTab("Searching results", treeTablePanel);
//
////        Action delete = new AbstractAction() {
////
////            @Override
////            public void actionPerformed(ActionEvent e) {
////                JXTreeTable table = (JXTreeTable) e.getSource();
////                int modelRow = Integer.valueOf(e.getActionCommand());
////                System.out.println("Remove file: "+table.getModel().getValueAt(modelRow, 0));
////            }
////        };
////        ButtonColumn test = new ButtonColumn(treeTable, delete, 6);
//
//        treeTable.setSortable(true);
//        treeTable.setClosedIcon(null);
//        treeTable.setOpenIcon(null);
//        treeTable.setExpandedIcon(null);
//        
//        treeTable.addMouseListener(new MouseAdapter() {
//
//            @Override
//            public void mouseClicked(final MouseEvent e) {
//                final int rowIndex = treeTable.rowAtPoint(e.getPoint());
//                final int colIndex = treeTable.columnAtPoint(e.getPoint());
//
//                if (rowIndex < 0) {
//                    return;
//                }
//
//                final LibraryNode selectedNode = (LibraryNode) treeTable.getPathForRow(rowIndex).getLastPathComponent();
//                
//                if(colIndex == 6){
//                    if (selectedNode.getType().equals(Kb00m.DIRECTORY)) {
//                        //If the parentNode is the default one just skip action
//                        if(selectedNode.getAction() == null)
//                            return;
//                        //Else remove the parentNode and associated nodes
//                        System.out.println("Deleting directory: " + selectedNode.getDirectory());
//                        treeTableModel.removeDirectory(selectedNode);
//                        //Reload data display
//                        treeTable.updateUI();
//                    }
//                    //If the node is a file
//                    else {
//                        System.out.println("Deleting file: " + selectedNode.getName());
//                        treeTableModel.removeFile(selectedNode);
//                        //Reload data display
//                        treeTable.updateUI();
//                    }
//                }
//            }
//        });
//        
//        treeTable.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
//        treeTable.getColumnModel().getColumn(0).setPreferredWidth(150);
//        treeTable.getColumnModel().getColumn(1).setPreferredWidth(40);
//        treeTable.getColumnModel().getColumn(2).setPreferredWidth(40);
//        treeTable.getColumnModel().getColumn(3).setPreferredWidth(267);
//        treeTable.getColumnModel().getColumn(6).setPreferredWidth(15);
//        //        HashMap shares = treeTableModel.getMap();
//        ////        DefaultTreeCellRenderer renderer = new DefaultTreeCellRenderer();
//        //        SearchNode nodeTmp;
//        //        for (Object parentNode : shares.keySet()) {
//        //            int num = treeTableModel.getChildCount(parentNode)-1;
//        //            do{
//        //                nodeTmp = (SearchNode) treeTableModel.getChild(parentNode, num);
//        //                DefaultTreeCellRenderer renderer = new DefaultTreeCellRenderer();
//        //              //Set the icon for leaf nodes.
//        //                if(nodeTmp.getType().equals("Video"))
//        //                    renderer.setLeafIcon(VideoIcon);
//        //                else if(nodeTmp.getType().equals("Music"))
//        //                    renderer.setLeafIcon(Mp3Icon);
//        //                else
//        //                    renderer.setLeafIcon(Mp3Icon);
//        //                treeTable.setTreeCellRenderer(renderer);
//        //                num--;
//        //            }while(num >= 0);
//        //        }
//        //        DefaultTreeCellRenderer renderer = new DefaultTreeCellRenderer();
//        //        renderer.setLeafIcon(Mp3Icon);
//        //        treeTable.setTreeCellRenderer(renderer);
////        DefaultTreeCellRenderer renderer = new DefaultTreeCellRenderer();
////        JXTree.DelegatingRenderer delegatingRenderer = new JXTree.DelegatingRenderer(renderer);
////        treeTable.setLeafIcon(VideoIcon);
//        //Reload data display
//        treeTable.updateUI();
//
//        
//        // Add the tabs to the JFrame
//        add(tabs);
//
//        setSize(1024, 768);
//        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
//        setLocation(d.width / 2 - 512, d.height / 2 - 384);
//        setVisible(true);
//        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//    }
//    
//    protected void shareDirectoryInGroup(LibraryNode dirNode) {
//        
//        LibEntry dir = dirNode.getNode();
//        
//        //If file specified does not exists just skip
//        if(!dir.exists())
//            return;
//        //If a single file is specified just add it
//        if(dir.isFile()){
//            shareFileInGroup(dirNode,dir);
//            return;
//        }
//        //sharing all files in specified parentNode
//        File[] list = dir.listFiles();
//        for(int i =0;i < list.length;i++) {
//            //If file found is a file
//            if (list[i].isFile()) {
//                shareFileInGroup(dirNode,list[i]);
//            }
//            //If this is a parentNode
//            else if(list[i].isDirectory()){
//                //Parsing sub-directories...
//                shareDirectoryInGroup(treeTableModel.addSubDirectory(dirNode,list[i]));
//            }
//        }
//        
//        //Library.storeLibrary(mySharedPath+System.getProperty("file.separator")+SHARES_FILENAME);
//    }
//    
//    protected void shareFileInGroup(LibraryNode dirNode, File child){
//        //We avoid sharing ".b00m.shares" and all files begining with a "."
//        if (!(child.getName().equals(".b00m.shares")) && !(child.getName().startsWith("."))) {
//                try {
//                    //Sharing Files and checksum localy in a catalog
//                    treeTableModel.addFile(dirNode, child);
//                } catch (Exception ex) {
//                    System.out.println("[!]Error while adding file "+child.getName()+" to share.\n[!]" + ex);
//                }
//            }
//    }
//    
//    public static void main(String[] args) {
//        AppStarter4 starter = new AppStarter4(args);
//        SwingUtilities.invokeLater(starter);
//    }
//}
//
//class AppStarter4 extends Thread {
//
//    private String[] args;
//
//    public AppStarter4(String[] args) {
//        this.args = args;
//    }
//
//    @Override
//    public void run() {
//        LibraryTest example = new LibraryTest();
//    }
//}