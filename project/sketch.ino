#include <WiFi.h>
#include <ThingSpeak.h>
#include <DHT.h>


const unsigned long channelNumber = 3551632;
const char* apiKey = "X7WCM66JIDK05L86";
const char* UniId = "12327129"


const int DHT_PIN = 15;
DHT sensorDHT(DHT_PIN, DHT22);


const int ledPin = 23;


const char* WiFi_name = "Wokwi-GUEST";
const char* WiFi_Pass = "";


WiFiClient clientObject;

void setup() {
  Serial.begin(115200);


  pinMode(ledPin, OUTPUT);
  digitalWrite(ledPin, LOW);
  sensorDHT.begin();

 
  WiFi.mode(WIFI_STA);
  WiFi.begin(WiFi_name, WiFi_Pass);

  Serial.print("Connecting to Wi-Fi");

  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
  }

  ThingSpeak.begin(clientObject);
}

void loop() {
  // Read temperature and humidity
  float temperature = sensorDHT.readTemperature();
  float humidity = sensorDHT.readHumidity();


  ThingSpeak.setField(1, temperature);
  ThingSpeak.setField(2, humidity);

 
  int status = ThingSpeak.writeFields(channelNumber, apiKey);

  if (status == 200) {
    Serial.println("Data sent to ThingSpeak successfully!");
    Serial.println(UniId);
  } else {
    Serial.print("ThingSpeak error. HTTP status code: ");
    Serial.println(status);
  }


  // ThingSpeak generally requires at least 15 seconds between updates
  delay(20000);
}
