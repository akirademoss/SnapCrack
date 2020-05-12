/*
An example get request that returns a specific user in the db
To run it use "node get-user <username>"
*/
const request = require('request')
const bodyparser = require(`body-parser`)

const cmdUsername = process.argv[2];

request.get('http://snapcrack.ngrok.io/user', { //posts to the server through an ngrok tunnel
  json: {
    username: cmdUsername,
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
