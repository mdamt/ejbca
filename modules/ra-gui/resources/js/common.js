/*************************************************************************
 *                                                                       *
 *  EJBCA: The OpenSource Certificate Authority                          *
 *                                                                       *
 *  This software is free software; you can redistribute it and/or       *
 *  modify it under the terms of the GNU Lesser General Public           *
 *  License as published by the Free Software Foundation; either         *
 *  version 2.1 of the License, or any later version.                    *
 *                                                                       *
 *  See terms of license at gnu.org.                                     *
 *                                                                       *
 *************************************************************************/

/* version: $Id$ */

"use strict";

// Executed when the document had been loaded
document.addEventListener("DOMContentLoaded", function(event) {
    console.log("Document loaded.");
    jsTouchUpDocument();
    // Auto fokus first found element with this tag on page load (JSF2.0 does not support HTML5 attributes)
	forEachInputElementByStyleClass("jsAutoFocusOnce", function(inputField) {
		inputField.focus();
		return true;
	});
    new SessionKeepAlive("sessionKeepAliveLink");
}, false);

function jsTouchUpDocument() {
    // Hide elements that should not be shown when JS is enabled
	forEachInputElementByStyleClass("jsHide", function(inputField) { inputField.style.display = "none"; });
	// Use title as HTML5 placeholder for elements marked with the style class (JSF2.0 does not support HTML5 attributes)
	forEachInputElementByStyleClass("jsTitleAsPlaceHolder", function(inputField) {
		inputField.placeholder = inputField.title;
		inputField.title = "";
	});
	// Delay "keyup" events for input elements marked with the provided styleClassName. (JSF2.0 AJAX work around.)
	forEachInputElementByStyleClass("jsDelayKeyUp", function(inputField) {
		new KeyUpEventDelay(inputField, 400);
	});
};

/**
 * Keep JSF session alive by polling back-end before the session has expired.
 * 
 * @param linkElementId ID of a-element pointing to keep alive link.
 */
function SessionKeepAlive(linkElementId) {
    var instance = this;
    this.timeToNextCheckInMs = 100; // Make first check after 100 ms.
    this.xmlHttpReq = new XMLHttpRequest();
    var linkComponent = document.getElementById(linkElementId);
    if (linkComponent) {
        this.link = linkComponent.getAttribute("href");
    }
    this.xmlHttpReq.onreadystatechange = function() {
        if (instance.xmlHttpReq.readyState == 4) {
            if (instance.xmlHttpReq.status == 200) {
                instance.timeToNextCheckInMs = instance.xmlHttpReq.responseText;
                window.setTimeout(instance.poll, instance.timeToNextCheckInMs);
        	} else {
                console.log("SessionKeepAlive failed with HTTP status code " + instance.xmlHttpReq.status);
        	}
        }
    };
    this.poll = function() {
    	instance.xmlHttpReq.open("GET", instance.link, true);
        try {
        	instance.xmlHttpReq.send();
        } catch (exception) {
            console.log("SessionKeepAlive failed: " + exception);
        }
    };
    if (this.link) {
    	window.setTimeout(this.poll, this.timeToNextCheckInMs);
    } else {
        console.log("Unable to find link element with id " + linkElementId + ". SessionKeepAlive will not be enabled.");
    }
};

/** Process the callback on each input element that matches the provided style class. */
function forEachInputElementByStyleClass(styleClassName, callback) {
	var inputFields = document.getElementsByTagName("input");
	for (var i = 0; i<inputFields.length; i++) {
		if (inputFields[i].className) {
			var styleClasses = inputFields[i].className.split(' ');
			for (var j = 0; j<styleClasses.length; j++) {
				if (styleClasses[j]==styleClassName) {
					// First remove the class name to avoid processing it multiple times if this method is invoked again
					inputFields[i].className = inputFields[i].className.replace(styleClassName, "").trim();
					// Invoke the callback with the matching element. If it returns true we stop looking for more elements.
					if (callback(inputFields[i])) {
						return;
					}
					break;
				}
			}
		}
	}
}

/**
 * Delays "onkeyup" event handler invocation while additional events are triggered.
 * 
 * @param inputElement the element to wrap the onkeyup handler for
 * @param timeoutMs delay in milliseconds
 */
function KeyUpEventDelay(inputElement, timeoutMs) {
    var instance = this;
    this.component = inputElement;
    this.originalHandler = inputElement.onkeyup;
    this.timeout = timeoutMs;
    this.timer = 0;

    this.delay = function(event) {
    	// Reschedule (prevent) any existing timeout to the original handler and schedule a new one
        window.clearTimeout(instance.timer);
        instance.timer = window.setTimeout(function() { instance.originalHandler.call(instance.component, event); }, instance.timeout);
    };
    
    if (this.originalHandler) {
    	this.component.onkeyup = this.delay;
    }
};

/** Can be invoked on AJAX requests to indicate that a background operation is running. */
function onAjaxEvent(data, elementId) {
    if (data.status == "begin") {
        document.getElementById(elementId).style.opacity = "0.2";
    } else if (data.status == "success") {
        document.getElementById(elementId).style.opacity = "1.0";
    }
}
/** Can be invoked on AJAX requests to indicate that an error has occurred. */
function onAjaxError(data, elementId) {
	console.log("onAjaxError: " + data.errorMessage);
    document.getElementById(elementId).style.opacity = "0.2";
}
