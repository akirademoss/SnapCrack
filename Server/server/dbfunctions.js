const mysql = require('mysql');

//Connect to the database
var con = mysql.createConnection({
  host: 'snapcrack.ece.iastate.edu',
  user: 'sdmay20-18',
  password: 'sd18',
  database: 'Snapcrack',
  port: '/var/run/mysqld/mysqld.sock'
});

con.connect(function(err) {
	if(err) {
		throw err;
		console.log(err);
	}
	else console.log("Database connected");
});

module.exports = {
	getAllUsers: function (res) {
		con.query("SELECT * FROM Users", function (err, result, fields) {
			if (err) {
				throw err;
				res.json(createJSONResponse(false, err));
			}
			console.log(result);
			res.json(createJSONResponse(true, result));
		})
	},
	deleteAllUsers: function (res) {
		con.query("DELETE FROM Users", function (err, result, fields) {
			if (err) {
				throw err;
				res.json(createJSONResponse(false, err));
			} else {
				console.log(result);
				res.json(createJSONResponse(true, result));
			}
		})
	},
	getUser: function (req, res) {
		var username = req.body.username;
		var newQuery = `SELECT * FROM Users WHERE username = ?`
		con.query(newQuery, username, function (err, result, fields) {
			if (err) {
				throw err;
				res.json(createJSONResponse(false, err));
			} else {
				console.log(`Found ${username}`);
				res.json(createJSONResponse(true, result));
			}
		})

	},
	verifyUser: function (req, res) {
		var username = req.body.username;
		var password = req.body.password;
		var newQuery = `SELECT * FROM Users WHERE username = ?`
		con.query(newQuery, username, function (err, result, fields) {
			console.log(result);
			var jsonString = JSON.stringify(result);
			var json = JSON.parse(jsonString);
			if (err) {
				throw err;
				res.json(createJSONResponse(false, err));
			} else {
				if(json[0] != null) {
					if(json[0].password != null) {
						if (json[0].password == password) {
							res.json(createJSONResponse(true, "success"));
						} else {
							res.json(createJSONResponse(false, "password doesn't match"));
						}
					} else {
						res.json(createJSONResponse(false, "No password for user"));
					}
				} else {
					res.json(createJSONResponse(false, "User not found"));
				}
			}
		})
	}, 
	addUser: function (req, res) {
		var values = [[req.body.username, req.body.password]];
		var newQuery = "INSERT INTO Users (username, password) VALUES ?";
		con.query(newQuery, [values], function (err, result, fields) {
			if (err) {
				console.log(err);
				console.log(`User with username ${req.body.username} already exists`);
				res.json(createJSONResponse(false, err));
			} else {
				console.log(`${req.body.username} has been added to the db`);
				res.json(createJSONResponse(true, result));
			}
		})
	},
	deleteUser: function (req, res) {
		var username = req.body.username;
		var newQuery = "DELETE FROM Users WHERE username = ?";
		con.query(newQuery, username, function (err, result, fields) {
			if (err) {
				console.log(err);
				console.log(`User with username ${username} does not exist`);
				res.json(createJSONResponse(false, err));
			} else {
				console.log(`${username} has been deleted`);
				res.json(createJSONResponse(true, result));
			}
		})
	},
	closedb: function () {
		con.end();
		console.log("Connection to the datbase has been terminated");
	}
};

function createJSONResponse(successVal, messageVal) {
	var response = {
		success: successVal, //true if successful, false if error
		message: messageVal //empty if successful, error message if error
	};
	return response;
}
