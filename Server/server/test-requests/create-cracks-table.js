/*
* Creates a new Cracks table if not already created
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
	var sqlCreate = "CREATE TABLE if not exists Cracks(id INT AUTO_INCREMENT PRIMARY KEY, type VARCHAR(255), imageName VARCHAR(255))";
	con.query(sqlCreate, function(err, result) {
		if (err) {
			console.log(err);
			throw err;
		} else {
			console.log("Table successfully created");
		}
	con.end(function(err) {
		if (err) {
			console.log(err.message);
		} else {
			console.log("Close connection!");
		}
	});
	});
});
