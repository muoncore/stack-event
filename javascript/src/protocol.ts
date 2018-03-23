import {Auth, MuonEvent, MuonEventResult} from "./MuonEventClient";

const messages = require("muon-core").Messages

export function create(muon: any): void {

  let api = exports.getApi(muon, muon.infrastructure().serviceName, muon.infrastructure());
  api.muon = muon

  muon.addServerStack(api)

  muon.replay = function (streamName: string, auth: Auth, config: any, callback: any, errorCallback: any, completeCallback: any) {
    return api.replay(streamName, auth, config, callback, errorCallback, completeCallback);
  }
  muon.emit = function(event: any, auth: Auth) {
    return api.emit(event, auth)
  }
}

export function getApi(muon: any, name: string, infrastructure: object): EventApi {
  console.log("MESSAGES = " + require("muon-core"))

  return new EventApi(muon, name, infrastructure)
}


export default class EventApi {

  constructor(readonly muon: any, readonly serviceName: string, readonly infrastructure: any) {}

  public name(): string {
    return "event"
  }

  public endpoints(): Array<string> {
    return [];
  }

  public async replay(streamName: string, auth: object, config: any,
                clientCallback: (event: MuonEvent) => Promise<void>,
                errorCallback: (error: any) => Promise<void>,
                completeCallback: () => Promise<EventReplayControl>) {
    return new Promise((res, rej) => {
      let muon = this.muon
      this.infrastructure.discovery.discoverServices(function(services: any) {
        let store = services.findServiceWithTags(["eventstore"])

        if (store == null || store == undefined) {
          errorCallback({
            status: "FAILED",
            cause: "No event store could be found, is Photon running?"
          })
          rej(new Error("No event store could be found, is Photon running?"))
        }

        config['stream-name'] = streamName

        let subscriber = muon.subscribe("stream://" + store.identifier + "/stream", auth, config, clientCallback, errorCallback, completeCallback)

        res(new EventReplayControl(subscriber.cancel))
      })
    })

  }

  public async emit(event: object, auth: Auth): Promise<MuonEventResult> {

    let client = this

    return new Promise(async (resolve, reject) => {

      let transport = await client.infrastructure.getTransport();

      client.infrastructure.discovery.discoverServices(function (services: any) {
        let eventStore = services.findServiceWithTags(["eventstore"])

        if (eventStore == undefined || eventStore == null) {
          reject({
            eventTime: null,
            orderId: null,
            status: "FAILED",
            cause: "No event store could be found, is Photon running?"
          })
          return
        }

        let transChannel = transport.openChannel(eventStore.identifier, client.name());

        let callback = function (resp: any) {
          if (!resp) {
            // logger.warn('client-api promise failed check! calling promise.reject()');
            reject(resp);
          } else {
            // logger.trace('promise calling promise.resolve() event.id=' + resp.id);
            let payload = messages.decode(resp.payload)
            // logger.debug("EVENT Incoming message is " + JSON.stringify(payload))
            resolve(payload);
          }
        };

        let ev: any = event

        ev["token"] = auth.token
        ev["provider"] = auth.provider
        let evMessage = messages.muonMessage(event, client.serviceName, eventStore.identifier, client.name(), "EventEmitted");

        transChannel.listen(callback);
        transChannel.send(evMessage);
      });
    });
  }
}

export class EventReplayControl {

  constructor(readonly exec: () => void) {}

  cancel(): void {
    this.exec()
  }
}
