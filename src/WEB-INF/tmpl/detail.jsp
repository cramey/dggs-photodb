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
		<meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
		<title>Alaska Division of Geological &amp; Geophysical Surveys Photo Database</title>
		<link rel="stylesheet" href="../css/apptmpl.min.css">
		<c:if test="${!empty image.geoJSON}">
		<link rel="stylesheet" href="../js/leaflet.css">
		<link rel="stylesheet" href="../js/leaflet.mouseposition.css">
		</c:if>
		<link rel="stylesheet" href="../css/detail.css">
		<c:if test="${!empty image.geoJSON}">
		<script>var geojson = ${image.geoJSON};</script>
		<script src="../js/leaflet.js"></script>
		<script src="../js/leaflet.mouseposition.js"></script>
		<script src="../js/util.js"></script>
		<script src="../js/detail.js"></script>
		</c:if>
	</head>
	<body ${!empty image.geoJSON && image.accuracy != 2 ? 'onload="init()"' : ''}>
		<div class="apptmpl-container">
			<div class="apptmpl-goldbar">
				<a class="apptmpl-goldbar-left" href="http://alaska.gov"></a>
				<span class="apptmpl-goldbar-right"></span>

				<a href="../private/">Login</a>
				<a href="../help/">Help</a>
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

				<c:if test="${!empty image.geoJSON && image.accuracy != 2}">
				<div id="map"></div>
				</c:if>
				<div style="clear: both"></div>
				<hr>
				<div class="footer"><%@include file="copyright.html" %></div>
			</div>
		</div>
	</body>
</html>
