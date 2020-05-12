/*
An example POST request to verify a user's login credentials
To run it use "node verify-user <username> <password>"
*/
const request = require('request')
const bodyparser = require(`body-parser`)

const cmdUsername = process.argv[2];
const cmdPassword = process.argv[3];

request.post('http://snapcrack.ngrok.io/login', { //posts to the server through an ngrok tunnel
  json: {
    username: cmdUsername,
    password: cmdPassword
  }
}, (error, res, body) => {
  if (error) {
    console.error(error)
    return
  }
  console.log(`statusCode: ${res.statusCode}`)
  console.log(`statusMessage: ${res.statusMessage}`)
  console.log(`body:`)
  console.log(JSON.stringify(body));
})
