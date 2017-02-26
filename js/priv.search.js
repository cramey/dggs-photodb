var map, aoi, features;
var selected = [];


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
		'description': document.getElementById('search-description'),
		'location': document.getElementById('search-location'),
		'aoi': aoi
	});

	Search.on('success', function(obj, dirty){
		// Reset selected if the query is dirty
		if(dirty) updateSelected(true);

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
				if(isSelected(o['id'])) o.selected = true;

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

		updateSelected(true);

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

	var search_description = document.getElementById('search-description');
	if(search_description) search_description.onchange = function(){
		Search.execute();	
	};

	var search_location = document.getElementById('search-location');
	if(search_location) search_location.onchange = function(){
		Search.execute();	
	};

	var selected_edit = document.getElementById('selected-edit');
	if(selected_edit) selected_edit.onclick = function(){
		if(selected.length > 0){
			window.location.href = 'edit/' + selected.join(',');
		}
	};

	var selected_spreadsheet = document.getElementById('selected-spreadsheet');
	if(selected_spreadsheet) selected_spreadsheet.onclick = function(){
		if(selected.length > 0){
			window.location.href = 'spreadsheet/' + selected.join(',');
		}
	};

	var selected_delete = document.getElementById('selected-delete');
	if(selected_delete) selected_delete.onclick = deleteSelected;

	var selected_all = document.getElementById('selected-all');
	if(selected_all) selected_all.onclick = selectToggleAll;



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
		if(sel) sel.style.display = 'inline';
		var stat = document.getElementById('selected-status');
		if(stat) stat.innerHTML = selected.length + ' images selected';
	}
}


function deleteSelected()
{
	if(selected.length < 1) return;

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
					Search.execute();
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


function resetSearch()
{
	last = null;

	aoi.clearLayers();
	features.clearLayers();

	var search = document.getElementById('search-field');
	if(search){
		search.value = '';
		search.focus();
	}

	var page = document.getElementById('search-page');
	if(page) page.value = '0';

	var show = document.getElementById('search-show');
	if(show){
		for(var j = 0; j < show.options.length; j++){
			if(show.options[j].value === '6'){
				show.options[j].selected = true;
				break;
			}
		}
	}

	var desc = document.getElementById('search-description');
	if(desc){
		for(var j = 0; j < desc.options.length; j++){
			if(desc.options[j].value === ''){
				desc.options[j].selected = true;
				break;
			}
		}
	}

	var loc = document.getElementById('search-location');
	if(loc){
		for(var j = 0; j < loc.options.length; j++){
			if(loc.options[j].value === ''){
				loc.options[j].selected = true;
				break;
			}
		}
	}

	var src = document.getElementById('search-results-control');
	if(src) src.style.display = 'none';

	var results = document.getElementById('search-results');
	if(results) results.innerHTML = '';
}


function imageError(el)
{
	el.onerror = null;
	el.src = '';

	var a = document.createElement('a');
	a.href = 'javascript:void(0)';
	var img = document.createElement('img');
	img.src = '../css/notfound.gif';
	img.style.width = '256px';
	img.style.height = '167px';
	a.appendChild(img);

	el.parentNode.parentNode.replaceChild(a, el.parentNode);

	return false;
}
