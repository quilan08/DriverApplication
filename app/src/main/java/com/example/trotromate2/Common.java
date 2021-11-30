package com.example.trotromate2;

public class Common {
    public static final String DRIVER_INFO_REFERENCES ="DriverInfo" ;
    public static final String DRIVER_LOCATION_REFERENCES ="DriversLocation" ;

    public static DriverInfoModel currentUser;

    public static String buildMessage() {
        if(Common.currentUser != null ){
            return  new StringBuilder("Welcome ").append(currentUser.getFirstname())
                    .append(" ")
                    .append(currentUser.getLastname()).toString();

        }
        else {
            return " ";
        }
    }
}
