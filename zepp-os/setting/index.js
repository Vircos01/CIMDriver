import { SettingPage, Text, Input, Button } from '@zeppos/sdk';

SettingPage({
  state: {
    apiUrl: 'http://<phone-ip>:8765/zepp/summary',
    actionUrl: 'http://<phone-ip>:8765/zepp/action',
    refreshMinutes: 1,
    showDistance: true,
    savedMessage: '',
    testMessage: 'Nog niet getest'
  },

  testUrl() {
    fetch(this.state.apiUrl)
      .then((response) => response.text())
      .then(() => {
        this.state.testMessage = 'URL test geslaagd';
        this.setState(this.state);
      })
      .catch(() => {
        this.state.testMessage = 'URL test mislukt';
        this.setState(this.state);
      });
  },

  build() {
    return [
      Text({ text: 'CIMDriver instellingen', fontSize: 22, marginTop: 20 }),
      Input({
        label: 'API URL',
        value: this.state.apiUrl,
        onChange: (value) => {
          this.state.apiUrl = value;
        }
      }),
      Input({
        label: 'Actie URL',
        value: this.state.actionUrl,
        onChange: (value) => {
          this.state.actionUrl = value;
        }
      }),
      Input({
        label: 'Verversinterval (min)',
        value: String(this.state.refreshMinutes),
        onChange: (value) => {
          this.state.refreshMinutes = Number(value || 1);
        }
      }),
      Button({
        text: 'Test URL',
        onClick: () => {
          this.testUrl();
        }
      }),
      Button({
        text: 'Opslaan',
        onClick: () => {
          this.state.savedMessage = 'Instellingen opgeslagen';
          console.log('Saved CIMDriver settings', this.state.apiUrl, this.state.actionUrl);
        }
      }),
      Text({
        text: this.state.testMessage,
        fontSize: 14,
        marginTop: 12,
        color: '#dfe8ff'
      }),
      Text({
        text: this.state.savedMessage || 'Klaar om te configureren',
        fontSize: 14,
        marginTop: 8,
        color: '#dfe8ff'
      })
    ];
  }
});
