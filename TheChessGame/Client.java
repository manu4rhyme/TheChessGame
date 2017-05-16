/*  DEVELOPED BY    :                AVINASH AGARWAL  
 *  DATE                        :                23rd February, 2014
 *  PLATFORM             :                JAVA 1.6.0_24-b07
 *   PROGRAM             :                The Chess Game
 */

import java.net.Socket;
import java.net.UnknownHostException;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.BufferedReader;
import java.io.InputStreamReader;

import java.awt.Color;

import javax.swing.JButton;
import javax.swing.JTextField;

class Client implements Runnable
{
        private String ip;
        private JButton b;
        protected static BufferedReader br;
        protected static PrintWriter pw;
        private JTextField port, ipF;
        private MainWin mw;
        protected static int myClientNum;
        
        Client(JButton b, JTextField tF, JTextField tF1)
        {
                port = tF;
                ipF = tF1;
                ip = tF1.getText();
                System.out.println(ip);
                this.b = b;                              
        }
               
        public void run()
        {
                System.out.println(SetUp.HostPort);
                try
                {
                        SetUp.socket = new Socket(ip, SetUp.HostPort);
                }
                catch(UnknownHostException xx)
                {
                        SetUp.status.setText("IP Unreachable");
                        b.setEnabled(true);
                        ipF.setEditable(true);
                        return;
                }
                catch(IOException e)
                {
                        SetUp.status.setText("Wrong IP/Port");
                        b.setEnabled(true);
                        ipF.setEditable(true);
                        port.setEditable(true);
                        return;
                }
                SetUp.status.setText("Setting up IO... ");
                
                try
                {
                        br = new BufferedReader(new InputStreamReader(SetUp.socket.getInputStream()));
                        pw = new PrintWriter(SetUp.socket.getOutputStream(), true);
                }
                catch(IOException e)
                {
                        SetUp.status.setText("Read/Write Error");
                }
                SetUp.status.setText("Connected");
                SetUp.isClientListening = false;
                
                pw.println(SetUp.name);
                
                Thread z = new Thread(new Listen());
                z.start();               
        }
        
        protected class Listen implements Runnable
        {
                protected String[] nameList;
                private int k;
                public void run()
                {
                         String incoming = ""; 
                         int count = 0;
                         k = 0;
                         String names = "";
                         nameList = new String[100];
                         
                         while(true)
                         {
                                incoming = null;
                                try
                                {
                                        incoming = br.readLine();
                                }
                                catch(IOException e){}
                                
                                if(incoming == null) continue;                                
                                switch(count)
                                {
                                        case 0 : names = incoming;
                                                 break;
                                                       
                                        case 1 : myClientNum = Integer.parseInt(incoming);
                                                       break;
                                                       
                                        case 2 : System.out.println(count);
                                                 if(incoming.equals("SPECTATOR"))
                                                 {
                                                     SetUp.isSpec = true;
                                                     SetUp.myColor = "WHITE";
                                                 }
                                                 else SetUp.myColor = incoming;
                                                 
                                                  mw = new MainWin();
                                                  Thread mwit = new Thread(mw);
                                                  mwit.start();
                                                  
                                                  String z = "";
                                                  for (int y = 0; y < names.length(); y++)
                                                  {                                                                
                                                        char c = names.charAt(y);
                                                        if(c != ';')    z += c;                                                       
                                                        else 
                                                        {
                                                              nameList[k] = z;
                                                              SetUp.insertColorStrings(z + "\n", SetUp.colors[k%8], true, mw.tp2);
                                                              z = "";
                                                              k++;
                                                        }
                                                  }
                                                  nameList[k] = SetUp.name;
                                                  SetUp.insertColorStrings(SetUp.name+"\n", SetUp.colors[k%8], true, mw.tp2);
                                                  mw.ctpc = SetUp.colors[k%8];
                                                  break;
                                                       
                                        default :      if(incoming.equals(" "))
                                                       {
                                                             mw.tp.setText(mw.tp.getText() + "\n<<Host Closed The Room>>");
                                                             mw.tp.setCaretPosition(mw.tp.getDocument().getLength());
                                                             mw.tp.setEnabled(false);
                                                             mw.tp2.setEnabled(false);
                                                             mw.inp.setEnabled(false);
                                                                                                                  
                                                             SetUp.hostClosed = true;
                                                       }
                                                       else if(incoming.equals("             YOU WIN"))
                                                       {
                                                             SetUp.insertColorStrings("\n" + incoming, Color.GREEN, true, mw.tp);
                                                             mw.tp.setCaretPosition(mw.tp.getDocument().getLength());
                                                       }
                                                       else if((incoming.substring(0,2)).equals("  "))
                                                       {
                                                           System.out.println(count);
                                                           System.out.println(incoming);
                                                           int m = Integer.parseInt(""+incoming.charAt(15));
                                                           int n = Integer.parseInt(""+incoming.charAt(17));
                                                           int ix = Integer.parseInt(""+incoming.charAt(19));
                                                           int iy = Integer.parseInt(""+incoming.charAt(21));
                                                           doMoveChanges(m,n,ix,iy);
                                                       }
                                                       
                                                       else if(incoming.charAt(0) == ' ') 
                                                       {
                                                           nameList[++k] = incoming.trim();
                                                           SetUp.insertColorStrings(nameList[k]+"\n", SetUp.colors[k%8], true, mw.tp2);
                                                           SetUp.insertColorStrings("\n<<" + nameList[k] + " joined the room>>", SetUp.colors[k%8], true, mw.tp);
                                                           mw.tp.setCaretPosition(mw.tp.getDocument().getLength());
                                                       }
                                                       else if(incoming.charAt(incoming.length()-1) == ' ') 
                                                                    doClientLeftChange(incoming.trim());
                                                       else
                                                       {
                                                           int l = incoming.length();
                                                           SetUp.insertColorStrings("\n" + nameList[Integer.parseInt(incoming.substring(l-1, l))] + " : ", SetUp.colors[Integer.parseInt(incoming.substring(l-1, l))], true, mw.tp);
                                                           SetUp.insertColorStrings(incoming.substring(0,l-1),SetUp.colors[Integer.parseInt(incoming.substring(l-1, l))], false, mw.tp);
                                                           mw.tp.setCaretPosition(mw.tp.getDocument().getLength());
                                                       }                                             
                                                       
                                }
                                count++;
                         }
                }
                
               protected void doClientLeftChange(String s)
                {
                       mw.tp2.setText("");      
                       String z = "";
                       int flag = 0;
                       int x = Integer.parseInt(s);
                       /*for (int y = 0; y < s.length(); y++)
                                                       {                                                                
                                                                char c = s.charAt(y);
                                                                if(c != ';')    z += c;                                                       
                                                                else 
                                                                {
                                                                        if(flag == 0)
                                                                        {   
                                                                            mw.tp.setText(mw.tp.getText() + "\n<<" + z + " left the room>>");
                                                                            mw.tp.setCaretPosition(mw.tp.getDocument().getLength());
                                                                        }
                                                                        else mw.tp2.setText(mw.tp2.getText() + z + "\n");
                                                                        
                                                                        z = "";
                                                                        flag++;
                                                                }
                       }*/
                       
                       for(int y = 0; y <= k; y++)
                            if(y != x) SetUp.insertColorStrings(nameList[y] + "\n", SetUp.colors[y%8], true, mw.tp2);
                            
                       SetUp.insertColorStrings("\n<<" + nameList[x] + " left the room>>", SetUp.colors[x%8], true, mw.tp); 
                            
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
                    
                    if(mw.isCheckMate(SetUp.situation))
                    {
                        SetUp.insertColorStrings("\nCHECKMATE! YOU LOSE!! ", Color.RED, true, mw.tp);
                        mw.tp.setCaretPosition(mw.tp.getDocument().getLength());   
                        pw.println("             YOU WIN");
                    }
                }
                
                
                SetUp.moveNum++;
        }
        
}