/* 
 * Copyright (C) 2008 Naom Nisan, Benny Pinkas, Assaf Ben-David.
 * See full copyright license terms in file ../GPL.txt
 * @author Assaf Ben-David 
 */

package communication;

import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;
import java.util.SortedMap;
import java.util.Vector;
import javax.net.ServerSocketFactory;
import javax.net.ssl.SSLServerSocketFactory;

import communication.Messages.BasicMsg;
import java.nio.charset.Charset;

//import javax.net.ssl.SSLServerSocketFactory;


import utils.Utils;

public class Server implements Runnable{
	
	Vector<SortedMap<String, Msg>> _msgs = null;
	LinkedList<MsgPair> _msgQ = new LinkedList<MsgPair>();
	int _port;
	Socket _connection = null;

	public Server(Vector<SortedMap<String, Msg>> msgs, int port) {
		_port = port;
		_msgs = msgs;
		Thread mo = new Thread(new MsgOrgenizer());
		mo.start();
	}
	
	@Override
	public void run() {
	    ServerSocket serversocket;
	    
	    try{
	    	
			//serversocket = SSLServerSocketFactory.getDefault().createServerSocket(_port);
                        serversocket = ServerSocketFactory.getDefault().createServerSocket(_port);
                        
                        Utils.printMsg("Server is running at port <" + _port + ">.");
                        
			while (true) {
                            _connection = serversocket.accept();
                            if (_connection == null)
                                    return;

                            Utils.printMsg("Accepted client at <" + _connection.getRemoteSocketAddress() + ">.");
                            // push the message into the queue.
                            synchronized (_msgQ){
                                    // String ip = _connection.getInetAddress().getHostAddress();
                                    InputStream inputStream = _connection.getInputStream();
                                    
                                    int remoteIpEndPointLength = inputStream.read();
                                    byte[] remoteIpEndPointData = new byte[remoteIpEndPointLength];
                                    
                                    inputStream.read(remoteIpEndPointData);
                                            
                                    String remoteIpEndPoint = new String(remoteIpEndPointData, Charset.forName("US-ASCII"));
                                    
                                    Msg msg = new Msg(BasicMsg.parseFrom(_connection.getInputStream()));
                                    _msgQ.add(new MsgPair(msg, remoteIpEndPoint));
                                    _msgQ.notifyAll();
                                    
                                    Utils.printMsg("Received msg " + msg.getID() + ".");
                            }
			}
	    }
	    catch (Exception e) {
	    	Utils.printErr("Server down: " + e.getMessage());
	    }
	}
	
	
	class MsgPair {
		public Msg _msg;
		public String _ip;

		public MsgPair(Msg msg, String ip) {
			_msg = msg;
			_ip = ip;
		}
	}
	
	class MsgOrgenizer implements Runnable {

		@Override
		public void run() {
			
			MsgPair mp = null;
			while (true){
				synchronized (_msgQ){
					// While there are messages recieved
					if (! _msgQ.isEmpty())
						// Take one
						mp = _msgQ.remove();
					else{
						mp = null;
						try {
							_msgQ.wait();
						} catch (InterruptedException e) {
							e.printStackTrace();
							System.exit(1);
						}
					}
				}
				if (mp != null)
					// Orgenize it
					deliver(mp);				
			}
		}

		protected void deliver(MsgPair mp) {
			
			int msgID = mp._msg.getID(); 
			SortedMap<String, Msg> msgs = _msgs.elementAt(msgID);
			synchronized (msgs){
				msgs.put(mp._ip,mp._msg);
				msgs.notifyAll();
			}
		}
	}
}