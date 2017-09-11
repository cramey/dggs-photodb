L.Control.SearchControl = L.Control.extend({
	options: {
		position: 'topleft',
		placeholder: 'Search for .. ',
		inputname: 'q',
		onSubmit: function(e){
			if('preventDefault' in e) e.preventDefault();
			return false;
		}
	},
	initialize: function(options){
		L.Util.setOptions(this, options);
	},
	onAdd: function(map){
		var container = L.DomUtil.create('div', 'search-container');
		this.form = L.DomUtil.create('form', 'form', container);
		this.form.onsubmit = this.options.onSubmit;

		var inputcontainer = L.DomUtil.create(
			'div', 'search-input-container', this.form
		);

		var input = L.DomUtil.create(
			'input', 'search-input', inputcontainer
		);
		input.type = 'text';
		input.id = input.name = this.options.inputname;
		input.setAttribute('placeholder', this.options.placeholder);
		input.setAttribute('autocomplete', 'off');

		var submitbutton = L.DomUtil.create(
			'button', 'search-submit-button', inputcontainer
		);
		submitbutton.setAttribute('type', 'submit');
		submitbutton.appendChild(document.createTextNode('Search'));

		var results = L.DomUtil.create('div', 'search-results', container);
		results.id = 'search-results';

		L.DomEvent.disableClickPropagation(container);
		L.DomEvent.disableScrollPropagation(container);

		return container;
	}
});

L.control.searchControl = function(options){
	return new L.Control.SearchControl(options);
};
