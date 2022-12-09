import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.ReceiverAdapter;
import org.jgroups.View;

import util.Mensagem;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedList;

public class Peer extends ReceiverAdapter {

    JChannel channel;
    String user_name=System.getProperty("user.name", "n/a");
    final List<String> state=new LinkedList<>();
    View viewAtual;

    private int inicio;

    private int finalP;

    private boolean coordenador;

    private final List<Integer> listaPrimos;


    public Peer() {
        this.coordenador = false;
        this.listaPrimos = new ArrayList<>();
    }

    public void viewAccepted(View new_view)
    {
        System.out.println("** view: " + new_view.toString());
        viewAtual = new_view;
    }

    public void receive(Message msgEntrada)
    {
        Mensagem mEntrada = (Mensagem) msgEntrada.getObject();
        if(mEntrada.getOperacao().equals("iniciar"))
        {
            coordenador = true;
        }
        if(coordenador)
        {
            if(mEntrada.getOperacao().equals("Solicita"))
            {
                Mensagem m = new Mensagem("Calcular");
                m.setInicial(this.getInicio());
                m.setFinalP(this.getFinalP());
                this.setInicio(this.getFinalP()+1);
                this.setFinalP(this.getFinalP()+100000);
                Message msgSend = new Message(msgEntrada.getSrc(), m);
                try {
                    this.channel.send(msgSend);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
            else if(mEntrada.getOperacao().equals(("SalvarPrimo")))
            {
                this.listaPrimos.add(mEntrada.getPrimo());
            }
        }

        if(mEntrada.getOperacao().equals("Calcular"))
        {
            try {
                calculaPrimos(mEntrada.getInicial(), mEntrada.getFinalP());
                Mensagem mensagem = new Mensagem("Solicita");
                Message msg2 = new Message(viewAtual.getCoord(), mensagem);

                channel.send(msg2);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
    private void start() throws Exception
    {
        channel=new JChannel().setReceiver(this);
        channel.connect("testes");
        channel.getState(null, 10000);
        this.setInicio(1);
        this.setFinalP(100000);
        channel.send(new Message(viewAtual.getCoord(), new Mensagem("iniciar")));
        System.out.println(viewAtual.getMembers());
        eventLoop();
        channel.close();
    }

    private void calculaPrimos(int inicial, int finalP)
    {
        System.out.println(finalP);
        try {
            for(int i = inicial; i< finalP;i++)
            {
                if(ehPrimo(i))
                {
                    Mensagem m = new Mensagem("SalvarPrimo");
                    m.setPrimo(i);
                    Message msg = new Message(viewAtual.getCoord(), m);

                    this.channel.send(msg);

                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void eventLoop()
    {
        BufferedReader in=new BufferedReader(new InputStreamReader(System.in));
        while(true)
        {
            try {
                System.out.println("Começa!");
                in.readLine();
                if(this.coordenador)
                {

                    for(int i = 0; i < listaPrimos.size(); i++)
                    {
                        System.out.println(listaPrimos.get(i));
                    }

                }
                else
                {
                    Mensagem m = new Mensagem("Solicita");
                    Message msg = new Message(viewAtual.getCoord(), m);
                    channel.send(msg);
                }
            }
            catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

    private boolean ehPrimo(int n)
    {
        if(n%2==0) return false;
        int c = 0;
        if(n < 2) return false;
        for(int i = 1; i < n / 2; i++)
            if(n % i == 0)
                if(++c > 1)
                    return false;

        return true;
    }

    public int getInicio() {
        return inicio;
    }

    public void setInicio(int inicio) {
        this.inicio = inicio;
    }

    public int getFinalP() {
        return finalP;
    }

    public void setFinalP(int finalP) {
        this.finalP = finalP;
    }

    public static void main(String[] args) throws Exception {
        new Peer().start();
    }
}