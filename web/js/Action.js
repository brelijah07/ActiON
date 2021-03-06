var Action = new Backbone.Marionette.Application();

Action.Device = Backbone.Model.extend({
	initialize: function() {
		this.set('device_id', this.get('id'));
		this.set('id', this.get('type') + '_' + this.get('id'));
		this.set('updating', false);

		if (this.primary) {
			this.listenTo(this, 'change:' + this.primary, this.setStatus);
			this.setStatus();

			if (this.inactive) {
				this.listenTo(this, 'change:' + this.primary, this.setInactive);
				this.setInactive();
			}
		}
	},

	setStatus: function() {
		this.set('status', this.get(this.primary));
	},

	setInactive: function() {
		this.set('inactive', this.get(this.primary) == this.inactive);
	},

	sendCommand: function(value, type, wait) {
		wait = wait === undefined ? true : wait;

		var model = this;
		model.set('updating', true);

		var id = model.get('id').split('_')[1];
		var type = type || model.get('type');
		Action.sendCommand(id, type, value, function() {
			if (!wait) {
				model.set('updating', false);
			}

			if (!Action.config.pusher_app) {
				Action.updateData();
			}
		});
	}
});

Action.Devices = Backbone.Collection.extend({
	model: Action.Device,
});

Action.DeviceTypes = Backbone.Collection.extend({
	initialize: function() {
		this.listenTo(this, 'add', function(model) {
			Action.devices.add(model);
		});

		this.listenTo(this, 'remove', function(model) {
			Action.devices.remove(model);
		});
	}
});

Action.Contact = Action.Device.extend({
	primary: 'contact',
	inactive: 'closed',
});
Action.Contacts = Action.DeviceTypes.extend({
	model: Action.Contact,
});

Action.Dimmer = Action.Device.extend({
	primary: 'switch',
	inactive: 'off',
});
Action.Dimmers = Action.DeviceTypes.extend({
	model: Action.Dimmer,
});

Action.Humidity = Action.Device.extend({
	primary: 'humidity',
});
Action.Humidities = Action.DeviceTypes.extend({
	model: Action.Humidity,
});

Action.Lock = Action.Device.extend({
	primary: 'lock',
});
Action.Locks = Action.DeviceTypes.extend({
	model: Action.Lock,
});

Action.Link = Action.Device.extend();
Action.Links = Action.DeviceTypes.extend({
	model: Action.Link,
});

Action.Momentary = Action.Device.extend();
Action.Momentaries = Action.DeviceTypes.extend({
	model: Action.Momentary,
});

Action.Motion = Action.Device.extend({
	primary: 'motion',
	inactive: 'inactive',
});
Action.Motions = Action.DeviceTypes.extend({
	model: Action.Motion,
});

Action.Presence = Action.Device.extend({
	primary: 'presence',
	inactive: 'present',
});
Action.Presences = Action.DeviceTypes.extend({
	model: Action.Presence,
});

Action.Switch = Action.Device.extend({
	primary: 'switch',
	inactive: 'off',
});
Action.Switches = Action.DeviceTypes.extend({
	model: Action.Switch,
});

Action.Temperature = Action.Device.extend({
	primary: 'temperature',
});
Action.Temperatures = Action.DeviceTypes.extend({
	model: Action.Temperature,
});

Action.Mode = Action.Device.extend({
	defaults: {
		name: 'Mode',
	}
});

Action.Weather = Action.Device.extend({
	defaults: {
		type: 'weather'
	},

	weatherIcons: {
		"chanceflurries": "snow",
		"chancerain": "rain",
		"chancesleet": "sleet",
		"chancesnow": "snow",
		"chancetstorms": "rain",
		"clear": "clear-day",
		"cloudy": "cloudy",
		"flurries": "snow",
		"fog": "fog",
		"hazy": "fog",
		"mostlycloudy": "cloudy",
		"mostlysunny": "clear-day",
		"partlycloudy": "partly-cloudy-day",
		"partlysunny": "partly-cloudy-day",
		"rain": "rain",
		"sleet": "sleet",
		"snow": "snow",
		"sunny": "clear-day",
		"tstorms": "rain",
		"nt_chanceflurries": "snow",
		"nt_chancerain": "rain",
		"nt_chancesleet": "sleet",
		"nt_chancesnow": "snow",
		"nt_chancetstorms": "rain",
		"nt_clear": "clear-night",
		"nt_cloudy": "cloudy",
		"nt_flurries": "snow",
		"nt_fog": "fog",
		"nt_hazy": "fog",
		"nt_mostlycloudy": "partly-cloudy-night",
		"nt_mostlysunny": "partly-cloudy-night",
		"nt_partlycloudy": "partly-cloudy-night",
		"nt_partlysunny": "partly-cloudy-night",
		"nt_sleet": "sleet",
		"nt_rain": "rain",
		"nt_snow": "snow",
		"nt_sunny": "clear-night",
		"nt_tstorms": "rain",
	},

	initialize: function() {
		this.set('id', 'weather');
		this.set('device_id', 'weather');
		this.setup();

		this.listenTo(this, 'change:status', this.setup);
	},

	setup: function() {
		var status = this.get('status');
		this.set(_.extend(status.conditions, status.astronomy));

		this.set('location', this.get('display_location').full);
		this.set('skycon', this.weatherIcons[this.get('icon')]);

		var sunrise = this.get('sunrise');
		var sunset = this.get('sunset');
		this.set('sunrise', sunrise.hour + ':' + sunrise.minute + ' AM');
		this.set('sunset', (sunset.hour - 12) + ':' + sunset.minute + ' PM');
	}
});

Action.DeviceView = Marionette.ItemView.extend({
	className: function() {
		return 'st-tile ' + this.model.get('type');
	},
	getTemplate: function() {
		var template = '#_st-' + this.model.get('type');
		if ($(template).length === 0) {
			template = '#_st-device';
		}

		return template;
	},
	bindings: {
		'.st-title': 'name',
		'.fa': {
			observe: 'status',
			update: 'getIcon',
		},
		':el': {
			classes: {
				'inactive': 'inactive',
			},
		}
	},
	icons: {},

	onRender: function() {
		this.stickit();

		this.listenTo(this.model, 'change:updating', function() {
			var updating = this.model.get('updating');
			this.$el.toggleClass('updating', updating);
		});
	},

	getIcon: function($el, val, model) {
		_.each(this.icons, function(icon, key) {
			$el.toggleClass(icon, key == val);
		});
	},
});

Action.ContactView = Action.DeviceView.extend({
	icons: {
		'closed': 'fa-compress',
		'open': 'fa-expand',
	},
});

Action.SwitchView = Action.DeviceView.extend({
	icons: {
		'off': 'fa-toggle-off',
		'on': 'fa-toggle-on',
	},
	events: {
		'click': 'toggle',
	},

	toggle: function() {
		this.model.sendCommand('toggle');
	}
});

Action.DimmerView = Action.SwitchView.extend({
	initialize: function() {
		this.bindings = _.extend({}, this.bindings, {
			'[name=dimmer]': {
				observe: 'level',
			},
		});

		this.events['change input'] = this.dimmerChange
		this.events['click input'] = function(event) {
			event.stopPropagation();
		};
	},

	dimmerChange: function(event) {
		event.stopPropagation();
		this.model.sendCommand(this.model.get('level'));
	},
});

Action.PresenceView = Action.DeviceView.extend({
	icons: {
		'not present': 'fa-map-marker-away',
		'present': 'fa-map-marker',
	},
});

Action.MotionView = Action.DeviceView.extend({
	icons: {
		'inactive': 'fa-square-o',
		'active': 'fa-square',
	},
});

Action.LockView = Action.DeviceView.extend({
	icons: {
		'unlocked': 'fa-unlock-alt',
		'locked': 'fa-lock',
	},
});

Action.MomentaryView = Action.DeviceView.extend({
	icons: {
		'': 'fa-circle-o',
	},
});

Action.LinkView = Action.DeviceView.extend({
	icons: {
		'': 'fa-circle-o',
	},

	initialize: function() {
		this.bindings = _.extend({}, this.bindings, {
			'a': {
				attributes: [{
					observe: 'status',
					name: 'href',
				}],
			}
		});
	},

});

Action.TemperatureView = Action.DeviceView.extend({
	initialize: function() {
		this.bindings = _.extend({}, this.bindings, {
			'.st-icon': {
				observe: 'status',
				onGet: function(val) {
					return val + '\xb0';
				},
			}
		});
	}
});

Action.ModeView = Action.DeviceView.extend({
	events: {
		'click .st-icon': 'showModePicker',
		'click .st-phrases': 'showPhrasePicker',
	},

	initialize: function() {
		this.bindings['.st-icon'] = 'status';
	},

	onRender: function() {
		this.stickit();

		this.listenTo(this.model, 'change:updating', function() {
			var updating = this.model.get('updating');
			this.$el.toggleClass('updating', updating);
		});

		this.jbox = new jBox('Modal', {
			addClass: 'list-picker'
		});
	},

	showModePicker: function(event) {
		event.stopPropagation();
		this.showPicker('mode');
	},

	showPhrasePicker: function(event) {
		event.stopPropagation();
		this.showPicker('phrase');
	},

	showPicker: function(type) {
		if (type == 'mode') {
			var method = _.bind(this.changeMode, this);
			var things = this.model.get('modes');
		} else {
			var method = _.bind(this.changePhrase, this);
			var things = this.model.get('phrases');
		}

		var content = $('<ul>');
		_.each(things, function(val) {
			var li = $('<li>').text(val);
			li.on('click', method);
			content.append(li);
		});

		this.jbox.setContent(content);
		this.jbox.setTitle('Choose Mode');
		this.jbox.open();
	},

	changeMode: function(event) {
		this.model.sendCommand($(event.currentTarget).text(), 'mode');
		this.jbox.close();
	},

	changePhrase: function(event) {
		this.model.sendCommand($(event.currentTarget).text(), 'hellohome', false);
		this.jbox.close();
	}
});

Action.HumidityView = Action.TemperatureView.extend({
	initialize: function() {
		this.bindings = _.extend({}, this.bindings, {
			'.st-icon': {
				observe: 'status',
				onGet: function(val) {
					return val + '%';
				},
			}
		});
	}
});

Action.WeatherView = Action.DeviceView.extend({
	bindings: {
		'.st-title': 'location',
		'.w-temperature': {
			observe: 'temp_f',
			onGet: function(val) {
				return val + '\xb0';
			}
		},
		'.w-humidity': {
			observe: 'relative_humidity',
			onGet: function(val) {
				return 'Humidity: ' + val;
			}
		},
		'.w-status': {
			observe: ['weather', 'feelslike_f'],
			onGet: function(val) {
				return val[0] + ', feels like ' + val[1] + '\xb0';
			}
		},
		'.sunrise': 'sunrise',
		'.sunset': 'sunset',
	},

	initialize: function() {
		this.skycons = new Skycons({
			color: 'white'
		});
	},

	onRender: function() {
		this.stickit();
		this.skycons.add(this.$el.find('canvas')[0], this.model.get('skycon'));
		this.skycons.play();
	},
});


Action.DevicesView = Marionette.CollectionView.extend({
	getChildView: function(item) {
		if (item instanceof Action.Contact) {
			return Action.ContactView;
		} else if (item instanceof Action.Dimmer) {
			return Action.DimmerView;
		} else if (item instanceof Action.Switch) {
			return Action.SwitchView;
		} else if (item instanceof Action.Motion) {
			return Action.MotionView;
		} else if (item instanceof Action.Temperature) {
			return Action.TemperatureView;
		} else if (item instanceof Action.Humidity) {
			return Action.HumidityView;
		} else if (item instanceof Action.Presence) {
			return Action.PresenceView;
		} else if (item instanceof Action.Lock) {
			return Action.LockView;
		} else if (item instanceof Action.Momentary) {
			return Action.MomentaryView;
		} else if (item instanceof Action.Link) {
			return Action.LinkView;
		} else if (item instanceof Action.Weather) {
			return Action.WeatherView;
		} else if (item instanceof Action.Mode) {
			return Action.ModeView;
		}

		return Action.DeviceView;
	},

	initialize: function() {

	},

	onRender: function() {}
});


Action.updateData = function() {
	$.ajax({
		url: Action.dataUri,
		dataType: 'jsonp',
		data: {
			access_token: Action.config.access_token,
		},
		success: function(data) {
			console.log(data);

			Action.dimmers.set(new Action.Dimmers(data.dimmers).models);
			Action.switches.set(new Action.Switches(data.switches).models);
			Action.momentaries.set(new Action.Momentaries(data.momentary).models);
			Action.locks.set(new Action.Locks(data.locks).models);
			Action.contacts.set(new Action.Contacts(data.contacts).models);
			Action.motions.set(new Action.Motions(data.motion).models);
			Action.presences.set(new Action.Presences(data.presence).models);
			Action.temperatures.set(new Action.Temperatures(data.temperature).models);
			Action.humidities.set(new Action.Humidities(data.humidity).models);
			Action.links.set(new Action.Links(data.links).models);

			var weather = new Action.Weather(data.weather);
			var mode = new Action.Mode(_.extend(data.hellohome, data.mode));

			var opts = {
				at: 0,
				merge: true
			};

			Action.devices.add(mode, opts);
			Action.devices.add(weather, opts);

			if (Action.config.pusher_app) {
				Action.setupPusher();
			} else if (Action.config.refresh) {
				Action.refreshTimeout = _.delay(Action.updateData, Action.config.refresh * 60 * 1000);
			}
		},
	});
};

Action.setupPusher = function() {
	if (!Action.pusher || Action.pusher.connection.state == 'disconnected') {
		Action.pusher = new Pusher(Action.config.pusher_app);
		var channel = Action.pusher.subscribe('devices');
		channel.bind('device_update', Action.pusherDeviceUpdate);
	}
};

Action.pusherDeviceUpdate = function(data) {
	var device = Action.devices.findWhere({
		device_id: data.id
	});
	if (device) {
		device.set(data.name, data.value);
		device.set('updating', false);
	}
};

Action.sendCommand = function(id, type, value, complete) {
	$.ajax({
		url: Action.commandUri,
		dataType: 'jsonp',
		data: {
			access_token: Action.config.access_token,
			id: id,
			type: type,
			value: value,
		},
		complete: complete || function() {}
	});
};

Action.addInitializer(function() {
	Action.devices = new Action.Devices();

	Action.contacts = new Action.Contacts();
	Action.dimmers = new Action.Dimmers();
	Action.humidities = new Action.Humidities();
	Action.locks = new Action.Locks();
	Action.links = new Action.Links();
	Action.momentaries = new Action.Momentaries();
	Action.motions = new Action.Motions();
	Action.presences = new Action.Presences();
	Action.switches = new Action.Switches();
	Action.temperatures = new Action.Temperatures();

	Action.dataUri = Action.config.uri + 'data';
	Action.commandUri = Action.config.uri + 'command';
	Action.updateData();

	Action.addRegions({
		container: '#container',
	});

	Action.container.show(new Action.DevicesView({
		collection: Action.devices
	}));

	var updateVisibility = _.debounce(function() {
		var state;
		if (typeof document.hidden !== "undefined") {
			state = "visibilityState";
		} else if (typeof document.mozHidden !== "undefined") {
			state = "mozVisibilityState";
		} else if (typeof document.msHidden !== "undefined") {
			state = "msVisibilityState";
		} else if (typeof document.webkitHidden !== "undefined") {
			state = "webkitVisibilityState";
		}

		if (document[state] == 'visible') {
			Action.updateData();
		}
	}, 50);

	$(document).on('show.visibility', updateVisibility);
	$(document).on('hide.visibility', updateVisibility);
});