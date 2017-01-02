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
		{ minZoom: 3, maxZoom: 17, zIndex: 3 }
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
			layers: '1,2,3,4,5,6,7,8,9,10,11,12,13',
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


function mirroredLayer(data, style)
{
	var layer = L.geoJson(null, {
		coordsToLatLngInvert: function(c){
			var ll = new L.LatLng(c[1], c[0], c[2]);
			if(ll.lng >= 0) ll.lng -= 360;
			else ll.lng += 360;

			return ll;
		},
		style: style
	});
	layer.on('layeradd', function(e){
		if('coordsToLatLng' in e.target.options){
			delete e.target.options.coordsToLatLng;
		} else {
			e.target.options.coordsToLatLng =
				e.target.options.coordsToLatLngInvert;

			// If the layer has an identifable feature, use that
			// otherwise, just break it down to geoJSON
			if('feature' in e.layer) {
				e.target.addData(e.layer.feature);
			} else {
				e.target.addData(e.layer.toGeoJSON());
			}
		}
	});

	if(data != null) layer.addData(data);
	return layer;
}