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
		</style>
		<script>
			var geojson = ${fn:length(common.geojson) == 1 && not empty common.geojson[0] ? common.geojson[0] : '\'\''};

			function init()
			{
				var dis = document.getElementsByTagName('a');
				for(var i = 0; i < dis.length; i++){
					var forid = dis[i].getAttribute('data-for-id');
					if(forid){
						var el = document.getElementById(forid);
						if(el){
							var enabled = !(el.disabled);
							dis[i].innerHTML = (enabled ? 'Disable' : 'Enable') + ' editing';
							dis[i].style.color = '#' + (enabled ? '080' : 'f00');
							dis[i].onclick = toggleEnabled;
						}
					}
				}
			}

			function toggleEnabled()
			{
				var forid = this.getAttribute('data-for-id');
				var el = document.getElementById(forid);
				if(el){
					var state = !(el.disabled);
					this.innerHTML = (state ? 'Enable' : 'Disable') + ' editing';
					this.style.color = '#' + (state ? 'f00' : '080');
					el.disabled = state;
				}
			}
		</script>
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
				<a href="../..">Photos</a>
			</div>

			<div class="apptmpl-content">
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
								<label for="summary">Title</label>
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
