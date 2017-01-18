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
		<style>
			#file-drop {
				display: none;
				text-align: center;
				font-weight: bold;
				border: 2px dashed #555;
				border-radius: 8px;
				margin: 8px 4px;
				padding: 10em;
			}
			#file-drop.drag { background-color: yellow; }
			#file-status progress { width: 100%; }
			#file-status div { text-align: center; }
		</style>
		<script>
			var dropok = false;

			function init()
			{
        // Test for "XHR2" support - AJAX style uploads
				var xhr2 = ('XMLHttpRequest' in window)
					&& ('upload' in new XMLHttpRequest());

				// Can I detect changes to the files selected to upload?
        var fileselect = document.getElementById('file-select');
        if(xhr2 && 'onchange' in fileselect){
          // If so, don't bother with the submit button
					elementDisplay('file-submit', 'none');
          fileselect.addEventListener('change', selectHandler);
        }

        // File drag and drop API?
        var filedrop = document.getElementById('file-drop');
        if(xhr2 && 'ondragover' in filedrop && 'ondrop' in filedrop){
          filedrop.style.display = 'block';
          filedrop.addEventListener('dragover', hoverHandler);
          filedrop.addEventListener('dragleave', hoverHandler);
          filedrop.addEventListener('drop', selectHandler);
					dropok = true;
        }
			}


			function elementDisplay(name, state)
			{
				var e = document.getElementById(name);
				if(e) e.style.display = state;
			}


			function statusUpdate(loaded, total)
			{
				var filestatus = document.getElementById('file-status');

				// If the function is called with no arguments
				// clear the status
				if(typeof loaded === 'undefined' && typeof total === 'undefined'){
					while(filestatus.lastChild)
						filestatus.removeChild(filestatus.lastChild);
					return;
				}

				var display = document.getElementById('file-display');
				if(!display){
					display = document.createElement('div');
					display.id = 'file-display';
					filestatus.appendChild(display);
				}

				var overall = document.getElementById('file-overall');
				if(!overall){
					overall = document.createElement('progress');
					overall.id = 'file-overall';
					overall.max = total;
					filestatus.appendChild(overall);
				}
				overall.value = loaded;

				while(display.lastChild) display.removeChild(display.lastChild);
				display.appendChild(document.createTextNode(
					'Uploaded ' + loaded +
					' of ' + total + ' bytes'
				));
			}


      function hoverHandler(event)
      {
        event.stopPropagation();
        event.preventDefault();
        event.target.className = (
					event.type === 'dragover' ? 'drag' : ''
				);
      }


      function selectHandler(event)
      {
				hoverHandler(event);

        var files = event.target.files || event.dataTransfer.files;
				var formdata = new FormData();
        for(var i = 0; i < files.length; i++){
					formdata.append('file'+i, files[i]);
				}
				formdata.append('format', 'json');

				elementDisplay('file-control', 'none');
				elementDisplay('file-drop', 'none');
				statusUpdate();

				var xhr = new XMLHttpRequest();
				xhr.upload.addEventListener(
					'progress',
					function(event){
						statusUpdate(event.loaded, event.total);
					},
					false
				);

				xhr.addEventListener('load', completeHandler, false);
				xhr.open('POST', 'upload', true);
				xhr.send(formdata);
      }


			function completeHandler(event)
			{
				statusUpdate();

				var fileselect = document.getElementById('file-select');
				if(fileselect) fileselect.value = '';

				elementDisplay('file-control', 'block');
				if(dropok) elementDisplay('file-drop', 'block');
				
				// Deal with straight-up HTTP errors
				if(event.target.status != 200){
					return alert(event.target.responseText);
				}

				var result = JSON.parse(event.target.responseText);
				if('errors' in result){
					var msg = 'Error occured during upload: ';
					for(var i = 0; i < result['errors'].length; i++){
						msg += ("\n" + result['errors'][i]);
					}
					alert(msg);
				}

				if('ids' in result){
					window.location = 'edit/' + result['ids'].join(',');
				}
			}
		</script>
	</head>
	<body onload="init()">
		<div class="apptmpl-container">
			<div class="apptmpl-goldbar">
				<a class="apptmpl-goldbar-left" href="http://alaska.gov"></a>
				<span class="apptmpl-goldbar-right"></span>

				<a href="../logout">Logout</a>
				<a href="../help">Help</a>
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
				<div style="margin: 0 0 8px 0">
					Supported image types:
					<span style="font-weight: bold">JPEG, PNG, BMP, GIF</span>
				</div>

				<form action="upload" method="POST" enctype="multipart/form-data">
					<div id="file-control">
						Select files: <input type="file" id="file-select" name="files" multiple="multiple">
						<input type="submit" id="file-submit" value="Upload">
					</div>
					<div id="file-drop">drag and drop files here</div>
					<div id="file-status"></div>
				</form>
			</div>
		</div>
	</body>
</html>
