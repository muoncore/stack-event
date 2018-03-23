

export default class MuonEventClient {

  muon: any

  constructor(muon: any) {
    this.muon = muon
  }

  public subscribe(stream: string, auth: Auth, exec: (event: MuonEvent) => Promise<void>): void {
    this.connect(stream, auth, exec)
  }

  public async emitWithAuth(auth: Auth, event: MuonEvent): Promise<MuonEventResult> {

    let accountUpdate = {
      "id": event.id,
      "event-type": event.eventType,
      "stream-name": event.streamName,
      "service-id": "aether",
      payload: event.payload
    }

    try {
      let ret = await this.muon.emit(accountUpdate, auth)
      // console.log("Persisted Event" + JSON.stringify(ret))
      return ret
    } catch (error) {
      console.log("Event Emit failed " + JSON.stringify(error))
      return {
        success:false,
        error: error
      }
    }
  }

  private connect(stream: string, auth: Auth,  exec: (event: MuonEvent) => void) {
    let __this: any

    this.muon.replay(stream, auth, {}, function (event: any) {

      let ev: MuonEvent = {
        payload: event.payload,
        id: event.id,
        service: event["service-id"],
        streamName: event["stream-name"],
        eventType: event["event-type"]
      } as MuonEvent

      exec(ev)
    }, function (error: any) {
      console.log("Stream listener disconnected from event store, reconnecting ", error)
      setTimeout(__this.connect, 1000)
    }, function () {
      console.log("Completed!")
      setTimeout(__this.connect, 1000)
    })
  }
}

export class MuonEventResult {

}

export class MuonEvent {

  constructor(readonly id: string,
              readonly payload: any,
              readonly service: string,
              readonly streamName: string,
              readonly eventType: string) {}
}

export class Auth {
    constructor(readonly provider: string,
    readonly token: string) {}
}
