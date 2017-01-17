var map, aoi, features;
var last = null;
var selected = [];
var searchok = true;
var skiphash = false;


function init()
{
	// Fix for Leaflet Issue #5180
	// See: https://github.com/Leaflet/Leaflet/issues/5180
	if(!L.Browser.mobile) L.Browser.touch = false;

	// Disable bfcache (firefox compatibility)
	if('onunload' in window){
		window.onunload = function(){};
	}

	if('onhashchange' in window){
		window.onhashchange = function(){
			if(!skiphash && window.location.hash.length > 1){
				decodeParameters(window.location.hash.substring(1));
				search(null, true);
			}
			skiphash = false;
		};
	}

	var search_button = document.getElementById('search-button');
	if(search_button) search_button.onclick = search;

	var search_prev = document.getElementById('search-prev');
	if(search_prev) search_prev.onclick = function(){ search(true); };

	var search_next = document.getElementById('search-next');
	if(search_next) search_next.onclick = function(){ search(false); };

	var search_show = document.getElementById('search-show');
	if(search_show) search_show.onchange = function(){
		var page = document.getElementById('search-page');
		if(page) page.value = '0';
		search();
	};

	var search_description = document.getElementById('search-description');
	if(search_description) search_description.onchange = search;

	var search_location = document.getElementById('search-location');
	if(search_location) search_location.onchange = search;

	var search_reset = document.getElementById('search-reset');
	if(search_reset){
		search_reset.onclick = function(){
			updateSelected(true);
			skiphash = true;
			window.location.hash = '';
			window.location.reload(false);
		};
	}

	var selected_edit = document.getElementById('selected-edit');
	if(selected_edit){
		selected_edit.onclick = function(){
			window.location.href = 'edit/' + selected.join(',');
		};
	}

	var selected_spreadsheet = document.getElementById('selected-spreadsheet');
	if(selected_spreadsheet){
		selected_spreadsheet.onclick = function(){
			window.location.href = 'spreadsheet/' + selected.join(',');
		};
	}

	var selected_delete = document.getElementById('selected-delete');
	if(selected_delete) selected_delete.onclick = deleteSelected;

	var selected_all = document.getElementById('selected-all');
	if(selected_all) selected_all.onclick = selectToggleAll;

	var q = document.getElementById('search-field');
	if(q){
		q.onkeydown = function(evt){
			var e = evt === undefined ? window.event : evt;	
			if(e.keyCode === 13){
				search();
				if('preventDefault' in e) e.preventDefault();
				return false;
			}
		};
		q.focus();
	}

	aoi = mirroredLayer(null, {
		color: '#f00',
		opacity: 1,
		weight: 2,
		radius: 6,
		fill: false,
		clickable: false,
		'z-index': 20
	});

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
		search();
	});

	if(window.location.hash.length > 1){
		decodeParameters(window.location.hash.substring(1));
		search(null, true);
	}
}


function search(back, noupdate)
{
	if(!searchok) return;
	searchok = false;

	var query = '';

	// Add the number of shown records (if it's not 6, the default)
	var show = document.getElementById('search-show');
	var sh = show ? Number(show.options[show.selectedIndex].value) : 6;
	if(sh !== 6) query += 'show=' + sh;
	
	// Add in the textual query (if there is one)
	var q = document.getElementById('search-field');
	if(q && q.value.length > 0){
		if(query.length > 0) query += '&';
		query += 'search=' + encodeURIComponent(q.value);
	}

	// Add in description filter
	var desc = document.getElementById('search-description');
	var de = desc.options[desc.selectedIndex].value;
	if(de.length > 0){
		if(query.length > 0) query += '&';
		query += 'description=' + encodeURIComponent(de);
	}

	// Add in location filter
	var loc = document.getElementById('search-location');
	var lo = loc.options[loc.selectedIndex].value;
	if(lo.length > 0){
		if(query.length > 0) query += '&';
		query += 'location=' + encodeURIComponent(lo);
	}

	// Add in the spatial query (if there is one)
	if(aoi.getLayers().length > 0){
		if(query.length > 0) query += '&';
		var geojson = JSON.stringify(
			aoi.getLayers()[0].toGeoJSON().geometry
		);

		// Prune geojson to 4 significant digits
		geojson = geojson.replace(/\d+\.\d+/g, function(match){
			return Number(match).toFixed(4);
		});

		query += 'aoi=' + encodeURIComponent(geojson);
	}

	// Don't run empty queries
	if(query.length === 0){
		skiphash = true;
		window.location.hash = '';
		var results = document.getElementById('search-results');
		var src = document.getElementById('search-results-control');
		if(src) src.style.display = 'none';
		if(results) results.innerHTML = '';
		features.clearLayers();
		searchok = true;
		return;
	}

	// This query is dirty if it's different from the previous
	// query (not counting the page we're on)
	var dirty = query !== last && last !== null ? true : false;

	var page = document.getElementById('search-page');
	var pg = dirty ? 0 : page.value;

	if(!dirty && typeof back === 'boolean'){
		pg = Math.max(0, Number(page.value) + (back ? -1 : 1));
	}

	// Reset selected if the query is dirty
	if(dirty) updateSelected(true);

	var params = query + '&page=' + pg;
		
	var xhr = (window.ActiveXObject ? new ActiveXObject('Microsoft.XMLHTTP') : new XMLHttpRequest());
	xhr.onreadystatechange = function(){
		if(xhr.readyState === 4){
			if(xhr.status === 200){
				var obj = JSON.parse(xhr.responseText);
				var results = document.getElementById('search-results');

				// If there are results, or the query is "dirty"
				// commit the query and the current page and update
				// the current query hash
				if(obj.length > 0 || dirty){
					page.value = pg;
					last = query;
					if(noupdate !== true){
						skiphash = true;
						window.location.hash = params;
					}
				}

				var src = document.getElementById('search-results-control');
				if(obj.length < 1){
					searchok = true;

					// If there's no results, and the query
					// isn't dirty, stop right here
					if(!dirty) return;

					// On the other hand, if the query is dirty
					// and there are no results, go ahead
					// and hide all the search controls
					if(src) src.style.display = 'none';
					if(results){
						results.innerHTML = '<div class="noresults">' +
							'<span>No results found.</span>' +
							'</div>';
					}

					return;
				}

				if(src) src.style.display = 'block';

				features.clearLayers();
				for(var i = 0; i < obj.length; i++){
					var o = obj[i];
					if(isSelected(o['ID'])) obj[i].selected = true;

					if('geoJSON' in o){
						// Clone the geojson object, allowing
						// the original reference to be freed
						var geojson = {
							type: 'Feature',
							properties: { color: '#9f00ff' },
							geometry: JSON.parse(JSON.stringify(o['geoJSON']))
						};
						features.addData(geojson);
					}
				}

				var map_el = document.getElementById('map');
				if(map_el) map_el.style.display = 'block';

				var tmpl = document.getElementById('tmpl-search');
				if(results && tmpl){
					results.innerHTML = Mustache.render(
						document.getElementById('tmpl-search').innerHTML,
						obj
					);

					var lx = results.getElementsByTagName('a');
					for(var i = 0; i < lx.length; i++){
						lx[i].onclick = function(){
							var id = Number(this.getAttribute('data-image-id'));
							if(this.className !== 'selected'){
								this.className = 'selected';
								selectAdd(id);
							} else {
								this.className = '';
								selectDel(id);
							}
							updateSelected();
						};
					}
				}
			}
			searchok = true;
		}
	};
	xhr.open('POST', 'search.json', true);
	xhr.setRequestHeader('Content-type', 'application/x-www-form-urlencoded');
	xhr.send(params);
}


function decodeParameters(params)
{
	if(params.length < 1) return;

	var kvs = params.split('&');
	for(var i = 0; i < kvs.length; i++){
		var idx = kvs[i].indexOf('=');
		var k = kvs[i].substring(0, idx);
		var v = decodeURIComponent(kvs[i].substring(idx + 1));

		switch(k){
			case 'aoi':
				aoi.addData(JSON.parse(v));
			break;

			case 'page':
				var page = document.getElementById('search-page');
				if(page) page.value = v;
			break;

			case 'search':
				var q = document.getElementById('search-field');
				if(q) q.value = v;
			break;

			case 'show':
				var show = document.getElementById('search-show');
				if(show){
					for(var j = 0; j < show.options.length; j++){
						if(show.options[j].value === v){
							show.options[j].selected = true;
							break;
						}
					}
				}
			break;

			case 'location':
				var loc = document.getElementById('search-location');
				if(loc){
					for(var j = 0; j < loc.options.length; j++){
						if(loc.options[j].value === v){
							loc.options[j].selected = true;
							break;
						}
					}
				}
			break;

			case 'description':
				var desc = document.getElementById('search-description');
				if(desc){
					for(var j = 0; j < desc.options.length; j++){
						if(desc.options[j].value === v){
							desc.options[j].selected = true;
							break;
						}
					}
				}
			break;
		}
	}
}


function selectToggleAll()
{
	var results = document.getElementById('search-results');
	if(!results) return;

	var count = 0;
	var total = 0;

	var lx = results.getElementsByTagName('a');
	for(var i = 0; i < lx.length; i++){
		if(lx[i].className === 'selected') ++count;
		if(lx[i].getAttribute('data-image-id')) ++total;
	}

	var doselect = (count === total ? false : true);

	for(var i = 0; i < lx.length; i++){
		var id = Number(lx[i].getAttribute('data-image-id'));
		if(id){
			if(doselect) selectAdd(id)
			else selectDel(id);
			lx[i].className = (doselect ? 'selected' : '');
		}
	}
	updateSelected();
}


function selectAdd(id)
{
	// Reminder: Array.indexOf isn't supported until IE9
	var found = false;
	for(var i = 0; i < selected.length; i++){
		if(id === selected[i]){
			found = true;
			break;
		}
	}
	
	if(!found) selected.push(id);
}


function selectDel(id)
{
	// Reminder: Array.indexOf isn't supported until IE9
	for(var i = 0; i < selected.length; i++){
		if(selected[i] === id){
			selected.splice(i, 1);
			return;
		}
	}
}


function isSelected(id)
{
	if('indexOf' in selected){
		if(selected.indexOf(id) >= 0) return true;
		return false;
	} else {
		for(var i = 0; i < selected.length; i++){
			if(selected[i] === id) return true;
		}
		return false;
	}
}


function updateSelected(empty)
{
	if(typeof empty === 'boolean' && empty) selected = [];

	var sel = document.getElementById('search-selected-control');
	if(selected.length < 1){
		if(sel) sel.style.display = 'none';
	} else {
		if(sel) sel.style.display = 'block';
		var stat = document.getElementById('selected-status');
		if(stat) stat.innerHTML = selected.length + ' images selected';
	}
}


function deleteSelected()
{
	var ok = confirm('Delete ' + selected.length + ' image(s)?');
	if(!ok) return;

	var xhr = (window.ActiveXObject ? new ActiveXObject('Microsoft.XMLHTTP') : new XMLHttpRequest());
	xhr.onreadystatechange = function(){
		if(xhr.readyState === 4){
			if(xhr.status === 200){
				var obj = JSON.parse(xhr.responseText);
				if(obj['success']){
					selected = [];
					var page = document.getElementById('search-page');
					if(page) page.value = '0';
					search();
					return;
				}
			}
			alert(xhr.responseText);
		}
	};
	xhr.open('POST', 'image.json', true);
	xhr.setRequestHeader('Content-type', 'application/x-www-form-urlencoded');
	xhr.send('action=delete&ids=' + selected.join(','));
}
