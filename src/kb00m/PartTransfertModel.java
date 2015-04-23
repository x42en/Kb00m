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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Observable;
import java.util.Observer;
import javax.swing.ImageIcon;
import org.jdesktop.swingx.treetable.AbstractTreeTableModel;
import org.jdesktop.swingx.treetable.TreeTableModel;


public class PartTransfertModel extends AbstractTreeTableModel implements TreeTableModel, Observer {

    //As we schedul multipart download register each of them under the download hash
    private HashMap<Transfert,ArrayList<Transfert>> multiPartTransfert = new HashMap<Transfert,ArrayList<Transfert>>();
    private Transfert root_ = null;

    protected PartTransfertModel() {
        this.root_ = new Transfert("",new ShareEntry("","",null,""));
        this.multiPartTransfert.put(this.root_, new ArrayList<Transfert>());
    }
    
    @Override
    public String toString(){
        String res = "";
        for(Transfert tr : this.multiPartTransfert.get(this.root_)){
            res += "File: "+tr.getName()+" ("+tr.getStatus()+")\n";
            for(Transfert pr : this.multiPartTransfert.get(tr))
                res += "\t-Part#"+tr.getPart()+" seeder:"+tr.getSeeder()+" ("+tr.getStatus()+")\n";
        }
        
        return res;
    }
    
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false;
    }

    @Override
    public int getColumnCount() {
        return 7;
    }

    @Override
    public String getColumnName(int column) {
        String name = null;
        switch (column) {
            case 0:
                name = "Type";
                break;
            case 1:
                name = "File Name";
                break;
            case 2:
                name = "Seeder";
                break;
            case 3:
                name = "Progress";
                break;
            case 4:
                name = "Checksum";
                break;
            case 5:
                name = "Susp";
                break;
            case 6:
                name = "Del";
                break;
            default:
                name= "unknown";
                break;
            
        }
        return name;
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        Class clazz = String.class;
        switch (columnIndex) {
            case 5:
                clazz = ButtonColumn.class;
                break;
            case 6:
                clazz = ButtonColumn.class;
                break;
        }
        return clazz;
    }

    @Override
    public Object getValueAt(Object value, int columnIndex) {
        Transfert tr = (Transfert) value;
        switch (columnIndex) {
            case 0:
                return tr.getType();
            case 1:
                return tr.getName();
            case 2:
                return tr.getSeeder();
            case 3:
                return (tr.getStatus().equals("Downloading...")) ? tr.getProgress() : tr.getStatus();
            case 4:
                return tr.getHash();
            case 5:
                if(Kb00m.GUI_MODE)
                        return (tr.getStatus().equals("Suspended")) ? new ImageIcon(getClass().getResource("/Picts/Small/resume.png")) : new ImageIcon(getClass().getResource("/Picts/Small/pause.png"));
                return "";
            case 6:
                if(Kb00m.GUI_MODE)
                        return  new ImageIcon(getClass().getResource("/Picts/Small/delete.png"));
                return "";
            default:
                return null;
        }
    }

    @Override
    public void setValueAt(Object aValue, Object value, int columnIndex) {
//        if((Transfert) aValue == this.root_){
//            
//        }
//        Transfert tr = (Transfert) value;
//        switch (columnIndex) {
//            case 3:
//                if(aValue.getClass() == String.class)
//                    this.lstTransfert.get(rowIndex).setStatus((String) aValue);
//                else
//                    this.lstTransfert.get(rowIndex).setProgress((Integer) aValue);
//                break;
//            default:
//                break;
//            
//        }
//        fireTableCellUpdated(rowIndex, columnIndex);
    }
    
    protected Transfert getTransfertAt(int i){
        return this.multiPartTransfert.get(this.root_).get(i);
    }

    protected Transfert getTransfert(String hash){
        for(Transfert tmp : this.multiPartTransfert.get(this.root_)){
            if(tmp.getHash().equals(hash))
                return tmp;
        }
        return null;
    }

    protected void addtransfert(String seeder, ShareEntry tr) {
        Transfert tmp = new Transfert(seeder ,tr);
        //If the transfert is unknown, we add it
        if (this.multiPartTransfert.get(this.root_).indexOf(tmp) == (-1)) {
            this.multiPartTransfert.get(this.root_).add(tmp);
            this.multiPartTransfert.put(tmp, new ArrayList<Transfert>());
        }
//        fireTableRowsInserted(this.lstTransfert.size() - 1, this.lstTransfert.size() - 1);
    }
    
    protected void addpart(String trName, int partNum, ShareEntry tr) {
        Transfert tmp = new Transfert(trName ,tr);
        
        Transfert tmpPart = new Transfert(trName+" #"+partNum ,tr);
        tmp.setPart(partNum);
        if(this.multiPartTransfert.get(this.root_).contains(tmp))
            this.multiPartTransfert.get(tmp).add(tmpPart);
//        fireTableRowsInserted(this.lstTransfert.size() - 1, this.lstTransfert.size() - 1);
    }
    
    protected void removeTransfertByHash(String hash) {
        for(Transfert t : this.multiPartTransfert.get(this.root_)){
            if(t.getHash().equals(hash)){
                //Don't break has it will remove all transfert of this hash
                this.multiPartTransfert.get(this.root_).remove(t);
            }
        }
        
//        fireTableRowsDeleted(this.lstTransfert.size() - 1, this.lstTransfert.size() - 1);
    }

    protected void removeTransfertByName(String name) {
        for(Transfert t : this.multiPartTransfert.get(this.root_)){
            if(t.getName().equals(name)){
                //Don't break has it will remove all transfert of this name
                this.multiPartTransfert.get(this.root_).remove(t);
            }
        }
        
//        fireTableRowsDeleted(this.lstTransfert.size() - 1, this.lstTransfert.size() - 1);
    }
    
    protected void removePartByHash(String hash, int partNum){
        
                //Parse part in order to find occurence of hash
                for(Transfert tr : this.multiPartTransfert.get(this.root_)){
                    for(Transfert p : this.multiPartTransfert.get(tr)){
                        if(p.getHash().equals(hash))
                            this.multiPartTransfert.get(tr).remove(p);
                    }
                }
    }

    protected void removeTransfert(Transfert tr) {

        this.multiPartTransfert.get(this.root_).remove(tr);
//        fireTableRowsDeleted(this.lstTransfert.size() - 1, this.lstTransfert.size() - 1);

    }

    protected void removeTransfert(int i) {

        this.multiPartTransfert.get(this.root_).remove(i);
//        fireTableRowsDeleted(this.lstTransfert.size() - 1, this.lstTransfert.size() - 1);

    }

    protected void updateTransferStatus(String fileName, String status) {
        for(Transfert t : this.multiPartTransfert.get(this.root_)){
            if(t.getName().equals(fileName)){
                t.setStatus(status);
                break;
            }
        }
        
//        fireTableRowsUpdated(this.lstTransfert.size() - 1, this.lstTransfert.size() - 1);
    }

    //
    protected void updateTransferProgress(String fileName, Integer progress) {
        for(Transfert t : this.multiPartTransfert.get(this.root_)){
            if(t.getName().equals(fileName)){
                t.setProgress(progress);
                break;
            }
        }
        
//        fireTableRowsUpdated(this.lstTransfert.size() - 1, this.lstTransfert.size() - 1);
    }
    
    protected void updatePartStatus(String downloadName, int partNum, String status) {
        
        for(Transfert tr : this.multiPartTransfert.get(this.root_)){
            if(tr.getName().equals(downloadName)){
                for(Transfert t : this.multiPartTransfert.get(tr)){
                    if(t.getPart() == partNum){
                        //Part are unique no need to continue
                        t.setStatus(status);
                        return;
                    }
                }
            }
        }
        
//        fireTableRowsUpdated(this.lstTransfert.size() - 1, this.lstTransfert.size() - 1);
    }

    //
    protected void updatePartProgress(String downloadName, int partNum, Integer progress) {
        for(Transfert tr : this.multiPartTransfert.get(this.root_)){
            if(tr.getName().equals(downloadName)){
                for(Transfert t : this.multiPartTransfert.get(tr)){
                    if(t.getPart() == partNum){
                        //Part are unique no need to continue
                        t.setProgress(progress);
                        return;
                    }
                }
            }
        }
        
//        fireTableRowsUpdated(this.lstTransfert.size() - 1, this.lstTransfert.size() - 1);
    }

    
    protected void clearTransfert() {
        this.multiPartTransfert.get(this.root_).clear();
//        fireTableRowsDeleted(this.lstTransfert.size() - 1, this.lstTransfert.size() - 1);
    }

    protected boolean isRegisterTransfer(String name) {
        for (Transfert t : this.multiPartTransfert.get(this.root_)) {
            //return the status of the specified user
            if (t.getName().equals(name)) {
                return true;
            }
        }

        return false;
    }

    protected boolean isRegisterTransfer(Transfert t) {
        if(this.multiPartTransfert.get(this.root_).indexOf(t) > 0)
            return true;

        return false;
    }

    @Override
    public Object getChild(Object o, int i) {
        Transfert tr = (Transfert) o;
        if(this.multiPartTransfert.get(this.root_).contains(tr))
            return this.multiPartTransfert.get(tr).get(i);
        else
            return this.multiPartTransfert.get(this.root_).get(i);
    }

    @Override
    public int getChildCount(Object o) {
        Transfert tr = (Transfert) o;
        if(this.multiPartTransfert.get(this.root_).contains(tr))
            return this.multiPartTransfert.get(tr).size();
        else
            return 0;
    }

    @Override
    public int getIndexOfChild(Object o, Object o1) {
        Transfert tr = (Transfert) o;
        if(this.multiPartTransfert.get(this.root_).contains(tr))
            return this.multiPartTransfert.get(this.root_).indexOf(tr);
        else
            return this.multiPartTransfert.get(tr).indexOf((Transfert) o1);
    }

    @Override
    public void update(Observable o, Object o1) {
//        throw new UnsupportedOperationException("Not supported yet.");
    }

    class Transfert {

        private ShareEntry tShare = null;
        private String tSeeder = null;
        private String tStatus = "init";
        private Integer tProgress = 0 ;
        private int tPart = 0;

        protected Transfert(String seeder, ShareEntry share) {
            this.tSeeder = seeder;
            this.tShare = share;
        }

        protected Transfert(String seeder, ShareEntry share, String status) {
            this.tSeeder = seeder;
            this.tShare = share;
            this.tStatus = status;
        }

        protected String getID() {
            return this.tShare.getID().toString();
        }

        protected String getName() {
            return this.tShare.getName();
        }

        protected String getType() {
            return this.tShare.getType();
        }

        protected String getHash() {
            return this.tShare.getHash();
        }

        protected String getSeeder() {
            return this.tSeeder;
        }
        
        protected int getPart(){
            return this.tPart;
        }

        protected String getStatus() {
            return this.tStatus;
        }

        protected Integer getProgress() {
            return this.tProgress;
        }
        
        protected void setPart(int part){
            this.tPart = part;
        }

        protected void setStatus(String status) {
            this.tStatus = status;
        }

        protected void setProgress(int progress) {
            this.tProgress = progress;
        }
    }
}
