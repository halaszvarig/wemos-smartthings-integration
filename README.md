# Wemos SmartThings Integration
This repository contains SmartThings device handlers, a configurator smartapp and Arduino code for several custom Wemos D1 mini based devices.

## Supported devices
Currently there are 2 device types supported, a temperature/humidity sensor and a relay switch and based on these it's quite easy to create additional device types.

#### Wemos Temperature Humidity Sensor
* Provides a HTTP API where the actual temperature and humidity values can be queried.
* Shows up as a sensor in the SmartThings classic mobile app.

#### Wemos Relay Switch
* Provides an HTTP API that can be used to toggle and retrieve the state of a relay.
* Shows up as a switch in the SmartThings classic mobile app.


## Setting app the Wemos part
_More info about Wemos: https://www.wemos.cc/_

The hardware parts can be purchased online from the official store: https://lolin.aliexpress.com/store/1331105

Arduino IDE and a few external libraries are required for programming the devices.

### Hardware components
Wemos Temperature Humidity Sensor
* Wemos D1 Mini https://wiki.wemos.cc/products:d1:d1_mini
* Tripler base https://wiki.wemos.cc/products:d1_mini_shields:tripler_base
* SHT30 Shield https://wiki.wemos.cc/products:d1_mini_shields:sht30_shield
* OLED Shield https://wiki.wemos.cc/products:d1_mini_shields:oled_shield

Wemos Relay Switch
* Wemos D1 Mini https://wiki.wemos.cc/products:d1:d1_mini
* Dual base https://wiki.wemos.cc/products:d1_mini_shields:dual_base
* Relay Shield https://wiki.wemos.cc/products:d1_mini_shields:relay_shield

### Software components
* Arduino IDE https://www.arduino.cc/en/main/software
* Hardware package for the Wemos board https://wiki.wemos.cc/tutorials:get_started:get_started_in_arduino
* WiFiManager library https://github.com/tzapu/WiFiManager
* ArduinoJson library https://github.com/bblanchon/ArduinoJson
https://github.com/arduino-libraries/NTPClient
* WEMOS SHT30 Shield library https://github.com/wemos/WEMOS_SHT3x_Arduino_Library
* WEMOS OLED Shield library https://github.com/stblassitude/Adafruit_SSD1306_Wemos_OLED

### Installation
Setup the Arduino IDE and install the dependencies.

Connect the components using the bases or using the stacking method, then use the Arduino IDE to compile and upload the specific code from the `arduino` folder into the boards.

### Initial wifi setup and basic operation
During this process the Arduino IDE's built-in serial monitor can be used to get more debug information regarding the Wifi connection.

The device code relies on WifiManager. On the first run, this library creates a Wifi access point named "AutoConnectAP". The user can connect to this temporary access point to store the real Wifi network SSID and password the device should use. After saving the settings, the Wemos device removes the temporary access point and connects to the specified Wifi network.

After successful connection, the device starts a web server and starts broadcasting its capabilities using the SSDP protocol.

The devices also provide a very basic unauthenticated API. Calling the `http://DEVICE_IP:80/` endpoint will return the current state of the device:

Example response of the Wemos Temperature Humidity Sensor:
```
{
  timestamp: "2019-02-22T20:03:39Z",
  temperature: "22.4"
  humidity: "50"
}
```

Example response of the Wemos Relay Switch:
```
{
  timestamp: "2019-02-22T20:03:39Z",
  state: "off"
}
```

Additionally, the state of the Wemos Relay Switch can be changed using the `relay` parameter.

To turn on the switch, call: `http://DEVICE_IP:80/?relay=on`

To turn off the switch, call: `http://DEVICE_IP:80/?relay=off`


## Setting app the SmartThings part
_More info about SmartThings: https://www.smartthings.com/_

The SmartThings device handlers and the smartapp can be installed using the online SmartThings IDE here: https://graph.api.smartthings.com/ide/apps

There are 2 installation methods:
* Installing from GitHub directly: https://docs.smartthings.com/en/latest/tools-and-ide/github-integration.html
* Creating empty new smartapp and device handlers using the "From Code" option and pasting the sourcecode into the IDE

After creation, the smartapp and the device handlers have to be published as well.

The published smartapp can be added to the SmartThings Hub using the SmartThings classic mobile application (Marketplace/SmartApps/My Apps/Wemos Configurator).

After adding the smartapp to the Hub, it will allow the user to select a device type (Wemos Temperature Humidity Sensor or Wemos Relay Switch) then it will start listening to SSDP broadcasts and will discover all available devices of the give type connected to the same network as the Hub, and the user will be able to save the found devices. This process can be repeated whenever needed.

The found and saved devices will use the previously installed and published device handlers. These device handlers rely on the HTTP based APIs provided by the code running on the devices, so the state is polled and changed using HTTP calls executed by the SmartThings Hub.

## Thanks
* [@SmartThingsCommunity](https://docs.smartthings.com/en/latest/cloud-and-lan-connected-device-types-developers-guide/building-lan-connected-device-types/) for the Generic UPnP Service Manager example
* [@esp8266](https://github.com/esp8266/Arduino) for the D1 mini support
* [@wemos](https://github.com/wemos/WEMOS_SHT3x_Arduino_Library) for the SHT3x lib
* [@stblassitude](https://github.com/stblassitude/Adafruit_SSD1306_Wemos_OLED) for the OLED lib
* [@tzapu](https://github.com/tzapu/WiFiManager) for WifiManager lib
* [@bblanchon](https://github.com/bblanchon/ArduinoJson) for ArduinoJson lib
* [@jimvb](https://github.com/jimvb/NodeMCU-Smartthings-Switchs) for the inspiration, my Relay Switch code is based on his HTTP Button implementation
