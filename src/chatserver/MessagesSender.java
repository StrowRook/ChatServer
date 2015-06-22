/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package chatserver;

import java.io.DataOutputStream;
import java.io.IOException;

import java.net.Socket;

import java.util.ArrayList;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @version 0.0.1
 * @author Rocchi Francesco
 */
public class MessagesSender extends Thread {
	private DataOutputStream dos;
	private ClientConnectionsManager ccm;
	private final Socket socket;
	private String username;
	private final Map<String, ArrayList<String>> connectedClientMessages;

	public MessagesSender(ClientConnectionsManager ccm, Socket socket, String username, Map<String, ArrayList<String>> connectedClientMessages) {
		this.ccm = ccm;
		this.socket = socket;
		this.username = username;
		this.connectedClientMessages = connectedClientMessages;
		try {
			dos = new DataOutputStream(socket.getOutputStream());
		} catch (IOException ex) {
			Logger.getLogger(MessagesSender.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	public void sendSingleMessage(String message) {
		try {
			dos.writeBytes(message + '\n');
		} catch (IOException ex) {
			Logger.getLogger(MessagesSender.class.getName()).log(Level.SEVERE, null, ex);
		}
		
		System.out.println("INVIATO: " + message);
	}
	
	@Override
	public void run() {
		while(socket.isConnected()) {
			if(connectedClientMessages.containsKey(username)) {
				if(!connectedClientMessages.get(username).isEmpty())
					sendSingleMessage(ccm.removeFromTheQueue(username));
			}
		}
		try {
			socket.close();
		} catch (IOException ex) {
			Logger.getLogger(MessagesSender.class.getName()).log(Level.SEVERE, null, ex);
		}
	}
}
