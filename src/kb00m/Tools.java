package kb00m;

/*
 * Code integrated in KB00m Software in december 2012 by Certiwise developpers
 * 
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
import java.net.URL;
import java.net.URLConnection;
import java.rmi.server.UID;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.InvalidParameterSpecException;
import java.security.spec.KeySpec;
import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import net.jxta.document.AdvertisementFactory;
import net.jxta.document.MimeMediaType;
import net.jxta.document.XMLElement;
import net.jxta.id.IDFactory;
import net.jxta.impl.access.pse.PSEAccessService;
import net.jxta.impl.content.ContentServiceImpl;
import net.jxta.impl.membership.pse.PSEMembershipService;
import net.jxta.impl.peergroup.CompatibilityUtils;
import net.jxta.impl.peergroup.StdPeerGroupParamAdv;
import net.jxta.peergroup.PeerGroup;
import net.jxta.peergroup.PeerGroupID;
import net.jxta.pipe.PipeID;
import net.jxta.pipe.PipeService;
import net.jxta.protocol.ModuleImplAdvertisement;
import net.jxta.protocol.PipeAdvertisement;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.encoders.Hex;


public class Tools {
    
    static final String ALGORITHM = "AES";                       //symmetric algorithm for data encryption
    static final String PADDING_MODE = "/CBC/PKCS5Padding";      //Padding for symmetric algorithm
    static final String CHAR_ENCODING = "UTF-8";                 //character encoding
    //final String CRYPTO_PROVIDER = "SunMSCAPI";             //provider for the crypto

    static int AES_KEY_SIZE = 256;  //symmetric key size (128, 192, 256) if using 256 you must have the Java Cryptography Extension (JCE) Unlimited Strength Jurisdiction Policy Files  installed
    
    public String getMotherBoardID(String plateform){
        String numID = null;
        
        if(plateform.equals("Windows")){
            String result = "";
            try {
            File file = File.createTempFile("getMBuid",".vbs");
            file.deleteOnExit();
            FileWriter fw = new java.io.FileWriter(file);

            String vbs =
            "Set objWMIService = GetObject(\"winmgmts:\\\\.\\root\\cimv2\")\n"
            + "Set colItems = objWMIService.ExecQuery _ \n"
            + " (\"Select * from Win32_BaseBoard\") \n"
            + "For Each objItem in colItems \n"
            + " Wscript.Echo objItem.SerialNumber \n"
            + " exit for ' do the first cpu only! \n"
            + "Next \n";

            fw.write(vbs);
            fw.close();
            Process p = Runtime.getRuntime().exec("cscript //NoLogo " + file.getPath());
            BufferedReader input =
            new BufferedReader
            (new InputStreamReader(p.getInputStream()));
            String line;
            while ((line = input.readLine()) != null) {
            result += line;
            }
            input.close();
            }
            catch(Exception e){
            e.printStackTrace();
            }
            numID = result.trim();
        }
        else if(plateform.equals("Linux") || plateform.equals("Mac")){
            
            //ioreg -l | awk '/IOPlatformUUID/ { print $4;}'
            //ioreg -l | awk '/IOPlatformSerialNumber/ { print $4;}'
            //ioreg -l | awk '/board-id/ { print $4;}'
            
            /*
             * ioreg -l | grep board-id | cut -d '"' -f 4 | head -1;
             * ioreg -l | grep IOPlatformSerialNumber | cut -d '"' -f 4 | head -1;
             * ioreg -l | grep IOPlatformUUID | cut -d '"' -f 4 | head -1
             */
            
            ProcessBuilder pb = new ProcessBuilder("bash", "-c",
                "ioreg -l | awk '/IOPlatformSerialNumber/ { print $4;}'");
            pb.redirectErrorStream(true);
            try {
                Process p = pb.start();
                String s;
                // read from the process's combined stdout & stderr
                BufferedReader stdout = new BufferedReader(
                    new InputStreamReader(p.getInputStream()));
                while ((s = stdout.readLine()) != null) {
                    System.out.println(s);
                }
                Integer tmp = p.waitFor();
                numID = tmp.toString();
                p.getInputStream().close();
                p.getOutputStream().close();
                p.getErrorStream().close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        
        return numID;
    }

    public enum WGETJavaResults {

        /**
         * Failure to connect to the URL.
         */
        FAILED_IO_EXCEPTION,
        /**
         * Failure to determine file type from the URL connection.
         */
        FAILED_UKNOWNTYPE,
        /**
         * File downloaded sucessfully.
         */
        COMPLETE
    }

    public static WGETJavaResults DownloadFile(URL theURL) throws IOException {
        URLConnection con;
        UID uid = new UID();

        con = theURL.openConnection();
        con.connect();

        String type = con.getContentType();
        System.out.println(type);

        if (type != null) {
            byte[] buffer = new byte[4 * 1024];
            int read;

            String[] split = type.split("/");
            String theFile = Integer.toHexString(uid.hashCode()) + "_"
                    + split[split.length - 1];

            FileOutputStream os = new FileOutputStream(theFile);
            InputStream in = con.getInputStream();

            while ((read = in.read(buffer)) > 0) {
                os.write(buffer, 0, read);
            }

            os.close();
            in.close();

            return WGETJavaResults.COMPLETE;
        } else {
            return WGETJavaResults.FAILED_UKNOWNTYPE;
        }
    }

    public static ByteArrayOutputStream getBytesFromFile(File f) throws IOException {
        FileInputStream cfis;

        cfis = new FileInputStream(f);

        int n;
        byte[] buffer = new byte[16];
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        while ((n = cfis.read(buffer)) != -1) {
            baos.write(buffer, 0, n);
        }

        cfis.close();

        return baos;
    }

    public static String getFilenameWithoutExtension(final String fileName) {
        final int extensionIndex = fileName.lastIndexOf(".");

        if (extensionIndex == -1) {
            return fileName;
        }

        return fileName.substring(0, extensionIndex);
    }

//    public static PipeAdvertisement GetPageAdvertisement() {
//
//        // Creating a Pipe Advertisement
//        PipeAdvertisement MyPipeAdvertisement = (PipeAdvertisement) AdvertisementFactory.newAdvertisement(PipeAdvertisement.getAdvertisementType());
//        PipeID MyPipeID = IDFactory.newPipeID(B00mCore.MAIN_PEERGROUP_ID, "Page".getBytes());
//        System.out.println("Creation of PipePageID : " + MyPipeID);
//
//        MyPipeAdvertisement.setPipeID(MyPipeID);
//        MyPipeAdvertisement.setType(PipeService.UnicastSecureType);
//        MyPipeAdvertisement.setName("Page");
//        MyPipeAdvertisement.setDescription("Allow Private Document Sharing");
//
//        return MyPipeAdvertisement;
//
//    }
//
//    public static PipeAdvertisement GetChatAdvertisement() {
//
//        // Creating a Pipe Advertisement
//        PipeAdvertisement MyPipeAdvertisement = (PipeAdvertisement) AdvertisementFactory.newAdvertisement(PipeAdvertisement.getAdvertisementType());
//        PipeID MyPipeID = IDFactory.newPipeID(B00mCore.MAIN_PEERGROUP_ID, "Chat".getBytes());
//        System.out.println("Creation of PipeChatID : " + MyPipeID);
//
//        MyPipeAdvertisement.setPipeID(MyPipeID);
//        MyPipeAdvertisement.setType(PipeService.UnicastType);
//        MyPipeAdvertisement.setName("Chat");
//        MyPipeAdvertisement.setDescription("Allow Global Communication");
//
//        return MyPipeAdvertisement;
//
//    }
//
//    public static PipeAdvertisement GetPipeAdvertisement() {
//
//        // Creating a Pipe Advertisement
//        PipeAdvertisement MyPipeAdvertisement = (PipeAdvertisement) AdvertisementFactory.newAdvertisement(PipeAdvertisement.getAdvertisementType());
//        PipeID MyPipeID = IDFactory.newPipeID(B00mCore.MAIN_PEERGROUP_ID, "b00mPipe".getBytes());
//        System.out.println("Creation of PipeID : " + MyPipeID);
//
//        MyPipeAdvertisement.setPipeID(MyPipeID);
//        MyPipeAdvertisement.setType(PipeService.PropagateType);
//        MyPipeAdvertisement.setName("Pipe");
//        MyPipeAdvertisement.setDescription("Allow Global Communication");
//
//        return MyPipeAdvertisement;
//
//    }
//    
//    public static PipeAdvertisement GetPipeAdvertisement(String seed) {
//
//        // Creating a Pipe Advertisement
//        PipeAdvertisement MyPipeAdvertisement = (PipeAdvertisement) AdvertisementFactory.newAdvertisement(PipeAdvertisement.getAdvertisementType());
//        PipeID MyPipeID = IDFactory.newPipeID(B00mCore.MAIN_PEERGROUP_ID, seed.getBytes());
//        System.out.println("Creation of PipeID : " + MyPipeID);
//
//        MyPipeAdvertisement.setPipeID(MyPipeID);
//        MyPipeAdvertisement.setType(PipeService.PropagateType);
//        MyPipeAdvertisement.setName("Pipe");
//        MyPipeAdvertisement.setDescription("Allow Global Communication");
//
//        return MyPipeAdvertisement;
//
//    }
//    
//    public static PipeAdvertisement GetSinglePipeAdvertisement(String Name) {
//
//        // Creating a Pipe Advertisement
//        PipeAdvertisement MyPipeAdvertisement = (PipeAdvertisement) AdvertisementFactory.newAdvertisement(PipeAdvertisement.getAdvertisementType());
//        PipeID MyPipeID = IDFactory.newPipeID(PeerGroupID.defaultNetPeerGroupID, (Name + "Pipe").getBytes());
//        System.out.println("Creation of Private PipeID : " + MyPipeID);
//
//        MyPipeAdvertisement.setPipeID(MyPipeID);
//        MyPipeAdvertisement.setType(PipeService.UnicastType);
//        MyPipeAdvertisement.setName("private");
//        MyPipeAdvertisement.setDescription("Allow Private Communication");
//
//        return MyPipeAdvertisement;
//
//    }
    
    protected static PipeAdvertisement getPipeAdvertisement(PeerGroupID id, String seed, boolean is_multicast) {
        PipeAdvertisement adv = (PipeAdvertisement )AdvertisementFactory.
            newAdvertisement(PipeAdvertisement.getAdvertisementType());
        // id = PeerGroupID.defaultNetPeerGroupID;
        PipeID MyPipeID = IDFactory.newPipeID(id, (seed + "Pipe").getBytes());
        adv.setPipeID(MyPipeID);
        if (is_multicast)
            adv.setType(PipeService.PropagateType); 
        else 
            adv.setType(PipeService.UnicastType); 
        adv.setName("Pipe Service");
        adv.setDescription("Allow communication beetwen peers");
        return adv;
    }

    public static ModuleImplAdvertisement createAllPurposePeerGroupImplAdv(String peerGroupName, String peerGroupDesc) {

        ModuleImplAdvertisement implAdv = CompatibilityUtils.createModuleImplAdvertisement(
                PeerGroup.allPurposePeerGroupSpecID, peerGroupName,
                peerGroupDesc);

        // Create the service list for the group.
        StdPeerGroupParamAdv paramAdv = new StdPeerGroupParamAdv();

        // set the services
        paramAdv.addService(PeerGroup.endpointClassID, PeerGroup.refEndpointSpecID);
        paramAdv.addService(PeerGroup.resolverClassID, PeerGroup.refResolverSpecID);
        paramAdv.addService(PeerGroup.membershipClassID, PeerGroup.refMembershipSpecID);
        paramAdv.addService(PeerGroup.accessClassID, PeerGroup.refAccessSpecID);

        // standard services
        paramAdv.addService(PeerGroup.discoveryClassID, PeerGroup.refDiscoverySpecID);
        paramAdv.addService(PeerGroup.rendezvousClassID, PeerGroup.refRendezvousSpecID);
        paramAdv.addService(PeerGroup.pipeClassID, PeerGroup.refPipeSpecID);
        paramAdv.addService(PeerGroup.peerinfoClassID, PeerGroup.refPeerinfoSpecID);

        paramAdv.addService(PeerGroup.contentClassID, ContentServiceImpl.MODULE_SPEC_ID);

        // Insert the newParamAdv in implAdv
        XMLElement paramElement = (XMLElement) paramAdv.getDocument(MimeMediaType.XMLUTF8);
        implAdv.setParam(paramElement);

        return implAdv;

    }

    public static ModuleImplAdvertisement createAllPurposePeerGroupWithPSEModuleImplAdv(String peerGroupName, String peerGroupDesc) {

        ModuleImplAdvertisement implAdv = CompatibilityUtils.createModuleImplAdvertisement(
                PeerGroup.allPurposePeerGroupSpecID, peerGroupName,
                peerGroupDesc);

        // Create the service list for the group.
        StdPeerGroupParamAdv paramAdv = new StdPeerGroupParamAdv();

        // set the services
        paramAdv.addService(PeerGroup.endpointClassID, PeerGroup.refEndpointSpecID);
        paramAdv.addService(PeerGroup.resolverClassID, PeerGroup.refResolverSpecID);
        paramAdv.addService(PeerGroup.membershipClassID, PSEMembershipService.pseMembershipSpecID);
        paramAdv.addService(PeerGroup.accessClassID, PSEAccessService.PSE_ACCESS_SPEC_ID);

        // standard services
        paramAdv.addService(PeerGroup.discoveryClassID, PeerGroup.refDiscoverySpecID);
        paramAdv.addService(PeerGroup.rendezvousClassID, PeerGroup.refRendezvousSpecID);
        paramAdv.addService(PeerGroup.pipeClassID, PeerGroup.refPipeSpecID);
        paramAdv.addService(PeerGroup.peerinfoClassID, PeerGroup.refPeerinfoSpecID);

        paramAdv.addService(PeerGroup.contentClassID, ContentServiceImpl.MODULE_SPEC_ID);

        // Insert the newParamAdv in implAdv
        XMLElement paramElement = (XMLElement) paramAdv.getDocument(MimeMediaType.XMLUTF8);
        implAdv.setParam(paramElement);

        return implAdv;

    }

    class CRTFilter implements FilenameFilter {

        public boolean accept(File dir, String name) {
            return (name.endsWith(".crt"));
        }
    }

    public static byte[] createChecksum(String filename) throws
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

    public static byte[] createHash(String text) throws Exception {
        Security.addProvider(new BouncyCastleProvider());

        MessageDigest complete = MessageDigest.getInstance("SHA-256", "BC");
        complete.update(text.getBytes());
        return Hex.encode(complete.digest());
    }

    public static String getHash(String text) throws Exception {
        byte[] b = createHash(text);
        String result = "";
        for (int i = 0; i < b.length; i++) {
            result +=
                    Integer.toString((b[i] & 0xff) + 0x100, 16).substring(1);
        }
        return result;
    }

    public static String getChecksum(String filename) throws Exception {
        byte[] b = createChecksum(filename);
        String result = "";
        for (int i = 0; i < b.length; i++) {
            result +=
                    Integer.toString((b[i] & 0xff) + 0x100, 16).substring(1);
        }
        return result;
    }
    
    protected static String encryptAES(String plainText, String password, byte[] salt) throws NoSuchAlgorithmException, NoSuchProviderException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, InvalidAlgorithmParameterException, UnsupportedEncodingException, IOException, InvalidKeySpecException, InvalidParameterSpecException
    {
            /* Derive the key, given password and salt. */
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
            KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, 65536, AES_KEY_SIZE);
            SecretKey tmp = factory.generateSecret(spec);
            SecretKey secret = new SecretKeySpec(tmp.getEncoded(), ALGORITHM);
            /* Encrypt the message. */
            Cipher cipher = Cipher.getInstance(ALGORITHM + PADDING_MODE);
            cipher.init(Cipher.ENCRYPT_MODE, secret);
            AlgorithmParameters params = cipher.getParameters();
            JPrivateMessage.setIV(params.getParameterSpec(IvParameterSpec.class).getIV());
            byte[] ciphertext = cipher.doFinal(plainText.getBytes(CHAR_ENCODING));
        
        return Base64.encodeBytes(ciphertext);
        //return new String(encryptedData, CHAR_ENCODING);
    }

    protected static String decryptAES(String encryptedData, String password, byte[] salt, byte[] aesIVr) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException, UnsupportedEncodingException, IOException, InvalidKeySpecException{
        
        /* Derive the key, given password and salt. */
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, 65536, AES_KEY_SIZE);
        SecretKey tmp = factory.generateSecret(spec);
        SecretKey secret = new SecretKeySpec(tmp.getEncoded(), ALGORITHM);

        /* Decrypt the message, given derived key and initialization vector. */
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, secret, new IvParameterSpec(aesIVr));
        byte[] rawData = Base64.decode(encryptedData);
        String plaintext = new String(cipher.doFinal(rawData));
        System.out.println(plaintext);    

        return plaintext;
        
    }
}
