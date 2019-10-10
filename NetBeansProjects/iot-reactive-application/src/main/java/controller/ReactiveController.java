/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package controller;

import io.vertx.core.AbstractVerticle;
import util.Runner;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import edgent.ControllerEdgent;
import io.vertx.core.eventbus.EventBus;
import io.vertx.mqtt.MqttClient;
import io.vertx.mqtt.MqttClientOptions;
import io.vertx.rxjava.core.eventbus.Message;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import model.Device;
import model.Sensor;
import model.SensorData;
import org.apache.edgent.connectors.mqtt.MqttStreams;
import org.apache.edgent.function.Functions;
import org.apache.edgent.topology.TStream;
import org.apache.edgent.topology.TWindow;
import org.apache.edgent.topology.Topology;
import rx.Single;

import tatu.TATUWrapper;

/**
 *
 * @author cleberlira
 */
public class ReactiveController extends AbstractVerticle {

    private String localAddress;
    private String username;
    private String password;
    private String jsonDevices;
    private Topology topology;
    private List<Device> listDevices;
    private int defaultCollectionTime;
    private int defaultPublishingTime;
    private Path pathLog;
    private ControllerEdgent controllerEdgent;
    private String gatewayID;
    private MqttClient mqttClient;
    private MqttClientOptions mqttOptions;
    private MqttStreams connector;

    public static void main(String[] args) {
        Runner.runExample(ReactiveController.class);
    }

    @Override
    public void start() {
        try {
            this.mqttOptions = new MqttClientOptions();
            this.mqttOptions.setLocalAddress("localhost");
            this.listDevices = new ArrayList<>();
            
            ControllerEdgent controlEdgent = new ControllerEdgent();
            Topology topologyFilter = controlEdgent.createTopology();
            
            vertx.runOnContext((e) -> {
                this.connector = new MqttStreams(topologyFilter, "tcp://localhost:1883", "device1");   
                
                TStream<String> temp = this.connector.subscribe("dev/ufbaino01/RES", 1);
                
                TWindow<String, Integer> tempWindowSensorData = temp.last(10, Functions.unpartitioned());
                TStream<List<SensorData>> tempSensorData = tempWindowSensorData.batch((tuple, u) -> {
                    List<SensorData> listData = new ArrayList<SensorData>();
                    SensorData sensorData = null;
                    for (String string : tuple) {
                        
                        if(TATUWrapper.isValidTATUAnswer(string)){
                                
                                
                            JsonParser parser = new JsonParser();

                            JsonElement element = parser.parse(string);
                            JsonObject jObject = element.getAsJsonObject();


                            JsonObject body = jObject.getAsJsonObject("BODY");
                                                        
                            JsonArray jsonArray = body.getAsJsonArray("temperatureSensor");

                            if(jsonArray != null){
                                
                                for (int i = 0; i < jsonArray.size(); i++) {
                                    JsonElement jsonElement = jsonArray.get(i);
                                    String value = String.valueOf(jsonElement.getAsDouble());
                                    //System.out.println(value);
                                    sensorData = new SensorData(value, LocalDateTime.now(), null, null, 0);  
                                    if(sensorData != null) 
                                        listData.add(sensorData);
                                }
                                
                            }
                        }
                    }
                    
                   return listData;
                });
                
                //tempWindowSensorData.feeder().filter(tuple -> Double.parseDouble(tuple.getValue()) > 25 && Double.parseDouble(tuple.getValue()) < 37);   
                //tempSensorData = paserTatuStreamFlow(tempSensorData);
                tempSensorData.sink(tuple -> tuple.stream().forEach((t) -> {
                    System.out.println("Sink 1 " + t.getValue());
                }));
                
              TStream<JsonObject> sensorDataJsonStream = tempSensorData.map((sensorData) -> {
                    //List<JsonObject> output = new ArrayList<JsonObject>();
                     
                     
                     JsonObject sensorDataJson = new JsonObject();
                     SensorData index = sensorData.get(1);
                     //System.out.println("print " + sensorData.get(1).getValue());
                     //String deviceId = index.getDevice().getDeviceId();
                     //String sensorId = index.getSensor().getSensorid();
                     //String dateTime = index.getLocalDateTime().toString();
                     //System.out.println("deviceId: " + deviceId);
                     
                     //String valueSensor = index.getValue();
                     //sensorDataJson.addProperty("deviceId", deviceId);
                     //sensorDataJson.addProperty("sensorId", sensorId);
                     //sensorDataJson.addProperty("localDateTime", dateTime);
                     //sensorDataJson.addProperty("valueSensor", valueSensor);
                     
                     //System.out.println("deviceId: " + deviceId);
                    JsonArray arrayData = new JsonArray();
                    for (SensorData sensorDataColect : sensorData) {
                        if(Double.parseDouble(sensorDataColect.getValue())<35)
                            arrayData.add(sensorDataColect.getValue());
                    //output.add(sensorDataJson);
                    }   
              
                    sensorDataJson.add("valueSensor", arrayData);
                    //System.out.println(sensorDataJson);
                    return sensorDataJson;
                });
               
               sensorDataJsonStream.sink((t) -> {
                    System.out.println("Sink 2 " + t);
                    vertx.eventBus().send("webmedia",  t);
               });
               
               //tempSensorData.sink(tuple -> tuple.stream().forEach((t) -> {
               //     System.out.println("Sink 1 " + t.getValue());
               // }));
                 
                
                controlEdgent.deployTopology(topologyFilter);
                System.out.println("Finish");
                
            });
           
            jsonDevices = "[{id:ufbaino01, latitude:53.290411, longitude:-9.074406, sensors:[{id:temperatureSensor, type:Thermometer, collection_time:30000, publishing_time: 60000}, {id:humiditySensor, type:HumiditySensor, collection_time:30000, publishing_time: 60000}]},{id:ufbaino02, latitude:53.2865012, longitude:-9.0712183,sensors:[{id:temperatureSensor, type:Thermometer, collection_time:30000, publishing_time: 60000}, {id:currentSensor01, type:EnergyMeter, collection_time:1000, publishing_time: 60000}]}, {id:ufbaino03, latitude:53.2865015, longitude:-9.0712185,sensors:[{id:temperatureSensor, type:Thermometer, collection_time:30000, publishing_time: 60000}, {id:currentSensor01, type:HumiditySensor, collection_time:1000, publishing_time: 60000}]}]";
            
            
            System.out.println("subscribing in topics:");

            loadDevices();
           
            this.mqttClient = MqttClient.create(vertx);

            this.mqttClient.connect(1883, mqttOptions.getLocalAddress(), s -> {

                for (Device device : listDevices) {
                    System.out.println(TATUWrapper.topicBase + device.getDeviceId() + "/#");
                    this.mqttClient.subscribe(TATUWrapper.topicBase + device.getDeviceId() + "/#", 1);
                    
                  
                }

            });
            
            
            //TStream<String> temp = this.connector.subscribe("dev/ufbaino01/RES/s", 1);
            //TStream<String> temp = topologyFilter.strings("");
            vertx.setPeriodic(2000, id  -> {
                this.mqttClient.publishHandler(hndlr -> {
                    System.out.println("There are new message in topic: " + hndlr.topicName());
                    System.out.println("Content(as string) of the message: " + hndlr.payload().toString());
                    System.out.println("QoS: " + hndlr.qosLevel());
                    vertx.eventBus().send("webmedia",  hndlr.topicName() + hndlr.payload().toString() );
                
                
                }).subscribe("dev/ufbaino01/RES", 1);
                
                
                
             });
            
            
            System.out.println("Finish");
        } catch (Exception e) {
            System.out.println("controller.ReactiveController.start()" + e.getMessage());
        }
        
        
        
        
    }
    
   
    private TStream<SensorData> paserTatuStreamFlow(TStream<String> tStream){
      

       TStream<SensorData> tStreamSensorData = tStream.map(tuple -> {
                    List<SensorData> listData = new ArrayList<SensorData>();
                    SensorData sensorData = null;
                    try{
                        
                        if(TATUWrapper.isValidTATUAnswer(tuple)){
                                
                                
                            JsonParser parser = new JsonParser();

                            JsonElement element = parser.parse(tuple);
                            JsonObject jObject = element.getAsJsonObject();


                            JsonObject body = jObject.getAsJsonObject("BODY");
                            
                            JsonElement elementTimeStamp = body.get("TimeStamp");
                            
                            long delay = 0;
                            if(elementTimeStamp != null){
                             
                                delay = System.currentTimeMillis()-elementTimeStamp.getAsLong();
                                //System.out.println("Delay Message " + this.Sensorid + ": " + delay);
                            
                            }
                            JsonArray jsonArray = body.getAsJsonArray("temperatureSensor");

                            if(jsonArray != null){
                                
                                for (int i = 0; i < jsonArray.size(); i++) {
                                    JsonElement jsonElement = jsonArray.get(i);
                                    String value = String.valueOf(jsonElement.getAsDouble());
                                    //System.out.println(value);
                                    sensorData = new SensorData(value, LocalDateTime.now(), null, null, delay);  
                                    //if(sensorData != null) 
                                    //    listData.add(sensorData);
                                }
                                
                            }
                            
                           
                        }
                        
                    }catch(Exception e){
                        System.out.println("Erro parser: " + e.getMessage());
                    }
                    
                    return sensorData;
		});
      
       return tStreamSensorData;
   } 
    
   public void filterData(){
       
   }

    public void init() {

    }

    public void loadDevices() {
        JsonParser parser = new JsonParser();
        JsonElement element = parser.parse(this.getJsonDevices());
        JsonArray jarray = element.getAsJsonArray();

        System.out.println("Tamanho do array " + jarray.size());

        for (JsonElement jsonElement : jarray) {
            if (jsonElement.isJsonObject()) {

                System.out.println("Loop 1");

                Device device = new Device(this.mqttOptions, this.gatewayID);
                JsonObject deviceElement = jsonElement.getAsJsonObject();
                device.setDeviceId(deviceElement.get("id").getAsString());
                device.setLatitude(deviceElement.get("latitude").getAsDouble());
                device.setLongitude(deviceElement.get("longitude").getAsDouble());

                JsonArray jsonArraySensors = deviceElement.getAsJsonArray("sensors");
                List<Sensor> listSensor = new ArrayList<>();

                for (JsonElement jsonElementSensor : jsonArraySensors) {
                    if (jsonElementSensor.isJsonObject()) {
                        JsonObject jSensor = jsonElementSensor.getAsJsonObject();
                        String sensorID = jSensor.get("id").getAsString();

                        System.out.println("this.getMqttClient() " +  this.getMqttClient());
                        Sensor sensor = new Sensor(this.mqttOptions,
                                sensorID, device, this.pathLog);

                        sensor.setType(jSensor.get("type").getAsString());
                        sensor.setCollectionTime(jSensor.get("collection_time").getAsInt());
                        sensor.setPublishingTime(jSensor.get("publishing_time").getAsInt());

                        System.out.println(sensor.getPublishingTime());

                        System.out.println(sensor.getCollectionTime());

                        //  sensor.sendFlowRequest();
                        System.out.println("Loop 2");

                        listSensor.add(sensor);
                    }
                }

                device.setListSensors(listSensor);
                this.listDevices.add(device);
            }
        }
    }

    /**
     * @return the jsonDevices
     */
    public String getJsonDevices() {
        return jsonDevices;
    }

    /**
     * @param jsonDevices the jsonDevices to set
     */
    public void setJsonDevices(String jsonDevices) {
        this.jsonDevices = jsonDevices;
    }

    /**
     * @return the gatewayID
     */
    public String getGatewayID() {
        return gatewayID;
    }

    /**
     * @param gatewayID the gatewayID to set
     */
    public void setGatewayID(String gatewayID) {
        this.gatewayID = gatewayID;
    }

    /**
     * @return the localAddress
     */
    public String getLocalAddress() {
        return localAddress;
    }

    /**
     * @return the mqttClient
     */
    public MqttClient getMqttClient() {
        return mqttClient;
    }

    /**
     * @param mqttClient the mqttClient to set
     */
    public void setMqttClient(MqttClient mqttClient) {
        this.mqttClient = mqttClient;
    }

}
