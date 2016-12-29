var map, project_control, aoi;
var last_query;

var popup_properties = {
	maxWidth: 240, maxHeight: 320,
	autoPanPadding: L.point(230,25)
};

var baselayers = {
	'Open Street Maps Monochrome': L.tileLayer(
		'http://{s}.tiles.wmflabs.org/bw-mapnik/{z}/{x}/{y}.png',
		{ minZoom: 3, maxZoom: 18, zIndex: 1 }
	),
	'Open Street Maps': new L.TileLayer(
		'//{s}.tile.openstreetmap.org/{z}/{x}/{y}.png',
		{ minZoom: 3, maxZoom: 19, zIndex: 2 }
	),
	'OpenTopoMap': new L.TileLayer(
		'http://{s}.tile.opentopomap.org/{z}/{x}/{y}.png',
		{ minZoom: 3, maxZoom: 17, zIndex: 3  }
	),
	'ESRI Imagery': new L.TileLayer(
		'//server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}',
		{ minZoom: 3, maxZoom: 19, zIndex: 4 }
	),
	'ESRI Topographic': new L.TileLayer(
		'//server.arcgisonline.com/ArcGIS/rest/services/World_Topo_Map/MapServer/tile/{z}/{y}/{x}',
		{ minZoom: 3, maxZoom: 19, zIndex: 5 }
	),
	'ESRI Shaded Relief': new L.TileLayer(
		'//server.arcgisonline.com/ArcGIS/rest/services/World_Shaded_Relief/MapServer/tile/{z}/{y}/{x}',
		{ minZoom: 3, maxZoom: 13, zIndex: 6 }
	),
	'ESRI DeLorme': new L.TileLayer(
		'//server.arcgisonline.com/ArcGIS/rest/services/Specialty/DeLorme_World_Base_Map/MapServer/tile/{z}/{y}/{x}',
		{ minZoom: 3, maxZoom: 11, zIndex: 7 }
	),
	'ESRI National Geographic': new L.TileLayer(
		'//server.arcgisonline.com/ArcGIS/rest/services/NatGeo_World_Map/MapServer/tile/{z}/{y}/{x}',
		{ minZoom: 3, maxZoom: 16, zIndex: 8 }
	),
	'GINA Satellite': new L.TileLayer(
		'http://tiles.gina.alaska.edu/tilesrv/bdl/tile/{x}/{y}/{z}',
		{ minZoom: 3, mazZoom: 15, zIndex: 9 }
	),
	'GINA Topographic': new L.TileLayer(
		'http://tiles.gina.alaska.edu/tilesrv/drg/tile/{x}/{y}/{z}',
		{ minZoom: 3, maxZoom: 12, zIndex: 10 }
	),
	'Stamen Watercolor': new L.TileLayer(
		'http://stamen-tiles-{s}.a.ssl.fastly.net/watercolor/{z}/{x}/{y}.png',
		{ minZoom: 3, maxZoom: 16, subdomains: 'abcd', zIndex: 11 }
	)
};

var overlays = {
	'PLSS (BLM)': new L.tileLayer.wms(
		'http://www.geocommunicator.gov/arcgis/services/PLSS/MapServer/WMSServer',
		{
			'layers': '1,2,3,4,5,6,7,8,9,10,11,12,13',
			transparent: true,
			format: 'image/png',
			minZoom: 3, maxZoom: 16, zIndex: 12
		}
	),
	'Quadrangles': new L.TileLayer(
		'http://tiles.gina.alaska.edu/tilesrv/quad_google/tile/{x}/{y}/{z}',
		{ minZoom: 3, maxZoom: 16, zIndex: 13 }
	)
};

L.drawLocal.draw.toolbar.buttons.marker = 'Select Project Data';
L.drawLocal.draw.handlers.marker.tooltip.start = 'Click map to download project-level data';
L.drawLocal.draw.toolbar.buttons.rectangle = 'Select Area of Interest';

L.Control.HelpControl = L.Control.extend({
	options: {
		position: 'bottomright',
		url: '#'
	},
	initialize: function(options){
		L.Util.setOptions(this, options);
	},
	onAdd: function(map){
		var container = L.DomUtil.create('div', 'help-control-container leaflet-bar');
		var a = L.DomUtil.create('a', 'help-control-link', container);
		a.href = this.options.url;
		a.target = '_blank';
		a.title = 'Open Help';

		var img = L.DomUtil.create('img', 'help-control-image', a);
		img.src = 'img/help-2x.png';

		return container;
	}
});
L.control.helpControl = function(options){
	return new L.Control.HelpControl(options);
};


L.Control.ProjectControl = L.Control.extend({
	options: {
		position: 'topleft',
		debug: false,
		projects: []
	},

	initialize: function(options){
		var services = {};
		var projects = options.projects;
		for(var i = 0; i < projects.length; i++){
			var datasets = projects[i].datasets;
			for(var j = 0; j < datasets.length; j++){
				if('service' in datasets[j]){
					if(datasets[j].service in services){
						services[datasets[j].service] += ',' + datasets[j].layers;
					} else {
						services[datasets[j].service] = datasets[j].layers;
					}
				}
			}
		}

		var zindex = 14;
		this._displayLayers = {}
		for(var k in services){
			this._displayLayers[k] = L.tileLayer.wms(k, {
				'layers': services[k],
				'transparent': true,
				'format': 'image/png8',
				'maxZoom': 19,
				'zIndex': zindex++
			});
		}

		L.Util.setOptions(this, options);
	},

	updateLayers: function()
	{
		var services = {};

		var boxes = this._container.getElementsByTagName('input');
		for(var i = 0; i < boxes.length; i++){
			if(boxes[i].checked){
				var service = boxes[i].getAttribute('data-service');
				var layers = boxes[i].getAttribute('data-layers');

				if(service in services) services[service] += ',' + layers;
				else services[service] = layers;
			}
		}

		for(var k in this._displayLayers){
			if(k in services){
				this._displayLayers[k].setParams({layers: services[k]}, false);
				map.addLayer(this._displayLayers[k]);
			} else {
				map.removeLayer(this._displayLayers[k]);
			}
		}
	},

	onAdd: function(map){
		for(var k in this._displayLayers){
			map.addLayer(this._displayLayers[k]);
		}

		var container = document.createElement('div');
		container.className = 'project-control-container';

		container.innerHTML = Mustache.render(
			document.getElementById('tmpl-projects').innerHTML, 
			this.options.projects
		);

		L.DomEvent.disableClickPropagation(container);
		L.DomEvent.disableScrollPropagation(container);

		return container;
	},

	onViewChange: function()
	{
		var cur = this._map.getBounds();

		var projects = this.options.projects;
		for(var i = 0; i < projects.length; i++){
			// Cache project element in array
			if(!('el' in projects[i])){
				projects[i].el = document.getElementById(
					'project_' + projects[i].ID
				);
			}
			var shown = 0;

			var datasets = projects[i].datasets;
			for(var j = 0; j < datasets.length; j++){
				// Cache dataset element in array
				if(!('el' in datasets[j])){
					datasets[j].el = document.getElementById(
						'dataset_' + datasets[j].ID
					);

					// While we're at it, cache bounds
					datasets[j]['bounds'] = L.latLngBounds(
						L.latLng(datasets[j].YMax, datasets[j].XMax),
						L.latLng(datasets[j].YMin, datasets[j].XMin)
					);

				}

				if(cur.overlaps(datasets[j].bounds)){
					datasets[j].el.style.display = 'block';
					++shown;
				} else {
					datasets[j].el.style.display = 'none';
				}
			}

			if(shown > 0) projects[i].el.style.display = 'block';
			else projects[i].el.style.display = 'none';
		}
	},

	hideAll: function(){ this.toggleAll(false); },
	showAll: function(){ this.toggleAll(true); },
	toggleAll: function(state)
	{
		var changed = false;
		var boxes = this._container.getElementsByTagName('input');
		for(var i = 0; i < boxes.length; i++){
			if(boxes[i].checked != state){
				boxes[i].checked = state;
				changed = true;
			}
		}
		if(changed) this.updateLayers();
	}
});

L.control.projectControl = function(options){
	return new L.Control.ProjectControl(options);
};


function init()
{
	aoi = L.geoJson(null, {
		style: {
			color: '#f00',
			opacity: 1,
			weight: 2,
			radius: 6,
			fill: false,
			interactive: false,
			'z-index': 99
		}
	});
	aoi.on('popupclose', function(){ aoi.clearLayers(); });

	// Default start point
	var start_lat = 65.014496;
	var start_lon = -155.478515;
	var start_zoom = 4;

	// Read hash location, if available
	var hash = window.location.hash.substr(1).split(':');
	if(hash.length === 3){
		start_lat = Number(hash[0]);
		start_lon = Number(hash[1]);
		start_zoom = Number(hash[2]);
	}

	map = L.map('map', {
		closePopupOnClick: false,
		worldCopyJump: true,
		attributionControl: false,
		zoomControl: false,
		minZoom: 3,
		maxZoom: 19,
		center: L.latLng(start_lat, start_lon),
		zoom: start_zoom,
		layers: [
			baselayers['Open Street Maps Monochrome'],
			aoi
		]
	});

	// Add search control
	map.addControl(L.control.searchControl({
		placeholder: 'Search for project names, counties, zip codes',
		position: 'topleft',
		onSubmit: function(e){
			var q = document.getElementById('q').value.trim();
			if(q.length == 0){
				var sr = document.getElementById('search-results');
				while(sr.lastChild){ sr.removeChild(sr.lastChild); }
				return;
			}

			var xhr = (window.ActiveXObject ? new ActiveXObject('Microsoft.XMLHTTP') : new XMLHttpRequest());
			xhr.onreadystatechange = function(){
				if(xhr.readyState === 4){
					if(xhr.status == 200){
						var sr = document.getElementById('search-results');
						while(sr.lastChild){ sr.removeChild(sr.lastChild); }

						var obj = JSON.parse(xhr.responseText);
						if('suggestions' in obj){
							var close = document.createElement('a');
							close.href = '#close';
							close.appendChild(document.createTextNode('\xD7'));
							close.className = 'closeButton';
							close.onclick = function(e){
								var sr = document.getElementById('search-results');
								while(sr.lastChild){ sr.removeChild(sr.lastChild); }

								if('preventDefault' in e) e.preventDefault();
								return false;
							};
							sr.appendChild(close);

							if(obj.suggestions.length == 0){
								var div = document.createElement('div');
								div.className = 'notfound';
								div.appendChild(document.createTextNode('No results found.'));
								sr.appendChild(div);
								return;
							}

							for(var i = 0; i < obj.suggestions.length; i++){
								var a = document.createElement('a');
								a.href = '#showlocation';
								a.appendChild(document.createTextNode(
									obj.suggestions[i].text
								));
								a.onclick = function(){
									var magickey = obj.suggestions[i].magicKey;
									return function(){
										moveToKey(magickey);

										if('preventDefault' in e) e.preventDefault();
										return false;
									};
								}();
								sr.appendChild(a);
							}
						}
					}
				}
			};

			var url = geocode_url + '/suggest?f=json&maxSuggestions=10&text=';
			url += encodeURIComponent(q);

			xhr.open('GET', url, true);
			xhr.send();

			if('preventDefault' in e) e.preventDefault();
			return false;
		}
	}));

	// Add zoom controls
	map.addControl(L.control.zoom({ position: 'topright' }));

	// Add mouse position control
	map.addControl(L.control.mousePosition({
		emptyString: 'Unknown', numDigits: 4
	}));

	// Add draw control
	map.addControl(
		new L.Control.Draw({
			position: 'topright',
			draw: {
				polygon: false, polyline: false,
				circle: false,
				rectangle: {
					showArea: false,
					shapeOptions: {
						color: '#f00',
						opacity: 1,
						weight: 2,
						radius: 6,
						fill: false,
						interactive: false
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

	// Add help button
	map.addControl(L.control.helpControl({
		position: 'topright',
		url: 'help.html'
	}));

	// Add layer control
	map.addControl(L.control.layers(
		baselayers, overlays, {
			position: 'bottomright', autoZIndex: false
		}
	));

	// Add scale control
	map.addControl(L.control.scale());

	map.on('draw:drawstart', function(e){
		aoi.closePopup();
		aoi.clearLayers();
	});
	map.on('draw:created', function(e){
		aoi.addLayer(e.layer);
	});
	map.on('draw:drawstop', function(e){
		if(aoi.getLayers().length > 0){
			var geojson = JSON.stringify(
				aoi.getLayers()[0].toGeoJSON().geometry
			);
			queryDatasets(geojson);
		}
	});

	// Load projects json and project control
	var xhr = (window.ActiveXObject ? new ActiveXObject('Microsoft.XMLHTTP') : new XMLHttpRequest());
	xhr.onreadystatechange = function(){
		if(xhr.readyState === 4){
			if(xhr.status == 200){
				var projects = JSON.parse(xhr.responseText);

				project_control = L.control.projectControl({
					'projects': projects,
					'debug': false
				});
				map.addControl(project_control);

				var viewchangewrapper = function(){
					// Update layer visibility based on checkboxes
					project_control.onViewChange();

					// Update the hash for the current view
					var ll = map.getCenter();
					window.location.hash = (
						ll.lat.toFixed(5) + ':' + ll.lng.toFixed(5) +
						':' + map.getZoom()
					);
				};
				map.on('moveend', viewchangewrapper);
				map.on('zoomend', viewchangewrapper);

				project_control.onViewChange();

				document.getElementById('q').focus();
			}
		}
	};
	xhr.open('GET', 'project', true);
	xhr.send();
}


// Query available datasets based on a geojson string
function queryDatasets(geojson)
{
	// Do nothing if the project control hasn't been initialized
	if(project_control === undefined) return;

	aoi.closePopup();

	// Prune geojson to 4 significant digits
	geojson = geojson.replace(/\d+\.\d+/g, function(match){
		return Number(match).toFixed(4);
	});

	var xhr = (window.ActiveXObject ? new ActiveXObject('Microsoft.XMLHTTP') : new XMLHttpRequest());
	xhr.onreadystatechange = function(){
		if(xhr.readyState === 4){
			if(xhr.status !== 200){
				alert('Error: ' + xhr.responseText);
			} else {
				var obj = JSON.parse(xhr.responseText);
				last_query = {};

				var ll;
				if(aoi.getLayers().length > 0){
					ll = aoi.getBounds().getCenter();
				} else {
					ll = map.getBounds().getCenter();
					ll.lat = (ll.lat + map.getBounds().getSouth()) / 2;
				}

				if(obj.length === 0){
					aoi.bindPopup(
						"Sorry, no data exists for the area you've selected. " +
						"Please try again.",
						popup_properties
					);
				} else {
					var proj = null, pul, md_enable = false;

					var div = L.DomUtil.create('div', 'dataset-popup-content');
					div.appendChild(document.createTextNode(
						"Below are the datasets available in the area you've " +
						"selected. Please scroll and choose one or more from " +
						"the list."
					));

					var ul = L.DomUtil.create('ul', 'dataset-popup-project-list', div);

					// Build a select all / deselect all control
					// if there are more than 7 options
					if(obj.length > 7){
						var ctl_li = L.DomUtil.create('li', 'dataset-popup-control', ul);
						var a_sel_all = L.DomUtil.create('a', null, ctl_li);
						a_sel_all.href = '#';
						a_sel_all.onclick = function(e){
							var els = aoi.getPopup().getContent().getElementsByTagName('input');
							for(var i = 0; i < els.length; i++){
								els[i].checked = true;
							}
							updatePopup();

							if(e.preventDefault) e.preventDefault();
							return false;
						};
						a_sel_all.appendChild(document.createTextNode(
							'Select All'
						));

						ctl_li.appendChild(document.createTextNode(' / '));

						var a_de_all = L.DomUtil.create('a', null, ctl_li);
						a_de_all.href = '#';
						a_de_all.onclick = function(e){
							var els = aoi.getPopup().getContent().getElementsByTagName('input');
							for(var i = 0; i < els.length; i++){
								els[i].checked = false;
							}
							updatePopup();

							if(e.preventDefault) e.preventDefault();
							return false;
						};
						a_de_all.appendChild(document.createTextNode(
							'Deselect All'
						));
					}

					for(var i = 0; i < obj.length; i++){
						last_query[obj[i].dataset_id] = {
							bytes: obj[i].bytes,
							files: obj[i].files
						};

						//var enabled = obj[i].dataset_name == 'Metadata' && md_enable ? true : false;
						enabled = obj[i].dataset_name !== 'Point Cloud' ? true : false;

						// Scan the projects. Is this dataset checked?
						// If this dataset's project has "metadata" go
						// ahead and enable that, too.
						var projects = project_control.options.projects;

						/*
						for(var j = 0; j < projects.length; j++){
							var datasets = projects[j].datasets;
							for(var k = 0; k < datasets.length; k++){
								if(datasets[k].ID == obj[i].dataset_id && datasets[k].el !== undefined){
									var els = datasets[k].el.getElementsByTagName('input');
									if(els.length > 0 && els[0].checked){
										enabled = true;
										md_enable = true;
									}
								}
							}
						}
						*/

						// Create a header for the project,
						// if the project has changed
						if(obj[i].project_name !== proj){
							var li = L.DomUtil.create(
								'li', 'dataset-popup-project-item', ul
							);
							li.appendChild(document.createTextNode(
								obj[i].project_name
							));
							pul = L.DomUtil.create(
								'ul', 'dataset-popup-dataset-list', li
							);

							proj = obj[i].project_name;
							md_enable = false;
						}

						var li = L.DomUtil.create(
							'li', 'dataset-popup-dataset-item', pul
						);

						var chk = document.createElement('input');
						chk.type = 'checkbox';
						chk.checked = enabled;
						chk.onchange = updatePopup;
						chk.id = 'popup-checkbox-' + obj[i].dataset_id;
						li.appendChild(chk);

						var lbl = L.DomUtil.create(
							'label', 'dataset-popup-dataset-label', li
						);
						lbl.htmlFor = 'popup-checkbox-' + obj[i].dataset_id;
						lbl.appendChild(document.createTextNode(
							obj[i].dataset_name
						));
					}

					L.DomUtil.create('div', 'dataset-popup-download', div);

					aoi.bindPopup(div, popup_properties);
					updatePopup();
				}
				aoi.openPopup(ll);
			}
		}
	}
	xhr.open('POST', 'query', true);
	xhr.setRequestHeader('Content-Type', 'application/x-www-form-urlencoded');
	xhr.send('geojson=' + encodeURIComponent(geojson));
}


// Updates the contents of the text for the dataset popup
function updatePopup()
{
	var el = aoi.getPopup().getContent();
	var chks = el.getElementsByTagName('input');
	var dldl = el.getElementsByClassName('dataset-popup-download')[0];

	var files = 0;
	var bytes = 0;

	var ids = [];
	for(var i = 0; i < chks.length; i++){
		if(chks[i].checked){
			var id = Number(chks[i].id.substr(15));
			ids.push(id);
			files += last_query[id].files;
			bytes += last_query[id].bytes;
		}
	}

	var geojson;
	if(aoi.getLayers().length > 0){
		geojson = JSON.stringify(
			aoi.getLayers()[0].toGeoJSON().geometry
		);
	} else {
		geojson = JSON.stringify(
			L.rectangle(map.getBounds()).toGeoJSON().geometry
		);
	}

	// Prune geojson to 4 significant digits
	geojson = geojson.replace(/\d+\.\d+/g, function(match){
		return Number(match).toFixed(4);
	});

	while(dldl.lastChild) dldl.removeChild(dldl.lastChild);

	if(files > 0){
		var a = document.createElement('a');
		a.href = 'download?geojson='
			+ encodeURIComponent(geojson)
			+ "&ids="
			+ encodeURIComponent(ids.join(','));
		a.appendChild(document.createTextNode('Download'));
		dldl.appendChild(a);

		dldl.appendChild(document.createTextNode(
			'Your custom download is ready. It is ' +
			'composed of ' + numberWithCommas(files) +
			' files and is ' + sizeToHumanReadable(bytes)
		));
	}
}


// Formats numbers with a comma after every three digits
function numberWithCommas(x)
{
	return x.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ",");
}


// Prints bytes in a human readable format
function sizeToHumanReadable(filesize)
{
	var i = -1;
	var units = ['kB','MB','GB','TB','PB','EB','ZB','YB'];
	do {
		filesize = filesize / 1024; i++;
	} while (filesize > 1024);
	return Math.max(filesize, 0.1).toFixed(2) + units[i];
}

function moveToKey(magickey)
{
	var sr = document.getElementById('search-results');
	while(sr.lastChild){ sr.removeChild(sr.lastChild); }

	var xhr = (window.ActiveXObject ? new ActiveXObject('Microsoft.XMLHTTP') : new XMLHttpRequest());
	xhr.onreadystatechange = function(){
		if(xhr.readyState === 4){
			if(xhr.status == 200){
				var obj = JSON.parse(xhr.responseText);
				if('candidates' in obj && obj.candidates.length > 0){
					var c = obj.candidates[0];
					document.getElementById('q').value = c.address;
					if(c.attributes.Xmin == 0){
						map.setView(
							L.latLng(c.location.y, c.location.x), 9
						);
					} else {
						map.fitBounds(
							L.latLngBounds(
								L.latLng(c.attributes.Ymax, c.attributes.Xmax),
								L.latLng(c.attributes.Ymin, c.attributes.Xmin)
							)
						);
					}
				}
			}
		}
	};

	var url = geocode_url + '/findAddressCandidates?f=json&outFields=*&magicKey=';
	url += encodeURIComponent(magickey);

	xhr.open('GET', url, true);
	xhr.send();
}


function showExtent(ymax, xmax, ymin, xmin)
{
	map.fitBounds(
		L.latLngBounds(
			L.latLng(ymax, xmax),
			L.latLng(ymin, xmin)
		)
	);
}


function downloadView()
{
	var geojson = JSON.stringify(
		L.rectangle(map.getBounds()).toGeoJSON().geometry
	);
	queryDatasets(geojson);
}
