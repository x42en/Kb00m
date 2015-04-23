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

import java.util.ArrayList;
import java.util.List;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;
import net.jxta.id.ID;
import net.jxta.protocol.PeerAdvertisement;


public class UserModel extends AbstractTableModel implements TableModel {

    private List<Users> lstPeople = new ArrayList<Users>();

    public UserModel() {
    }
    
    @Override
    public String toString(){
        String res = "";
        for(Users usr : lstPeople){
            res += usr.getName()+" ("+usr.getStatus()+")\n";
        }
        
        return res;
    }
    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false;
    }

    @Override
    public int getRowCount() {
        return this.lstPeople.size();
    }

    @Override
    public int getColumnCount() {
        return 2;
    }

    @Override
    public String getColumnName(int column) {
        String name = null;
        switch (column) {
            case 0:
                name = "Name";
                break;
            case 1:
                name = "Status";
                break;
        }
        return name;
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return String.class;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        Object value = null;
        Users person = this.lstPeople.get(rowIndex);
        switch (columnIndex) {
            case 0:
                value = person.getName();
                break;
            case 1:
                value = person.getStatus();
                break;
        }
        return value;
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        Users person = this.lstPeople.get(rowIndex);
        switch (columnIndex) {
            case 0:
                if (aValue instanceof String) {
                    person.setName(aValue.toString());
                }
                break;
            case 1:
                if (aValue instanceof String) {
                    person.setStatus(aValue.toString());
                }
                break;
        }
        fireTableCellUpdated(rowIndex, rowIndex);
    }
    
    public Users getUserAt(int i){
        return this.lstPeople.get(i);
    }

    public void addUser(String id, String name) {
        Users tmp = new Users(id, name);
        //If the user is unknown, we add it
        if (this.lstPeople.indexOf(tmp) == (-1)) {
            this.lstPeople.add(tmp);
        }
        fireTableRowsInserted(this.lstPeople.size() - 1, this.lstPeople.size() - 1);
    }

    public void addUser(String id, String name, String status) {
        Users tmp = new Users(id, name, status);
        //If the user is unknown, we add it
        if (this.lstPeople.indexOf(tmp) == (-1)) {
            this.lstPeople.add(tmp);
        }
        fireTableRowsInserted(this.lstPeople.size() - 1, this.lstPeople.size() - 1);
    }
    
    public void addUser(PeerAdvertisement padv) {
        String id = padv.getPeerID().toString();
        String name = padv.getName();
        String status = padv.getDescription();
        
        Users tmp = new Users(id, name, status);
        //If the user is unknown, we add it
        if (this.lstPeople.indexOf(tmp) == (-1)) {
            this.lstPeople.add(tmp);
        }
        fireTableRowsInserted(this.lstPeople.size() - 1, this.lstPeople.size() - 1);
    }

//    public void removeUserByName(String name) {
//        this.lstPeople.remove(this.lstPeople.indexOf(name));
//        fireTableRowsDeleted(this.lstPeople.size() - 1, this.lstPeople.size() - 1);
//    }
    
    public void removeUserByName(String name) {
        int i = 0;
        for(Users u : this.lstPeople){
            if(u.getName().equals(name)){
                this.lstPeople.remove(this.lstPeople.indexOf(u));
                break;
            }
            i++;
        }
        
        fireTableRowsDeleted(i, i);
    }

    public void removeUser(Users tmp) {
        int i = this.lstPeople.indexOf(tmp);
        this.lstPeople.remove(i);
        fireTableRowsDeleted(i, i);
    }

    public void removeUser(int i) {

        this.lstPeople.remove(i);
        fireTableRowsDeleted(i, i);

    }

    public void updateUserStatus(PeerAdvertisement padv) {
        String name = padv.getName();
        String status = padv.getDescription();
        int i = 0;
        for (Users usr : this.lstPeople) {
            //If we found the specific user
            if (usr.getName().equals(name)) {
                usr.setStatus(status);
                fireTableRowsUpdated(i, i);
                //No need to continue, userName are unique
                return;
            }
            i++;
        }
        //if we're still there it means the user is not known
        Users tmp = new Users(padv.getPeerID().toString(), name);
        tmp.setStatus(status);
        this.lstPeople.add(tmp);
        fireTableRowsInserted(this.lstPeople.size() - 1, this.lstPeople.size() - 1);
    }

    //
    public void updateUserStatus(String name, String status) {
        int i = 0;
        for (Users usr : this.lstPeople) {
            //If we found the specific user
            if (usr.getName().equals(name)) {
                usr.setStatus(status);
                //No need to continue, userName are unique
                break;
            }
            i++;
        }
        fireTableRowsUpdated(i, i);
    }

    //Destroy all the people wich still have a Init status after 1 discovery
    public void disconnectOffline() {
        for (Users usr : this.lstPeople) {
            //Do not concern the offline people
            if (usr.getStatus().equals("Off")) {
                removeUserByName(usr.getName());
            }
        }
    }

    //Position all the status to Init in order to identify the offline people
    public void offAllStatus(String me) {
        for (Users usr : this.lstPeople) {
            //Do not concern ourselves
            if (usr.getName().equals("Me")) {
                continue;
            }

            usr.setStatus("Off");
        }
        fireTableRowsUpdated(0, this.lstPeople.size() - 1);
    }

    public String getUserStatus(String peer) {
        for (Users usr : this.lstPeople) {
            //return the status of the specified user
            if (usr.getName().equals(peer)) {
                return usr.getStatus();
            }
        }
        //If no user is found
        return null;
    }

    public List listUsers() {
        List temp = new ArrayList();

        for (Users usr : this.lstPeople) {
            //Do not concern the offline people
            if (usr.getStatus().endsWith("(OffLine)")) {
                continue;
            }
            //retrieve a list of user like: "User1 (Online)"
            temp.add(usr.getName() + " (" + usr.getStatus() + ")");
        }

        return temp;
    }

    public void clearUsers() {
        this.lstPeople.clear();
        fireTableRowsDeleted(0,0);
    }

    public boolean isKnownUser(String name) {
        for (Users usr : this.lstPeople) {
            //return the status of the specified user
            if (usr.getName().equals(name)) {
                return true;
            }
        }

        return false;
    }

    public boolean isKnownUser(ID peerID) {
        for (Users usr : this.lstPeople) {
            //return the status of the specified user
            if (usr.getID().equals(peerID.toString())) {
                return true;
            }
        }

        return false;
    }

    protected class Users {

        String Name = null;
        String ID = null;
        String Status = "init";

        protected Users(String URI, String NickName) {
            this.Name = NickName;
            this.ID = URI;
        }

        protected Users(String URI, String NickName, String Status) {
            this.Name = NickName;
            this.Status = Status;
            this.ID = URI;
        }

        protected String getName() {
            return this.Name;
        }

        protected String getStatus() {
            return this.Status;
        }

        protected String getID() {
            return this.ID;
        }

        protected void setStatus(String status) {
            this.Status = status;
        }

        protected void setName(String name) {
            this.Name = name;
        }
    }
}

//class UserTest extends JFrame {
//
//    private JTabbedPane tabs = new JTabbedPane();
//    private UserModel tableModel = new UserModel();
//    private JXTable table = new JXTable((TableModel) tableModel);
//    public static String DELETE = "Delete";
//    public static String FOLLOW = "Follow";
//
//    public UserTest() {
//        super("B00M user test");
//
////        tableModel.addTreeModelListener(new MyTreeModelListener());
//
//        tableModel.addUser("976865754654", "Batman");
//        tableModel.updateUserStatus("Batman", "OffLine");
//        tableModel.addUser("8765436758976", "Robin", "Invisible");
//
//        tableModel.addUser("87574656536436", "Joker");
//
//        // Build the tree table panel
//        JPanel TablePanel = new JPanel(new BorderLayout());
//        TablePanel.add(new JScrollPane(table));
//        tabs.addTab("Found Users", TablePanel);
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
//                    ((UserModel) table.getModel()).removeUser(rowIndex);
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
//        AppStarter2 starter = new AppStarter2(args);
//        SwingUtilities.invokeLater(starter);
//    }
//}
//
//class AppStarter2 extends Thread {
//
//    private String[] args;
//
//    public AppStarter2(String[] args) {
//        this.args = args;
//    }
//
//    @Override
//    public void run() {
//        UserTest example = new UserTest();
//    }
//}