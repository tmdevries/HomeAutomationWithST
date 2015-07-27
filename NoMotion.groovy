/**
 *  Customizable No Motion Thresholds
 *
 *  Copyright 2015 Tara De Vries
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
    name: "Customizable No Motion Thresholds",
    namespace: "tmdevries",
    author: "Tara De Vries",
    description: "Choose variable thresholds for modes when lights should turn off with no motion.",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png"
)


preferences {
	page name:"deviceSelection"
    page name:"modesSetup"
}

def deviceSelection() {
	dynamicPage(name: "deviceSelection", title: "First, select your lights, switches, and motion sensors", nextPage: "modesSetup", uninstall: true) {
    	section("Lights and sensors to customize") {
		input "motionList", "capability.motionSensor", title: "Which motion sensor(s)?", required: true, multiple: true
    	input "switchList", "capability.switch", title: "Which light(s)/switch(es)?", required: true, multiple: true
		}
    }
}

def modesSetup() {
	dynamicPage(name: "modesSetup", title: "For each mode, decide on how many minutes to wait after motion has stopped", install: true, uninstall: true) {
        section("Specify wait time", hideable:true, hidden:false) {
            location.modes?.each() {
        	   	//name = name.tr(' !+', '___')
            	input "${it.name}Minutes", "decimal", title:it.name, required:false
        	}
        }
    }
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
	subscribe(motionList, "motion.active", motionHandler)
    subscribe(motionList, "motion.inactive", noMotionHandler)
}

def motionHandler(evt) {
	if (state.scheduledToTurnOff == true) {
    	log.debug "Unscheduling lights turning off."
    	unschedule()
        state.scheduledToTurnOff = false
	}
}

def noMotionHandler(evt) {
	def currentMode = location.mode
    if (settings["${currentMode}Minutes"] != null) {
    	def waitTime = settings["${currentMode}Minutes"]
    	log.debug "${app.label} will turn lights off in ${waitTime} if there is no more motion"
        state.scheduledToTurnOff = true
    	runIn(waitTime*60, turnOffLights)
    }
}

def turnOffLights() {
	log.debug "${app.label} turning lights off"
	switchList.each {
    	it.off()
    }
    state.scheduledToTurnOff = false
}
