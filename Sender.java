/*************************************
 * Filename:  Sender.java
 *************************************/

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;

public class Sender extends NetworkHost

{
    /*
    * Predefined Constant (static member variables):
    *
    *   int MAXDATASIZE : the maximum size of the Message data and
    *                     Packet payload
    *
    *
    * Predefined Member Methods:
    * (these methods belong to NetworkHost, so...)
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
    // state information for the sender.

    int currentSeqNum; // current sequence number
    int ack; // ack field (default 1)

    HashMap<Integer, Packet> pktList;  // keep a copy for sent packets
    HashMap<Integer, Double> packetInChannel; // key: seq number value: start time
    Queue<Message> pq; // sender buffer
    HashMap<Integer, Packet> nextPkt; // acknowledged packets

    int winSize; // window base
    int sendBase; // send base
    int inc; // default timer interval

    // timer variables
    double start; // start time of this packet
    double end; // end time of prev packet

    // Also add any necessary methods (e.g. for checksumming)

    // check sum of a packet
    private int checkSum(Message msg) {
        int sum = 0;
        byte[] bytes_arr = msg.getData().getBytes();

        for (byte bt : bytes_arr) {
            // assume each character is a 8-bit integer
            int num = bt & 0xff;
            sum += num;
        }
        // add ack and sequence number
        sum += (ack + currentSeqNum);
        return sum;
    }

    // check sum from receiver 
    private int checkSumFromRcv(Packet pktFromRcv) {
        int sum = 0;
        byte[] bytes_arr = pktFromRcv.getPayload().getBytes();

        for (byte bt : bytes_arr) {
            // assume each character is a 8-bit integer
            int num = bt & 0xff;
            sum += num;
        }
        // add ack and sequence number
        sum += (pktFromRcv.getAcknum() + pktFromRcv.getSeqnum());
        return sum;
    }

    private double timeInteval(double startTime, double endTime) {
        return inc - endTime + startTime;
    }


    // This is the constructor. Don't touch!!!
    public Sender(int entityName, EventList events, double pLoss, double pCorrupt, int trace, Random random) {
        super(entityName, events, pLoss, pCorrupt, trace, random);
    }


    // This routine will be called whenever the app layer at the sender
    // has a message to send. The job of your protocol is to insure that
    // the data in such a message is delivered in-order, and correctly, to
    // the receiving application layer.
    protected void Output(Message message) {
        Message msg;
        // buffer messages
        if (sendBase + winSize <= currentSeqNum) {
            pq.add(message);
            return;
        }

        else {
            msg = message;
        }

        // get field values
        String data = msg.getData();
        int checkSumVal = checkSum(msg);
        Packet pkt = new Packet(currentSeqNum, ack, checkSumVal, data);

        // keep a copy of the packet
        Packet pkt_copy = new Packet(pkt);
        pktList.put(currentSeqNum, pkt_copy);

        start = getTime();
        packetInChannel.put(currentSeqNum, start);


        if (packetInChannel.size() == 1) {
            startTimer(inc);
        } else {
            // wait in the queue
        }

        // send the packet
        udtSend(pkt);
        // increment the seq number
        currentSeqNum++;

        while (!pq.isEmpty() && currentSeqNum<winSize+sendBase) {
            Output(pq.poll());
        }
    }



    // This routine will be called whenever a packet sent from the receiver
    // (i.e. as a result of a udtSend() being done by a receiver procedure)
    // arrives at the sender. "packet" is the (possibly corrupted) packet
    // sent from the receiver.
    protected void Input(Packet packet) {
        int seq = packet.getSeqnum();
        int rcvBase = packet.getAcknum();
        if (packet.getChecksum() == checkSumFromRcv(packet)) {
            // case 1
            if (seq == sendBase) {
                stopTimer();
                end = getTime();
                sendBase++;
                start = packetInChannel.get(seq);
                packetInChannel.remove(seq);

                while (nextPkt.containsKey(sendBase)) {
                    packetInChannel.remove(sendBase);
                    sendBase++;
                }

                if (packetInChannel.containsKey(sendBase)) {
                    startTimer(timeInteval(start, end));
                    System.out.println("start timer for "+ sendBase);
                }
                else {
                }
            }
            // case 2
            else if (seq > sendBase) {
                // reveive following packets
                nextPkt.put(seq, packet);
                
                if (seq  >= sendBase + winSize) {
                    udtSend(pktList.get(rcvBase));
                    startTimer(inc);
                }
                else {
                    // continue;
                }

            }
            // case 3
            else if (seq < sendBase){
                // ignore
            }
            else {
                // this case should not happen
            }

        }
        else {
            // ignore corrupted packet from RCV
        }

        while (!pq.isEmpty() && sendBase + winSize > currentSeqNum) {
            Output(pq.poll());
        }
    }


    // This routine will be called when the senders's timer expires (thus
    // generating a timer interrupt). You'll probably want to use this routine
    // to control the retransmission of packets. See startTimer() and
    // stopTimer(), above, for how the timer is started and stopped.
    protected void TimerInterrupt() {

        // we don't need to stop the timer since it already time out
        udtSend(pktList.get(sendBase));
        startTimer(inc);
        start = getTime();
        packetInChannel.put(sendBase, start);
    }

    // This routine will be called once, before any of your other sender-side
    // routines are called. It can be used to do any required
    // initialization (e.g. of member variables you add to control the state
    // of the sender).
    protected void Init() {
        // initializations
        winSize = 20;
        currentSeqNum = 0;
        ack = 1;
        sendBase = 0;
        pktList = new HashMap<>();
        pq = new LinkedList<>();
        packetInChannel = new HashMap<>();
        nextPkt = new HashMap<>();
        inc = 40;
    }

}