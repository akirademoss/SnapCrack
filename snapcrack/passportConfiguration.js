const LocalStrategy = require('passport-local').Strategy;
const bcrypt = require('bcrypt');
const mysql = require('mysql');

/**
 * This is the SQL connection.
 * @type {Connection}
 */
var connectionSQL = mysql.createConnection({
    host: "snapcrack.ece.iastate.edu",
    user: "vm-user",
    password: "password123"
});

/**
 * Will create a connection that will use the project database
 */
connectionSQL.connect(function(err) {
    if (err) throw err;
    connectionSQL.query("USE Snapcrack");
});

/**
 *
 * @param passport      - The passport const var used
 * @param getByUsername - The user retrieved by there user name
 */
function initialize(passport)
{
    /**
     * This will auth the user if there username is found, and there password matches
     * @param username  - user name to check
     * @param password  - password to check with
     * @param done      - if the process is done
     * @returns {Promise<*>}
     */
    const authenticateUser = async (username, password, done) => {

        var query = "SELECT * FROM users WHERE username = '" + username + "'";

        connectionSQL.query(query, async function (err, result) {
            if (err) throw err;

            if (result.length === 0) {
                return done(null, false, {message: "No user with that username found!"})
            }

            try {
                if (await bcrypt.compare(password, result[0].password)) {
                    return done(null, result[0]);
                }
                else {
                    return done(null, false, {message: "Incorrect Password"});
                }
            } catch(e) {
                return done();
            }
        });
    };

    passport.use(new LocalStrategy ({usernameField: 'username',
                                    passwordField: 'password' }, authenticateUser));

    passport.serializeUser((user, done) => { done(null, user.id) });
    passport.deserializeUser((id, done) => {
        var query = "SELECT * FROM users WHERE id = '" + id + "'";
        connectionSQL.query(query, function(err, result)
        {
            if (err) throw err;
            done(err,result[0]);
        })
    });
}

module.exports = initialize;