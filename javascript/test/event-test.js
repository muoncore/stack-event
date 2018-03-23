var bichannel = require('muon-core').channel();
var event = require("../distribution/proto");
var assert = require('assert');
var expect = require('expect.js');
var messages = require('muon-core').Messages;
var sinon = require("sinon")
require("sexylog")

describe("test event protocol:", function () {

    this.timeout(8000);

    it("event emit", function (done) {

        var transport = { openChannel: function (remoteService, protocolName) {} };
        var mockTransport = sinon.mock(transport);

        var channel = bichannel.create("faketransport")

        channel.rightConnection().listen(function(msg) {

            logger.info("Got a message " + JSON.stringify(msg))
            assert(msg.target_service == "myeventstore")

            channel.rightConnection().send(
                messages.muonMessage({
                    eventTime: "1234",
                    orderId: "1234",
                    status: "PERSISTED",
                    cause: null
                }, "faked", "evstore", "event", "EventProcessed")
            )
        })

        mockTransport.expects("openChannel").once().withArgs("myeventstore", "event").returns(channel.leftConnection());

        var infrastructure = {
            getTransport: function() { return {
                then: function(exec) {
                    exec(transport)
                }
            } },
            discovery: {
                discoverServices: function(exec) {
                    exec({
                        findServiceWithTags: function(tags) {
                            assert(tags == "eventstore")
                            return {
                                identifier: "myeventstore",
                                tags: ["eventstore", "awesomeService"],
                                codecs: ["application/json"],
                                connectionUrls: []
                            }
                        }
                    })
                }
            }
        }

        var api = event.getApi({}, 'server', infrastructure);

        api.emit({
            "event-type": null,
            "stream-name": null,
            schema: null,
            "caused-by-id":null,
            "caused-by-relation": null,
            "service-id": null,
            service: null,
            "order-id": null,
            "event-time": null,
            payload: null
        }, {token: "awesome"}).then(function(response) {
            assert(response)
            assert(response.orderId == "1234")
            assert(response.status == "PERSISTED")
            done();
        })

        setTimeout(function() {
            mockTransport.verify()
        }, 500)
    });
});
