/**********This was for MIR data collection. noy used for XMIR*/

package com.arcore.AI_ResourceControl;

class AIModel implements Comparable<AIModel>  {

    String name;
    Integer delegate;
    double avgInfTime;
    int id;

    public AIModel(AIModel other) {
        this.name = other.name;
        this.delegate =  other.delegate;
        this.avgInfTime= other.avgInfTime;
        this.id =  other.id;

    }

    public AIModel( String name, String delgt, double avgInfTime) {

        this.name = name;
        switch (delgt) {
            case "CPU":
               delegate=0;
                break;
            case "GPU":
                delegate=1;
                break;
            case "NNAPI":
                delegate=2;
                break;
            case "SERVER":
                delegate=3;
                break;
            default:
                delegate=0;
        }
        this.delegate = delegate;
        this.avgInfTime = avgInfTime;
    }

    public void assignID(int assigned_id){

        this.id=assigned_id;
    }
    public int getID(){

        return this.id;
    }
    @Override
    public int compareTo(AIModel other) {
        // Compare based on avgInfTime
        return Double.compare(this.avgInfTime, other.avgInfTime);
    }

}


