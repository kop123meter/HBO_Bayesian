package com.arcore.AI_ResourceControl;

public class serverAddress {
    private String HBO_SERVER_IP_ADDRESS = "192.168.10.164";
    private int HBO_PORT =  1909;

    private String OFFLOAD_SERVER_IP_ADDRESS = "192.168.10.183";
    private int OFFLOAD_PORT = 4545;

    serverAddress(){}

    public String getHBO_SERVER_IP_ADDRESS() {
        return HBO_SERVER_IP_ADDRESS;
    }

    public String getOFFLOAD_SERVER_IP_ADDRESS() {
        return OFFLOAD_SERVER_IP_ADDRESS;
    }

    public int getHBO_PORT() {
        return HBO_PORT;
    }

    public int getOFFLOAD_PORT() {
        return OFFLOAD_PORT;
    }
}
