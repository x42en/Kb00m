package kb00m;

/*
 * (C) 2013 - Certiwise Software Services
 * 
 * This code is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public 
 * License as published by the Free Software Foundation; either 
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This code is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
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

import java.util.ArrayList;
import java.util.List;
import javax.swing.AbstractListModel;
import javax.swing.MutableComboBoxModel;
import net.jxta.id.ID;
import net.jxta.peergroup.PeerGroup;
import net.jxta.protocol.PeerGroupAdvertisement;



public class PeerGroupModel extends AbstractListModel implements MutableComboBoxModel {

    private ArrayList<PGroup> lstGroup = null;
    private PGroup selectedGroup = null;

    public PeerGroupModel() {
        lstGroup = new ArrayList<PGroup>();
    }
    
    protected PGroup addCustomGroup(String title){
        PGroup tmp = new PGroup(title);
        addElement(tmp);
        
        return tmp;
    }

    protected PGroup addCustomGroup(PeerGroupAdvertisement group){
        PGroup tmp = new PGroup(group);
        addElement(tmp);
        
        return tmp;
    }
    
//    protected PGroup addCustomGroup(String name, String desc, String id){
//        PGroup tmp = new PGroup(name,desc);
//        tmp.setID(id);
//        addElement(tmp);
//        
//        return tmp;
//    }
    
    protected void removeGroupByName(String name){
        removeElement(this.lstGroup.indexOf(name));
    }
    
    protected void removeGroup(int i){
        removeElementAt(i);
    }
    
    protected void setPeerGroup(PeerGroup pg){
        PGroup search = new PGroup(pg.getPeerGroupAdvertisement());
        PGroup tmp = this.lstGroup.get(this.lstGroup.indexOf(search));
        tmp.setPeerGroup(pg);
    }
    
    protected void updateGroupDesc(String name, String desc){
        for(PGroup grp : this.lstGroup){
            //If we found the specific group
            if(grp.getName().equalsIgnoreCase(name)){
                grp.setDesc(desc);
                //No need to continue, CustomGroup names are unique
                break;
            }
        }
    }
    
    protected CustomGroup getGroupByName(String name){
       for(PGroup grp : this.lstGroup){
            //retrieve the ID of a specific group
            if(grp.getName().equalsIgnoreCase(name)) return grp.getGroup();
       }
       return null;
    }
    
    protected PGroup getCustomGroupByName(String name){
       for(PGroup grp : this.lstGroup){
            //retrieve the ID of a specific group
            if(grp.getName().equalsIgnoreCase(name)) return grp;
       }
       return null;
    }
    
    protected List<String> listGroupsName(){
        List<String> listOfGroupsTMP = new ArrayList<String>();
        
        for(PGroup grp : this.lstGroup){
            //retrieve a list of group like: "Group1" "Group2"
            listOfGroupsTMP.add(grp.getName());
        }
        
        return listOfGroupsTMP;
    }
    
    protected List<PGroup> listGroups(){
        return this.lstGroup;
    }
    
    protected boolean isKnownGroup(ID groupID){
        for(PGroup grp : this.lstGroup){
            //return the status of the specified user
            if(grp.getID().equals(groupID.toString()))
                return true;
        }
        
        return false;
    }
    
    protected void clearGroups(){
        this.lstGroup.clear();
    }
    
    protected List getGroups(){
        return this.lstGroup;
    }

    @Override
    public int getSize() {
        return this.lstGroup.size();
    }

    @Override
    public Object getElementAt(int i) {
        return this.lstGroup.get(i);
    }
    
    public String getNameAt(int i) {
        return this.lstGroup.get(i).getName();
    }
    
    public String getDescAt(int i) {
        return this.lstGroup.get(i).getDesc();
    }
    
    public String getAuthAt(int i) {
        return this.lstGroup.get(i).getAuth();
    }
    
    public String getIDAt(int i) {
        return this.lstGroup.get(i).getID();
    }

    @Override
    public void setSelectedItem(Object o) {
        this.selectedGroup = (PGroup) o;
    }

    @Override
    public Object getSelectedItem() {
        return this.selectedGroup;
    }

    @Override
    public void addElement(Object o) {
        this.lstGroup.add((PGroup) o);
    }

    @Override
    public void removeElement(Object o) {
        this.lstGroup.remove((PGroup) o);
    }

    @Override
    public void insertElementAt(Object o, int i) {
        this.lstGroup.add(i, (PGroup) o);
    }

    @Override
    public void removeElementAt(int i) {
        this.lstGroup.remove(i);
    }
}

class PGroup {

        String Name = null;
        String ID = null;
        String Desc = null;
        String Auth = "clear";
        CustomGroup customGrp = null;
        PeerGroup peerGrp = null;
        PeerGroupAdvertisement adv = null;
        
        protected PGroup(PeerGroupAdvertisement pgrp){
            this.Name = pgrp.getName();
            this.Desc = pgrp.getDescription();
            if(this.Desc.contains("Password")){
                this.Auth = "password";
            }
            else if(this.Desc.contains("Certificates")){
                this.Auth = "certificates";
            }
            this.ID = pgrp.getPeerGroupID().toString();
            this.adv = pgrp;
        }

        protected PGroup(String title){
            this.Name = title;
        }

        protected String getName(){
            return this.Name;
        }

        protected String getAuth(){
            return this.Auth;
        }
        protected String getDesc(){
            return this.Desc;
        }
        protected String getID(){
            return this.ID;
        }
        protected PeerGroupAdvertisement getGrpAdv(){
            return this.adv;
        }
        protected CustomGroup getGroup(){
            return this.customGrp;
        }
        protected void setGroup(CustomGroup grp){
            this.customGrp = grp;
        }
        protected void setPeerGroup(PeerGroup peerGrp){
            this.peerGrp = peerGrp;
        }
        protected void setDesc(String desc){
            this.Desc = desc;
        }
        protected void setName(String name){
            this.Name = name;
        }
        protected void setAuth(String auth){
            this.Auth = auth;
        }
        protected void setID(String id){
            this.ID = id;
        }
        
        @Override
        public String toString(){
            return this.Name;
        }
    }
//class PeerGroupTest extends JFrame {
//
//    private JTabbedPane tabs = new JTabbedPane();
//    private PeerGroupModel tableModel = new PeerGroupModel();
//    private JXTable table = new JXTable((TableModel) tableModel);
//    
//    public PeerGroupTest() {
//        super("B00M groups test");
//
////        tableModel.addTreeModelListener(new MyTreeModelListener());
//
//        tableModel.addCustomGroup("B00M", "The main group","8757541809897659809");
//        tableModel.addCustomGroup("SuperGroup", "A new cool group","98765753654365476");
//        tableModel.updateGroupDesc("B00M", "The standard group");
//        
//        // Build the tree table panel
//        JPanel TablePanel = new JPanel(new BorderLayout());
//        TablePanel.add(new JScrollPane(table));
//        tabs.addTab("Found Groups", TablePanel);
//
//        table.setSortable(true);
//        table.doLayout();
//
//        table.addMouseListener(new MouseAdapter() {
//
//            @Override
//            public void mouseClicked(final MouseEvent e) {
//                final int rowIndex = table.rowAtPoint(e.getPoint());
//
//                if (rowIndex < 0) {
//                    return;
//                }
//
//                if (e.getClickCount() == 2) {
//                    JTable table = (JTable) e.getSource();
//                    ((PeerGroupModel) table.getModel()).removeGroup(rowIndex);
//                }
//
//            }
//        });
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
//    public static void main(String[] args) {
//        AppStarter3 starter = new AppStarter3(args);
//        SwingUtilities.invokeLater(starter);
//    }
//}
//
//class AppStarter3 extends Thread {
//
//    private String[] args;
//
//    public AppStarter3(String[] args) {
//        this.args = args;
//    }
//
//    @Override
//    public void run() {
//        PeerGroupTest example = new PeerGroupTest();
//    }
//}