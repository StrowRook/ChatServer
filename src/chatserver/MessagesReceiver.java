/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package chatserver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import java.net.Socket;

import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * @version 0.0.1
 * @author Rocchi Francesco
 */
public class MessagesReceiver extends Thread {
	private final Socket socket;
	private BufferedReader br;
	private String message;
	private ClientConnectionsManager ccm;
	
	/**
	 * Costruttore della classe ClientConnection
	 * @param socket Socket di connessione con un client specifico
	 * @param ccm Oggetto istanza della classe ClientConnectionsManager
	 */
	public MessagesReceiver(Socket socket, ClientConnectionsManager ccm) {
		this.socket = socket;
		this.ccm = ccm;
		message = "";
		try {
			br  = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		} catch (IOException ex) {
			Logger.getLogger(MessagesReceiver.class.getName()).log(Level.SEVERE, null, ex);
		}
	}
	
	@Override
	public void run() {
		try {
       			while (!socket.isClosed() && message != null) {
				message = br.readLine();
				ccm.parse(message, socket);
				System.out.println("RICEVUTO: " + message);
			}
			socket.close();
		} catch (IOException ex) {
			Logger.getLogger(ClientConnectionsManager.class.getName()).log(Level.SEVERE, null, ex);
		}
	}
}