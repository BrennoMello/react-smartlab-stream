/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package model;


import java.time.LocalDateTime;

/**
 *
 * @author cleberlira
 */

public class SensorData {
    private String value;
    private Device device;
    private Sensor sensor;
    private LocalDateTime localDateTime;
    private long delay;

    
    public SensorData(String value, LocalDateTime localDateTime, 
            Sensor sensor, Device device, long delay){
        this.value = value;
        this.localDateTime = localDateTime;
        this.sensor = sensor;
        this.device = device;
        this.delay = delay;
    }
    
    public SensorData(String value, LocalDateTime localDateTime, 
            Sensor sensor, Device device){
        this.value = value;
        this.localDateTime = localDateTime;
        this.sensor = sensor;
        this.device = device;
        
    }
    
    public SensorData(String value, Sensor sensor, Device device){
        this.value = value;
        this.sensor = sensor;
        this.device = device;
    }
    
    
    public long getDelay() {
        return delay;
    }

    public void setDelay(long delay) {
        this.delay = delay;
    }
    
    
    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }


    public LocalDateTime getLocalDateTime() {
        return localDateTime;
    }

    public void setLocalDateTime(LocalDateTime localDateTime) {
        this.localDateTime = localDateTime;
    }

    /**
     * @param device the device to set
     */
    public void setDevice(Device device) {
        this.device = device;
    }

    /**
     * @param sensor the sensor to set
     */
    public void setSensor(Sensor sensor) {
        this.sensor = sensor;
    }

    /**
     * @return the device
     */
    public Device getDevice() {
        return device;
    }

    /**
     * @return the sensor
     */
    public Sensor getSensor() {
        return sensor;
    }
    
    
    
	
}