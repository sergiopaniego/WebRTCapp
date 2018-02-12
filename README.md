# WebRTCAndroidExample
This is the simpliest example of an app that uses WebRTC to send video I could made. I followed some tutorials I found on the Internet,
specially [this series of posts](https://vivekc.xyz/getting-started-with-webrtc-for-android-daab1e268ff4) and Google's official [Codelab](https://codelabs.developers.google.com/codelabs/webrtc-web/#0).

I wrote a brief post on the issue that you can check out [here](https://medium.com/@SergioPaniego/tutorial-on-how-to-make-the-simplest-webrtc-android-app-daacb5c8d133).

I'm using WebSocket protocol to communicate with the server.

## How to run it
First install the Web Socket module 
```
npm install websocket
```
Having the module install, you only need to have nodeJS installed in your computer to run the server part that will work as signaling service. To start the server just type the following command

```
node index.js
```
