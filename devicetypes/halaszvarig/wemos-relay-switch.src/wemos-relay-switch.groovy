/**
 *  Wemos Relay Switch
 *  Device handler for ESP8266 based relay having an HTTP API and automatic discovery support via UPnP
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
	definition (name: "Wemos Relay Switch", namespace: "halaszvarig", author: "Gabor Halaszvari") {
		capability "Switch"
		capability "Refresh"

		command "DeviceTrigger"
	}

	simulator {
		// TODO: define status and reply messages here
	}

	tiles {
		standardTile("button", "device.switch", width: 3, height: 2, canChangeIcon: true) {
			state "off", label: 'Off', action: "switch.on", icon: "st.switches.switch.off", backgroundColor: "#ffffff", nextState: "on"
			state "on", label: 'On', action: "switch.off", icon: "st.switches.switch.on", backgroundColor: "#79b821", nextState: "off"
		}
		standardTile("refresh", "device.refresh", inactiveLabel: false, decoration: "flat", width: 3, height: 1) {
			state "default", action:"refresh.refresh", icon:"st.secondary.refresh"
		}
		main "button"
		details (["button","refresh"])
	}
}

// parse events into attributes
def parse(String description) {
	log.debug "Parsing '${description}'"
		def name = null
		def value = null

		def msg = parseLanMessage(description)
		def body = msg.body
		def slurper = new groovy.json.JsonSlurper()
		def data = slurper.parseText(body)

		switch (data.state) {
			case "on":
				if (device.currentValue("switch") == "off") {
					log.debug "Parsed state is different than the stored, setting state to: on"
					name = "switch"
					value = "on"
				}
				break
			case "off":
				if (device.currentValue("switch") == "on") {
					log.debug "Parsed state is different than the stored, setting state to: off"
					name = "switch"
					value = "off"
				}
				break
		}

		def result = createEvent(name: name, value: value)
		return result
}

def getOnPath() {
	return "/?relay=on"
}

def getOffPath() {
	return "/?relay=off"
}

def getRefreshPath() {
	return "/"
}

def on() {
	log.debug "---ON COMMAND---"
	sendEvent(name: "switch", value: "on", isStateChange: true)
	runCmd(getOnPath())
}

def off() {
	log.debug "---OFF COMMAND---"
	sendEvent(name: "switch", value: "off", isStateChange: true)
	runCmd(getOffPath())
}

def refresh() {
	log.debug "---REFRESH COMMAND---"
	sendEvent(name: "refresh", value: "")
	runCmd(getRefreshPath())
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
