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
		<link rel="stylesheet" href="css/apptmpl.min.css">
		<link rel="stylesheet" href="js/leaflet.css">
		<link rel="stylesheet" href="js/leaflet.draw.css">
		<link rel="stylesheet" href="js/leaflet.mouseposition.css">
		<link rel="stylesheet" href="css/search.css">
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
				<a href=".">Photos</a>
			</div>

			<div class="apptmpl-content">
				<div id="search-control">
					<input type="hidden" name="search-page" id="search-page" value="0">
					<input type="text" id="search-field" name="search-field" placeholder="Search for .. " autocomplete="off">
					<button id="search-button">Search</button>
					<div id="map"></div>
					<div id="search-results-control">
						<button id="search-reset">Reset</button> |
						<button id="search-prev">Previous</button> |
						Showing: <select name="search-show" id="search-show">
							<option value="6" selected>6</option>
							<option value="12">12</option>
							<option value="24">24</option>
							<option value="96">96</option>
						</select>
						| <button id="search-next">Next</button>
					</div>
				</div>
				<div id="search-results"></div>
				<div style="clear: both"></div>
			</div>
		</div>
		<script src="js/mustache-2.3.0.min.js"></script>
		<script src="js/leaflet.js"></script>
		<script src="js/leaflet.draw.js"></script>
		<script src="js/leaflet.mouseposition.js"></script>
		<script src="js/util.js"></script>
		<script src="js/pub.search.js"></script>
		<script id="tmpl-search" type="x-tmpl-mustache">
			{{#.}}
				<a href="detail/{{ID}}" title="{{filename}}">
					<img src="thumbnail/{{ID}}">
					<div>{{summary}}</div>
					<div>{{credit}} {{taken}}</div>
				</a>
			{{/.}}
		</script>
	</body>
</html>