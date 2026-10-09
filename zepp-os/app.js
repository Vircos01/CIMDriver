App({
  globalData: {
    apiUrl: 'http://<phone-ip>:8765/zepp/summary',
    actionUrl: 'http://<phone-ip>:8765/zepp/action',
    refreshMinutes: 1
  },

  onCreate() {
    console.log('CIMDriver Zepp app created');
  },

  onDestroy() {
    console.log('CIMDriver Zepp app destroyed');
  }
});
