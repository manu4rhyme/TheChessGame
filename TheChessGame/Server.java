/*  DEVELOPED BY    :                AVINASH AGARWAL  
 *  DATE                        :                22nd February, 2014
 *  PLATFORM             :                JAVA 1.6.0_24-b07
 *   PROGRAM             :                The Chess Game
 */

import java.net.Socket;
import java.net.ServerSocket;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.InputStreamReader;

import java.awt.Color;

import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.JTextField;

class Server implements Runnable
{

    JButton b;
    Thread t;
    MainWin mw;

    Server(int port, JButton b, JTextField pf) 
    {
        this.b = b;

        MultiCastServer mcs = new MultiCastServer();
        Thread a = new Thread(mcs);

        try
        {
            SetUp.serverSocket = new ServerSocket(port);
        }
        catch(IOException e)
        {
            SetUp.status.setText("Could not listen on port : "+port);
            pf.setEditable(true);
            b.setEnabled(true);
            return;
        }

        SetUp.status.setText("Awaiting First Connection...");

        b.setEnabled(false);

        t = new Thread(new Server());
        t.start();                
        SetUp.isServerRunning = true;
        a.start();
    }

    Server()
    {

    }

    public void run()
    {
        try
        {
            SetUp.clientSocket[SetUp.clientSocketNum] = SetUp.serverSocket.accept();
        }
        catch(IOException e)
        {
            SetUp.status.setText("Accept Failed : "+ SetUp.HostPort);
            b.setEnabled(true);
            return;
        }

        if(SetUp.clientSocketNum == 0)
        {
            mw = new MainWin();
            Thread z = new Thread(mw);
            z.start();
            mw.ctpc = SetUp.colors[0];
        }

        clientHandler ch = new clientHandler(SetUp.clientSocketNum);
        new Thread(ch).start();

        SetUp.clientSocketNum++;
        run();
    }

    class clientHandler implements Runnable
    {
        Socket sock;
        int clientNum;
        String clientName;
        Color clientColor;
        
        clientHandler(int sockNum)
        {
            this.sock = SetUp.clientSocket[sockNum];
            clientNum = sockNum;
            clientColor = SetUp.colors[(clientNum+1)%8];
            try
            {
                SetUp.br[sockNum] = new BufferedReader(new InputStreamReader(sock.getInputStream()));
                SetUp.pWs[sockNum] = new PrintWriter(sock.getOutputStream(), true);
            }
            catch(IOException e)
            {
                if(clientNum == 0) SetUp.status.setText("Read/Write Error");
                return;
            }

            String x = "";
            for(int i = 0; i <= clientNum; i++) 
            {
                if(SetUp.names[i] != "") x += SetUp.names[i] + ";";
            }
            SetUp.pWs[sockNum].println(x);
            SetUp.pWs[sockNum].println(clientNum);

            if(clientNum == 0) SetUp.pWs[sockNum].println(SetUp.myColor.equals("WHITE") ? "BLACK" : "WHITE");
            else SetUp.pWs[sockNum].println("SPECTATOR");
        }

        public void run()
        {

            String incoming = "";
            int count = 0;
            boolean brk = false;

            while(true)
            {
                try
                {
                    incoming = SetUp.br[clientNum].readLine();
                }
                catch(IOException e) {}

                if(incoming == null) continue;

                switch(count)    
                {   

                    case 0 :           clientName = incoming;  
                    SetUp.names[clientNum+1] = clientName;
                    SetUp.insertColorStrings(clientName+ "\n", clientColor, true, mw.tp2);
                    SetUp.insertColorStrings("\n<<" + clientName + " joined the room>>", clientColor, true, mw.tp);
                    mw.tp.setCaretPosition(mw.tp.getDocument().getLength());
                    sendToAll(" "+clientName, clientNum);
                    break;

                    default :    
                    if(incoming.equals(" "))
                    {
                        SetUp.insertColorStrings("\n<<" + clientName + " left the room>>", clientColor, true, mw.tp);
                        mw.tp.setCaretPosition(mw.tp.getDocument().getLength());
                        brk = true;
                        doClientLeftChange(clientName);
                        break;
                    }

                    if(incoming.equals("             YOU WIN"))
                    {
                        SetUp.insertColorStrings("\n" + incoming, Color.GREEN, true, mw.tp);
                        mw.tp.setCaretPosition(mw.tp.getDocument().getLength());
                    }
                    else if((incoming.substring(0,2)).equals("  "))
                    {
                        int m = Integer.parseInt(""+incoming.charAt(15));
                        int n1 = Integer.parseInt(""+incoming.charAt(17));
                        int ix = Integer.parseInt(""+incoming.charAt(19));
                        int iy = Integer.parseInt(""+incoming.charAt(21));
                        doMoveChanges(m,n1,ix,iy);
                        break;
                    }

                    String s = incoming.substring(0, incoming.length()-1);
                    int n = (int)(incoming.charAt(incoming.length() - 1));
                    sendToAll(s + ((n-47)), n-48);
                    SetUp.insertColorStrings("\n" + clientName + " : ", clientColor, true, mw.tp);
                    SetUp.insertColorStrings(s, clientColor, false , mw.tp);
                    mw.tp.setCaretPosition(mw.tp.getDocument().getLength());

                }     
                count++;

                if(brk) break;
            }

        }

        protected void doClientLeftChange(String s)
        {
            int x = 0;
            String y = "";
            for(int i = 1; i <= SetUp.clientSocketNum; i++)
            {
                if(SetUp.names[i].equals(s))
                {
                    SetUp.names[i] = "";
                    x = i - 1;
                }
                else if(!SetUp.names[i].equals("")) y += SetUp.names[i]+";";
            }

            sendToAll((x+1) + " ", x);
            
            mw.tp2.setText("");
            SetUp.insertColorStrings(SetUp.name+"\n", SetUp.colors[0], true, mw.tp2);

            for (int i = 1; i <= SetUp.clientSocketNum; i++)
            {
                if(SetUp.names[i] != "") SetUp.insertColorStrings(SetUp.names[i]+"\n", SetUp.colors[i%8], true, mw.tp2);
            }
        }
    }

    protected static void sendToAll(String s, int num)
    {
        for(int i = 0; i < SetUp.clientSocketNum; i++)
        {
            if(i != num) SetUp.pWs[i].println(s);
        }
    }

    private void doMoveChanges(int m, int n, int x, int y)
    {
        mw.selectedPiece = SetUp.situation[x][y].substring(1,SetUp.situation[x][y].length()-1);
        char c = SetUp.situation[m][n].charAt(SetUp.situation[m][n].length()-1);
        SetUp.situation[m][n] = SetUp.situation[x][y].charAt(0) + mw.selectedPiece + c;
        SetUp.situation[x][y] = "" + (SetUp.situation[x][y].charAt(SetUp.situation[x][y].length()-1));
        mw.paintEnemyMoved(c,m,n,x,y);

        for(int i = 0; i < 8; i++)
        {
            for(int j = 0; j < 8; j++)
                mw.situ[i][j].setActionCommand(i + " " + j + " f");
        }

        if(mw.isAttacked(SetUp.situation))
        {
            SetUp.insertColorStrings("\nYou are under Check", Color.RED, true, mw.tp);
            mw.tp.setCaretPosition(mw.tp.getDocument().getLength());

            String temp[][] = new String[8][8];
            for(int i = 0; i < 8; i++)
                System.arraycopy(SetUp.situation[i], 0, temp[i], 0, 8);

            if(mw.isCheckMate(temp))
            {
                SetUp.insertColorStrings("\nCHECKMATE! YOU LOSE!! ", Color.RED, true, mw.tp);
                mw.tp.setCaretPosition(mw.tp.getDocument().getLength());   

                for(int i = 0; i < SetUp.clientSocketNum; i++)
                    SetUp.pWs[i].println("             YOU WIN");
            }

        }
    }

}