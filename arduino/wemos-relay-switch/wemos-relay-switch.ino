
#include <ESP8266WiFi.h>
#include <ESP8266WebServer.h>
#include <ESP8266SSDP.h>
#include <DNSServer.h>
#include <Ticker.h>

// Dependencies
#include <WiFiManager.h>      // https://github.com/tzapu/WiFiManager
#include <NTPClient.h>        // https://github.com/arduino-libraries/NTPClient
#include <ArduinoJson.h>      // https://github.com/bblanchon/ArduinoJson/releases/tag/v5.0.7

#define HTTP_PORT 80          // Port for the webserver, can be overridden

Ticker ticker;
WiFiUDP ntpUDP;
NTPClient timeClient(ntpUDP);
ESP8266WebServer server(HTTP_PORT);

int relayPin = D1;
String current_timestamp;
String current_state = "off";

// Initializes and sets the initial values
void setup() {
  // Setting up the serial port
  Serial.begin(115200);

  // Setting up the pins
  pinMode(LED_BUILTIN, OUTPUT);
  pinMode(relayPin, OUTPUT);
  
  // Setting up the initial state: led off, relay off
  digitalWrite(LED_BUILTIN, HIGH);
  digitalWrite(relayPin, LOW);

  // Setting up the WifiManager (connects to a known wifi or starts configuration mode - look for "AutoConnectAP")
  Serial.println("Connecting to wifi network...");
  ticker.attach(1, toggleLed); // Make the led blinking slowly during the connection attempt
  WiFiManager wifiManager;
  wifiManager.setAPCallback(configModeCallback);
  if (!wifiManager.autoConnect()) {
    Serial.println("Wifi failed to connect and hit timeout, going to reset");
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
  SSDP.setDeviceType("urn:schemas-upnp-org:device:WemosRelaySwitch:1");
  SSDP.setName("Wireless Relay Switch");
  SSDP.setSerialNumber("00000001");
  SSDP.setURL("index.html");
  SSDP.setModelName("Wireless Relay Switch");
  SSDP.setModelNumber("00000001");
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

// Handles web server requests, toggles the relay, returns current state
void handleRequest() {
  String response;
  String desired_state;

  if (server.hasArg("relay")) {
    desired_state = server.arg("relay");
  }

  desired_state.toLowerCase();
  if (desired_state == "on") {
    digitalWrite(relayPin, HIGH);
    current_state = "on";
  } else if (desired_state == "off") {
    digitalWrite(relayPin, LOW);
    current_state = "off";
  } else if (desired_state != "") {
    server.send(400, "text/html", "400 Bad Request\n\n");
  }

  timeClient.update();
  current_timestamp = timeClient.getFormattedDate();

  Serial.println(current_timestamp);
  Serial.print("Relay: ");
  Serial.println(current_state);

  StaticJsonDocument<200> doc;
  JsonObject root = doc.to<JsonObject>();
  root["timestamp"] = current_timestamp;
  root["state"] = current_state;
  serializeJson(doc, response);

  server.send(200, "text/html", response);
}

// Handles web server requests for undefined URLs
void handleRequestNotFound() {
  server.send(404, "text/plain", "404 File Not Found\n\n");
}
