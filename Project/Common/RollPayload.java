package Project.Common;

import java.util.Random;
//rra23 11/10/24
public class RollPayload extends Payload {
    private int result;

    public void RollPayload(){
        this.setPayloadType(PayloadType.ROLL);
    }


    public int getResult(){
        return result;
    }

    
}
