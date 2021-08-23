package com.gmail.station.contact;

public class ContactModel {

    private String customer_id;
    private String customer_name;
    private String station_id;
    private String station_name;
    private String message;

    public ContactModel() {}
    public ContactModel(String customer_name, String customer_id, String station_id, String station_name, String message) {
        this.customer_name = customer_name;
        this.customer_id = customer_id;
        this.station_id = station_id;
        this.station_name = station_name;
        this.message = message;
    }

    public String getCustomer_name() {
        return customer_name;
    }

    public void setCustomer_name(String customer_name) {
        this.customer_name = customer_name;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getCustomer_id() {
        return customer_id;
    }

    public void setCustomer_id(String customer_id) {
        this.customer_id = customer_id;
    }

    public String getStation_id() {
        return station_id;
    }

    public void setStation_id(String station_id) {
        this.station_id = station_id;
    }

    public String getStation_name() {
        return station_name;
    }

    public void setStation_name(String station_name) {
        this.station_name = station_name;
    }
}
