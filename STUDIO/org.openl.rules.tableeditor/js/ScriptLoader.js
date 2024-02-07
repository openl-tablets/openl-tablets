/**
 * Loads and evaluates both internal (with body) and external javascripts.
 * In Prototype 1.6.1 script tags referencing external files are ignored.
 *
 * @requires Prototype 1.6.1 javascript library
 *
 * @author Andrei Astrouski
 */
var ScriptLoader = Class.create({

    initialize: function() {
	},

    /**
     * Extracts script elements from the html string.
     *
     * @param html the html string
     * @return array of script elements
     */
    extractScripts: function(html) {
        if (html) {
            html = html.toString();
            var matchScripts = new RegExp(Prototype.ScriptFragment, 'img');
            var scripts = html.match(matchScripts) || [];
            var div = new Element('div');
            (div = $(div)).innerHTML =
                ',' + scripts.join(','); // hack for IE
            return div.select('script');
        }
    },

    /**
     * Evaluates all scripts from the html string.
     *
     * @param html the html string
     */
    evalScripts: function(html) {
        //NOTE: IE evaluates scripts in random order (especially the v.6)
        this.extractScripts(html).each(this.evalScript);
    },
    
    /**
     * Evaluates a script in the global context.
     *
     * @param script the script element
     */
    evalScript: function(script) {
        if (script) {
            var head = $$("head")[0];
            var newScript = new Element("script");
            newScript.type = "text/javascript";
            if (scriptSrc = script.src) {
                newScript.src = scriptSrc;
            } else if (scriptBody = (script.innerHTML || script.text)) {
                if (Prototype.Browser.IE) {
                    newScript.text = scriptBody;
                } else {
                    newScript.appendChild(document.createTextNode(scriptBody));
                }
                /* // Another way to eval script body
                if (window.execScript) { // IE
                    window.execScript(scriptBody);
                } else {
                    if (window.eval) {
                        window.eval(scriptBody);
                    }
                    //setTimeout(scriptBody, 0);
                }
                */
            }
            setTimeout(function() {
                head.appendChild(newScript);
            }, 10);
        }
    }

});
