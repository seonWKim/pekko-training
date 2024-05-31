const WebSocket = require('ws');
let ws = null;

const readline = require('readline').createInterface({
  input: process.stdin,
  output: process.stdout
});

readline.on('line', (input) => {
  const command = input.split(' ')[0];
  const message = input.split(' ').slice(1).join(' ');

  switch (command) {
    case 'connect':
      if (ws !== null) {
        console.log('Already connected. Please close the current connection first.');
        return;
      }
      ws = new WebSocket('ws://localhost:8080/greeter');
      ws.on('open', () => console.log('Connected to the server.'));
      ws.on('message', (data) => console.log(`Received: ${data}`));
      ws.on('close', () => console.log('Connection closed.'));
      ws.on('error', (error) => console.log(`Error: ${error.message}`));
      break;
    case 'send':
      if (ws === null) {
        console.log('Not connected. Please connect first.');
        return;
      }
      ws.send(message);
      break;
    case 'close':
      if (ws === null) {
        console.log('Not connected. No need to close.');
        return;
      }
      ws.close();
      ws = null;
      break;
    default:
      console.log('Unknown command. Please use connect, send <message>, or close.');
      break;
  }
});

console.log('Please enter a command: connect, send <message>, or close.');
