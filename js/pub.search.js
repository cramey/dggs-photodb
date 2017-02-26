var map, aoi, features;

function init()
{
	// Fix for Leaflet Issue #5180
	// See: https://github.com/Leaflet/Leaflet/issues/5180
	if(!L.Browser.mobile) L.Browser.touch = false;

	// Disable bfcache (firefox compatibility)
	if('onunload' in window) window.onunload = function(){};

	aoi = mirroredLayer(null, {
		color: '#f00',
		opacity: 1,
		weight: 2,
		radius: 6,
		fill: false,
		clickable: false,
		'z-index': 20
	});

	Search.init({
		'sort': document.getElementById('search-sort'),
		'page': document.getElementById('search-page'),
		'show': document.getElementById('search-show'),
		'search': document.getElementById('search-field'),
		'aoi': aoi
	});

	Search.on('success', function(obj){
		features.clearLayers();

		var results = document.getElementById('search-results');
		var src = document.getElementById('search-results-control');

		if('docs' in obj && obj['docs'].length > 0){
			var search_prev = document.getElementById('search-prev');
			if(search_prev){
				search_prev.disabled = (obj['start'] === 0);
			}
					
			var search_next = document.getElementById('search-next');
			if(search_next){
				search_next.disabled = (
					(obj['docs'].length + obj['start']) === obj['numFound']
				);
			}

			// Show search controls
			if(src) src.style.display = 'block';

			var stats = document.getElementById('search-stats');
			if(stats){
				stats.innerHTML = 'Displaying ' + (obj['start'] + 1) +
					' to ' + (obj['start'] + obj['docs'].length) +
					' of ' + obj['numFound'];
			}

			for(var i = 0; i < obj['docs'].length; i++){
				var o = obj['docs'][i];
				if('geojson' in o){
					// Clone the geojson object, allowing
					// the original reference to be freed
					var geojson = {
						type: 'Feature',
						properties: { color: '#9f00ff' },
						geometry: JSON.parse(JSON.stringify(o['geojson']))
					};
					features.addData(geojson);
				}
			}
		} else {
			if(src) src.style.display = 'none';
		}

		var tmpl = document.getElementById('tmpl-search');
		if(results && tmpl){
			results.innerHTML = Mustache.render(tmpl.innerHTML, obj);
		}
	});

	Search.on('failure', function(err){
		features.clearLayers();

		var src = document.getElementById('search-results-control');
		if(src) src.style.display = 'none';

		var results = document.getElementById('search-results');
		var tmpl = document.getElementById('tmpl-search');
		if(results && tmpl){
			results.innerHTML = Mustache.render(tmpl.innerHTML, {
				'error': {'msg': 'Cannot connect to search service'}
			});
		}

		if(typeof console !== 'undefined'){
			console.log(err);
		}
	});

	Search.on('reset', function(){
		features.clearLayers();

		var src = document.getElementById('search-results-control');
		if(src) src.style.display = 'none';

		var results = document.getElementById('search-results');
		if(results) results.innerHTML = '';

		var sf = document.getElementById('search-field');
		if(sf) sf.focus();
	});

	var search_reset = document.getElementById('search-reset');
	if(search_reset){
		search_reset.onclick = function(){
			Search.reset();
			Search.skipdecode = true;
			window.location.hash = '';
		};
	}

	var search_button = document.getElementById('search-button');
	if(search_button) search_button.onclick = function(){
		Search.execute();
	};

	var search_prev = document.getElementById('search-prev');
	if(search_prev) search_prev.onclick = function(){
		Search.prev();
	};

	var search_next = document.getElementById('search-next');
	if(search_next) search_next.onclick = function(){
		Search.next();
	};

	var search_show = document.getElementById('search-show');
	if(search_show) search_show.onchange = function(){
		Search.execute();
	};

	var search_sort = document.getElementById('search-sort');
	if(search_sort) search_sort.onchange = function(){
		Search.execute();
	};

	var search_field = document.getElementById('search-field');
	if(search_field){
		search_field.onkeydown = function(evt){
			var e = evt === undefined ? window.event : evt;	
			if(e.keyCode === 13){
				Search.execute()
				if('preventDefault' in e) e.preventDefault();
				return false;
			}
		};
		search_field.focus();
	}

	if('onhashchange' in window){
		window.onhashchange = function(){
			Search.decode(
				window.location.hash.substring(1)
			);
		};
	}

	features = mirroredLayer(null, function(f){
		return {
			color: f.properties.color,
			opacity: 1,
			weight: 2,
			radius: 6,
			fill: false,
			'z-index': 20
		};
	});

	map = L.map('map', {
		closePopupOnClick: false,
		worldCopyJump: true,
		attributionControl: false,
		zoomControl: false,
		minZoom: 3,
		maxZoom: 19,
		center: L.latLng(62.99515, -155.21484),
		zoom: 3,
		layers: [
			baselayers['Open Street Maps'],
			features,
			aoi
		]
	});

	// Add mouse position control
	map.addControl(L.control.mousePosition({
		emptyString: 'Unknown', numDigits: 4
	}));

	// Add scale control
	map.addControl(L.control.scale());

	// Add zoom controls
	map.addControl(L.control.zoom({ position: 'topleft' }));

	// Add layer control
	map.addControl(L.control.layers(
		baselayers, overlays, {
			position: 'bottomright', autoZIndex: false
		}
	));

	// Modify the button text
	L.drawLocal.draw.toolbar.buttons.rectangle = 'Draw an area of interest';
	L.drawLocal.draw.handlers.rectangle.tooltip.start = 'Click and draw to draw an area of interest';

	// Initialize drawing control
	map.addControl(
		new L.Control.Draw({
			position: 'topleft',
			draw: {
				polygon: false, polyline: false,
				marker: false, circle: false,
				rectangle: {
					metric: true,
					shapeOptions: {
						color: '#f00',
						opacity: 1,
						weight: 2,
						radius: 6,
						fill: false,
						clickable: false
					}
				}
			},
			edit: {
				featureGroup: aoi,
				edit: false,
				remove: false
			}
		})
	);

	// When drawing starts, empty out the
	// old drawing
	map.on('draw:drawstart', function(e){
		aoi.clearLayers();
	});

	// When you draw, add it to the aoi
	// and the anti-aoi
	map.on('draw:created', function(e){
		aoi.addLayer(e.layer);
	});
	
	// Search when drawing ends
	map.on('draw:drawstop', function(e){
		Search.execute();
	});

	if(window.location.hash.length > 1){
		Search.decode(
			window.location.hash.substring(1)
		);
	}
}


function imageError(el)
{
	el.onerror = null;
	el.src = '';

	var a = document.createElement('a');
	a.href = 'javascript:void(0)';
	var img = document.createElement('img');
	img.src = 'css/notfound.gif';
	img.style.width = '256px';
	img.style.height = '167px';
	a.appendChild(img);

	el.parentNode.parentNode.replaceChild(a, el.parentNode);

	return false;
}
