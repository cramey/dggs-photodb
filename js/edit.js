var map, features;

function init()
{
	// Fix for Leaflet Issue #5180
	// See: https://github.com/Leaflet/Leaflet/issues/5180
	if(!L.Browser.mobile) L.Browser.touch = false;

	// Disable bfcache (firefox compatibility)
	if('onunload' in window){
		window.onunload = function(){};
	}

	var summary = document.getElementById('summary');
	if(summary){
		var f = function(){
			var l = document.getElementById('summary-limit');
			if(!this.disabled){
				var m = (100 - summary.value.length);
				l.innerHTML = ('(' + (m > 0 ? '+' : '') + m + ')');
				l.style.color = m >= 0 ? '#080' : '#f00';
			} else {
				l.innerHTML = '';
			}
		};
		summary.onchange = f;
		summary.onkeyup = f;
		summary.onchange();
	}

	var save = document.getElementById('button-save');
	if(save) save.onclick = saveImage;

	var del = document.getElementById('button-delete');
	if(del) del.onclick = deleteImage;

	var dis = document.getElementsByTagName('a');
	for(var i = 0; i < dis.length; i++){
		var forid = dis[i].getAttribute('data-for-id');
		if(forid){
			var el = document.getElementById(forid);
			if(el){
				var state = el.disabled;
				dis[i].innerHTML = (state ? 'Enable' : 'Disable') + ' editing';
				dis[i].style.color = '#' + (state ? 'f00' : '080');
				dis[i].onclick = toggleEnabled;
			}
		}
	}

	features = L.geoJson();
	features.on('layeradd', function(e){
		updateLonLat(e.layer);

		if(!('dragging' in e.layer)){
			e.layer.options.draggable = true;
			e.layer.options.keyboard = false;
		}

		e.layer.on('dblclick', function(e){
			features.removeLayer(this);
		});
		e.layer.on('dragend', function(e){
			updateLonLat(this);
		});
	});
	features.on('layerremove', function(e){
		updateLonLat();
	});
	if(geojson) features.addData(geojson);

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
			features
		]
	});

	// Add zoom control
	map.addControl(L.control.zoom({ position: 'topleft' }));

	// Add mouse position control
	map.addControl(L.control.mousePosition({
		emptyString: 'Unknown', numDigits: 4
	}));

	// Add scale bar
	map.addControl(L.control.scale({ position: 'bottomleft' }));

	// Add layer control
	map.addControl(L.control.layers(
		baselayers, overlays, {
			position: 'bottomright', autoZIndex: false
		}
	));

	map.on('click', function(e){
		if(features.getLayers().length < 1){
			features.addLayer(L.marker(e.latlng, {
				draggable: true, keyboard: false
			}));
		}
	});
}


function toggleEnabled()
{
	var forid = this.getAttribute('data-for-id');
	var el = document.getElementById(forid);
	if(el){
		var state = el.disabled;
		this.innerHTML = (state ? 'Disable' : 'Enable') + ' editing';
		this.style.color = '#' + (state ? '080' : 'f00');
		el.disabled = !state;
		if(typeof el.onchange === 'function') el.onchange();
	}
}


function saveImage()
{
	var button = document.getElementById('button-save');
	if(button) button.innerHTML = 'Saving.. ';

	var FIELDS = ['ids', 'taken', 'credit', 'summary', 'description', 'tags', 'accuracy', 'ispublic'];

	var params = '';
	for(var i = 0; i < FIELDS.length; i++){
		var el = document.getElementById(FIELDS[i]);
		if(!el) continue;

		switch(el.tagName){
			case 'TEXTAREA':
			case 'INPUT':
				if(el.value.length > 0 && !el.disabled){
					if(params.length > 0) params += '&';
					params += FIELDS[i] + '=';
					params += encodeURIComponent(el.value);
				}
			break;

			case 'SELECT':
				for(var j = 0; j < el.options.length; j++){
					if(el.options[j].selected && !el.disabled){
						if(params.length > 0) params += '&';
						params += FIELDS[i] + '=';
						params += encodeURIComponent(el.options[j].value);
					}
				}
			break;
		}
	}

	var flayers = features.getLayers();
	if(flayers.length > 0){
		if(params.length > 0) params += '&';
		params += 'geojson=';
		params += encodeURIComponent(
			JSON.stringify(flayers[0].toGeoJSON().geometry)
		);
	}

	var xhr = (window.ActiveXObject ? new ActiveXObject('Microsoft.XMLHTTP') : new XMLHttpRequest());
	xhr.onreadystatechange = function(){
		if(xhr.readyState === 4){
			if(xhr.status === 200){
				var obj = JSON.parse(xhr.responseText);
				if(obj['success']){
					if(button) button.innerHTML = 'Save Complete';
					setTimeout(function(){
						var button = document.getElementById('button-save');
						if(button) button.innerHTML = 'Save Changes';
					}, 5000);
					return;
				}
			}
			alert('Saving failed:\n' + xhr.responseText);
			if(button) button.innerHTML = 'Save Changes';
		}
	};
	xhr.open('POST', '../image.json', true);
	xhr.setRequestHeader('Content-type', 'application/x-www-form-urlencoded');
	xhr.send(params);
}

function deleteImage()
{
	var ids = document.getElementById('ids');
	if(!ids) return;

	var c = ids.value.split(',').length;
	var ok = confirm('Delete ' + c + ' image(s)?');
	if(!ok) return;

	var xhr = (window.ActiveXObject ? new ActiveXObject('Microsoft.XMLHTTP') : new XMLHttpRequest());
	xhr.onreadystatechange = function(){
		if(xhr.readyState === 4){
			if(xhr.status === 200){
				var obj = JSON.parse(xhr.responseText);
				if(obj['success']){
					window.location = '../';
					return;
				}
			}
			alert(xhr.responseText);
		}
	};
	xhr.open('POST', '../image.json', true);
	xhr.setRequestHeader('Content-type', 'application/x-www-form-urlencoded');
	xhr.send('action=delete&ids=' + ids.value);
}


function updateLonLat(layer)
{
	var el = document.getElementById('lonlat');
	if(!el) return;

	if(typeof layer !== 'undefined'){
		var ll = layer.getLatLng();
		el.innerHTML = ll.lng.toFixed(5) + ', ' + ll.lat.toFixed(5);
	} else {
		el.innerHTML = 'None';
	}
}

