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
		<link rel="stylesheet" href="../../css/edit.css">
		<style>
			.apptmpl-container { min-width: 750px !important; }
		</style>
		<script src="../../js/spreadsheet.js"></script>
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
				<a href="..">Photos</a>
			</div>

			<div class="apptmpl-content">
				<table class="spreadsheet">
					<thead>
						<tr>
							<th class="col-id">ID</th>
							<th class="col-taken">Taken</th>
							<th class="col-credit">Credit</th>
							<th class="col-title">Title</th>
							<th class="col-description">Description</th>
							<th class="col-accuracy">Location Accuracy</th>
							<th class="col-tags">
								Tags<br>
								<a id="link-appendtags" href="javascript:void(0)">append</a> /
								<a id="link-striptags" href="javascript:void(0)">remove</a>
							</th>
							<th class="col-security">Security</th>
							<th class="col-controls">&nbsp;</th>
						</tr>
					</thead>
					<tbody>
						<c:forEach items="${images}" var="image">
						<c:set var="tags"><c:forEach items="${image.tags}" var="tag" varStatus="stat"><c:if test="${stat.count != 1}">, </c:if><c:out value="${tag.name}" /></c:forEach></c:set>
						<tr>
							<td class="col-id">
								<input type="hidden" name="ids" value="${image.ID}">
								<a href="javascript:void(0)">${image.ID}<div><img src="../../thumbnail/${image.ID}">${image.filename}</div></a>
							</td>
							<td class="col-taken">
								<input type="text" name="taken" value="<fmt:formatDate pattern="M/d/yyyy" value="${image.taken}" />"/>
							</td>
							<td class="col-credit">
								<textarea name="credit" rows="1"><c:out value="${image.credit}"/></textarea>
							</td>
							<td class="col-title">
								<textarea name="summary" rows="1"><c:out value="${image.summary}"/></textarea>
							</td>
							<td class="col-description">
								<textarea name="description" rows="1"><c:out value="${image.description}"/></textarea>
							</td>
							<td class="col-accuracy">
								<select name="accuracy">
									<option value="0" ${image.accuracy == 0 ? 'selected' : ''}>Good</option>
									<option value="1" ${image.accuracy == 1 ? 'selected' : ''}>Fair</option>
									<option value="2" ${image.accuracy == 2 ? 'selected' : ''}>Poor</option>
								</select>
							</td>
							<td class="col-tags">
								<input type="text" name="tags" value="<c:out value="${tags}"/>">
							</td>
							<td class="col-security">
								<select name="ispublic">
									<option value="true" ${image.isPublic ? 'selected' : ''}>Public</option>
									<option value="false" ${!image.isPublic ? 'selected' : ''}>Private</option>
								</select>
							</td>
							<td class="col-controls">
								<button class="button" name="button-save">Save</button>
							</td>
						</tr>
						</c:forEach>
					</tbody>
					<tfoot>
						<tr>
							<td colspan="8">
								<button class="button" id="button-edit">Edit Mode (Do not save)</button>
							</td>
							<td class="col-controls">
								<button class="button" id="button-saveall">Save All</button>
							</td>
						</tr>
					</tfoot>
				</table>

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
