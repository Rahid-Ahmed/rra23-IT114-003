package Project.Server;

import java.io.IOException;
import java.net.Socket;
import java.util.Objects;
import java.util.function.Consumer;
import java.io.File;
import java.io.FileWriter;
import java.util.Scanner;

import Project.Server.BaseServerThread;
import Project.Server.Room;
import Project.Common.Payload;
import Project.Common.ConnectionPayload;
import Project.Common.PayloadType;
import Project.Common.PrivateMessagePayload;

/**
 * A server-side representation of a single client.
 * This class is more about the data and abstracted communication
 */
public class ServerThread extends BaseServerThread {
    public static final long DEFAULT_CLIENT_ID = -1;
    private Room currentRoom;
    private long clientId;
    private String clientName;
    private Consumer<ServerThread> onInitializationComplete; // callback to inform when this object is ready

    /**
     * Wraps the Socket connection and takes a Server reference and a callback
     * 
     * @param myClient
     * @param server
     * @param onInitializationComplete method to inform listener that this object is
     *                                 ready
     */
    protected ServerThread(Socket myClient, Consumer<ServerThread> onInitializationComplete) {
        Objects.requireNonNull(myClient, "Client socket cannot be null");
        Objects.requireNonNull(onInitializationComplete, "callback cannot be null");
        info("ServerThread created");
        // get communication channels to single client
        this.client = myClient;
        this.clientId = ServerThread.DEFAULT_CLIENT_ID;// this is updated later by the server
        this.onInitializationComplete = onInitializationComplete;

    }

    public void setClientName(String name) {
        if (name == null) {
            throw new NullPointerException("Client name can't be null");
        }
        this.clientName = name;
        onInitialized();
    }
    public String getClientName(){
        return clientName;
    }

    public long getClientId() {
        return this.clientId;
    }

    protected Room getCurrentRoom() {
        return this.currentRoom;
    }

    protected void setCurrentRoom(Room room) {
        if (room == null) {
            throw new NullPointerException("Room argument can't be null");
        }
        currentRoom = room;
    }

    @Override
    protected void onInitialized() {
        onInitializationComplete.accept(this); // Notify server that initialization is complete
    }

    @Override
    protected void info(String message) {
        System.out.println(String.format("ServerThread[%s(%s)]: %s", getClientName(), getClientId(), message));
    }

    @Override
    protected void cleanup() {
        currentRoom = null;
        super.cleanup();
    }
    
    @Override
    protected void disconnect(){
        //sendDisconnect(clientId, clientName);
        super.disconnect();
    }
    // handle received message from the Client
    @Override
    protected void processPayload(Payload payload) { //rra23 10-17-24
        try {
            switch (payload.getPayloadType()) {
                case CLIENT_CONNECT:
                    ConnectionPayload cp = (ConnectionPayload) payload;
                    setClientName(cp.getClientName());
                    loadMuteList(); //rra23 12/10/24
                    break;
                case MESSAGE:
                    currentRoom.sendMessage(this, payload.getMessage());
                    if (payload.getMessage().contains("/flip")){
                        currentRoom.flip(this);
                    } 
                    if (payload.getMessage().contains("/roll")){
                        currentRoom.roll(payload.getMessage().replace("/roll", ""), this);
                    } 
                    if (payload.getPayloadType().equals(PayloadType.PRIVATE_MESSAGE)){ //rra23 11/25/24
                        currentRoom.privateMessage(this, payload.getMessage());
                    }
                    if (payload.getPayloadType().equals(PayloadType.MUTE)){ //rra23 12/10/24
                        saveMuteList();
                    }
                    if (payload.getMessage().contains("/mute")){
                        int muteCounter = 0;
                        if (muteCounter % 2 == 0){
                            sendMessage("a muted you");
                            muteCounter++;
                        }
                    }
                    if (payload.getMessage().contains("/unmute")){
                        int unmuteCounter = 0;
                        if (unmuteCounter % 2 == 0) {
                            sendMessage("a unmuted you");
                            unmuteCounter++;
                        }
                    }
                    if(payload.getMessage().contains("/createroom")){
                        currentRoom.handleCreateRoom(this, payload.getMessage().replace("/createroom", ""));
                    }
                    if(payload.getMessage().contains("/joinroom")){
                        currentRoom.handleJoinRoom(this, payload.getMessage().replace("/joinroom", ""));
                    }
                    break;
                case ROOM_CREATE:
                    currentRoom.handleCreateRoom(this, payload.getMessage());
                    break;
                case ROOM_JOIN:
                    currentRoom.handleJoinRoom(this, payload.getMessage());
                    break;
                case DISCONNECT:
                    currentRoom.disconnect(this);
                    break;
                case ROLL:
                    currentRoom.roll(payload.getMessage(), this); //rra23 11/10/24
                    break;
                case FLIP:
                    currentRoom.flip(this); //rra23 11/11/24
                    break;
                case MUTE:
                    currentRoom.mute(this, payload.getMessage()); //rra23 11/25/24
                    break;
                case UNMUTE:
                    currentRoom.unmute(this, payload.getMessage()); //rra23 11/25/24
                    break;
                default:
                    break;
            }
        } catch (Exception e) {
            System.out.println("Could not process Payload: " + payload);
            e.printStackTrace();
        }
    }

    public void loadMuteList(){ //rra23 12/10/24
        String fileName = this.getClientName() + "muteList";
        File file = new File(fileName);

        if (!file.exists()) {
            return;
        }

        try (Scanner scan = new Scanner(file)){
            int lineNumber = 1;
            while(scan.hasNextLine()){
                String text = scan.nextLine();
                String[] mutedUsers = text.split(",");
            }
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    private void saveMuteList(){ //rra23 12/10/24
        String fileName = this.getClientName() + "muteList";
        try(FileWriter fw = new FileWriter(fileName)){
            fw.write("muted User,");
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    // send methods to pass data back to the Client

    public boolean sendClientSync(long clientId, String clientName){
        ConnectionPayload cp = new ConnectionPayload();
        cp.setClientId(clientId);
        cp.setClientName(clientName);
        cp.setConnect(true);
        cp.setPayloadType(PayloadType.SYNC_CLIENT);
        return send(cp);
    }

    /**
     * Overload of sendMessage used for server-side generated messages
     * 
     * @param message
     * @return @see {@link #send(Payload)}
     */
    public boolean sendMessage(String message) {
        return sendMessage(ServerThread.DEFAULT_CLIENT_ID, message);
    }

    /**
     * Sends a message with the author/source identifier
     * 
     * @param senderId
     * @param message
     * @return @see {@link #send(Payload)}
     */
    public boolean sendMessage(long senderId, String message) { //rra23 10-17-24
        Payload p = new Payload();
        p.setClientId(senderId);
        p.setMessage(message);
        p.setPayloadType(PayloadType.MESSAGE);
        return send(p);
    }

    /**
     * Tells the client information about a client joining/leaving a room
     * 
     * @param clientId   their unique identifier
     * @param clientName their name
     * @param room       the room
     * @param isJoin     true for join, false for leaivng
     * @return success of sending the payload
     */
    public boolean sendRoomAction(long clientId, String clientName, String room, boolean isJoin) {
        ConnectionPayload cp = new ConnectionPayload();
        cp.setPayloadType(PayloadType.ROOM_JOIN);
        cp.setConnect(isJoin); //<-- determine if join or leave
        cp.setMessage(room);
        cp.setClientId(clientId);
        cp.setClientName(clientName);
        return send(cp);
    }

    //rra23 11/10/24
    public boolean sendRoll(long ClientId, String message){
        Payload rp = new Payload();
        rp.setPayloadType(PayloadType.ROLL);
        rp.setClientId(ClientId);
        rp.setMessage(message);
        return send(rp);
    }

    //rra23 11/11/24
    public boolean sendFlip(long ClientId, String message){
        Payload fp = new Payload();
        fp.setPayloadType(PayloadType.FLIP);
        fp.setClientId(ClientId);
        fp.setMessage(message);
        
        return send(fp);
    }

    //rra23 11/25/24
    public boolean sendPrivateMessage(String recipient, String message){
            Payload pmp = new PrivateMessagePayload();
            pmp.setPayloadType(PayloadType.PRIVATE_MESSAGE);
            pmp.setMessage(message);
            send(pmp);
            return send(pmp);
        }

    //rra23 11/25/24
    public boolean sendMute(long ClientId){
        Payload mp = new Payload();
        mp.setPayloadType(PayloadType.MUTE);
        mp.setClientId(ClientId);
        mp.setMessage("Mute successful");
        return send(mp);
    }

    //rra23 11/25/24
    public boolean sendUnmute(long ClientId){
        Payload ump = new Payload();
        ump.setPayloadType(PayloadType.MUTE);
        ump.setClientId(ClientId);
        ump.setMessage("Unmute successful");
        return send(ump);
    }

    /**
     * Tells the client information about a disconnect (similar to leaving a room)
     * 
     * @param clientId   their unique identifier
     * @param clientName their name
     * @return success of sending the payload
     */
    public boolean sendDisconnect(long clientId, String clientName) {
        ConnectionPayload cp = new ConnectionPayload();
        cp.setPayloadType(PayloadType.DISCONNECT);
        cp.setConnect(false);
        cp.setClientId(clientId);
        cp.setClientName(clientName);
        return send(cp);
    }

    /**
     * Sends (and sets) this client their id (typically when they first connect)
     * 
     * @param clientId
     * @return success of sending the payload
     */
    public boolean sendClientId(long clientId) {
        this.clientId = clientId;
        ConnectionPayload cp = new ConnectionPayload();
        cp.setPayloadType(PayloadType.CLIENT_ID);
        cp.setConnect(true);
        cp.setClientId(clientId);
        cp.setClientName(clientName);
        return send(cp);
    }

    // end send methods
}