var Search = {
	ok: true,
	skipdecode: false,
	last: null,
	url: 'search.json',
	events: {},
	fields: {},

	init: function(fields)
	{
		this.fields = fields;
	},

	on: function(e, f)
	{
		this.events[e] = f;
	},

	execute: function(clean)
	{
		if(!this.ok) return;
		this.ok = false;

		var params = this.params();
		var dirty = params !== this.last ? true : false;
		this.last = params;

		var p = this.get('page');
		var page = p.length == 1 ? Number(p[0]) : 0;

		if(dirty && clean !== true){
			page = 0;
			this.set('page', page);
		}

		if(page > 0){
			if(params.length > 0) params += '&';
			params += 'page=' + page;
		}

		if(clean !== true){
			this.skipdecode = true;
			window.location.hash = params;
		}

		var src = this;
		var xhr = (window.ActiveXObject ? new ActiveXObject('Microsoft.XMLHTTP') : new XMLHttpRequest());
		xhr.onreadystatechange = function(){
			if(xhr.readyState === 4){
				if(xhr.status === 200){
					var obj = JSON.parse(xhr.responseText);

					if('success' in src.events){
						src.events.success(obj, dirty);
					}
				} else {
					if('failure' in src.events){
						src.events.failure(xhr.responseText);
					}
				}
				src.ok = true;
			}
		};
		xhr.open('POST', this.url, true);
		xhr.setRequestHeader('Content-type', 'application/x-www-form-urlencoded');
		xhr.send(params);
	},

	decode: function(params)
	{
		if(this.skipdecode){
			this.skipdecode = false;
			return;
		}

		this.reset();
		if(params.length < 1) return;

		var kvs = params.split('&');
		for(var i = 0; i < kvs.length; i++){
			var idx = kvs[i].indexOf('=');
			var k = kvs[i].substring(0, idx);
			var v = decodeURIComponent(kvs[i].substring(idx + 1));
			this.set(k, v);
		}
		this.execute(true);
	},

	params: function()
	{
		var params = '';
		for(var i in this.fields){
			if(i !== 'page'){
				var v = this.get(i);
				for(var j = 0; j < v.length; j++){
					if(params.length > 0) params += '&';
					params += encodeURIComponent(i);
					params += '=';
					params += encodeURIComponent(v[j]);
				}
			}
		}
		return params;
	},

	prev: function()
	{
		var p = this.get('page');
		if(p.length === 1){
			this.set('page', Number(p[0]) - 1);
			this.execute();
		}
	},

	next: function()
	{
		var p = this.get('page');
		if(p.length === 1){
			this.set('page', Number(p[0]) + 1);
			this.execute();
		}
	},

	get: function(n)
	{
		if(n == null || !(n in this.fields)) return [];

		var o = this.fields[n];

		if(o instanceof L.Layer) {
			if(o.getLayers().length === 0) return [];
			var geojson = JSON.stringify(
				o.getLayers()[0].toGeoJSON().geometry
			);

			// Prune geojson to 4 significant digits
			geojson = geojson.replace(/\d+\.\d+/g, function(match){
				return Number(match).toFixed(4);
			});

			return [geojson];
		}

		if('tagName' in o){
			switch(o.tagName){
				case 'TEXTAREA':
				case 'INPUT':
					if(o.value.length > 0) return [o.value];
				return [];

				case 'SELECT':
					var se = [];
					for(var i = 0; i < o.options.length; i++){
						if(o.options[i].selected && o.options[i].value.length > 0){
							se.push(o.options[i].value);
						}
					}
				return se;
			}
		}
		return [];
	},

	set: function(n, v)
	{
		if(n == null || !(n in this.fields)) return;

		var o = this.fields[n];

		if(o instanceof L.Layer){
			o.addData(JSON.parse(v));
			return;
		}

		if('tagName' in o){
			switch(o.tagName){
				case 'TEXTAREA':
				case 'INPUT':
					o.value = v;
				return;

				case 'SELECT':
					for(var i = 0; i < o.options.length; i++){
						if(o.options[i].value === v){
							o.options[i].selected = true;
						}
					}
				return;
			}
		}
	},

	reset: function()
	{
		this.last = null;
		for(var i in this.fields){
			var o = this.fields[i];

			if(o instanceof L.Layer){
				o.clearLayers();
				continue;
			}

			if('tagName' in o){
				switch(o.tagName){
					case 'TEXTAREA':
					case 'INPUT':
						o.value = o.defaultValue;
					continue;

					case 'SELECT':
						for(var i = 0; i < o.options.length; i++){
							if(o.options[i].defaultSelected){
								o.options[i].selected = true;
							}
						}
					continue;
				}
			}
		}
		if('reset' in this.events) this.events.reset();
	}
};
