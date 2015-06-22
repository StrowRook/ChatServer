/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package chatserver;

import java.io.DataOutputStream;
import java.io.IOException;

import java.net.ServerSocket;
import java.net.Socket;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 * @version 0.0.1
 * @author Rocchi Francesco
 */
public class ClientConnectionsManager {
	private ServerSocket serverSocket;
	private Socket connSocket;
	private ChatServer chat;
	private final Map<String, ArrayList<String>> connectedClientMessages;
	private ArrayList<String> tempMessages;
	private final JSONParser parser;
	private String messageType;
	private JSONObject root;
	private String username;
	private boolean readable;

	/**
	 * Costruttore della classe ClientConnectionsManager: crea una nuova socket per il server e accetta eventuali
	 * connessioni da parte dei client.
	 * @param chat Oggetto istanza della classe ChatServer
	 */
	public ClientConnectionsManager(ChatServer chat) {
		this.chat = chat;
		parser = new JSONParser();
		connectedClientMessages = new HashMap<>();
		try {
			serverSocket = new ServerSocket(9989);
			while(true) {
				connSocket = serverSocket.accept();
				MessagesReceiver mr = new MessagesReceiver(connSocket, this);
				mr.start();
			}
		} catch (IOException ex) {
			Logger.getLogger(ClientConnectionsManager.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	/**
	 * Analizza e gestisce il contenuto del messaggio passato come parametro. Se è un messaggio di login, controlla
	 * l'username e la password e conferma o respinge l'accesso. Se è un messaggio da scambiare con l'altro client,
	 * memorizza il messaggio sul database e lo invia all'altro client.	 * 
	 * @param message Messaggio da analizzare
	 * @param socket
	 */
	public void parse(String message, Socket socket) {
		try {
			if(message != null) {
				JSONObject obj = new JSONObject();
				root = (JSONObject) parser.parse(message);
				messageType = (String) root.get("type");
				switch(messageType) {
					case "login":
						username = (String) root.get("username");
						MessagesSender ms = new MessagesSender(this, socket, username, connectedClientMessages);
						ms.start();
						if(chat.login(username, (String) root.get("password"))) {
							connectedClientMessages.put(username, new ArrayList<>());
							
							obj.put("status", "ok");
							obj.put("messages", chat.getAllMessagesByUser(username));
							
							ms.sendSingleMessage(obj.toJSONString());
						} else {
							obj.put("status", "fail");
							ms.sendSingleMessage(obj.toJSONString() + '\n');
						}
						break;
					case "text":
						String addressee = (String) root.get("addressee");
						// Inserisce il messaggio nel database
						chat.insertMessage((String) root.get("text"), (String) root.get("sender"), addressee, (String) root.get("time"), (String) root.get("date"));
						// Se il client è presente nella Map, allora significa che è connesso, perciò...
						if(connectedClientMessages.containsKey(addressee)) {
							// ... mette in coda alla lista di messaggi, il messagggio corrente
							System.out.println("È online");
							putInTheQueue(addressee, message);
						} else 
							System.out.println("È offline");
						break;
					case "search":
						String searcher = (String) root.get("searcher");
						if(chat.searchUserByUsername((String) root.get("username"))) {
							root.put("status", "ok");
							putInTheQueue(searcher, root.toJSONString() + '\n');
						} else {
							root.remove("username");
							root.put("status", "fail");
							putInTheQueue(searcher, root.toJSONString() + '\n');
						}
				}
			} else if(message == null || socket.isClosed()) {
				if(connectedClientMessages.containsKey(username))
					connectedClientMessages.remove(username);
			}
		} catch (ParseException ex) {
			Logger.getLogger(MessagesReceiver.class.getName()).log(Level.SEVERE, null, ex);
		}
	}
	
	private void putInTheQueue(String addressee, String message) {
		tempMessages = connectedClientMessages.get(addressee);
		synchronized(tempMessages) {
			tempMessages.add(message);
		}
	}
	
	public String removeFromTheQueue (String username) {
		tempMessages = connectedClientMessages.get(username);
		synchronized(tempMessages) {
			String message = tempMessages.get(0);
			tempMessages.remove(0);
			return message;
		}
	}
}
