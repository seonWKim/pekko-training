const WebSocket = require('ws');
let ws = null;

const readline = require('readline').createInterface({
  input: process.stdin,
  output: process.stdout
});

readline.on('line', (input) => {
  const command = input.split(' ')[0];
  const args = input.split(' ').slice(1);

  switch (command) {
    case 'connect':
      if (ws !== null) {
        console.log('Already connected. Please close the current connection first.');
        return;
      }
      const port = args[0] || '8080'; // Default to 8080 if no port is provided
      ws = new WebSocket(`ws://localhost:${port}/greeter`);
      ws.on('open', () => console.log('Connected to the server.'));
      ws.on('message', (data) => console.log(`Received: ${data}`));
      ws.on('close', () => {
        console.log('Connection closed.');
        ws = null;
      });
      ws.on('error', (error) => {
        if (error.code === 'ECONNREFUSED') {
          console.log(`Failed to connect to port ${port}. Please try again.`);
          ws = null;
        } else {
          console.log(`Error: ${error.message}`);
        }
      });
      break;
    case 'send':
      if (ws === null) {
        console.log('Not connected. Please connect first.');
        return;
      }
      const message = args.join(' ');
      ws.send(message);
      break;
    case 'close':
      if (ws === null) {
        console.log('Not connected. No need to close.');
        return;
      }
      ws.close();
      break;
    default:
      console.log('Unknown command. Please use connect <port>, send <message>, or close.');
      break;
  }
});

console.log('Please enter a command: connect <port>, send <message>, or close.');
