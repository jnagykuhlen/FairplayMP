/* 
 * Copyright (C) 2008 Naom Nisan, Benny Pinkas, Assaf Ben-David.
 * See full copyright license terms in file ../GPL.txt
 * @author Assaf Ben-David 
 */

package communication;

import java.io.OutputStream;
import java.net.Socket;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import javax.net.SocketFactory;
import javax.net.ssl.SSLSocketFactory;

//import javax.net.ssl.SSLSocketFactory;

import utils.Utils;
import communication.Messages.BasicMsg;
import java.io.IOException;
import java.nio.charset.Charset;

public class Client {

	static final SocketFactory _socketFactory = SocketFactory.getDefault();		
	// static final SocketFactory _socketFactory = SSLSocketFactory.getDefault();
	static Hashtable<String, String> _players = null;
	static String[] _CP = null;
	static String[] _RP = null;
	static Collection<Thread> _senders = new LinkedList<Thread>();
        static String _localIpEndPoint;
	
	public static void init(Hashtable<String, String> players, String[] CP,
			String[] RP, String localIpEndPoint) {
		_players = players;
		_CP = CP;
		_RP = RP;
                _localIpEndPoint = localIpEndPoint;
	}

	public static void finishSending() {
		Iterator<Thread> it = _senders.iterator();
		while (it.hasNext()) {
			try {
				it.next().join();
			} catch (InterruptedException e) {
				Utils.printErr("Exception while waiting for a sending thread to finisn.");
			}
		}
	}

	public void sendToCP(CPMsgs msgs) {
                Utils.printMsg("Preparing message " + msgs.pop(0).getID() + " for CPs...");
            
		for (int i = 0 ; i < _CP.length ; i++)
			Send(_CP[i], msgs.pop(i));
		
		Utils.printMsg("Sending message " + msgs.pop(0).getID() + " to CPs.");
	}

	public void sendToRP(String name, Msg msg) {
		String ip = _players.get(name);
		Send(ip, msg);
	}

	public void sendToRP(Msg msg) {
                Utils.printMsg("Preparing message " + msg.getID() + " for RPs...");
            
		for (int i = 0 ; i < _RP.length ; i++)
			Send(_players.get(_RP[i]), msg);
		
		Utils.printMsg("Sending message " + msg.getID() + " to RPs.");
	}

	protected void Send(String ip, Msg msg) {
		Thread sender = new Thread(new Sender(ip, msg));
		sender.start();
		_senders.add(sender);
	}
	
	protected class Sender implements Runnable {
		
		String _ip;
                int _port;
		Msg _msg;
                BasicMsg _basicMsg;
		
		public Sender(String ipEndPoint, Msg msg) {
                    String[] ipEndPointParts = ipEndPoint.split(":");
                    _ip = ipEndPointParts[0];
                    _port = Integer.parseInt(ipEndPointParts[1]);
                    _msg = msg;
                    _basicMsg = _msg.getBasicMsg();
		}

		@Override
		public void run() {
                    
                    
                    while (true){//for (int i = 0 ; i < 10 ; i++) {
                        Socket socket = null;
                        try {
                            Utils.printMsg("Connecting client to <" + _ip + ":" + _port + ">.");
                            
                            socket = _socketFactory.createSocket(_ip, _port);
                            if(socket == null)
                                Utils.printMsg("Cannot create socket.");
                            else
                                Utils.printMsg("Successfully created socket.");
                            
                            byte[] ipEndPointData = _localIpEndPoint.getBytes(Charset.forName("US-ASCII"));
                            
                            OutputStream outputStream = socket.getOutputStream();
                            outputStream.write(ipEndPointData.length);
                            outputStream.write(ipEndPointData);
                            
                            _basicMsg.writeTo(socket.getOutputStream());

                            // Close the connection and finish
                            socket.close();
                            
                            Utils.printMsg("Closed client.");
                            return;
                        }
                        catch(Exception e){
                            if(socket != null)
                            try {
                                socket.close();
                            }
                            catch(IOException ioException) {}
                            Utils.printMsg("Connection failed [" + e.getClass().getName() + "]: " + e.getMessage());
                            e.printStackTrace();
                            // Utils.printMsg(e.getStackTrace().toString());
                            try {
                                    Thread.sleep(100);
                            } 
                            catch (InterruptedException e1) {}						
                        }
                    }			
		}
	}
}
