/**
 *  ActiON Dashboard 3.0.2
 *
 *  ActiON Dashboard is a web application to contol and view status of your devices.
 *  The dashboard is optimized for mobile devices as well as large screens.
 *  Once the dashboard url is generated, it could be used in any modern browser.
 *  There is no need to install SmartThings Mobile application on the device that will run the dashboard.
 *
 *  http://github.com/625alex/ActiON-Dashboard
 *
 *  Donations accepted via PayPal at alex.smart.things@gmail.com
 *
 *  Copyright © 2014 Alex Malikov
 *
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
definition(
	name: "ReactiON Dashboard",
	namespace: "dianoga",
	author: "Brian Steere, Alex Malikov",
	description: "Self contained web dashboard with optional superpowers.",
	category: "Convenience",
	iconUrl: "https://s3.amazonaws.com/smartthings-device-icons/unknown/thing/thing-circle.png",
	iconX2Url: "https://s3.amazonaws.com/smartthings-device-icons/unknown/thing/thing-circle@2x.png",
	oauth: true)


preferences {
	page(name: "selectDevices", title: "Devices", install: false, unintall: true, nextPage: "selectPhrases") {
		section("Allow control of these things...") {
			input "switches", "capability.switch", title: "Which Switches?", multiple: true, required: false
			input "dimmers", "capability.switchLevel", title: "Which Dimmers?", multiple: true, required: false
			input "momentaries", "capability.momentary", title: "Which Momentary Switches?", multiple: true, required: false
			input "locks", "capability.lock", title: "Which Locks?", multiple: true, required: false
		}

		section("View state of these things...") {
			input "contacts", "capability.contactSensor", title: "Which Contact?", multiple: true, required: false
			input "presence", "capability.presenceSensor", title: "Which Presence?", multiple: true, required: false
			input "temperature", "capability.temperatureMeasurement", title: "Which Temperature?", multiple: true, required: false
			input "humidity", "capability.relativeHumidityMeasurement", title: "Which Hygrometer?", multiple: true, required: false
			input "motion", "capability.motionSensor", title: "Which Motion?", multiple: true, required: false
		}
	}

	page(name: "selectPreferences", title: "Preferences", install: true, unintall: true) {
		section("Dashboard Preferences...") {
			label title: "Title", required: false
			input "viewOnly", title: "View Only", "bool", required: true, defaultValue: false
		}

		section("Automatically refresh dashboard...") {
			input "interval", "decimal", title: "Interval (in minutes)", required: true, defaultValue:2
		}

		section("Reset AOuth Access Token...") {
			paragraph "Activating this option will invalidate access token. The new ActiON Dashboard URL will be printed to the logs. Access token will keep resetting until this option is turned off."
			input "resetOauth", "bool", title: "Reset AOuth Access Token?", defaultValue: false
		}

		section("Send text message to...") {
			paragraph "Optionally, send text message containing the ActiON Dashboard URL to phone number. The URL will be sent in two parts because it's too long."
			input "phone", "phone", title: "Which phone?", required: false
		}
	}

	page(name: "selectPhrases", title: "Hello Home", content: "selectPhrases")
}


def selectPhrases() {
	def phrases = location?.helloHome?.getPhrases()*.label
	phrases?.sort()
	log.debug "phrases: $phrases"

	return dynamicPage(name: "selectPhrases", title: "Other Tiles", install: false, uninstall: true, nextPage: "selectPreferences") {
		if (phrases) {
			section("Hello, Home!") {
				input "showHelloHome", title: "Show Hello, Home! Phrases", "bool", required: true, defaultValue: true
				input "phrases", "enum", title: "Which phrases?", multiple: true, options: phrases, required: false
			}
		}

		section("Show...") {
			input "showMode", title: "Show Mode", "bool", required: true, defaultValue: true
			input "showClock", title: "Show Clock", "enum", multiple: false, required: true, defaultValue: "Digital", options: ["Digital", "Analog", "None"]
			input "showWeather", title: "Show Weather", "bool", required: true, defaultValue: true
			input "weatherLocation", title: "Weather Location", "text", required: false
		}

		section("Show Link 1...") {
			input "link1title", "text", title:"Link 1 Title", required: false
			input "link1url", "text", title:"Link 1 URL", required: false
		}

		section("Show Link 2...") {
			input "link2title", "text", title:"Link 2 Title", required: false
			input "link2url", "text", title:"Link 2 URL", required: false
		}

		section("Show Link 3...") {
			input "link3title", "text", title:"Link 3 Title", required: false
			input "link3url", "text", title:"Link 3 URL", required: false
		}
	}
}

mappings {
	path("/data") {
		action: [
			GET: "list",
		]
	}
	path("/ui") {
		action: [
			GET: "html",
		]
	}
	path("/command") {
		action: [
			GET: "command",
		]
	}
}

def command() {
	if (viewOnly) {
		return false;
	}

	log.debug "command received with params $params"

	def id = params.id
	def type = params.type
	def value = params.value

	def device
	def endState
	def attribute

	def response = [:]

	try {
		if (value == "toggle" && (type == "dimmer" || type == "switch")) {
			device = (type == "dimmer" ? dimmers : switches)?.find{it.id == id}
			attribute = "switch"

			if (device) {
				if(device.currentValue('switch') == "on") {
					log.debug "Switching device off"
					device.off()
					endState = "off"
				} else {
					log.debug "Switching device on"
					device.on()
					endState = "on"
				}
			} else {
				log.debug "Device not found"
			}
		} else if (type == "dimmer" && value == "0") {
			device = dimmers?.find{it.id == id}
			attribute = "switch"
			endState = "off"

			if (device) {
				device.setLevel(0)
				device.off()
			}
		} else if (type == "dimmer") {
			device = dimmers?.find{it.id == id}
			attribute = "level"
			endState = Math.min(value as Integer, 99) as String

			if (device) {
				device.setLevel(Math.min(value as Integer, 99))
			}
		} else if (type == "lock") {
			device = locks?.find{it.id == id}
			attribute = "lock"

			if (device) {
				log.debug "current lock status ${device.currentValue('lock')}"
				if(device.currentValue('lock') == "locked") {
					device.unlock()
					endState = "unlocked"
				} else {
					device.lock()
					endState = "locked"
				}

			}
		} else if (type == "mode") {
			setLocationMode(value)
		} else if (type == "helloHome") {
			device = "helloHome"
			log.debug "executing Hello Home '$value'"
			location.helloHome.execute(value)
		} else if (type == "momentary") {
			device = momentaries?.find{it.id == id}
			if (device) {
				device.push()
			}
		}

		response.status = "ok"
	} catch (Exception e) {
		response.status = 'error'
		log.error e
	}

	render contentType: "application/javascript", data: "${params.callback}(${response.encodeAsJSON()})"
}

def installed() {
	log.debug "Installed with settings: ${settings}"

	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"

	unsubscribe()
	initialize()
}

def initialize() {
	subscribe(app, getURL)
	getURL(null)

	scheduledWeatherRefresh()
	schedule('0 */15 * * * ?', scheduledWeatherRefresh)

	// Put links into an array for data
	state.links = [];
	if(link1url) {
		state.links.push([id: '1', name: link1title, status: link1url, type: 'link']);
	}
	if(link2url) {
		state.links.push([id: '2', name: link2title, status: link2url, type: 'link']);
	}
	if(link3url) {
		state.links.push([id: '3', name: link3title, status: link3url, type: 'link']);
	}

	subscribe(locks, 'lock', handleEvent)
	subscribe(switches, 'switch', handleEvent)
	subscribe(dimmers, 'switch', handleEvent)
	subscribe(dimmers, 'level', handleEvent)
	subscribe(momentaries, 'momentary', handleEvent)
	subscribe(contacts, 'contact', handleEvent)
	subscribe(presence, 'presence', handleEvent)
	subscribe(motion, 'motion', handleEvent)
	subscribe(temperature, 'temperature', handleEvent)
	subscribe(humidity, 'humidity', handleEvent)

	// I care about battery too
	subscribe(locks, 'battery', handleEvent)
	subscribe(motion, 'battery', handleEvent)
	subscribe(temperature, 'battery', handleEvent)
	subscribe(humidity, 'battery', handleEvent)
	subscribe(presence, 'battery', handleEvent)
	subscribe(contacts, 'battery', handleEvent)
}

def subscribeDevice(device) {
	device.supportedAttributes.each {
		subscribe(device, "$it", handleEvent)
	}
}

def handleEvent(event) {
	def data = [
		id: event.deviceId,
		name: event.name,
		value: event.value
	]

	pusherPost('device_update', 'devices', data)
}

def pusherPost(event, channel, data) {
	def bodyData = new groovy.json.JsonBuilder(data).toString()
	def body = new groovy.json.JsonBuilder([name: event, data: bodyData, channel: channel]).toString()

	def path = '/apps/93248/events'
	def timestamp = (int)now()/1000
	def bodyMD5 = generateMD5(body)

	def query = "POST\n" +
		"${path}\n" +
		"auth_key=c33d132d225427a60024" +
		"&auth_timestamp=${timestamp}" +
		"&auth_version=1.0" +
		"&body_md5=${bodyMD5}"

	def params = [
		uri: "http://api.pusherapp.com${path}",
		query: [
			auth_key: 'c33d132d225427a60024',
			auth_timestamp: timestamp,
			auth_version: '1.0',
			body_md5: bodyMD5,
			auth_signature: hmac_sha256('50b60e184f62252abfd6', query)
		],
		body: body,
		headers: [
			'Content-type': 'application/json'
		]
	]

	log.debug "Params: $params"
	httpPost(params)
}

def generateMD5(String s) {
	def digest = java.security.MessageDigest.getInstance("MD5")
	digest.update(s.bytes);
	new java.math.BigInteger(1, digest.digest()).toString(16).padLeft(32, '0')
}

def hmac_sha256(String secretKey, String data) {
	try {
		def secretKeySpec = new javax.crypto.spec.SecretKeySpec(secretKey.getBytes("UTF-8"), "HmacSHA256")
		def mac = javax.crypto.Mac.getInstance("HmacSHA256")
		mac.init(secretKeySpec)
		def digest = mac.doFinal(data.getBytes("UTF-8"))
		return byteArrayToString(digest)
	} catch (java.security.InvalidKeyException e) {
		log.error "Invalid key exception while converting to HMac SHA256"
	}
}

private def byteArrayToString(byte[] data) {
	def bigInteger = new BigInteger(1, data)
	def hash = bigInteger.toString(16)
	//Zero pad it
	while (hash.length() < 64) {
 		hash = "0" + hash
	}
 	return hash
}

def getURL(e) {
	if (resetOauth) {
		log.debug "Reseting Access Token"
		state.accessToken = null
	}

	if (!state.accessToken) {
		createAccessToken()
		log.debug "Creating new Access Token: $state.accessToken"
	}

	def url1 = "https://graph.api.smartthings.com/api/smartapps/installations/${app.id}/ui"
	def url2 = "?access_token=${state.accessToken}"
	log.debug "${title ?: location.name} ActiON Dashboard URL: $url1$url2"
	if (phone) {
		sendSmsMessage(phone, url1)
		sendSmsMessage(phone, url2)
	}
}


def scheduledWeatherRefresh() {
	log.debug "Refreshing weather"

	def conditions = getWeatherFeature('conditions', weatherLocation)
	def astronomy = getWeatherFeature('astronomy', weatherLocation)

	state.weather = [:]
	state.weather.conditions = conditions.current_observation
	state.weather.astronomy = astronomy.moon_phase
}

def index() {
	["index", "list", "html"]
}

def list() {
	render contentType: "application/javascript", data: "${params.callback}(${data().encodeAsJSON()})"
}

def data() {
	def things = [
		locks: locks?.collect{[type: "lock", id: it.id, name: it.displayName, lock: it.currentLock, battery: it.currentBattery ]}?.sort{it.name},
		switches: switches?.collect{[type: "switch", id: it.id, name: it.displayName, status: it.currentValue('switch')]}?.sort{it.name},
		dimmers: dimmers?.collect{[type: "dimmer", id: it.id, name: it.displayName, status: it.currentValue('switch'), level: it.currentValue('level')]}?.sort{it.name},
		momentary: momentaries?.collect{[type: "momentary", id: it.id, name: it.displayName]}?.sort{it.name},
		contacts: contacts?.collect{[type: "contact", id: it.id, name: it.displayName, status: it.currentValue('contact')]}?.sort{it.name},
		presence: presence?.collect{[type: "presence", id: it.id, name: it.displayName, status: it.currentValue('presence')]}?.sort{it.name},
		motion: motion?.collect{[type: "motion", id: it.id, name: it.displayName, status: it.currentValue('motion')]}?.sort{it.name},
		temperature: temperature?.collect{[type: "temperature", id: it.id, name: it.displayName, status: roundNumber(it.currentValue('temperature'), "°")]}?.sort{it.name},
		humidity: humidity?.collect{[type: "humidity", id: it.id, name: it.displayName, status: roundNumber(it.currentValue('humidity'), "%")]}?.sort{it.name},
		links: state.links,
	]

	if(showWeather) {
		things.weather = [type: 'weather', status: state.weather]
	}

	if(showMode) {
		things.mode = [
			type: 'mode',
			status: location.mode.toString(),
			modes: location.modes?.collect{it.name}
		]
	}

	if(showHelloHome) {
		things.hellohome = [
			type: 'hellohome',
			status: null,
			phrases: phrases
		]
	}

	return things
}

def roundNumber(num, unit) {
	if (num == null || num == "") return "n/a"
	if (!"$num".isNumber()) return num
	else {
		try {
			return Math.round("$num".toDouble()) + (unit ?: "")
		} catch (e) {
			return num
		}
	}
}

def html() {
	render contentType: "text/html", data: thePage()
}

def thePage() {
"""
<!DOCTYPE html>
<html>
<head>
	<meta name='viewport' content='width=device-width' />
    <meta name='apple-mobile-web-app-capable' content='yes' />
	<meta name='apple-mobile-web-app-status-bar-style' content='black' />
	<title>ReAction</title>

	<link rel='stylesheet' href='//maxcdn.bootstrapcdn.com/font-awesome/4.2.0/css/font-awesome.min.css'>
	<link rel='stylesheet' href='//code.jquery.com/mobile/1.4.4/jquery.mobile-1.4.4.min.css' />

	<script type='text/javascript' src='//code.jquery.com/jquery-2.1.1.min.js'></script>
	<script type='text/javascript' src='//code.jquery.com/mobile/1.4.4/jquery.mobile-1.4.4.min.js'></script>
	<script type='text/javascript' src='//cdnjs.cloudflare.com/ajax/libs/underscore.js/1.7.0/underscore-min.js'></script>
	<script type='text/javascript' src='//cdnjs.cloudflare.com/ajax/libs/backbone.js/1.1.2/backbone-min.js'></script>
	<script type='text/javascript' src='//cdnjs.cloudflare.com/ajax/libs/backbone.marionette/2.2.2/backbone.marionette.min.js'></script>
	<script type='text/javascript' src='//cdnjs.cloudflare.com/ajax/libs/backbone.stickit/0.8.0/backbone.stickit.min.js'></script>

	<script type='text/javascript' src='//cdn.jsdelivr.net/coolclock/2.1.4/coolclock.min.js'></script>
	<script type='text/javascript' src='//cdnjs.cloudflare.com/ajax/libs/packery/1.1.2/packery.pkgd.min.js'></script>
	<script type='text/javascript' src='//cdn.rawgit.com/darkskyapp/skycons/master/skycons.js'></script>

	<link rel='stylesheet' href='//rawgit.com/Dianoga/ActiON/master/web/css/app.css' />
	<script type='text/javascript' src='//rawgit.com/Dianoga/ActiON/master/web/js/Action.js'></script>

	<script type='text/javascript'>
		\$().ready(function() {
			Action.config = {
				uri: 'https://graph.api.smartthings.com/api/smartapps/installations/${app.id}/',
				access_token: '${state.accessToken}',
				refresh: ${interval},
			};
			Action.start();
		});
	</script>
</head>
<body>
	<div id='container' data-role='page'>
	</div>

	<!-- Underscore Templates -->
	<script type='text/template' id='_st-device'>
		<div class='st-tile-content'>
			<div class='st-title'>Device</div>
			<div class='st-icon'><i class='fa'></i></div>
		</div>
	</script>
	<script type='text/template' id='_st-link'>
		<div class='st-tile-content'>
			<div class='st-title'>Device</div>
			<div class='st-icon'><a><i class='fa fa-link'></i></a></div>
		</div>
	</script>
	<script type='text/template' id='_st-mode'>
		<div class='st-tile-content'>
			<div class='st-title'>Device</div>
			<div class='st-icon'></div>
			<div class='st-phrases'></div>
		</div>
	</script>
	<script type='text/template' id='_st-weather'>
		<div class='st-tile-content'>
			<div class='st-title'>Device</div>
			<canvas class='skycon' width='50' height='50'></canvas>
			<div class='w-temperature' />
			<div class='w-status' />
			<div class='w-humidity' />
			<div class='w-suntimes'>
				<span class='sunrise' />
				<i class='fa fa-sun-o' />
				<span class='sunset' />
			</div>
		</div>
	</script>
	<script type='text/template' id='_st-dimmer'>
		<div class='st-tile-content'>
			<div class='st-title'>Contact</div>
			<div class='st-icon'><i class='fa'></i></div>
			<div class='full-width-slider'>
				<input name='dimmer' min='0' max='100' type='range' data-show-value='false'
					data-mini='true' data-popup-enabled='true' data-disabled='false' data-highlight='true' step='5' />
			</div>
		</div>
	</script>
</body>
</html>
"""
}