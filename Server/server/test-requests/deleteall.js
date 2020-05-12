/*
An example get request that deletes all users in the db
*/
const request = require('request')
const bodyparser = require(`body-parser`)

request.delete('http://snapcrack.ngrok.io/users', {
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
