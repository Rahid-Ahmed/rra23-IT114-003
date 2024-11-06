package Project;

import java.util.Random;
//rra23 11/6/24
public class RollPaylod extends Payload {
    private String clientName;
    private int diceSize;

    public RollPayload(){
        this.setPayloadType(PayloadType.ROLL);
    }

    public String getClientName(){
        return this.clientName;
    }

    public int Roll(){
        Random rand = new Random();
        int x = rand.nextInt(diceSize) + 1;
    }

    public String toString(){
        String var10001 = super.toString();
        return var10001 + 
    }

    
}
