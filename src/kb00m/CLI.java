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


import java.io.*;
import java.util.Scanner;
import net.jxta.rendezvous.RendezvousEvent;

public class CLI  extends Thread {

    // Creates a new instance of B00m
    public CLI(String nickName, File dir) {
        
            String word = null;
            DataInputStream is = null;
            DataInputStream inputLine =null;
            
            String responseLine;
            
            System.setProperty("net.jxta.endpoint.WireFormatMessageFactory.CBJX_DISABLE", "true");
            
            //Launch the main process
            Core b00m = new Core(nickName);
            b00m.launchB00m();
            nickName = b00m.getNickName();
            
            //Start standard services
            b00m.startServices(dir);
            
            try {
                //Gives time to service to finish starting
                sleep(1000);
            } catch (InterruptedException ex) {
                System.out.println("Sorry, Interruption unexpected... "+ex.getMessage());
            }
            
            //Start the shell emulation
            Scanner LineInput=new Scanner(System.in);
            String cmd = null;
            String context = "";
            String group = "";
            String[] cliArgs = null;
            
            //Enter main loop to read cli instruction
            while (true) {
                System.out.print(nickName+"@JXTA"+context+">");
                cmd=LineInput.nextLine();
                cliArgs = cmd.split(" ");
                if (context.equals("") && cliArgs.length==1 && cliArgs[0].equals("exit")) {
                    break;
                }else if (context.equals("") && cliArgs.length==1 && cliArgs[0].equals("pwd")) {
                    String currentDir = new File(".").getAbsolutePath();
                    System.out.println(currentDir);
                }else if (context.equals("") && cliArgs.length==3 && cliArgs[0].equals("send")) {
                    System.out.println("TODO: implements some text messaging function\nSomething like: send user message");
                }else if (context.equals("") && cliArgs.length==2 && cliArgs[0].equals("chat")) {
                    group = cliArgs[1];
                    context = "[ChatRoom:"+group+"]";
                }else if (context.equals("") && (cliArgs.length==2 || cliArgs.length==3) && cliArgs[0].equals("share")) {
                    System.out.println("TODO: implements some share function\nSomething like: share [peerGroup] file");
                }else if (context.equals("") && (cliArgs.length==2 || cliArgs.length==3) && cliArgs[0].equals("download")) {
                    System.out.println("TODO: implements some download function\nSomething like: download [peerGroup] fileID");
                }else if (context.equals("") && (cliArgs.length==2 || cliArgs.length==3) && cliArgs[0].equals("list")) {
                    if(cliArgs[(cliArgs.length-1)].equals("users")) System.out.println(b00m.listUsersCli());
                    else if(cliArgs[(cliArgs.length-1)].equals("groups")) System.out.println(b00m.listGroupsCli());
                    else if(cliArgs[(cliArgs.length-1)].equals("files")){
                        if(cliArgs[(cliArgs.length-2)].equals("list")){
                            System.out.println(b00m.listSharesCli());
                        }else{
                            System.out.println(b00m.listSharesCli(cliArgs[(cliArgs.length-2)]));
                        }
                    }
                }else if (context.equals("") && cliArgs.length==2 && cliArgs[0].equals("create")) {
                    System.out.println("TODO: implements creation of private P2P function\nSomething like: create peerGroup");
                }else if (context.equals("") && cliArgs.length==2 && cliArgs[0].equals("join")) {
                    group = cliArgs[1];
                    context = "[PeerGroup:"+group+"]";
                }else if (cliArgs.length==1 && cliArgs[0].equals("/quit")) {
                    if(context.isEmpty()){ System.out.println("You are already at the root level\nIf you want to quit "+Kb00m.APP_NAME+" type: exit");continue;}
                    context = "";
                    group = "";
                }else if (context.equals("") && cliArgs.length==1 && cliArgs[0].equals("about")) {
                    System.out.println("\n############ ..:: ABOUT ::.. ##############\n"+
                            "This is "+Kb00m.APP_NAME+" v"+Kb00m.APP_VERSION+" (cli-mode)\n"+
                            "Application proudly proposed to you by the Alchemist Factory\n"+
                            "Please help us and visit "+Kb00m.APP_URL+" to donate !!\n"+
                            "This app is Open-Source and under GNU Privacy Licence...\n"+
                            "that does NOT means we do not need some money :)\n"+
                            "Any contribution is welcome:\nDEVELOPMENT/TRANSLATION/MARKETING/ADVERTISEMENT!!\n"+
                            "We wish you to enjoy this app, be careful among others, \nbe respectful, don't ignore the laws\n\n"+
                            "---------- HAPPY SHARING !! --------------\n\n"+
                            "###########################################\n\n\n");
                }else if (context.equals("") && cliArgs.length==1 && cliArgs[0].equals("license")) {
                    try {
                        FileInputStream fStream = new FileInputStream("GNU_GPL_2.1");
                        BufferedReader in = new BufferedReader(new InputStreamReader(fStream));
                        System.out.println("\n############ ..:: GNU GPL LICENSE 2.0 ::.. ##############\n");
                        while (in.ready()) {
                            System.out.println(in.readLine());
                        }
                        in.close();
                        System.out.println("###########################################\n\n\n");
                    } catch (IOException e) {
                        System.out.println("License File not found please check http://www.gnu.org/licenses/gpl-2.0.txt\n");
                    }
                }else if (context.equals("") && cmd.equals("help    ;)")) {
                    System.out.println("Are you kidding dude?! You can't be THAT stupid ?!! :D");
                }else if (context.equals("") && cmd.equals("help")) {
                    System.out.println("############## ..:: "+Kb00m.APP_NAME+" HELP ::.. ###############\n\n"+
                                        "Here are the main command\n"+
                                        " about\t\t--About this app\n"+
                                        " pwd\t\t--Print the current path\n"+
                                        " list\t\t--Allow listing groups, users or files in group\n\t\t\tFormat is:  list [peerGroup] users|files\n\t\t\tOr on global context:  list groups|users|files\n"+
                                        " create\t\t--Allow creation of a private P2P group\n\t\t\tFormat is:  create peerGroup\n"+
                                        " join\t\t--Allow joining a private P2P group\n\t\t\tFormat is:  join peerGroup\n"+
                                        " send\t\t--Send text message to someone\n\t\t\tFormat is:  send user message\n"+
                                        " chat\t\t--Allow you to enter in a chatRoom\n\t\t\tFormat is:  enter chatroom\n"+
                                        " share\t\t--Allow sharing files in a group\n\t\t\tFormat is:  share [peerGroup] file\n"+
                                        " download\t--Download a file from a group\n\t\t\tFormat is:  download [peerGroup] fileID\n"+
                                        " exit\t\t--Quit the application\n\n"+
                                        " \t==== CONTEXTUAL HELP ====\n"+
                                        " /quit\t\t--Quit the chatroom\n"+
                                        " ?\t\t--Print the chat help while chating\n\n"+
                                        "###################################################\n\n"+
                                        "Note: the main group is called b00m and is already joined\nFurther help is coming soon... But we also need to sleep sometimes !! ;)\n");
                } else if(!context.equals("") && cliArgs.length==1 && cliArgs[0].equals("?")){
                    System.out.println("=== CONTEXTUAL HELP ===\n"+
                                        "Type: /quit    to quit the chatroom");
                } else if(!context.equals("") && !cmd.isEmpty()){
                    if(!context.equals("")) b00m.sendChatMsg(group,cmd);
                }else{
                    if (context.equals("") && !cmd.isEmpty())
                        System.out.println("Unknown command... need help ?\nType: help    ;)");
                }
                
            }
            
            //When typing exit, close the app
            b00m.B00mStop();
            //Quitting nicely
            System.out.println("[+]Bye bye.");
            System.exit(0);
        
    }
    
    public synchronized void rendezvousEvent(RendezvousEvent event) {
        if (event.getType() == RendezvousEvent.RDVCONNECT || event.getType() == RendezvousEvent.RDVRECONNECT) {
            notify();
        }
    }
    
}
