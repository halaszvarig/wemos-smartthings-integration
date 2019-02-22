/**
 *  Wemos Temperature Humidity Sensor1
 *
 *  Wemos D1 mini based temperature humidity sensor using the SHT30 Shield and the OLED Shield
 *  with SSDP and HTTP API support
 *
 *  2019 Gabor Halaszvari <halaszvari@gmail.com>
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */
#include <ESP8266WiFi.h>      //https://github.com/esp8266/Arduino
#include <ESP8266WebServer.h>
#include <ESP8266SSDP.h>
#include <DNSServer.h>
#include <Ticker.h>
#include <WiFiUdp.h>
#include <Wire.h>

// Dependencies
#include <WiFiManager.h>      // https://github.com/tzapu/WiFiManager
#include <NTPClient.h>        // https://github.com/arduino-libraries/NTPClient
#include <Adafruit_SSD1306.h> // https://github.com/stblassitude/Adafruit_SSD1306_Wemos_OLED
#include <WEMOS_SHT3X.h>      // https://github.com/wemos/WEMOS_SHT3x_Arduino_Library
#include <ArduinoJson.h>      // https://github.com/bblanchon/ArduinoJson/releases/tag/v5.0.7

#define HTTP_PORT 80          // Port for the webserver, can be overridden

Ticker ticker;
WiFiUDP ntpUDP;
NTPClient timeClient(ntpUDP);
ESP8266WebServer server(HTTP_PORT);
Adafruit_SSD1306 display(0);
SHT3X sht30(0x45);

String current_timestamp;
String current_temperature;
String current_humidity;

long interval = 10000;
long previousMillis = 0;

// Initializes and sets the initial values
void setup() {
  // Setting up the serial port
  Serial.begin(115200);

  // Setting up the pins
  pinMode(LED_BUILTIN, OUTPUT);

  // Setting up the initial state: led off, display init
  digitalWrite(LED_BUILTIN, HIGH);
  display.begin(SSD1306_SWITCHCAPVCC, 0x3C);

  // Setting up the WifiManager (connects to a known wifi or starts configuration mode - look for "AutoConnectAP")
  Serial.println("Connecting to wifi network...");
  ticker.attach(1, toggleLed); // Make the led blinking slowly during the connection attempt
  WiFiManager wifiManager;
  wifiManager.setAPCallback(configModeCallback);
  if (!wifiManager.autoConnect()) {
    Serial.println("Failed to connect and hit timeout, going to reset");
    ESP.reset();
    delay(1000);
  }
  Serial.print("Wifi connected to: ");
  Serial.println(WiFi.SSID());
  Serial.print("Assigned IP address: ");
  Serial.println(WiFi.localIP());
  ticker.detach();

  // Keep the led turned on
  digitalWrite(LED_BUILTIN, LOW);

  // Setting up the web server
  Serial.printf("Starting the web server...\n");
  server.on("/", handleRequest);
  server.on("/description.xml", HTTP_GET, []() {
    SSDP.schema(server.client());
  });
  server.onNotFound(handleRequestNotFound);
  server.begin();

  // Setting up the SSDP handler
  SSDP.setSchemaURL("description.xml");
  SSDP.setHTTPPort(HTTP_PORT);
  SSDP.setDeviceType("urn:schemas-upnp-org:device:WemosTemperatureHumiditySensor:1");
  SSDP.setName("Wireless Temperature/Humidity Sensor");
  SSDP.setSerialNumber("00000001");
  SSDP.setURL("index.html");
  SSDP.setModelName("Wireless Temperature/Humidity Sensor");
  SSDP.setModelNumber("00000002");
  SSDP.setModelURL("https://github.com/halaszvarig/wemos-smartthings-integration");
  SSDP.setManufacturer("Gabor Halaszvari");
  SSDP.setManufacturerURL("https://github.com/halaszvarig/wemos-smartthings-integration");
  SSDP.begin();

  Serial.print("Server started at ");
  Serial.print("http://");
  Serial.print(WiFi.localIP());
  Serial.print(":");
  Serial.print(HTTP_PORT);
  Serial.println("/");
}

// Loops
void loop() {
  unsigned long currentMillis = millis();
  if (currentMillis - previousMillis > interval) {
    previousMillis = currentMillis;

    timeClient.update();
    current_timestamp = timeClient.getFormattedDate();
    Serial.println(current_timestamp);

    // Clear the buffer.
    display.clearDisplay();
    display.setTextSize(1);
    display.setCursor(0, 0);
    display.setTextColor(WHITE);

    if (sht30.get() == 0) {
      current_temperature = String(sht30.cTemp, 1);
      current_humidity = String(sht30.humidity, 0);

      display.println("T: ");
      display.setTextSize(2);
      display.print(current_temperature);
      display.println("C");
      Serial.print(current_temperature);
      Serial.println("C");

      display.setTextSize(1);
      display.println("H: ");
      display.setTextSize(2);
      display.print(current_humidity);
      display.println("%");
      Serial.print(current_humidity);
      Serial.println("%");
    } else {
      display.println("Error!");
    }
    display.display();
  }
  server.handleClient();
}

// Toggles the state of the builtin led
void toggleLed() {
  int state = digitalRead(LED_BUILTIN);
  digitalWrite(LED_BUILTIN, !state);
}

// Gets called when WiFiManager enters configuration mode
void configModeCallback(WiFiManager *myWiFiManager) {
  Serial.println("Wifi configuration mode started, connect to: ");
  Serial.println(WiFi.softAPIP());
  Serial.println(myWiFiManager->getConfigPortalSSID());
  ticker.attach(0.2, toggleLed);  // Make the led blinking faster during configuration mode
}

// Handles web server requests, returns current state
void handleRequest() {
  String response;

  StaticJsonDocument<200> doc;
  JsonObject root = doc.to<JsonObject>();
  root["timestamp"] = current_timestamp;
  root["temperature"] = current_temperature;
  root["humidity"] = current_humidity;
  serializeJson(doc, response);

  server.send(200, "text/html", response);
}

// Handles web server requests for undefined URLs
void handleRequestNotFound() {
  server.send(404, "text/plain", "404 File Not Found\n\n");
}
