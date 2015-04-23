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

import java.awt.GraphicsEnvironment;
import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;


public class Kb00m {

    public final static String APP_VERSION = "0.9";
    public final static String APP_NAME = "KB00m";
    public final static String APP_DESC = "Search, Chat, Share... change the World!!";
    public final static String APP_ICON = "logo.png";
    public final static String APP_URL = "http://localhost/b00m/java/";
    public final static String APP_URL_JAR = APP_URL+"Kb00m.jar";
    public final static String APP_NODES = "http://nodes.certiwise.com/register.php";
    public final static String OS_FILE_SEPARATOR = System.getProperty("file.separator");
    public final static String OS_NAME = System.getProperty("os.name").toLowerCase(); //Detect OS we're working on
    public final static String DELETE = "Delete";
    public final static String DOWNLOAD = "Downloading";
    public final static String PAUSE = "Pause";
    public final static String RESUME = "Resume";
    public final static String FOLLOW = "Follow";
    public final static String FOLLOW_PAUSE = "FollowPaused";
    public final static String DIRECTORY = "Directory";
    public final static String FILE = "File";
    public final static Integer PATTERN_LEN = 5 ;
    public static boolean GUI_MODE = !GraphicsEnvironment.isHeadless();
    protected static final Logger b00mLog = Logger.getLogger("");
	
   public static void main(String[] args) {
        
        //JXTA is extremly verbose, not so much log is necessary
        //If you need some more, change the Level value to FINE or INFO
        //LogManager.getLogManager().addLogger(b00mLog);
        b00mLog.setLevel(Level.SEVERE);
        
        //if (GraphicsEnvironment.isHeadless()) {
        if(!GUI_MODE || (args.length==2 && args[0].equals("server"))){
            //CLI mode
            File d = new File(args[1]);
            if(d.isDirectory())
                new CLI(args[0], d);
            else
                System.out.println("The directory specified is not valid!!\nUsage: "+APP_NAME+" server /default/shared/directory\n\n");
            
        } else {
            //GUI mode
            if(OS_NAME.indexOf("mac") >= 0){
                System.setProperty("apple.laf.useScreenMenuBar", "true");
                System.setProperty("com.apple.mrj.application.apple.menu.about.name", APP_NAME);
            }
            
            new GUI().initCore();
        }
    }
}
