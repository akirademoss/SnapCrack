/*
An example get request that returns all users in db
*/
const request = require('request')
const bodyparser = require(`body-parser`)

request.get('http://snapcrack.ngrok.io/users', {
}, (error, res, body) => {
  if (error) {
    console.error(error)
    return
  }
  console.log(`statusCode: ${res.statusCode}`)
  console.log(`statusMessage: ${res.statusMessage}`)
  console.log(`body:`)
  console.log(JSON.parse(body));
})
