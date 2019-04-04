package com.yuning.config;

import java.util.UUID;

public class config{
	public class bleUUID {
	    public static final String SERVICEUUID = "0000fff0-0000-1000-8000-00805f9b34fb";
	    public static final String CHARACTERISTICUUID1 = "0000fff1-0000-1000-8000-00805f9b34fb";
	    public static final String CHARACTERISTICUUID3 = "0000fff3-0000-1000-8000-00805f9b34fb";  
	    public static final String CHARACTERISTICUUID4 = "0000fff4-0000-1000-8000-00805f9b34fb";  	    

	    /* montague 20150103 begin*/
	    public static final String BATTERY_SERVICE_UUID = "0000180f-0000-1000-8000-00805f9b34fb";
	    public static final String BATTERY_CHAR_UUID = "00002a19-0000-1000-8000-00805f9b34fb";
	    /* montague 20150103 end*/	
	    /* montague 20150104 begin*/
	    public static final String PAIR_SERVICE_UUID = "0000ffe0-0000-1000-8000-00805f9b34fb";
	    public static final String PAIR_CHAR_UUID = "0000ffe1-0000-1000-8000-00805f9b34fb";
	    /* montague 20150104 end*/		    
	}
    
    public static final int BATTERY_EXTREMELY_LOW_EXLEVLE = 2;
    public static final int BATTERY_LOW_LEVLE = 1;
    public static final int BATTERY_HIGH_LEVLE = 0;
    public static final int BATTERY_NULL_LEVLE = 3;
    public static int BATTERY_LEVEL = BATTERY_NULL_LEVLE;
}
