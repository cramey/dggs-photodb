<%@
	page trimDirectiveWhitespaces="true"
%><%@
	taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"
%><%@
	taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions"
%><%@
	taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"
%><!DOCTYPE html>
<html lang="en">
	<head>
		<meta charset="utf-8">
		<meta http-equiv="x-ua-compatible" content="IE=edge">
		<title>Alaska Division of Geological &amp; Geophysical Surveys Photo Database</title>
		<link rel="stylesheet" href="../css/apptmpl.min.css">
		<link rel="stylesheet" href="../js/leaflet.css">
		<link rel="stylesheet" href="../js/leaflet.mouseposition.css">
		<style>
			.apptmpl-container { min-width: 500px !important; }
			a img { border: none; }
			dl { margin: 0; padding: 0; }
			dt { font-weight: bold; }
			dd { margin: 0 0 8px 16px; }
			#image {
				width: 55%;
				float: right;
				margin: 0 0 8px 8px;
			}
			#map {
				display: inline-block;
				margin: 16px 0 0 0;
				height: 300px;
				width: 43%;
			}
		</style>
	</head>
	<body onload="init()">
		<div class="apptmpl-container">
			<div class="apptmpl-goldbar">
				<a class="apptmpl-goldbar-left" href="http://alaska.gov"></a>
				<span class="apptmpl-goldbar-right"></span>

				<a href="help/">Help</a>
			</div>

			<div class="apptmpl-banner">
				<a class="apptmpl-banner-logo" href="http://dggs.alaska.gov"></a>
				<div class="apptmpl-banner-title">Photo Database</div>
				<div class="apptmpl-banner-desc">Alaska Division of Geological &amp; Geophysical Surveys</div>
			</div>

			<div class="apptmpl-breadcrumb">
				<a href="http://alaska.gov">State of Alaska</a> &gt;
				<a href="http://dnr.alaska.gov">Natural Resources</a> &gt;
				<a href="http://dggs.alaska.gov">Geological &amp; Geophysical Surveys</a> &gt;
				<a href="..">Photos</a>
			</div>

			<div class="apptmpl-content">
				<a href="../image/${image.ID}">
					<img id="image" title="${image.filename}" src="../image/${image.ID}">
				</a>

				<dl>
					<c:if test="${!empty image.summary}">
					<dt>Title</dt>
					<dd>${image.summary}</dd>
					</c:if>

					<c:if test="${!empty image.credit}">
					<dt>Credit</dt>
					<dd>${image.credit}</dd>
					</c:if>

					<c:if test="${!empty image.description}">
					<dt>Description</dt>
					<dd>${image.description}</dd>
					</c:if>

					<c:if test="${!empty image.taken}">
					<dt>Taken</dt>
					<dd><fmt:formatDate pattern="M/d/yyyy" value="${image.taken}"/></dd>
					</c:if>

					<c:if test="${!empty image.tags}">
					<dt>Tagged</dt>
					<dd><c:forEach items="${image.tags}" var="tag" varStatus="stat"><c:if test="${stat.count != 1}">,&nbsp;</c:if><a href="../search#search=${tag.name}">${tag.name}</a></c:forEach></dd>
					</c:if>
				</dl>

				<c:if test="${!empty image.geoJSON}">
				<div id="map"></div>
				</c:if>
				<div style="clear: both"></div>
			</div>
		</div>
		<script src="../js/leaflet.js"></script>
		<script src="../js/leaflet.mouseposition.js"></script>
		<script src="../js/util.js"></script>
		<script>
			var map;
			function init()
			{
				<c:if test="${!empty image.geoJSON}">
				// Fix for Leaflet Issue #5180
				// See: https://github.com/Leaflet/Leaflet/issues/5180
				if(!L.Browser.mobile) L.Browser.touch = false;

				// Disable bfcache (firefox compatibility)
				if('onunload' in window){
					window.onunload = function(){};
				}

				var geojson = ${image.geoJSON};

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
				features.addData(geojson);

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
				</c:if>
			}
		</script>
	</body>
</html>
