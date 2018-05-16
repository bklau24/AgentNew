package agent;

import java.util.List;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;

public class AgentThread extends Thread {
    
    private final int team;
    private final int number;
    private final List<String> names = new LinkedList<>();
    private String secretWord;
    private final String filename;
    private final Random rand = new Random();
    private final int t1;
    private final int t2;
    private int port;
    private int n;
    private int m;
    
    //boolean game;

    public AgentThread(int n, int nb, String fname, int min, int max) throws FileNotFoundException, InterruptedException{
        this.filename = fname;
        this.team = n;
        this.number = nb;
        this.t1 = min;
        this.t2 = max;
        readData();
    }
    
    private void readData() throws FileNotFoundException {
	Scanner scanner = new Scanner(new File(filename));
	while(scanner.hasNextLine()) {
            String line = scanner.nextLine();
            cutLines(line);
	}
	scanner.close();
    }
    
    public void cutLines(String line){ 
        String[] lineParts = line.split("\\s+");
	if(lineParts.length == 1){
            this.secretWord = lineParts[0];
            if(this.team == 1){
                AgentMain.oneSecrets.add(secretWord);
            }
            else{
                AgentMain.twoSecrets.add(secretWord);
            }
        }
        else {
            for (String name : lineParts) {
                names.add(name);
            }
        }
    }
    
    public void n(int n){
        this.n = n;
    }
    
    public void m(int m){
        this.m = m;
    }
    
    public int getTeam(){
        return this.team;
    }
    
    public List<String> getNames(){
        return this.names;
    }
    
    public String getSecret(){
        return this.secretWord;
    }

    public int getNumber(){
        return this.number;
    }
    
    public void setPort(int port){
        this.port = port;
    }
    
    public int getPort(){
        return port;
    }
    
    public int getTime(){
        return rand.nextInt((t2 - t1) +1) + t1;
    }
    
    public int bustedOne(int n){
        AgentMain.bustedOneTeam.add(n);
        return AgentMain.bustedOneTeam.size();
    }
    
    public int bustedTwo(int n){
        AgentMain.bustedTwoTeam.add(n);
        return AgentMain.bustedTwoTeam.size();
    }
    
    @Override
    public void run() {
      
        //game = true;
        //while(game){
            new Thread(this){
                @Override
                public void run() {

                    Random r = new Random();
                    List<String> secrets = new LinkedList<>();
                    List<String> toldSecrets = new LinkedList<>();

                    final int team = getTeam();
                    final int number = getNumber();
                    final List<String> names = getNames();
                    final String secretWord = getSecret();
                    secrets.add(secretWord);

                    //ha már minden általa ismert titkot elárult, a szerveroldali ügynök nem végez több tevékenységet
                    while(secrets.size() > 0){
                        int port = rand.nextInt((20100 - 20000) +1) + 20000;
                        int p = getPort();
                        while(port == p){
                            port = rand.nextInt((20100 - 20000) +1) + 20000;
                        }
                        setPort(port);
                        System.out.println("Creating server socket on port " + port);
                        try (
                            ServerSocket server = new ServerSocket(port);
                        )   {
                                int t = getTime();
                                server.setSoTimeout(t);
                                try(
                                    Socket client = server.accept();
                                    Scanner sc = new Scanner(client.getInputStream());
                                    PrintWriter pw = new PrintWriter(client.getOutputStream());   
                                )   {
                                        System.out.println("Connected on port " + port);

                                        System.out.println("[server on port " + port + "] My secrets: " + secrets);

                                        //a szerver elküldi a kliensnek az álnevei közül az egyiket
                                        int r1 = r.nextInt((3 - 1) + 1) + 1;
                                        String send = names.get(r1 - 1);
                                        pw.println(send);
                                        pw.flush();

                                        //megkapja a kliens tippjét, hogy a szerver melyik ügynökséghez tartozhat
                                        //ha helyes a tipp, elküldi a kliensnek az OK szöveget, ha nem, bontja a kapcsolatot
                                        int tip = sc.nextInt();
                                        sc.nextLine();
                                        System.out.println("[server on port " + port + "] client's tip: " + tip);
                                        if(tip == team){
                                                pw.println("OK - good tip");
                                                pw.flush();

                                            //ha azonos ügynökséghez tartoznak, akkor mindketten elküldenek egy-egy titkos szöveget, és felveszik
                                            //az ismert titkaik közé

                                            if(sc.hasNextLine()){
                                                String answer = sc.nextLine();
                                                System.out.println("[server on port " + port + "] " + answer);
                                                if(answer.equals("OK - same team")){
                                                    int n = r.nextInt((secrets.size() - 1) + 1) + 1;
                                                    String told = secrets.get(n-1); //véletlenszerűen kiválaszt egy titkot
                                                    pw.println(told);               //és elküldi a kliensnek
                                                    pw.flush();

                                                    int isTold = sc.nextInt();
                                                    sc.nextLine();
                                                    if(isTold != 1){
                                                        toldSecrets.add(told);          //ez a titok átkerül az elárult titkok közé
                                                        secrets.remove(told);           //kiveszem az el nem árultak közül
                                                        System.out.println("[server on port " + port + "] Oops i told a secret (" + told + "). My secrets now: " + secrets);
                                                    }
                                                    String otherSecret = sc.nextLine(); //a kliens is küld egy általa ismert titkot
                                                    if(secrets.contains(otherSecret) || toldSecrets.contains(otherSecret)){
                                                        pw.println(1); //ha már megvolt ez a titok, visszaküld egy 1-t a szervernek, így nem számít elárulásnak
                                                        pw.flush();
                                                        System.out.println("[server on port " + port + "] I already have this secret.");
                                                    }
                                                    else {
                                                        secrets.add(otherSecret);
                                                        pw.println(0); //ez már elárulásnak számít
                                                        pw.flush();
                                                         System.out.println("[server on port " + port + "] Thank you for the secret. My secrets now: " + secrets);
                                                    }
                                                }

                                                //ha másik ügynökség, akkor a kliens tippel, hogy a szervernek mi a sorszáma, ha téves, bontja a kapcsolatot
                                                else if(answer.equals("???")){
                                                    int clientTip = sc.nextInt();
                                                    sc.nextLine();
                                                    if(clientTip == number){
                                                        //ha helyes a sorszám, elküldi az általa ismert titkok egyikét, de csak olyat, amelyiket eddig még nem árult el
                                                        int n = r.nextInt((secrets.size() - 1) + 1) + 1;
                                                        String told = secrets.get(n-1); //véletlenszerűen kiválaszt egy titkot
                                                        pw.println(told);               //és elküldi a kliensnek
                                                        pw.flush();

                                                        //ha a titkot már ismerte az, aki megkapta, az nem számít a titok elárulásának
                                                        //ha még nem kapta meg, akkor kiveszi az ismert titkok közül és berakja az elárultak közé
                                                        int c = sc.nextInt();
                                                        sc.nextLine();
                                                        if(c != 1){
                                                            secrets.remove(told);
                                                            toldSecrets.add(told);
                                                            System.out.println("[server on port " + port + "] Oops i told a secret (" + told + "). My secrets now: " + secrets);
                                                        }
                                                    }
                                                    else {
                                                        client.close();
                                                        System.out.println("Connection broken on port " + port + " [Wrong number tip.]");
                                                    }
                                                }
                                            }
                                            System.out.println("End of communication on port " + port);
                                        }
                                        else {
                                             client.close();
                                             System.out.println("Connection broken on port " + port + " [Wrong team tip.]");
                                        }
                                        //ha az ügynök megszerezte a másik ügynökség összes titkát, akkor az ő ügynöksége győz
                                        if(team == 1){
                                            //game = false;
                                            for(String s : AgentMain.twoSecrets){
                                                if(!(secrets.contains(s)) && !(toldSecrets.contains(s))){
                                                    System.out.println("Not enough secret.");
                                                    //game = true;
                                                    //return;
                                                }
                                            }
                                        }
                                        else{
                                            //game = false;
                                            for(String s : AgentMain.oneSecrets){
                                                if(!(secrets.contains(s)) && !(toldSecrets.contains(s))){
                                                    System.out.println("Not enough secret.");
                                                    //game = true;
                                                    //return;
                                                }
                                            }
                                        }
                                    }
                            }catch (IOException ex) {}
                    }
                    if(team == 1){
                        int i = bustedOne(number);
                        if(i == n){
                            //ha valamelyik ügynököt letartóztatták, akkor hozzáadom egy listához, ha ez a lista megegyezik az egyik
                            //csapat ügynökeinek a számával, akkor letartóztatták mind, és a másik csapat nyert
                            System.out.println("Got it all. The winner is team Two.");
                            //game = false;
                        }
                        System.out.println("Busted agent in team One.");
                    }
                    else {
                        int i = bustedTwo(number);
                        if(i == m){
                            System.out.println("Got it all. The winner is team One.");
                            //game = false;
                        }
                        System.out.println("busted agent in team Two.");
                    }
                }
            }.start();

            new Thread(this) {
                @Override
                public void run() {

                    Random r = new Random();
                    Map<String, Integer> agents = new HashMap();
                    int otherTeam;
                    List<String> secrets = new LinkedList<>();
                    int otherNumber;
                    Map<String, Integer> wrongNumbers = new HashMap();
                    List<String> toldSecrets = new LinkedList<>();
                    Map<String, Integer> wrongAgents = new HashMap();

                    final int team = getTeam();
                    final String secretWord = getSecret();
                    secrets.add(secretWord);

                    while(secrets.size() > 0){
                        //olyan szerverhez kell csatlakoznia, ami nem saját maga
                        int port = rand.nextInt((20100 - 20000) +1) + 20000;
                        int p = getPort();
                        while(port == p){
                            port = rand.nextInt((20100 - 20000) +1) + 20000;
                        }
                        setPort(port);
                        System.out.println("Creating socket on port " + port);
                        try (
                            Socket client = new Socket("localhost",port);
                        )   {
                                int t = getTime();
                                client.setSoTimeout(t); 
                                try(
                                    Scanner sc = new Scanner(client.getInputStream());
                                    PrintWriter pw = new PrintWriter(client.getOutputStream());   
                                )   {
                                        System.out.println("[client on port " + port +  "] My secrets: " + secrets);

                                        //megkapja a szerver álnevét, majd megnézi a mapben, hogy már találkozott-e vele
                                        //ha igen, akkor tudja, hogy melyik ügynökséghez tartozik, ha nem, akkor tippel
                                        String serverName = sc.nextLine();
                                        boolean contains = agents.containsKey(serverName);
                                        boolean containsWrong = wrongAgents.containsKey(serverName);
                                        if(contains){
                                            System.out.println("[client on port " + port +  "] Hello agent " + serverName + "! We have already met.");
                                            otherTeam = agents.get(serverName);
                                            pw.println(otherTeam);
                                            pw.flush();
                                        }
                                        else if(containsWrong){
                                            System.out.println("[client on port " + port +  "] Hello agent " + serverName + "! We have already met.");
                                            int wrong = wrongAgents.get(serverName);
                                            if(wrong == 1){
                                                otherTeam = 2;
                                                pw.println(otherTeam);
                                                pw.flush();
                                            }
                                            else {
                                                otherTeam = 1;
                                                pw.println(otherTeam);
                                                pw.flush();
                                            }
                                        }
                                        else {
                                            System.out.println("[client on port " + port +  "] Hello agent " + serverName + "! We haven't met before.");
                                            otherTeam = r.nextInt((2 - 1) + 1) + 1;
                                            pw.println(otherTeam);
                                            pw.flush();
                                        }

                                        //ha nem tudta, és helyes a tipp, akkor lementi, hogy az adott név melyik ügynökséghez tartozik
                                        //ezután ha azonos ügynökséghez tartoznak, a kliens is elküldi az OK szöveget
                                        //majd mindketten elküldenek egy-egy titkos szót
                                        if(sc.hasNextLine()){
                                            String answer = sc.nextLine();
                                            System.out.println("[client on port " + port + "] " + answer);
                                            if(answer.equals("OK - good tip")){
                                                agents.put(serverName, otherTeam);
                                                if(team == otherTeam){
                                                    pw.println("OK - same team");
                                                    pw.flush();
                                                    //ha a titkot már ismerte, aki megkapja, az nem számít a titok elárulásának
                                                    String otherSecret = sc.nextLine();
                                                    System.out.println("[client on port " + port + "] Other's secret: " + otherSecret);
                                                    if(secrets.contains(otherSecret) || toldSecrets.contains(otherSecret)){
                                                        pw.println(1); //ha már megvolt ez a titok, visszaküld egy 1-t a szervernek, így nem számít elárulásnak
                                                        pw.flush();
                                                        System.out.println("[client on port " + port + "] I already have this secret.");
                                                    }
                                                    else {
                                                        secrets.add(otherSecret);
                                                        pw.println(0); //ez már elárulásnak számít
                                                        pw.flush();
                                                        System.out.println("[client on port " + port + "] Thank you for the secret. My secrets now: " + secrets);
                                                    }
                                                    //miután megkapta a titkot, ős is elárul egyet
                                                    int n = r.nextInt((secrets.size() - 1) + 1) + 1;
                                                    String told = secrets.get(n-1); //véletlenszerűen kiválaszt egy titkot
                                                    pw.println(told);               //és elküldi a szervernek
                                                    pw.flush();

                                                    int isTold = sc.nextInt();
                                                    sc.nextLine();
                                                    if(isTold != 1){
                                                        toldSecrets.add(told);          //ez a titok átkerül az elárult titkok közé
                                                        secrets.remove(told);            //kiveszem az el nem árultak közül
                                                        System.out.println("[client on port " + port + "] Oops i told a secret (" + told + "). My secrets now: " + secrets);
                                                    }
                                                }
                                                //ha másik ügynökséghez tartozik, elküldi hogy ???, majd tippel, hogy mi lehet a másik ügynök sorszáma
                                                else {
                                                    pw.println("???");
                                                    pw.flush();
                                                    boolean c = wrongNumbers.containsKey(serverName);
                                                    //ha már találkozott vele, akkor olyan tippet nem ad, ami biztos rossz
                                                    if(c){ 
                                                        otherNumber = r.nextInt((5 - 1) + 1) + 1;
                                                        while (otherNumber == wrongNumbers.get(serverName)) {
                                                            otherNumber = r.nextInt((5 - 1) + 1) +1;
                                                        }
                                                        pw.println(otherNumber);
                                                        pw.flush();
                                                        if(sc.hasNextLine()){
                                                            String otherSecret = sc.nextLine();
                                                            System.out.println("[client on port " + port + "] Other's secret: " + otherSecret);
                                                            //ha már ismerte a kapott titkot, visszajelez, mert ez a szervernél nem számít a 
                                                            //titok elárulásának (1-megkapta már, 0-még nem, tehát a 0 számít elárulásnak)
                                                            if(secrets.contains(otherSecret) || toldSecrets.contains(otherSecret)){
                                                                pw.println(1);
                                                                pw.flush();
                                                                System.out.println("[client on port " + port + "] I already have this secret.");
                                                            }
                                                            else {
                                                                secrets.add(otherSecret); //mivel még nem ismerte, felveszi
                                                                pw.println(0);
                                                                pw.flush();
                                                                System.out.println("[client on port " + port + "] Thank you for the secret. My secrets now: " + secrets);
                                                            }
                                                        }
                                                        //ha nincs több válasz, akkor a szerver bontotta a kapcsolatot, és rossz volt a tip
                                                        //ezt elmentem, hogy legközelebb ezt a tippet már ne adja ha találkoznak
                                                        else {
                                                            wrongNumbers.put(serverName, otherNumber);
                                                        }
                                                    }
                                                    //ha még nem találkozott vele
                                                    else {
                                                        System.out.println("[client on port " + port + "] We haven't met before.");
                                                        otherNumber = r.nextInt((5 - 1) + 1) + 1;
                                                        pw.println(otherNumber);
                                                        pw.flush();
                                                        if(sc.hasNextLine()){
                                                            String otherSecret = sc.nextLine();
                                                            System.out.println("[client on port " + port + "] Other's secret: " + otherSecret);
                                                            if(secrets.contains(otherSecret) || toldSecrets.contains(otherSecret)){
                                                                pw.println(1);
                                                                pw.flush();
                                                                System.out.println("[client on port " + port + "] I already have this secret.");
                                                            }
                                                            else {
                                                                secrets.add(otherSecret); //mivel még nem ismerte, felveszi
                                                                pw.println(0);
                                                                pw.flush();
                                                                System.out.println("[client on port " + port + "] Thank you for the secret. My secrets now: " + secrets);
                                                            }
                                                        }
                                                        else {
                                                            wrongNumbers.put(serverName, otherNumber);
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        else {
                                            wrongAgents.put(serverName, otherTeam);
                                            client.close();
                                        }
                                    }      
                            } catch (IOException ex) {} 
                    }
                }
            }.start();
        //}
    } 
}