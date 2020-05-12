/*
An example get request that deletes a specified user from the db
To run it use "node delete-user <username>"
*/
const request = require('request')
const bodyparser = require(`body-parser`)

const cmdUsername = process.argv[2];

request.delete('http://snapcrack.ngrok.io/user', { //posts to the server through an ngrok tunnel
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
