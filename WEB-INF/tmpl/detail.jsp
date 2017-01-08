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
		<c:if test="${!empty image.geoJSON}">
		<link rel="stylesheet" href="../js/leaflet.css">
		<link rel="stylesheet" href="../js/leaflet.mouseposition.css">
		</c:if>
		<style>
			.apptmpl-container { min-width: 500px !important; }
			.footer { font-size: 12px; }
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
		<c:if test="${!empty image.geoJSON}">
		<script>var geojson = ${image.geoJSON};</script>
		<script src="../js/leaflet.js"></script>
		<script src="../js/leaflet.mouseposition.js"></script>
		<script src="../js/util.js"></script>
		<script src="../js/detail.js"></script>
		</c:if>
	</head>
	<body ${!empty image.geoJSON ? 'onload="init()"' : ''}>
		<div class="apptmpl-container">
			<div class="apptmpl-goldbar">
				<a class="apptmpl-goldbar-left" href="http://alaska.gov"></a>
				<span class="apptmpl-goldbar-right"></span>

				<a href="private/">Login</a>
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
					<dd><c:out value="${image.summary}" /></dd>
					</c:if>

					<c:if test="${!empty image.credit}">
					<dt>Credit</dt>
					<dd><c:out value="${image.credit}" /></dd>
					</c:if>

					<c:if test="${!empty image.description}">
					<dt>Description</dt>
					<dd><c:out value="${image.description}" /></dd>
					</c:if>

					<c:if test="${!empty image.taken}">
					<dt>Taken</dt>
					<dd><fmt:formatDate pattern="M/d/yyyy" value="${image.taken}"/></dd>
					</c:if>

					<c:if test="${!empty image.tags}">
					<dt>Tagged</dt>
					<dd><c:forEach items="${image.tags}" var="tag" varStatus="stat"><c:if test="${stat.count != 1}">,&nbsp;</c:if><a href="../search#search=<c:out value="${tag.name}"/>"><c:out value="${tag.name}" /></a></c:forEach></dd>
					</c:if>
				</dl>

				<c:if test="${!empty image.geoJSON}">
				<div id="map"></div>
				</c:if>
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
