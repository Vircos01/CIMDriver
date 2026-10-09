import { Page, Text, Box, Button, Scroll } from '@zeppos/sdk';

Page({
  state: {
    tripStatus: 'idle',
    distanceKm: 0,
    fuelStatus: 'ready',
    apiUrl: 'http://<phone-ip>:8765/zepp/summary',
    actionUrl: 'http://<phone-ip>:8765/zepp/action',
    lastUpdated: null,
    actionMessage: ''
  },

  build() {
    return Scroll({
      children: [
        Box({
          style: {
            width: 100,
            height: 100,
            background: '#0B3D91',
            borderRadius: 18,
            marginTop: 20,
            alignItems: 'center',
            justifyContent: 'center'
          },
          children: [
            Text({
              text: 'CIMDriver',
              color: '#ffffff',
              fontSize: 24,
              fontWeight: 'bold'
            })
          ]
        }),
        Text({
          text: `Status: ${this.state.tripStatus}`,
          color: '#ffffff',
          fontSize: 24,
          marginTop: 20
        }),
        Text({
          text: `Afstand: ${this.state.distanceKm} km`,
          color: '#dfe8ff',
          fontSize: 18,
          marginTop: 10
        }),
        Text({
          text: `Brandstof: ${this.state.fuelStatus}`,
          color: '#dfe8ff',
          fontSize: 18,
          marginTop: 10
        }),
        Text({
          text: this.state.lastUpdated ? `Laatst: ${new Date(this.state.lastUpdated).toLocaleTimeString()}` : 'Laatst: onbekend',
          color: '#b9c9ff',
          fontSize: 14,
          marginTop: 10
        }),
        Button({
          text: 'Test URL',
          onClick: () => this.testUrl()
        }),
        Button({
          text: 'Vernieuw',
          onClick: () => this.refreshStatus()
        }),
        Button({
          text: 'Zakelijk',
          onClick: () => this.submitAction('classify_trip', 'BUSINESS')
        }),
        Button({
          text: 'Privé',
          onClick: () => this.submitAction('classify_trip', 'PRIVATE')
        }),
        Button({
          text: 'Tanken ok',
          onClick: () => this.submitAction('approve_fuel', 'COMPLETED')
        }),
        Text({
          text: this.state.actionMessage || 'Klaar',
          color: '#dfe8ff',
          fontSize: 14,
          marginTop: 12
        })
      ]
    });
  },

  onInit() {
    this.refreshStatus();
  },

  refreshStatus() {
    const url = this.state.apiUrl;

    fetch(url)
      .then((response) => response.json())
      .then((data) => {
        this.state.tripStatus = data.tripStatus || 'idle';
        this.state.distanceKm = Number(data.distanceKm || 0);
        this.state.fuelStatus = data.fuelStatus || 'ready';
        this.state.lastUpdated = data.lastUpdated || Date.now();
        this.actionMessage = 'URL werkt';
        this.setState(this.state);
      })
      .catch(() => {
        this.state.tripStatus = 'offline';
        this.state.distanceKm = 0;
        this.state.fuelStatus = 'onbekend';
        this.state.lastUpdated = Date.now();
        this.state.actionMessage = 'URL niet bereikbaar';
        this.setState(this.state);
      });
  },

  testUrl() {
    fetch(this.state.apiUrl)
      .then((response) => response.text())
      .then(() => {
        this.state.actionMessage = 'URL test geslaagd';
        this.setState(this.state);
      })
      .catch(() => {
        this.state.actionMessage = 'URL test mislukt';
        this.setState(this.state);
      });
  },

  submitAction(action, tripType) {
    const url = this.state.actionUrl;
    const payload = JSON.stringify({ action, tripType });

    fetch(url, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: payload
    })
      .then((response) => response.json())
      .then((data) => {
        this.state.actionMessage = data.message || 'Opgeslagen';
        this.setState(this.state);
        this.refreshStatus();
      })
      .catch(() => {
        this.state.actionMessage = 'Kon actie niet verzenden';
        this.setState(this.state);
      });
  }
});
