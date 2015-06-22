/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package chatserver;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JOptionPane;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 * @version 0.0.1
 * @author Rocchi Francesco
 */
public class ChatServer {
	private String driver;
	private String dbAddress;
	private final GUI gui;
	private String[] columnTitle;
	private String[][] table;
	private Connection connection;
	private Statement stat;
	private ResultSet resultSet;
	private final ClientConnectionsManager ccm;

	public ChatServer() {
		gui = new GUI(this);
		ccm = new ClientConnectionsManager(this);
	}
	
	public String[] getColumnTitle() {
		return columnTitle;
	}

	public String[][] getTable() {
		return table;
	}

	public int createDBConnection(String ip, String port) {
		try {
			driver = "com.mysql.jdbc.Driver";
			Class.forName(driver);
			dbAddress = "jdbc:mysql://" + ip + ":" + port + "/chat?user=strowrook&password=24011996";
			connection = DriverManager.getConnection(dbAddress);
			stat = connection.createStatement();
			updateUsersTable();
		} catch (ClassNotFoundException ex) {
			return -2;
		} catch (SQLException ex) {
			return ex.getErrorCode();
		}
		return -1;
	}

	public void disconnectFromDB() {
		try {
			connection.close();
		} catch (SQLException ex) {
			Logger.getLogger(ChatServer.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	public void updateUsersTable() {
		try {
			resultSet = stat.executeQuery("SELECT Username, Password, DataIscrizione as \"Data Iscrizione\", OraIscrizione as \"Ora Iscrizione\" FROM Utenti;");
			columnTitle = new String[resultSet.getMetaData().getColumnCount()];
			for (int i = 1; i <= columnTitle.length; i++) {
				columnTitle[i - 1] = resultSet.getMetaData().getColumnLabel(i);
			}

			resultSet.last();
			table = new String[resultSet.getRow()][columnTitle.length];
			resultSet.beforeFirst();

			int j = 0;
			while (resultSet.next()) {
				for (int i = 1; i <= columnTitle.length; i++) {
					table[j][i - 1] = resultSet.getString(i);
				}
				j++;
			}
		} catch (SQLException ex) {
			JOptionPane.showMessageDialog(gui, "La query inserita è errata!", "Errore query!", JOptionPane.ERROR_MESSAGE);
		}
	}

	public void insertUser(String username, String password) {
		try {
			stat.executeUpdate("INSERT INTO Utenti VALUES (\"" + username + "\", \"" + SHA1Cipher(password) + "\", CURDATE( ), CURTIME( ));");
			updateUsersTable();
		} catch (SQLException ex) {
			JOptionPane.showMessageDialog(gui, "Non è stato possibile aggiungere il nuovo utente!", "Errore!", JOptionPane.ERROR_MESSAGE);
		}
	}

	public void deleteUser(int row) {
		try {
			stat.executeUpdate("DELETE FROM chat.Utenti WHERE Utenti.Username = \"" + table[row][0] + "\"");
			updateUsersTable();
		} catch (SQLException ex) {
			Logger.getLogger(ChatServer.class.getName()).log(Level.SEVERE, null, ex);
		}
	}
	
	public boolean login(String username, String password) {
		try {
			resultSet = stat.executeQuery("SELECT Username, Password FROM Utenti WHERE Username=\"" + username + "\"");
			resultSet.next();
			if(resultSet.getString(2).equals(password))
				return true;
		} catch (SQLException ex) {
			return false;
		}
		return false;
	}
	
	public String getAllMessagesByUser(String username) {
		try {
			resultSet = stat.executeQuery("SELECT * FROM Messaggi WHERE (Mittente = \"" + username + "\") OR (Destinatario = \"" + username + "\");");
			JSONArray messages = new JSONArray();
			
			while (resultSet.next()) {
				JSONObject singleMessage = new JSONObject();
				singleMessage.put("sender", resultSet.getString(2));
				singleMessage.put("addressee", resultSet.getString(3));
				singleMessage.put("text", resultSet.getString(4));
				singleMessage.put("time", resultSet.getString(5));
				singleMessage.put("date", resultSet.getString(6));
				messages.add(singleMessage);
			}
			
			return messages.toJSONString();
		} catch (SQLException ex) {
			JOptionPane.showMessageDialog(gui, "La query inserita è errata!", "Errore query!", JOptionPane.ERROR_MESSAGE);
		}
		return null;
	}

	public void insertMessage(String text, String sender, String addressee, String time, String date) {
		try {
			stat.executeUpdate("INSERT INTO Messaggi(Mittente, Destinatario, Contenuto, OraRicezione, DataRicezione) VALUES (\"" + sender + "\", \"" + addressee + "\", \"" + text + "\", \"" + time + "\", \"" + date + "\")");
		} catch (SQLException ex) {
			Logger.getLogger(ChatServer.class.getName()).log(Level.SEVERE, null, ex);
		}
	}
	
	public boolean searchUserByUsername(String username) {
		try {
			resultSet = stat.executeQuery("SELECT Username FROM Utenti WHERE Username = \"" + username + "\"");
			if(resultSet.next())
				return true;
		} catch (SQLException ex) {
			Logger.getLogger(ChatServer.class.getName()).log(Level.SEVERE, null, ex);
		}
		return false;
	}
	
	private String SHA1Cipher(String str) {
		try {
			// Si crea un oggetto MessageDigest e gli si assegna l'oggetto che implementa l'algoritmo SHA-1
			MessageDigest md = MessageDigest.getInstance("SHA-1");
			// Si aggiorna il contenuto dell'oggetto md con il contenuto della stringa su cui vogliamo
			// effettuare l'hash
			md.update(str.getBytes());
			// Si completa l'elaborazione hash, ottenendo un insieme di bytes
			byte[] digest = md.digest();
			// Si crea di nuovo la stringa per poter visualizzare il risultato
			StringBuilder sb = new StringBuilder();
			for (byte b : digest) {
					sb.append(String.format("%02x", b & 0xff));
			}
			return sb.toString();
		} catch (NoSuchAlgorithmException ex) {
			Logger.getLogger(ChatServer.class.getName()).log(Level.SEVERE, null, ex);
		}
		return null;
	}

	/**
	 * @param args the command line arguments
	 */
	public static void main(String[] args) {
		new ChatServer();
	}
}
