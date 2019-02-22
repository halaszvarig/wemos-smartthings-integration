/**
 *  Wemos Temperature Humidity Sensor
 *  Device handler for ESP8266 based temperature and humidity sensor having an HTTP API and automatic discovery support via UPnP
 *  (based on Generic UPnP Device from SmartThings and HTTP Button from James VanBennekom)
 *
 *  Copyright 2016 SmartThings
 *  Copyright 2017 James VanBennekom
 *  Modifications copyright 2018 Gabor Halaszvari <halaszvari@gmail.com>
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
metadata {
	definition (name: "Wemos Temperature Humidity Sensor", namespace: "halaszvarig", author: "Gabor Halaszvari") {
		capability "Temperature Measurement"
		capability "Relative Humidity Measurement"
		capability "Refresh"
		command "DeviceTrigger"

		attribute "lastCheckin", "String"
		attribute "lastCheckinDate", "String"
	}

	simulator {
		for (int i = 0; i <= 100; i += 10) {
			status "${i}F": "temperature: $i F"
		}

		for (int i = 0; i <= 100; i += 10) {
			status "${i}%": "humidity: ${i}%"
		}
	}

	tiles(scale: 2) {
		multiAttributeTile(name:"temperature", type:"generic", width:6, height:4) {
			tileAttribute("device.temperature", key: "PRIMARY_CONTROL") {
				attributeState("temperature", label:'${currentValue}°',
					backgroundColors:[
						[value: 32, color: "#153591"], // 0C
						[value: 54, color: "#1e9cbb"], // 12C
						[value: 64, color: "#90d2a7"], // 18C
						[value: 75, color: "#44b621"], // 24C
						[value: 82, color: "#f1d801"], // 28C
						[value: 86, color: "#d04e00"], // 30C
						[value: 90, color: "#bc2323"]  // 32C
					]
				)
			}
			tileAttribute("device.multiAttributesReport", key: "SECONDARY_CONTROL") {
				attributeState("multiAttributesReport", label:'${currentValue}' //icon:"st.Weather.weather12",
				)
			}
		}
		valueTile("temperature2", "device.temperature", inactiveLabel: false) {
			state "temperature", label:'${currentValue}°', icon:"st.Weather.weather2",
				backgroundColors:[
					[value: 32, color: "#153591"], // 0C
					[value: 54, color: "#1e9cbb"], // 12C
					[value: 64, color: "#90d2a7"], // 18C
					[value: 75, color: "#44b621"], // 24C
					[value: 82, color: "#f1d801"], // 28C
					[value: 86, color: "#d04e00"], // 30C
					[value: 90, color: "#bc2323"]  // 32C
				]
		}
		valueTile("humidity", "device.humidity", inactiveLabel: false, width: 2, height: 2) {
			state "humidity", label:'${currentValue}%', unit:"%", icon:"https://raw.githubusercontent.com/bspranger/Xiaomi/master/images/XiaomiHumidity.png",
				backgroundColors:[
					[value: 0, color: "#FFFCDF"],
					[value: 4, color: "#FDF789"],
					[value: 20, color: "#A5CF63"],
					[value: 23, color: "#6FBD7F"],
					[value: 56, color: "#4CA98C"],
					[value: 59, color: "#0072BB"],
					[value: 76, color: "#085396"]
				]
		}
		valueTile("lastcheckin", "device.lastCheckin", inactiveLabel: false, decoration:"flat", width: 2, height: 2) {
			state "lastcheckin", label:'Last Event:\n ${currentValue}'
		}
		standardTile("refresh", "device.generic", width: 2, height: 2) {
			state "default", label:'Refresh', action: "refresh", icon:"st.secondary.refresh-icon"
		}
		main("temperature2")
		details(["temperature", "humidity", "lastcheckin", "refresh"])
	}
}

def installed() {
	initialize()
}

def updated() {
	unsubscribe()
	initialize()
}

def initialize() {
	runEvery1Minute("refresh");
}

def refresh() {
	log.debug "---REFRESH COMMAND---"
	sendEvent(name: "refresh", value: "")
	runCmd(getRefreshPath())
}

def getRefreshPath() {
	return "/"
}

def runCmd(String path) {
	log.debug "Preparing request: GET $path"
	try {
		def hubAction = new physicalgraph.device.HubAction([
				 method: "GET",
				 path: path,
				 headers: [HOST: getHostAddress()],
				 body: ""
			],
			device.deviceNetworkId
		)
		log.debug "hubAction: $hubAction"
		return sendHubCommand(hubAction)
	}
	catch (Exception e) {
		log.debug "Hit Exception $e on $hubAction"
	}
}

def parse(String description) {
	log.debug "Parsing '${description}'"
	def name = null
	def value = null

	def msg = parseLanMessage(description)
	def body = msg.body
	def slurper = new groovy.json.JsonSlurper()
	def data = slurper.parseText(body)
	log.debug "Data: '${data}'"

	def now = formatDate()
	def nowDate = new Date(now).getTime()

	def evt1 = createEvent(name: "lastCheckin", value: now, displayed: false)
	def evt2 = createEvent(name: "lastCheckinDate", value: nowDate, displayed: false)

	def evt3 = createEvent(name: "temperature", value: data.temperature)
	def evt4 = createEvent(name: "humidity", value: data.humidity)

	return [evt1, evt2, evt3, evt4]
}

def sync(ip, port) {
	def existingIp = getDataValue("ip")
	def existingPort = getDataValue("port")
	if (ip && ip != existingIp) {
		updateDataValue("ip", ip)
	}
	if (port && port != existingPort) {
		updateDataValue("port", port)
	}
}

private getHostAddress() {
	def ip = getDataValue("ip")
	def port = getDataValue("port")

	return convertHexToIP(ip) + ":" + convertHexToInt(port)
}

private Integer convertHexToInt(hex) {
	Integer.parseInt(hex,16)
}

private String convertHexToIP(hex) {
	[convertHexToInt(hex[0..1]),convertHexToInt(hex[2..3]),convertHexToInt(hex[4..5]),convertHexToInt(hex[6..7])].join(".")
}

def formatDate() {
	def correctedTimezone = ""
	def timeString = "HH:mm:ss"

	// If user's hub timezone is not set, display error messages in log and events log, and set timezone to GMT to avoid errors
	if (!(location.timeZone)) {
		correctedTimezone = TimeZone.getTimeZone("GMT")
		log.error "${device.displayName}: Time Zone not set, so GMT was used. Please set up your location in the SmartThings mobile app."
		sendEvent(name: "error", value: "", descriptionText: "ERROR: Time Zone not set, so GMT was used. Please set up your location in the SmartThings mobile app.")
	} else {
		correctedTimezone = location.timeZone
	}

	return new Date().format("yyyy MMM dd ${timeString}", correctedTimezone)
}
