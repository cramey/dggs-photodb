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
		<link rel="stylesheet" href="../../js/leaflet.draw.css">
		<link rel="stylesheet" href="../../js/leaflet.mouseposition.css">
		<link rel="stylesheet" href="../../css/edit.css">
		<style>
			.apptmpl-container { min-width: 450px !important; }
		</style>
		<script>
		<c:choose>
			<c:when test="${fn:length(common.geojson) == 1 && not empty common.geojson[0]}">var geojson = ${common.geojson[0]};</c:when>
			<c:when test="${fn:length(common.geojson) gt 0}">var geojson = {};</c:when>
			<c:otherwise>var geojson = false;</c:otherwise>
		</c:choose>
		</script>
		<script src="../../js/leaflet.js"></script>
		<script src="../../js/leaflet.draw.js"></script>
		<script src="../../js/leaflet.draw.js"></script>
		<script src="../../js/leaflet.mouseposition.js"></script>
		<script src="../../js/util.js"></script>
		<script src="../../js/edit.js"></script>
	</head>
	<body onload="init()">
		<div class="apptmpl-container">
			<div class="apptmpl-goldbar">
				<a class="apptmpl-goldbar-left" href="http://alaska.gov"></a>
				<span class="apptmpl-goldbar-right"></span>

				<a href="../upload.html">Upload</a>
				<a href="../../logout">Logout</a>
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
				<input type="hidden" name="ids" id="ids" value="<c:out value="${ids_str}"/>">

				<div id="map-container">
					<div id="map"></div>
					<a id="map-toggle" href="javascript:void(0)"></a>
				</div>

				<c:if test="${!empty common.taken}">
					<fmt:formatDate pattern="M/d/yyyy" var="taken" value="${common.taken[0]}" />
				</c:if>
				<div class="tbl-container">
					<table class="ffields">
						<tbody>
							<tr>
								<th>
									<label>Longitude/Latitude</label>
								</th>
								<td id="lonlat">Not applicable</td>
							</tr>
							<tr>
								<th>
									<label for="taken">Taken</label>
									<a data-for-id="taken" href="javascript:void(0)"></a>
								</th>
								<td>
									<input type="text" id="taken" name="taken" size="9" tabindex="1" value="${empty taken ? '' : taken}" ${fn:length(common.taken) == 0 ? 'disabled' : ''}>
								</td>
							</tr>
							<tr>
								<th>
									<label for="credit">Credit</label>
									<a data-for-id="credit" href="javascript:void(0)"></a>
								</th>
								<td>
									<textarea id="credit" name="credit" cols="30" rows="2" tabindex="2" ${fn:length(common.credit) == 0 ? 'disabled' : ''}><c:out value="${empty common.credit ? '' : common.credit[0]}"/></textarea>
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
									<textarea id="summary" name="summary" cols="35" rows="4" tabindex="3" ${fn:length(common.summary) == 0 ? 'disabled' : ''}><c:out value="${empty common.summary ? '' : common.summary[0]}"/></textarea>
								</td>
							</tr>
							<tr>
								<th>
									<label for="description">Description</label>
									<a data-for-id="description" href="javascript:void(0)"></a>
								</th>
								<td>
									<textarea id="description" name="description" cols="35" rows="6" tabindex="4" ${fn:length(common.description) == 0 ? 'disabled' : ''}><c:out value="${empty common.description ? '' : common.description[0]}"/></textarea>
								</td>
							</tr>
							<tr>
								<th>
									<label for="accuracy">Location Accuracy</label>
									<a data-for-id="accuracy" href="javascript:void(0)"></a>
								</th>
								<td>
									<select name="accuracy" id="accuracy" tabindex="5" ${fn:length(common.accuracy) == 0 ? 'disabled' : ''}>
										<option value="0" ${fn:length(common.accuracy) gt 0 && common.accuracy[0] == 0 ? 'selected' : ''}>Good</option>
										<option value="1" ${fn:length(common.accuracy) gt 0 && common.accuracy[0] == 1 ? 'selected' : ''}>Fair</option>
										<option value="2" ${fn:length(common.accuracy) gt 0 && common.accuracy[0] == 2 ? 'selected' : ''}>Poor</option>
									</select>
								</td>
							</tr>
							<tr>
								<th>
									<label for="tags">Tags</label>
									<a data-for-id="tags" href="javascript:void(0)"></a>
								</th>
								<td>
									<input type="text" id="tags" name="tags" size="35" tabindex="6" placeholder="panorama, sampling, fault, glacier, helicopter, Denali" value="<c:out value="${empty common.tags ? '' : common.tags[0]}"/>" ${fn:length(common.tags) == 0 ? 'disabled' : ''}>
								</td>
							</tr>
							<tr>
								<th>
									<label for="ispublic">Security</label>
									<a data-for-id="ispublic" href="javascript:void(0)"></a>
								</th>
								<td>
									<select name="ispublic" id="ispublic" tabindex="7" ${fn:length(common.ispublic) == 0 ? 'disabled' : ''}>
										<option value="true" ${fn:length(common.ispublic) gt 0 && common.ispublic[0] ? 'selected' : ''}>Public</option>
										<option value="false" ${fn:length(common.ispublic) gt 0 && !common.ispublic[0] ? 'selected' : ''}>Private</option>
									</select>
								</td>
							</tr>
						</tbody>
						<tfoot>
							<tr>
								<td colspan="2" style="text-align: right; background-color: #fff">
									<button class="button" id="button-delete">Delete Image(s)</button>
									<button class="button" id="button-spreadsheet">Spreadsheet Mode (Do not save)</button>
									<button class="button" tabindex="8" id="button-save">Save Changes</button>
								</td>
							</tr>
						</tfoot>
					</table>
				</div>

				<div style="clear: both"></div>

				<div id="images">
					<c:forEach items="${ids}" var="id">
					<a href="../../image/${id}"><img src="../../thumbnail/${id}"></a>
					</c:forEach>
				</div>

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
