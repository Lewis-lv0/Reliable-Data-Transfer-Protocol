/*************************************
 * Filename:  Receiver.java
 *************************************/
import java.util.Random;

import java.util.HashMap;


public class Receiver extends NetworkHost

{
    /*
     * Predefined Constants (static member variables):
     *
     *   int MAXDATASIZE : the maximum size of the Message data and
     *                     Packet payload
     *
     *
     * Predefined Member Methods:
     *
     *  void startTimer(double increment):
     *       Starts a timer, which will expire in
     *       "increment" time units, causing the interrupt handler to be
     *       called.  You should only call this in the Sender class.
     *  void stopTimer():
     *       Stops the timer. You should only call this in the Sender class.
     *  void udtSend(Packet p)
     *       Puts the packet "p" into the network to arrive at other host
     *  void deliverData(String dataSent)
     *       Passes "dataSent" up to app layer. You should only call this in the
     *       Receiver class.
     *  double getTime()
     *       Returns the current time in the simulator.  Might be useful for
     *       debugging.
     *  void printEventList()
     *       Prints the current event list to stdout.  Might be useful for
     *       debugging, but probably not.
     *
     *
     *  Predefined Classes:
     *
     *  Message: Used to encapsulate a message coming from app layer
     *    Constructor:
     *      Message(String inputData): 
     *          creates a new Message containing "inputData"
     *    Methods:
     *      boolean setData(String inputData):
     *          sets an existing Message's data to "inputData"
     *          returns true on success, false otherwise
     *      String getData():
     *          returns the data contained in the message
     *  Packet: Used to encapsulate a packet
     *    Constructors:
     *      Packet (Packet p):
     *          creates a new Packet, which is a copy of "p"
     *      Packet (int seq, int ack, int check, String newPayload)
     *          creates a new Packet with a sequence field of "seq", an
     *          ack field of "ack", a checksum field of "check", and a
     *          payload of "newPayload"
     *      Packet (int seq, int ack, int check)
     *          chreate a new Packet with a sequence field of "seq", an
     *          ack field of "ack", a checksum field of "check", and
     *          an empty payload
     *    Methods:
     *      boolean setSeqnum(int n)
     *          sets the Packet's sequence field to "n"
     *          returns true on success, false otherwise
     *      boolean setAcknum(int n)
     *          sets the Packet's ack field to "n"
     *          returns true on success, false otherwise
     *      boolean setChecksum(int n)
     *          sets the Packet's checksum to "n"
     *          returns true on success, false otherwise
     *      boolean setPayload(String newPayload)
     *          sets the Packet's payload to "newPayload"
     *          returns true on success, false otherwise
     *      int getSeqnum()
     *          returns the contents of the Packet's sequence field
     *      int getAcknum()
     *          returns the contents of the Packet's ack field
     *      int getChecksum()
     *          returns the checksum of the Packet
     *      String getPayload()
     *          returns the Packet's payload
     *
     */

    // Add any necessary class variables here. They can hold
    // state information for the receiver.
    int rcvBase;
    int sequenceNumSize;
    HashMap<Integer, Packet> pktBuf; // SeqNum:Packet

    // Also add any necessary methods (e.g. for checksumming)
    private int checkSum(Packet pkt) {
        int sum = 0;
        byte[] bytes_arr = pkt.getPayload().getBytes();

        for (byte bt : bytes_arr) {
            // assume each character is a 8-bit integer
            int num = bt & 0xff;
            sum += num;
        }
        // add ack and sequence number
        sum += (pkt.getAcknum() + pkt.getSeqnum());
        return sum;
    }

    // This is the constructor. Don't touch!!!
    public Receiver(int entityName,
                       EventList events,
                       double pLoss,
                       double pCorrupt,
                       int trace,
                       Random random)
    {
        super(entityName, events, pLoss, pCorrupt, trace, random);
    }

    
    // This routine will be called whenever a packet sent from the sender
    // (i.e. as a result of a udtSend() being done by a Sender procedure)
    // arrives at the receiver.  "packet" is the (possibly corrupted) packet
    // sent from the sender.
    protected void Input(Packet packet)
    {   
        int seqNum = packet.getSeqnum();
        String dataSent = packet.getPayload();
        // correctly received packet
        if (seqNum == rcvBase && packet.getChecksum() == checkSum(packet)) {
            deliverData(dataSent);
            Packet pkt = new Packet(seqNum, 1, seqNum + 1);
            udtSend(pkt);
            rcvBase++;
            while (!pktBuf.isEmpty() && pktBuf.containsKey(rcvBase)) {
                deliverData(pktBuf.get(rcvBase).getPayload());
                rcvBase++;
            }
        }
        // detect dulplicate or buffer coming packets
        else if (packet.getChecksum() == checkSum(packet)) {
            // ignore the packet and send ack
            Packet pkt = new Packet(seqNum, 1, seqNum + 1);
            udtSend(pkt);
            if (seqNum > rcvBase) {
                // buffer the packet
                pktBuf.put(seqNum, packet);
            }
        }
        else {
            // ignore
        }
    }
    

    
    // This routine will be called once, before any of your other receiver-side
    // routines are called. It can be used to do any required
    // initialization (e.g. of member variables you add to control the state
    // of the receiver).
    protected void Init()
    {
        rcvBase = 0;
        sequenceNumSize = 16;
        pktBuf = new HashMap<>();
    }

}
