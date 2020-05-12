/*
* Creates a new Users table if not already created
 */

var mysql = require('mysql');
var con = mysql.createConnection({
	host: 'snapcrack.ece.iastate.edu',
	user: 'sdmay20-18',
	password: 'sd18',
	database: 'Snapcrack',
	port: '/var/run/mysqld/mysqld.sock'
});

con.connect(function(err) {
	if (err) throw err;
	console.log("Connected to server!");
	var sqlCreate = "CREATE TABLE if not exists Users(id INT AUTO_INCREMENT PRIMARY KEY, username VARCHAR(255), password VARCHAR(255))";
	con.query(sqlCreate, function (err, result) {
		if (err) {
			console.log(err);
			throw err;
		} else {
			console.log("Table successfully created!");
		}

        con.end(function(err) {
         if (err) {
           return console.log(err.message);
         } else{
           console.log("Close connection!");
         }
       });
	});
});
