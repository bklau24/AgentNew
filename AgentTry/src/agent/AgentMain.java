package agent;

import java.io.FileNotFoundException;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

public class AgentMain  {
    
    protected static List<Integer> bustedOneTeam = new LinkedList<>();
    protected static List<Integer> bustedTwoTeam = new LinkedList<>();
    protected static List<String> oneSecrets = new LinkedList<>();
    protected static List<String> twoSecrets = new LinkedList<>();

    public static void main(String[] args) throws FileNotFoundException, InterruptedException {
        List<AgentThread> agents = new LinkedList<>();
        String filename;
        final int t1 = 1000;
        final int t2 = 3000;
        
	System.out.println("Az elso ugynokseg ugynokeinek szama: ");
	Scanner sc = new Scanner(System.in);
        int n = sc.nextInt();
            while ( n > 5){
		System.out.println("Az ugynokok szama max 5 lehet. Adjon meg új számot!");
		sc = new Scanner(System.in);
                n = sc.nextInt();
            }
		
	System.out.println("A masodik ugynokseg ugynokeinek szama: ");
	int m = sc.nextInt();
            while ( m > 5){
		System.out.println("Az ugynokok szama max 5 lehet. Adjon meg új számot!");
		sc = new Scanner(System.in);
                m = sc.nextInt();
            }
                
        for(int i = 1; i <= n; i++){        //az első ügynökség ügynökeinek létrehozása (szálak)
            filename = "agent1-" + i + ".txt";
            AgentThread agent = new AgentThread(1, i, filename, t1, t2);
            agent.start();
            agents.add(agent);
            agent.n(n);
        }
        
        for(int i = 1; i <= m; i++){        //a második ügynökség ügynökeinek létrehozása (szálak)
            filename = "agent2-" + i + ".txt";
            AgentThread agent = new AgentThread(2, i, filename, t1, t2);
            agent.start();
            agents.add(agent);
            agent.m(m);
        }
        
        for(AgentThread agent : agents){
            agent.join();
        }
    }
    
}
