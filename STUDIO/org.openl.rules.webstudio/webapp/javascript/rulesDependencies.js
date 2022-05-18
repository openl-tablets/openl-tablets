function init(data) {
    var rgraph = new $jit.ForceDirected({
        injectInto: 'rulesDepsPanel',

        Node: {
            overridable: true,
            dim: 5
        },
        Edge: {
            overridable: true,
            color: '#23a4ff',
            lineWidth: 0.8,
            type: 'arrow'
        },

        Navigation: {
            enable: true,
            //Enable panning events only if we're dragging the empty
            //canvas (and not a node).
            panning: 'avoid nodes',
            zooming: 10 //zoom speed. higher is more sensible
        },

        //Edge length
        levelDistance: 100,

        fps: 50,

        // Add node events
        Events: {
            enable: true,
            type: 'Native',
            //Change cursor style when hovering a node
            onMouseEnter: function() {
                rgraph.canvas.getElement().style.cursor = 'pointer';
            },
            onMouseLeave: function() {
                rgraph.canvas.getElement().style.cursor = '';
            },
            //Update node positions when dragged
            onDragMove: function(node, eventInfo, e) {
                var pos = eventInfo.getPos();
                node.pos.setc(pos.x, pos.y);
                rgraph.plot();
            },
            onClick: function(node, eventInfo, e) {
              //set final styles
              /*This part of code make error in IE 9
                rgraph.graph.eachNode(function(n) {
                    if(n.id != node.id) delete n.selected;
                    n.setData('dim', 5, 'end');
                    n.eachAdjacency(function(adj) {
                      adj.setDataset('end', {
                        lineWidth: 0.8,
                        color: '#23a4ff'
                      });
                    });
                  });

                if(!node.selected) {
                    node.selected = true;
                    node.setData('dim', 7, 'end');
                    node.eachAdjacency(function(adj) {
                      adj.setDataset('end', {
                        lineWidth: 2,
                        color: '#aa3329'
                      });
                    });

                  } else {
                    delete node.selected;
                  }
                  //trigger animation to final styles
                rgraph.fx.animate({
                    modes: ['node-property:dim',
                            'edge-property:lineWidth:color'],
                    duration: 300
               });
               */
            }
        },

        //Add node click handler and some styles.
        //This method is called only once for each node/label crated.
        onCreateLabel: function(domElement, node) {
            domElement.innerHTML = "<a href='" + node.data.tableUrl + "?id=" + node.id + "'>" + node.name + "</a>";
            var style = domElement.style;
            style.cursor = 'pointer';
            style.fontSize = "0.8em";
            style.color = "#fff";
        },
        //This method is called when rendering/moving a label.
        //This is method is useful to make some last minute changes
        //to node labels like adding some position offset.
        onPlaceLabel: function(domElement, node) {
            var style = domElement.style;
            var left = parseInt(style.left);
            var top = parseInt(style.top);
            var w = domElement.offsetWidth;
            style.left = (left - w / 2) + 'px';
            style.top = (top + 8) + 'px';
        }

    });

    // Load graph.
    rgraph.loadJSON(data);

    // Compute positions incrementally and animate.
    rgraph.computeIncremental({
        property: 'end',
        onStep: function(perc) {
            //perc + '% loaded...'
        },
        onComplete: function() {
            // 'done'
            rgraph.animate({
                modes: ['polar'],
                transition: $jit.Trans.Elastic.easeOut,
                duration: 2000
            });
        }
    });
}
