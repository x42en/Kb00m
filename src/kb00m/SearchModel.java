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

import java.text.DecimalFormat;
import java.util.*;
import javax.swing.ImageIcon;
import net.jxta.content.Content;
import net.jxta.content.ContentID;
import net.jxta.content.ContentShare;
import org.jdesktop.swingx.treetable.AbstractMutableTreeTableNode;
import org.jdesktop.swingx.treetable.AbstractTreeTableModel;
import org.jdesktop.swingx.treetable.TreeTableModel;




public final class SearchModel extends AbstractTreeTableModel implements TreeTableModel,Observer {

//    private List<SearchNode> parentList = new ArrayList<SearchNode>();
    private HashMap<SearchNode, ArrayList<SearchNode>> nodeList = new HashMap<SearchNode, ArrayList<SearchNode>>();
    private SearchNode root_ = null;
    
    protected SearchModel() {
        this.root_ = new SearchNode("",new ShareEntry("","",new Long(0),""),"","");
//        this.parentList.add(this.root_);
        this.nodeList.put(this.root_, new ArrayList<SearchNode>());
    }
    
    @Override
    public String toString(){
        String res = null;
        for(SearchNode search : nodeList.get(this.root_)){
            res += "Search pattern: "+search.getPattern()+" ("+search.getStatus().toString()+")\n";
            for(SearchNode result : nodeList.get(search)){
                res += "\t- "+result.getName()+" shared by "+result.getSeeder()+"\n";
            }
            res += "\n";
        }
        return res;
    }

    protected SearchNode addSearch(String pattern) {
        SearchNode search = new SearchNode(pattern, "", "Searching...", null, "", Kb00m.RESUME, Kb00m.DELETE);
        
        return addSearch(search);
    }
    
    protected SearchNode addSearch(SearchNode search){
        if(!this.nodeList.get(this.root_).contains(search)){
            this.nodeList.get(this.root_).add(search);
            this.nodeList.put(search, new ArrayList<SearchNode>());
        }
        update(null,null);
        return search;
    }

    protected SearchNode addShare(SearchNode parent, ShareEntry share) {
        
        //If the share has already been added just return
//        if(isInShareResults(parent,share))
//            return;
        SearchNode res = null;
        
        //If the search is not registred just exit
        if(!this.nodeList.get(this.root_).contains(parent)){
            return null;
        }
        
        //Only add the share if the search exists
        if(this.nodeList.containsKey(parent)){
            res = new SearchNode("", share, null,Kb00m.DOWNLOAD);
            res.setID(share.getID());
            this.nodeList.get(parent).add(res);
        }
        
//        if(this.nodeList.get(parent).indexOf(res) <= 0)
//            this.nodeList.get(parent).add(res);
        
//        if(!isInShareResults(parent,res))
//            this.nodeList.get(parent).add(res);
        return res;  
    }
    
    protected boolean isInSearchList(SearchNode search){
        return this.nodeList.get(this.root_).contains(search);
    }

    protected boolean isInShareResults(SearchNode search, ShareEntry share){
        boolean found = false;
//        return (this.nodeList.get(search).indexOf(share) > 0) ? true : false;
        for(SearchNode node : this.nodeList.get(search)){
            if((node.getNode().getHash().equals(share.getHash())) && (node.getNode().getSeeder().equals(share.getSeeder()))){
                found = true;
                break;
            }
        }
        return found;
    }

    protected void removeSearch(SearchNode node) {
        if(this.nodeList.containsKey(node)){
            this.nodeList.get(this.root_).remove(node);
            this.nodeList.remove(node);
        }
    }
    
    protected void removeSeederShares(String seeder){
        for(SearchNode search : this.nodeList.get(this.root_)){
            for(SearchNode node : this.nodeList.get(search)){
                if(node.getSeeder().equals(seeder))
                    this.nodeList.get(search).remove(node);
            }
        }
    }
    
    protected void setSearchStatus(SearchNode node, String status) {
        if(this.nodeList.get(this.root_).contains(node)){
            String value = null;
            for(SearchNode search : this.nodeList.keySet()){
                if(search.equals(node)){
                    if(status.equals(Kb00m.PAUSE))
                        value = "Search suspended";
                    else if(status.equals(Kb00m.RESUME))
                        value = "Searching...";
                    else if(status.equals(Kb00m.FOLLOW))
                        value = "Tracking...";
                    else if(status.equals(Kb00m.FOLLOW_PAUSE))
                        value = "Tracking suspended";
                    else
                        value = "Search stopped";
                    search.setSeed(value);
                    search.setStatus(status);
                    break;
                }
            }
            this.nodeList.get(this.root_).get(this.nodeList.get(this.root_).indexOf(node)).setSeed(value);
            this.nodeList.get(this.root_).get(this.nodeList.get(this.root_).indexOf(node)).setStatus(status);
        }
    }
    
    protected SearchNode getSearchNode(String pattern){
        for(SearchNode search : this.nodeList.get(this.root_)){
            if(search.getPattern().equals(pattern))
                return search;
        }
        return null;
    }

    protected HashMap getMap(){
        return this.nodeList;
    }
    
    protected String getStatus(SearchNode node){
        return node.getStatus().toString();
    }
    
    protected SearchNode getParent(SearchNode child){
        //if this is a search node just skip
        if(this.nodeList.get(this.root_).contains(child))
            return this.root_;
        
        for(SearchNode search : this.nodeList.get(this.root_)){
            if(this.nodeList.get(search).contains(child))
                return search;
        }
        return null;
    }
    
    @Override
    public SearchNode getRoot() {
        return this.root_;
    }

    @Override
    public int getColumnCount() {
        return 8;
    }

    @Override
    public Object getValueAt(Object node, int column) {
        SearchNode treenode = (SearchNode) node;
        
        switch (column) {
            case 0:
                return treenode.getName().isEmpty() ?  treenode.getPattern() : "";
            case 1:
                return treenode.getName().isEmpty() ?  "" : treenode.getType();
            case 2:
                return treenode.getSeeder();
            case 3:
                return treenode.getName();
            case 4:
                 if(treenode.getType().equals(Kb00m.DIRECTORY))
                         return "";
                 DecimalFormat df = new DecimalFormat("###.##");
                    Float size = new Float(treenode.getSize());
                    if(size == null  || size <= 0)
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
            case 5:
                return treenode.getHash();
            case 6:
                if(Kb00m.GUI_MODE){
                    if(treenode.getType().equals(Kb00m.DIRECTORY)){
                        if(treenode.getStatus().equals(Kb00m.PAUSE))
                            return new ImageIcon(getClass().getResource("/Picts/Small/resume.png"));
                        else if(treenode.getStatus().equals(Kb00m.RESUME))
                            return new ImageIcon(getClass().getResource("/Picts/Small/pause.png"));
                        else if(treenode.getStatus().equals(Kb00m.FOLLOW))
                            return new ImageIcon(getClass().getResource("/Picts/Small/follow_pause.png"));
                        else if(treenode.getStatus().equals(Kb00m.FOLLOW_PAUSE))
                            return new ImageIcon(getClass().getResource("/Picts/Small/follow.png"));
                    }
                    else{
                        return "";
                    }
                }
                else{
                    if(treenode.getType().equals(Kb00m.DIRECTORY))
                        return treenode.getStatus();
                    else
                        return "";
                }
            case 7:
                if(Kb00m.GUI_MODE){
                    return (treenode.getType().equals(Kb00m.DIRECTORY)) ? new ImageIcon(getClass().getResource("/Picts/Small/delete.png")) : new ImageIcon(getClass().getResource("/Picts/Small/downloadIcon.png"));
                }
                return "";
            case 8:
                return treenode.getStatus();
            default:
                return "Unknown";
        }
    }

    @Override
    public Object getChild(Object o, int i) {
        SearchNode node = (SearchNode) o;
        if (node.equals(this.root_)) {
            return this.nodeList.get(this.root_).get(i);
        } else if (this.nodeList.containsKey(node)) {
            return this.nodeList.get(node).get(i);
        } else {
            return this.root_;
        }
    }

    @Override
    public int getChildCount(Object o) {
        SearchNode node = (SearchNode) o;
        if (node == null) {
            return 0;
        }else if (node.equals(this.root_)) {
            return this.nodeList.get(this.root_).size();
        } else if (this.nodeList.get(this.root_).contains(node)) {
            return this.nodeList.get(node).size();
        } else {
            return 0;
        }
    }

    @Override
    public int getIndexOfChild(Object o, Object o1) {
        if (o == null || o1 == null) {
            return -1;
        } else if (o1.equals(this.root_)) {
            return 0;
        } else if (o.equals(this.root_)) {
            return this.nodeList.get(this.root_).indexOf((SearchNode) o1);
        } else if (this.nodeList.get(this.root_).contains((SearchNode) o)) {
            return this.nodeList.get((SearchNode) o).indexOf((SearchNode) o1);
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
            case 7:
                clazz = ButtonColumn.class;
                break;
        }
        return clazz;
    }

    @Override
    public String getColumnName(int column) {
        switch (column) {
            case 0:
                return "Search pattern";
            case 1:
                return "File type";
            case 2:
                return "Seeder";
            case 3:
                return "File Name";
            case 4:
                return "File Size";
            case 5:
                return "File Hash";
            case 6:
                return "Status";
            case 7:
                return "Action";

            default:
                return "Unknown";
        }
    }

    @Override
    public void update(Observable o, Object o1) {
//        throw new UnsupportedOperationException("Not defined.");
    }
}
class SearchNode extends AbstractMutableTreeTableNode {

    private String pattern = null;
    private String seeder = null;
    private Object status = null;
    private Object action = null;
    private ShareEntry shareNode = null;

    protected SearchNode() {
    }
    
    protected SearchNode(String fPattern, ShareEntry entry, Object status, Object action){
        this.pattern = fPattern;
        this.action = action;
        this.status = status;
        this.shareNode = entry;
        this.seeder = entry.getSeeder();
    }

    protected SearchNode(String fPattern, String fName, String seed, Long fSize, String fHash, Object status, Object action) {
        this.pattern = fPattern;
        this.action = action;
        this.status = status;
        this.shareNode = new ShareEntry(seed, fName, fSize, fHash);
        this.seeder = this.shareNode.getSeeder();
    }

    protected SearchNode(String fPattern, String fName, String seed, String fCateg, Long fSize, String fHash, Object status, Object action) {
        this.pattern = fPattern;
        this.action = action;
        this.status = status;
        this.shareNode = new ShareEntry(seed, fName, fSize, fHash);
        this.shareNode.setCategory(fCateg);
        this.seeder = this.shareNode.getSeeder();
    }

    protected ShareEntry getNode() {
        return this.shareNode;
    }

    protected String getName() {
        return this.shareNode.getName();
    }

    protected String getPattern() {
        return this.pattern;
    }

    protected String getSeeder() {
        return this.shareNode.getSeeder();
    }

    protected String getCategory() {
        return this.shareNode.getCategory();
    }

    protected String getType() {
        return this.shareNode.getType();
    }

    protected String getHash() {
        return this.shareNode.getHash();
    }

    protected Long getSize() {
        return this.shareNode.getSize();
    }

    protected ContentID getID() {
        return this.shareNode.getID();
    }
    
    protected void setID(ContentID id){
        this.shareNode.setID(id);
    }
    
    protected void setStatus(String s) {
        this.status = s;
    }

    protected void setSeed(String s) {
        this.shareNode.setSeeder(s);
        this.seeder = this.shareNode.getSeeder();
    }

    protected Object getStatus() {
        return this.status;
    }

    protected Object getAction() {
        return this.action;
    }

    @Override
    public String toString() {
        if (this.seeder != null) {
            return getCategory() + ", " + getSeeder() + ", " + getName() + " ," + getSize() + " ," + getHash();
        } else if (this.pattern != null) {
            return "search: " + this.pattern;
        } else {
            return "root node";
        }
    }

    @Override
    public Object getValueAt(int i) {
        switch (i) {
            case 0:
                return getPattern();
            case 1:
                return getCategory();
            case 2:
                return getSeeder();
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


class ShareEntry {

    private String shareName = null;
    private String hash = null;
    private String category = null;
    private Long size = null;
    private ContentID ID = null;
    private Content content = null;
    private List<ContentShare> shares = null;
    private String seeder = null;
    private String type = Kb00m.FILE;

    protected ShareEntry(String seed, String shareName, Long size, String hash) {
        this.seeder = seed;
        this.shareName = shareName;
        this.hash = hash;
        this.size = size;
        
        if(shareName.isEmpty())
            this.type = Kb00m.DIRECTORY;
        
        if (shareName.endsWith(".mp3") || shareName.endsWith(".ogg") || shareName.endsWith(".wmv")) {
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

    }

    protected String getName() {
        return this.shareName;
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

    protected String getSeeder() {
        return this.seeder;
    }

    protected Content getContent() {
        return this.content;
    }

    protected List getShares() {
        return this.shares;
    }

    protected String getType() {
        return this.type;
    }

    protected Long getSize() {
        return this.size;
    }

    protected void setContent(Content content) {
        this.content = content;
    }

    protected void setCategory(String cat) {
        this.category = cat;
    }

    protected void setName(String name) {
        this.shareName = name;
    }
    
    protected void setSeeder(String seed) {
        this.seeder = seed;
    }
//
//    protected void setHash(String hash) {
//        this.hash = hash;
//    }
//
    protected void setID(ContentID uri) {
        this.ID = uri;
    }
}

