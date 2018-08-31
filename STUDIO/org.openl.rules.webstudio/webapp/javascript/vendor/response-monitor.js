/**
 * response-monitor.js
 * https://github.com/witstep/response-monitor.js
 * Copyright (c) Jorge Paulo
 * Licensed under the MIT license
 */
(function (global, undefined) {
    'use strict';

    var factory = function (window) {

        if (typeof window.document !== 'object') {
            throw new Error('response-monitor.js requires a `window` with a `document` object');
        }

        //the constructor
        var ResponseMonitor = function (trigger, options){
            var self = this;
            this.trigger = trigger;
            this.downloadTimer = null;

            this._validateTrigger();
            this._validateURL();

            //no options at all, TRY to use spin.js as default spinner
            if(typeof options === 'undefined')
                options = this._configDefaultSpinner(options);

            //dummy function as default for all events
            var f = function(){};

            //default options
            this.options = {
                timeout: 120,                     //seconds to wait for a response (required)
                interval: 1000,                   //milliseconds interval to check the cookie
                onRequest: f,                     //request to the server made
                onMonitor: f,                     //called once in defined interval while checking for the cookie
                onTimeout: f,                     //the download didn't start within the time limit
                onResponse: f,                    //the wait is over, the response from the server is ready
                cookiePrefix: 'response-monitor'  //should match the prefix on the server
            };

            //use the # in the URI as default timeout
            if(typeof options.timeout === 'undefined' && typeof this.hash !== 'undefined')
                options.timeout = parseInt(this.hash.split('#')[1]);

            //merge user options with default options
            for (var attrname in options)
                this.options[attrname] = options[attrname];

            //defined here, because IE<=9 doest support passing args in setInterval()
            this.monitor = function(){
                var token = self._getCookie();
                if(typeof token !== 'undefined'){
                    //using closure to support older versions of IE
                    window.setTimeout(function(){self.options.onResponse(token);}, 0);
                    self._terminate();
                }else if (self.countdown < 0){
                    self._terminate();
                    window.setTimeout(self.options.onTimeout, 0);
                    return;
                }
                self.options.onMonitor(self.countdown--);
            };
        };

        ResponseMonitor.prototype._configDefaultSpinner = function(options){
            var self = this;

            try{
                this.spinner = new Spinner();
            }catch(e){
                var SpinJS = require('spin');
                this.spinner = new SpinJS();
            }

            var onTerminate = function(status){
                self.spinner.stop();
                if(status===0) console.log('error');
            };
            return {
                onRequest: function(){
                    self.spinner.spin();
                    if(typeof self.trigger.nodeName !== 'undefined')//form or anchor
                        self.trigger.appendChild(self.spinner.el);
                    else//url
                        document.body.appendChild(self.spinner.el);
                },
                onResponse: onTerminate,
                onTimeout: function(){onTerminate(); console.log('timeout');},
            };
        };

        ResponseMonitor.prototype.execute = function(event) {
            if(this.downloadTimer)
                return;//already running...

            if( typeof this.trigger.href !== 'undefined' )
                this.trigger.href="javascript:void(0);";//prevent navigation away from the page during the request
            this.countdown=this.options.timeout * 1000 / this.options.interval;

            // if( typeof event !== 'undefined' )
            //     event.preventDefault(); //we are overriding form.submit and a.click

            /*the cookie name is not constant to allow simultaneous monitoring of
             multiple requests*/
            var date = new Date();
            var token = date.getTime();

            this.cookie = {
                name: this.options.cookiePrefix+'_'+token,
                value: token
            };
            this._createIframe();



            //check whether the query string already has parameters or not
            var separator = this.url.indexOf('?') !== -1 ? "&" : "?";

            //note that the complete cookie name is not passed to the server, only the prefix
            var url = this.url+separator+this.options.cookiePrefix+'='+token;

            if(typeof this.form !== 'undefined')
                this._submitForm(url);
            else
                this.iframe.src = url;

            this.options.onRequest(this.cookie.name);

            this.monitor();//check immediately
            this.downloadTimer = window.setInterval(this.monitor, this.options.interval);
        };

        ResponseMonitor.prototype._terminate = function () {
            //restore the link, removed to prevent navigation
            if( typeof this.trigger.href !== 'undefined' )
                this.trigger.href = this.url + this.hash;

            //remove the hidden form field from forms with GET method
            if( typeof this.form !== 'undefined' ){
                if(this.form.ACTION == 'GET')
                    this.form.removeChild(this.hiddenInput);
            }


            window.clearInterval(this.downloadTimer);
            this.downloadTimer = null;
            this._expireCookie(this.cookie.name);
            this._removeIframe();
        };

        ResponseMonitor.prototype._getCookie = function() {
            var parts = document.cookie.split(this.cookie.name + "=");
            if (parts.length === 2) {
                var text = parts.pop().split(";").shift();
                text = decodeURIComponent(text);
                if (text.charAt(0) === '"' && text.charAt(text.length - 1) === '"') {
                    text = text.substring(1, text.length - 1);
                }
                return text;
            }
        };

        ResponseMonitor.prototype._expireCookie = function(name) {
            document.cookie =
                encodeURIComponent(name) +
                "=deleted; expires=" +
                new Date(0).toUTCString();
        };

        ResponseMonitor.prototype._submitForm = function(url){
            this.form.target=this.iframe.name;

            if(this.form.method.toUpperCase() == 'GET' &&
                document.getElementById(this.options.cookiePrefix)===null
            ){//GET
                this.hiddenInput = document.createElement("input");
                this.hiddenInput.type = "hidden";
                this.hiddenInput.name = this.options.cookiePrefix;
                this.hiddenInput.value= this.cookie.value;
                this.form.appendChild(this.hiddenInput);
                this.form.submit();
            }else{//POST
                this.form.action = url;
                this.form.submit();
            }
        };

        ResponseMonitor.prototype._createIframe = function (){
            this.iframe = document.createElement('iframe');
            this.iframe.id = this.options.cookiePrefix+'_iframe_'+this.cookie.value;
            this.iframe.name = this.iframe.id;
            this.iframe.style.display = 'none';
            document.body.appendChild(this.iframe);
        };

        ResponseMonitor.prototype._removeIframe = function(){
            if(this.countdown < 0){//stop the request if it is a timeout
                try{
                    if (navigator.appName == 'Microsoft Internet Explorer')
                        this.iframe.contentWindow.document.execCommand('Stop');
                    else//
                        this.iframe.contentWindow.stop();
                }catch(e){
                    //not possible to stop the request to different (sub-) domains
                }
            }
            document.body.removeChild(this.iframe);
        };

        ResponseMonitor.prototype._invalidTriggerError = function(){
            return new Error("Invalid request trigger: '"+this.trigger+"'\nValid request triggers are HTML anchors, forms and URLs as strings.");
        };


        ResponseMonitor.prototype._validateTrigger = function(){
            var self = this;
            if(typeof this.trigger === 'undefined' || this.trigger === null){
                throw this._invalidTriggerError();
            }else if(typeof this.trigger.href !=='undefined' && this.trigger.nodeName == 'A'){
                this.trigger.onclick = function(event){self.execute(event);};
                this.url = this.trigger.href;
            }else if(typeof this.trigger.action !=='undefined' && this.trigger.nodeName == 'FORM'){
                this.trigger.onsubmit = function(event){self.execute(event);};
                this.form = this.trigger;
                this.url = this.trigger.action; //HTML form
            }else if(typeof this.trigger.nodeName === 'undefined'){
                this.url =  this.trigger; //assuming URL string as last resort;
            }else{
                throw this._invalidTriggerError();
            }
        };

        ResponseMonitor.prototype._validateURL = function(){
            var parser = document.createElement('a');
            parser.href = this.url;
            if( parser.hostname !== '' && parser.protocol.indexOf('http') !== -1 ) {
                this.url = parser.href.replace(parser.hash,'');//detach hash from url
                this.hash = parser.hash;
                return true;//looks like it can be resolved to a valid HTTP(S) url
            }
            return false;
        };

        ResponseMonitor.register = function(trigger, options){
            var elementArray;
            //check if it is a single element or a collection
            if (typeof trigger.nodeName !== 'undefined')
                elementArray = new Array(trigger);
            else
                elementArray = trigger;

            for(var i=0; i < elementArray.length; i++){
                if(typeof elementArray[i].nodeName === 'undefined')
                    throw new Error('Only HTML elements can be used as triggers');

                //not garbage collected because it will be referenced by a.onclick or form.onsubmit
                new ResponseMonitor(elementArray[i], options);
            }
        };

        ResponseMonitor.prototype.setTimeout = function(timeout){
            this.options.timeout = timeout;
        };

        ResponseMonitor.prototype.setCookiePrefix = function(cookiePrefix){
            this.options.cookiePrefix = cookiePrefix;
        };

        return ResponseMonitor;
    };

    // AMD and CommonJS support pattern borrowed from https://github.com/ScottHamper/Cookies
    var responseMonitorExport = typeof global.document === 'object' ? factory(global) : factory;

    // AMD support
    if (typeof define === 'function' && define.amd) {
        define(function () { return responseMonitorExport; });
        // CommonJS/Node.js support
    } else if (typeof exports === 'object') {
        // Support Node.js specific `module.exports` (which can be a function)
        if (typeof module === 'object' && typeof module.exports === 'object') {
            exports = module.exports = responseMonitorExport;
        }
        // But always support CommonJS module 1.1.1 spec (`exports` cannot be a function)
        exports.ResponseMonitor = responseMonitorExport;
    } else {
        global.ResponseMonitor = responseMonitorExport;
    }

})(typeof window === 'undefined' ? this : window);