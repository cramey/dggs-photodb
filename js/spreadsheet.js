function init()
{
	// IE string.trim() fix
	if(!String.prototype.trim){
		String.prototype.trim = function () {
			return this.replace(/^[\s\uFEFF\xA0]+|[\s\uFEFF\xA0]+$/g, '');
		};
	}

	var tareas = document.getElementsByTagName('textarea');
	for(var i = 0; i < tareas.length; i++){
		tareas[i].onfocus = function(){
			if(this.parentNode.offsetHeight < 75){
				this.parentNode.style.height = '75px';
			}
			if(this.parentNode.offsetWidth < 300){
				this.parentNode.style.width = '300px';
			}
		};
		tareas[i].onblur = function(){
			if(this.parentNode.style.height === '75px'){
				this.parentNode.style.height = '';
			}
			if(this.parentNode.style.width === '300px'){
				this.parentNode.style.width = '';
			}
		};
	}
	
	var taken = document.getElementsByName('taken');
	if(taken.length > 0) taken[0].focus();

	var edit = document.getElementById('button-edit');
	if(edit){
		edit.onclick = function(){
			var els = document.getElementsByName('ids');
			var ids = '';
			for(var i = 0; i < els.length; i++){
				if(ids.length > 0) ids += ',';
				ids += els[i].value;
			}
			window.location = '../edit/' + ids;
		};
	}

	var append = document.getElementById('link-appendtags');
	if(append){
		append.onclick = function(){
			var ntag = prompt("Append To All Tags\n    Which tag?");
			if(ntag === null) return;

			var tags = document.getElementsByName('tags');
			for(var i = 0; i < tags.length; i++){
				var ts = tagsSplit(tags[i].value);
				ts.push(ntag);
				tags[i].value = ts.join(', ');
			}
		};
	}

	var remove = document.getElementById('link-striptags');
	if(remove){
		remove.onclick = function(){
			var rtag = prompt("Remove From All Tags\n    Which tag?");
			if(rtag === null) return;

			var tags = document.getElementsByName('tags');
			for(var i = 0; i < tags.length; i++){
				var ts = tagsSplit(tags[i].value);
				tagsRemove(ts, rtag);
				tags[i].value = ts.join(', ');
			}
		};
	}

	var saves = document.getElementsByName('button-save');
	for(var i = 0; i < saves.length; i++){
		saves[i].onclick = saveRow;
	}

	var saveall = document.getElementById('button-saveall');
	if(saveall){
		saveall.onclick = function(){
			var saves = document.getElementsByName('button-save');
			for(var i = 0; i < saves.length; i++) saves[i].click();
		};
	}
}


function tagsSplit(str)
{
	var tags = str.split(',');
	for(var i = 0; i < tags.length; i++){
		tags[i] = tags[i].trim();
		if(tags[i] === '') tags.splice(i, 1);
	}
	return tags;
}


function tagsRemove(tags, tag)
{
	var tag = tag.toLowerCase();
	for(var i = 0; i < tags.length; i++){
		if(tags[i].toLowerCase() === tag){
			tags.splice(i, 1);
			return;
		}
	}
}


function saveRow()
{
	var params = '';

	var tr = this.parentNode.parentNode;
	var inputs = tr.getElementsByTagName('INPUT');
	for(var i=0; i < inputs.length; i++){
		if(params.length > 0) params += '&';
		params += inputs[i].name + '=';
		params += encodeURIComponent(inputs[i].value);
	}

	var tareas = tr.getElementsByTagName('TEXTAREA');
	for(var i=0; i < tareas.length; i++){
		if(params.length > 0) params += '&';
		params += tareas[i].name + '=';
		params += encodeURIComponent(tareas[i].value);
	}

	var selects = tr.getElementsByTagName('SELECT');
	for(var i=0; i < selects.length; i++){
		for(var j=0; j < selects[i].options.length; j++){
			if(selects[i].options[j].selected){
				if(params.length > 0) params += '&';
				params += selects[i].name + '=';
				params += encodeURIComponent(selects[i].options[j].value);
			}
		}
	}

	var button = this;
	button.innerHTML = 'Saving..';

	var xhr = (window.ActiveXObject ? new ActiveXObject('Microsoft.XMLHTTP') : new XMLHttpRequest());
	xhr.onreadystatechange = function(){
		if(xhr.readyState === 4){
			if(xhr.status === 200){
				var obj = JSON.parse(xhr.responseText);
				if(obj['success']){
					if(button) button.innerHTML = 'Save OK';
					return;
				}
			}
			if(button) button.innerHTML = 'Save Error';
			alert('Saving failed:\n' + xhr.responseText);
		}
	};
	xhr.open('POST', '../image.json', true);
	xhr.setRequestHeader('Content-type', 'application/x-www-form-urlencoded');
	xhr.send(params);
}
