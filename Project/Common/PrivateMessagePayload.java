package Project.Common;

public class PrivateMessagePayload extends Payload{
    private String RecipientId;
    

public void setRecipientName(String recipientId) { //rra23 11/25/24
    this.RecipientId = recipientId;
}

public String getRecipientName(String recipientId) { //rra23 11/25/24
    return this.RecipientId;
}

}