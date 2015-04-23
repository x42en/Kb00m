package kb00m;

/*
 * © 2013 - Certiwise Software Services (www.certiwise.com)
 *
 * KB00m code is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2.1 of the License, or (at your option) any later
 * version.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place - Suite 330, Boston, MA 02111-1307, USA.
 *
 * Author : benoit Malchrowicz Contact Mail: bmz at certiwise dot com Softwares
 * : JXTA Version 2.7, JDK Version 1.6.0_05, NetBeans IDE Version 7.1.1,
 * BouncyCastle Version 1.47
 *
 */
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.Properties;
import java.util.Stack;
import javax.swing.JFileChooser;

//This class will create initialization configuration for class 
//and Sets Peers Share folder
public final class Config extends JFileChooser {

    /**
     * Creates a new instance of Config
     */
    private boolean firstTime = true;
    private String SharedPath = null;
    private File dirShared = null;
    private File configFile = null;
    private String PeerName = null;
    private static String GrpName = Core.MAINGROUP_NAME;

    protected Config() {
        this.configFile = new File(new File("." + Core.MAINGROUP_NAME), Core.MAINGROUP_NAME + ".cfg");

        displayLog("[+]Searching for Configuration file.");
        if (this.configFile.exists()) {
            displayLog("[+]Configuration file Found.");
            this.firstTime = false;
            
            //If we are not alone don't touch others configuration
            if(isAlone()){
                displayLog("[+]Cleaning OLD sessions");
                cleaningConfigDir();
            }
                
        } else {
            displayLog("[-]Configuration file !NOT! Found.");
            this.firstTime = true;
        }
    }
    
    private boolean isAlone(){
        try {
                ServerSocket tmp = new ServerSocket(1337);
                if (tmp != null) {
                    tmp.close();
                }
            } catch (IOException ex) {
                return false;
            }
        return true;
    }

    private void displayLog(String s) {
        if (Kb00m.GUI_MODE) {
            GUI.addLogMsg(s);
        } else {
            System.out.println(s);
        }
    }

    protected boolean isFirstTime() {
        return this.firstTime;
    }

//    protected void searchForConfigFile(){ //Search for Configuration file
//        displayLog("[+]Searching for Configuration file.");
//        if(this.configFile.exists() && this.configFile.isFile()){
//            displayLog("[+]Configuration file Found.");    
//        }
//        else{
//           displayLog("[-]Configuration file !NOT! Found."); 
//           createConfigFile(); 
//        }
//        
//        readingConfigFile();
//        
//    }
//    protected void readingConfigFile(){
//        readingConfigFile(new File(new File("."+GrpName), GrpName+".cfg"));
//    }
    protected void readingConfigFile() //Starts reading Configuration file and finds shared path from it.
    {
        displayLog("[+]Reading file: " + this.configFile.getName());

        try {
            FileInputStream in = new FileInputStream(this.configFile);
            Properties props = new Properties();

            props.load(in);
            
            in.close();
            
            String path = props.getProperty("SharedFolder");
            if (path != null) {
                File temp = new File(path);
                if (temp.exists()) {
                    displayLog("[+]Shared Path Exists");
                    this.SharedPath = temp.getAbsolutePath();
                    this.dirShared = temp;

                } else {
                    displayLog("[-]Path NOT Exists!!!");
                    createShareFolder(temp);
                }
            } else {
                displayLog("[-]Path is NOT configured, using default user directory!");
                this.SharedPath = System.getProperty("usr.dir");
//                writeConfigValue("SharedFolder", SharedPath);
                this.dirShared = new File(this.SharedPath);
            }
            this.PeerName = props.getProperty("PeerName");
            if (this.PeerName == null) {
                this.PeerName = "anonymous";
                writeConfigValue("PeerName", this.PeerName);
            }
            
        } catch (IOException ex) {
            displayLog("[!]IOException while reading value in config file: " + ex.getMessage());
        }

//        try {
//            
//            BufferedReader in = new BufferedReader(new FileReader(this.configFile));
//            String str,path;
//            int index;
//            while((str=in.readLine())!=null){
//                if(str.startsWith("SharedFolder")){
//                    index = str.indexOf("=");
//                    path = str.substring(index+1);
//                    displayLog("[+]Shared Path is: " + path);
//                    File temp = new File(path);
//                    if(temp.exists()){
//                        displayLog("[+]Shared Path Exists");
//                        SharedPath = temp.getAbsolutePath();
//                        dirShared = temp;
//                        
//                    }else{
//                        displayLog("[-]Path NOT Exists!!!");
//                        createShareFolder(temp);
//                    }
//                }
//                if(str.startsWith("PeerName")){
//                    index = str.indexOf("=");
//                    PeerName = str.substring(index+1);
//                    displayLog("[+]PeerName is: " + PeerName);
//                }
//            }
//            in.close();
//            
//        } catch (FileNotFoundException ex) {
//            displayLog("[!]Config file not found "+ex.getMessage());
//        }catch(IOException e){
//            displayLog("[!]IOException while searching config file "+e.getMessage());
//        }
    }

    private void createShareFolder(File pathname) //if the shared path doesnot exit this method will create it
    {
        displayLog("[+]Creating Share Folder...");
        if (pathname.mkdirs()) {
            displayLog("[+]Shared Folder Successfully Created.");
            this.SharedPath = pathname.getAbsolutePath();
        }

    }

    protected void createConfigFile() {
        displayLog("[+]**** Please Select Your Install Folder ****");

        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int retVal = chooser.showOpenDialog(this);
        if (retVal == JFileChooser.APPROVE_OPTION) {
            displayLog("[+]Selected Path is: " + chooser.getSelectedFile().getAbsolutePath());

            this.dirShared = chooser.getSelectedFile();

        } else {
            displayLog("[+]Default Path is: " + System.getProperty("usr.dir"));
            this.dirShared = new File(System.getProperty("usr.dir"));
        }
        this.SharedPath = this.dirShared.getAbsolutePath();

        //Create the config file and directory
        try {
            File dir = new File("." + Core.MAINGROUP_NAME);
            dir.mkdirs();
            this.configFile.createNewFile();

        } catch (IOException ex) {
            displayLog("[!]IOException while creating config file: " + ex.getMessage());
        }

        displayLog("[+]" + GrpName + ".cfg file Successfully Created.");
        displayLog("[+]Writing Data into Configuration File");
        writeConfigValue("SharedFolder", SharedPath);

    }

    protected void writeConfigValue(String key, String value) {
        try {
            FileOutputStream out = new FileOutputStream(this.configFile,true);
            FileInputStream in = new FileInputStream(this.configFile);
            Properties props = new Properties();

            props.load(in);
            
            props.setProperty(key, value);
            props.store(out, Kb00m.APP_NAME+" properties");
            in.close();
            out.close();
        } catch (IOException ex) {
            displayLog("[!]IOException while writing value in config file: " + ex.getMessage());
        }
    }
    
    protected void cleaningConfigDir(){
        File dir = new File("." + Core.MAINGROUP_NAME);
        boolean configFound = false;
        File[] currList;
        Stack<File> stack = new Stack<File>();
        stack.push(dir);
        while (! stack.isEmpty()) {
            if (stack.lastElement().isDirectory()) {
                currList = stack.lastElement().listFiles();
                if (currList.length > 0) {
                    for (File curr: currList) {
                        stack.push(curr);
                    }
                } else {
                    stack.pop().delete();
                }
            } else {
                //Exept config file
                if(stack.peek().getName().equals(Core.MAINGROUP_NAME + ".cfg")){
                    if(configFound)
                        break;
                    configFound = true;
                    stack.pop();
                    continue;
                }
                stack.pop().delete();
            }
        }
    }

    protected String getSharedPath() {
        return this.SharedPath;
    }

    protected String getPeerName() {
        return this.PeerName;
    }

    protected File getSharedDir() {
        return this.dirShared;
    }

    protected void setSharedPath(File sharedDir) {
        this.SharedPath = sharedDir.getPath();
        this.dirShared = sharedDir;
    }
//    protected boolean isFirstTime() //Search for initialization file, if not found assumes that it is
//    {                           // the first time that program is being executed and will create 
//                                //Initialization File
//        //Default path
//        SharedPath = System.getProperty("user.dir" );
//        
//        //Detect OS we're working on
//        if (Kb00m.OS_NAME.indexOf("win") >= 0 || Kb00m.OS_NAME.indexOf("mac") >= 0 || Kb00m.OS_NAME.indexOf("nix") >= 0 || Kb00m.OS_NAME.indexOf("nux") >= 0 || Kb00m.OS_NAME.indexOf("sunos") >= 0) {
//                displayLog("[+]You are runing "+Kb00m.APP_NAME+" on: " + Kb00m.OS_NAME);
//                //Allow intercept mac osx call when quit app with Cmd+Q in GUI
//                if (Kb00m.OS_NAME.indexOf("mac") >= 0)
//                {   
//                    displayLog("[+]Setting mac os sudden call interception");
//                    System.setProperty("apple.eawt.quitStrategy", "CLOSE_ALL_WINDOWS");
//                }
//        }else {
//                System.out.println("Your OS is not supported yet!!");
//                System.exit(0);
//        }
//        
//        File configFile = new File(new File("."+GrpName), GrpName+".cfg");
//        
//        if(configFile.exists() && configFile.isFile()){
//            firstTime = false;
//        }
//        else{
//            firstTime =true;
//        }
//        return firstTime;
//    }
}
