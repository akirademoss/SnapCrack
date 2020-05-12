const express = require('express');
const mysql = require('mysql');
const bodyParser = require('body-parser');
const dbfunctions = require('./dbfunctions');

const multer = require('multer');

const app = express();
app.use(bodyParser.json());
const port = 3980

// For testing if the server is up and connections are working
app.route('/hello').post(function(req, res) {
	res.send('Hello World');
});

//For requests pertaining to a single user
app.route('/user')
.get(function(req, res) { //gets a user's info from db
	dbfunctions.getUser(req, res);
})
.post(function(req, res) { //adds a new user to the db
	dbfunctions.addUser(req, res);
})
.put(function(req, res) { //updates a user's info in the db
	res.send('Change a user');
})
.delete(function(req, res) {
	dbfunctions.deleteUser(req, res);
});

//For requests pertaining to the whole list of users
app.route('/users')
.get(function(req, res) {
	res.header("Content-Type", 'application/json');
	dbfunctions.getAllUsers(res);
})
.delete(function(req, res) {
	dbfunctions.deleteAllUsers(res);
});

//For verifying login credentials
app.route('/login')
.post(function(req, res) { //checks loging credentials against db records
	dbfunctions.verifyUser(req, res);
});

//app.route('/image')
//.get("/", express.static(path.join(__dirname, "./images")));



app.listen(port, function() {
	console.log(`Server listening on port ${port}`);
});
