package com.polytechnique.marc.amaury.set;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;


final class ConnectionList {
	PrintWriter out;
	String login;
	ConnectionList tail;
	ConnectionList(String l, PrintWriter h, ConnectionList tl){
		login = l;
		out = h;
		tail = tl;
	}
}

public class ChatServer {

	static void sleepSeconds(int i) {
		try {
			Thread.sleep(i*1000);
		} catch (InterruptedException e) {
		}
	}

	static public ServerSocket createServer(int server_port){ 	
		try {
			return new ServerSocket(server_port);
		} catch (IOException e) {
			throw new RuntimeException("Impossible d'attendre sur le port "+ server_port);
		}
	}

	static Socket acceptConnection(ServerSocket s) {
		try {
			return s.accept();
		} catch (IOException e) {
			throw new RuntimeException("Impossible de recevoir une connection");
		}
	}

	static Socket establishConnection(String ip, int port) {
		try {
			return new Socket(ip,port);
		} 
		catch (UnknownHostException e){
			throw new RuntimeException("Impossible de resoudre l'adresse");
		}
		catch (IOException e){
			throw new RuntimeException("Impossible de se connecter a l'adresse");	
		}
	}

	static PrintWriter connectionOut(Socket s){
		try {
			return new PrintWriter(s.getOutputStream(),	true);
		} catch (IOException e) {
			throw new RuntimeException("Impossible d'extraire le flux sortant");
		}
	}

	static BufferedReader connectionIn(Socket s){
		try {
			return new BufferedReader(new InputStreamReader(s.getInputStream()));
		} catch (IOException e) {
			throw new RuntimeException("Impossible d'extraire le flux entrant");
		}
	}

	static ConnectionList outs = null;
	static boolean killed = false;

	static void print_all(String message){
		ConnectionList cl = outs;
		
		while(cl != null){
			cl.out.println(message);	
			cl = cl.tail;
		}		
	}
	
	public static void main(String args[]){
		ServerSocket server = createServer(7777);
		
		while(!killed){
			final Socket s = acceptConnection(server);

			final PrintWriter s_out = connectionOut(s);
			final BufferedReader s_in = connectionIn(s);


			Thread t = new Thread(){
				public void run(){
					String my_login = null;
					try {
						while(true){

							String line;

							try { line = s_in.readLine(); }
							catch (IOException e){
								throw new RuntimeException("Cannot read from socket");
							}

							Scanner sc = new Scanner(line);
							sc.useDelimiter(" ");

							String token = sc.next();
							if(my_login != null){
								if(token.equals("SEND")){
									sc.useDelimiter("\n");
									String message = sc.next();
									print_all(my_login + ":" + message);
								} else
									if(token.equals("LOGOUT")){
										s_out.println("Bye Bye " + my_login );
										throw new RuntimeException("Requested by user");
									} else
										throw new RuntimeException("Unknown Command");
							} else 
								if(token.equals("LOGIN")){
									my_login = sc.next();
									ConnectionList cl = outs;
									while(cl != null){
										if(cl.login.equals(my_login)){
											my_login = null;
											throw new RuntimeException("Login already used");
										}
										cl = cl.tail;
									}
									outs = new ConnectionList(my_login,s_out,outs);
									print_all("Welcome "+my_login);
								} else 
								if(token.equals("KILL")){
								killed = true;	
								throw new RuntimeException("Waiting for next connection to kill");
								} else {
									throw new RuntimeException("Expecting LOGIN command");
								}
						}	
					} catch(RuntimeException error){
						s_out.println("DISCONNECTED: exn "+error);
						s_out.flush();
						try { s.close(); } catch(IOException e){}
						if(my_login != null){
						ConnectionList cl = null;
						while(outs != null){
							if(outs.out == s_out){
							} else {
								cl = new ConnectionList(outs.login, outs.out, cl);
							}
							outs = outs.tail;
						}
						outs = cl;
						print_all(my_login + " left.");
						}
					}
				}
			};
			t.start();
		};
		System.exit(0);
	}

}

