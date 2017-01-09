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
		<title>Alaska Division of Geological &amp; Geophysical Surveys Photos</title>
		<link rel="stylesheet" href="../../css/apptmpl.min.css">
		<link rel="stylesheet" href="../../js/leaflet.css">
		<link rel="stylesheet" href="../../js/leaflet.mouseposition.css">
		<style>
			.apptmpl-container { min-width: 500px !important; }
			.footer { font-size: 12px; }
			fieldset { border: none; }
			label { display: block; font-weight: bold; font-size: 14px; }
			input { margin: 0 0 16px 0; }
			th, td { vertical-align: top; }
			th { text-align: left; padding: 0 16px 0 0; }
			th a { font-size: 12px; }
			#map {
				display: block
				margin: 16px 0 0 0;
				height: 300px;
				width: 400px;
			}
		</style>
		<script>var geojson = ${fn:length(common.geojson) == 1 && not empty common.geojson[0] ? common.geojson[0] : 'null'};</script>
		<script src="../../js/leaflet.js"></script>
		<script src="../../js/leaflet.mouseposition.js"></script>
		<script src="../../js/util.js"></script>
		<script>
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
					if(!('dragging' in e.layer)){
						e.layer.options.draggable = true;
						e.layer.options.keyboard = false;
					}

					e.layer.on('dblclick', function(e){
						features.removeLayer(this);
					});
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
				var FIELDS = ['ids', 'taken', 'credit', 'summary', 'description', 'tags'];

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
							if(obj['success']) return;
						}
						alert('Saving failed:\n' + xhr.responseText);
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
		</script>
	</head>
	<body onload="init()">
		<div class="apptmpl-container">
			<div class="apptmpl-goldbar">
				<a class="apptmpl-goldbar-left" href="http://alaska.gov"></a>
				<span class="apptmpl-goldbar-right"></span>

				<a href="../upload.html">Upload</a>
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
				<a href="../..">Photos</a>
			</div>

			<div class="apptmpl-content">
				<input type="hidden" name="ids" id="ids" value="<c:out value="${ids}"/>">
				<div style="float: right">
					<div id="map"></div>
				</div>

				<c:if test="${!empty common.taken}">
					<fmt:formatDate pattern="M/d/yyyy" var="taken" value="${common.taken[0]}" />
				</c:if>
				<table>
					<tbody>
						<tr>
							<th>
								<label for="taken">Taken</label>
								<a data-for-id="taken" href="javascript:void(0)"></a>
							</th>
							<td>
								<input type="text" id="taken" name="taken" size="9" value="${empty taken ? '' : taken}" ${fn:length(common.taken) == 0 ? 'disabled' : ''}>
							</td>
						</tr>
						<tr>
							<th>
								<label for="credit">Credit</label>
								<a data-for-id="credit" href="javascript:void(0)"></a>
							</th>
							<td>
								<textarea id="credit" name="credit" cols="30" rows="2" ${fn:length(common.credit) == 0 ? 'disabled' : ''}><c:out value="${empty common.credit ? '' : common.credit[0]}"/></textarea>
							</td>
						</tr>
						<tr>
							<th>
								<label for="summary">
									Title <span id="summary-limit"></span>
								</label>
								<a data-for-id="summary" href="javascript:void(0)"></a>
							</th>
							<td>
								<textarea id="summary" name="summary" cols="35" rows="4" ${fn:length(common.summary) == 0 ? 'disabled' : ''}><c:out value="${empty common.summary ? '' : common.summary[0]}"/></textarea>
							</td>
						</tr>
						<tr>
							<th>
								<label for="description">Description</label>
								<a data-for-id="description" href="javascript:void(0)"></a>
							</th>
							<td>
								<textarea id="description" name="description" cols="35" rows="6" ${fn:length(common.description) == 0 ? 'disabled' : ''}><c:out value="${empty common.description ? '' : common.description[0]}"/></textarea>
							</td>
						</tr>
						<tr>
							<th>
								<label for="tags">Tags</label>
								<a data-for-id="tags" href="javascript:void(0)"></a>
							</th>
							<td>
								<input type="text" id="tags" name="tags" size="30" value="<c:out value="${empty common.tags ? '' : common.tags[0]}"/>" ${fn:length(common.tags) == 0 ? 'disabled' : ''}>
							</td>
						</tr>
					</tbody>
					<tfoot>
						<tr>
							<td>&nbsp;</td>
							<td style="text-align: right">
								<button id="button-delete">Delete Image(s)</button>
								&nbsp;&nbsp;
								&nbsp;&nbsp;
								&nbsp;&nbsp;
								<button id="button-save">Save Changes</button>
							</td>
						</tr>
					</tfoot>
				</table>

				<div style="clear: both"></div>

				<hr>

				<div class="footer">
					Image is available for free public use courtesy of DGGS, unless
					otherwise credited. Please cite the photographer and "Alaska Division
					of Geological &amp; Geophysical Surveys" when using this image. If a
					non-DGGS source is credited, you must obtain permission from the
					copyright holder and/or follow their citation specifications.
				</div>
			</div>
		</div>
	</body>
</html>
