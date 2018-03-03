"use strict";

var uuid = require('uuid');
var RSVP = require('rsvp');
require('sexylog');
var messages = require('muon-core').Messages;

var serviceName;
var protocolName = 'event';

exports.create = function(muon) {


  var api = exports.getApi(muon.infrastructure().serviceName, muon.infrastructure());
  api.muon = muon

  muon.addServerStack(api)

  muon.replay = function (streamName, auth, config, callback, errorCallback, completeCallback) {
    return api.replay(streamName, auth, config, callback, errorCallback, completeCallback);
  }
  muon.emit = function(event, auth) {
    return api.emit(event, auth)
  }
}

exports.getApi = function (name, infrastructure) {
  serviceName = name;

  var api = {
    name: function () {
      return protocolName;
    },
    endpoints: function () {
      return [];
    },
    replay: function(streamName, auth, config, clientCallback, errorCallback, completeCallback) {
      var ret = {}
      var muon = this.muon
      infrastructure.discovery.discoverServices(function(services) {
        var store = services.findServiceWithTags(["eventstore"])

        if (store == null || store == undefined) {
          errorCallback({
            status: "FAILED",
            cause: "No event store could be found, is Photon running?"
          })
          return
        }

        config['stream-name'] = streamName

        logger.debug("Found event store: " +JSON.stringify(store))

        var subscriber = muon.subscribe("stream://" + store.identifier + "/stream", auth, config, clientCallback, errorCallback, completeCallback)

        ret.cancel = subscriber.cancel
      })
      return ret
    },
    emit: function (event, auth) {

      var promise = new RSVP.Promise(function (resolve, reject) {

        var transportPromise = infrastructure.getTransport();
        transportPromise.then(function (transport) {

          infrastructure.discovery.discoverServices(function (services) {
            var eventStore = services.findServiceWithTags(["eventstore"])

            if (eventStore == undefined || eventStore == null) {
              reject({
                eventTime: null,
                orderId: null,
                status: "FAILED",
                cause: "No event store could be found, is Photon running?"
              })
              return
            }

            var transChannel = transport.openChannel(eventStore.identifier, protocolName);

            var callback = function (resp) {
              if (!resp) {
                logger.warn('client-api promise failed check! calling promise.reject()');
                reject(resp);
              } else {
                logger.trace('promise calling promise.resolve() event.id=' + resp.id);
                var payload = messages.decode(resp.payload)
                logger.debug("EVENT Incoming message is " + JSON.stringify(payload))
                resolve(payload);
              }
            };

            event.token = auth.token
            event.provider = auth.provider
            var evMessage = messages.muonMessage(event, serviceName, eventStore.identifier, protocolName, "EventEmitted");

            transChannel.listen(callback);
            transChannel.send(evMessage);
          });
        });
      });


      return promise;
    }
  }
  return api;
}
