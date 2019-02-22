# Wemos SmartThings Integration
This repository contains device handlers for custom wifi connected Wemos based devices and a configurator smartapp that can discover and register the devices using SSDP.

## Supported devices

### Wemos Temperature Humidity Sensor
Components:
* Tripler base
* Wemos D1 Mini
* SHT30 Shield
* Oled Shield

### Wemos Relay Switch
Components:
* Dual base
* Wemos D1 Mini
* Relay Shield

## Arduino Dependencies
* Install the hardware package, and setup the board https://wiki.wemos.cc/tutorials:get_started:get_started_in_arduino
* Install WiFiManager https://github.com/tzapu/WiFiManager
* Install ArduinoJson https://github.com/bblanchon/ArduinoJson
https://github.com/arduino-libraries/NTPClient
* Install Arduino library for the WEMOS SHT30 Shield https://github.com/wemos/WEMOS_SHT3x_Arduino_Library
* Adafruit_SSD1306 https://github.com/stblassitude/Adafruit_SSD1306_Wemos_OLED
