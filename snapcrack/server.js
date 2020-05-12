require('dotenv').config();
//All of the modules that we will require
const expressUse = require("express");        //Express for talking between the node and mysql and html + css
const mysql = require('mysql');               //NodeJs plug for talking to the mysql database and node
const app = expressUse();                     //Setting up the app
const bcryptUse = require("bcrypt");          //Encryption for passwords
const passport = require("passport");         //Used for authentication
const flash = require("express-flash");       //For flashing warnings
const session = require("express-session");   //Session management for login/logout
const path = require('path');
const https = require('https');
const requestHTML = require("request");

//Self made stuff to import
const initializePassport = require("./passportConfiguration");      //This is the passport configuration
initializePassport(
    passport
);

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


function windIntUpdate(input){
    if(input == null)
    {
        return 0;
    }
    return(input);
}
//Port that we listen to?
app.listen(process.env.PORT);

//View setup, and template setup
app.set('view-engine', 'ejs');
app.use(expressUse.urlencoded({extended: false}));

//Configuring flash and session with our secret key
app.use(flash());
app.use(session(
    {
        secret: process.env.SECRET,
        resave: false,
        saveUninitialized: false
    }
));

//Initializing passport and its session
app.use(passport.initialize());
app.use(passport.session());

app.use(expressUse.static(path.join(__dirname, '/public')));

/**
 * Main page
 * Access is: /
 */
app.get('/', (request, response) => {
    response.render("index.ejs");
});

/**
 * Login Page for User
 * Access is: /login
 * NOTE: This only server the page with a get request, does not process anything with the page
 */
app.get('/login', checkIfNotAuth, (request, response) => {
    response.render("login.ejs");
});

/**
 * After the user hits login on the page
 */
app.post('/login', checkIfNotAuth , passport.authenticate('local' , {
    successRedirect: '/dashboard',
    failureRedirect: '/login',
    failureFlash: true
}));

/**
 * Create Account page
 * Access is: /create
 * NOTE: This only serves the page with a get request, does not process anything with the page
 */
app.get('/create', checkIfNotAuth, (request, response) => {
    response.render("register.ejs");
});


/**
 * After the user hits the create account button
 * Access is: /create with a post request
 */
app.post('/create', checkIfNotAuth , async (request, response) => {
    var user;

    if(request.body.samePassword == request.body.password)
    {
        try {


            //Lets encrypt the password using bcrypt, this is the reason why you have to have it be async
            const hashedPassword = await bcryptUse.hash(request.body.password, 10);
            user = {
                username: request.body.username,
                password: hashedPassword
            };
        }
        catch {
            //If for some reason we had a fail, send the user back to the /create page
            response.redirect('/create');
        }

        var query = "SELECT * FROM users WHERE username = '" + user.username + "'";
        connectionSQL.query(query, async function (err, result) {
            if(err) throw err;

            if(result[0])
            {
                let messages = {
                    error: "There is already an account with this username!"
                }
                response.render("register.ejs", {messages: messages});
            }
            else
            {
                var sql = "INSERT INTO users (username,password) VALUES ('" + user.username + "', '" + user.password + "')";

                connectionSQL.query(sql, function (err, result) {
                    if(err) throw err;
                });
                response.redirect("/login");
            }
        });
    }
    else {
        let messages = {
            error: "Passwords do not match!"
        }
        response.render("register.ejs", {messages: messages});
    }

});

/**
 * Page that user is dumped on after login
 * Access is: /dashboard
 */
app.get('/dashboard', checkIfAuthed ,(request, response) => {

    var sql = "SELECT * FROM cracks WHERE id = '" + request.id+ "'";
    connectionSQL.query(sql, function(err, result) {
        if(err) throw err;
        response.render("dashboard.ejs", {crackList: result});
    });

});

/**
 * How to access the cracks
 * Access is: /crack/:id
 */
app.get('/crack/:id',(request, response) => {

    var sql = "SELECT * FROM cracks WHERE id = '" + request.params.id + "'";
    connectionSQL.query(sql, function(err, result) {
        if(err) throw err;

        var preferences;
        var displayPref = result;

        if(displayPref.length == 1)
        {
            var sql = "SELECT * FROM location WHERE location = '" + displayPref[0].zipcode + "'";
            connectionSQL.query(sql, function(err, result) {
                if (err) throw err;

                var zipData = result;

                /**
                 * Getting the alerts
                 */
                let options = {
                    url: 'https://maps.google.com' + '/alerts/active?point=' + zipData[0].latitude + ',' + zipData[0].longitude,
                    headers: {
                        'User-Agent': 'test'
                    }
                };
                console.log("Accessing: " + options.url);
                //Alerts
                requestHTML(options, (err,res,body) => {

                    const alertsObj = JSON.parse(body);
                    console.log("Have an account already?" + JSON.stringify(alertsObj, null, 2));

                    /**
                     * Getting the urls for the google API
                     */

                    let options = {
                        url: 'https://maps.google.com' + '/points/' + zipData[0].latitude + ',' + zipData[0].longitude,
                        headers: {
                            'User-Agent': 'test'
                        }
                    };

                    console.log("Accessing: " + options.url);
                    //Main!
                    requestHTML(options, (err,res,body) => {

                        const mainBody = JSON.parse(body);
                        console.log("Body of Main Call: " + JSON.stringify(id, null, 2));

                        let options = {
                            url: mainBody.properties.forecast,
                            headers: {
                            'User-Agent': 'test'
                        } };

                        console.log("Accessing: " + options.url);

                        requestHTML(options, (err,res,body) => {

                            const forcast = JSON.parse(body);
                            console.log("Body of Crack Call: " + JSON.stringify(crack, null, 2));

                            let options = {
                                url: mainBody.properties.observationStations,
                                headers: {
                                    'User-Agent': 'test'
                                } };

                            console.log("Accessing: " + options.url);

                            requestHTML(options, (err,res,body) => {

                                const sortedCrackList = JSON.parse(body);
                                console.log("Crack List: " + JSON.stringify(crackList, null, 2));

                                if (stationListBody.features == 0) {

                                } else {

                                    let options = {
                                        url: crackList.features[0].id + "/observations/latest",
                                        headers: {
                                            'User-Agent': 'test'
                                        }
                                    };

                                    console.log("Accessing: " + options.url);

                                    requestHTML(options, (err, res, body) => {

                                        const currentStationBody = JSON.parse(body);
                                        console.log("" + JSON.stringify(currentcrack, null, 2));

                                        //Fill data and render
                                        preferences = {
                                            id: crackList[0].id,
                                            Location: crackList[0].location,
                                            Date: crackList[0].date,
                                            Tags: crackList[0].tag,

                                        };
                                        //Date Processing
                                        let today = new Date();
                                        let days = ['Sunday', 'Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday'];
                                        let months = ['January', 'February', 'March', 'April', 'May', 'June', 'July', 'August', 'September', 'October', 'November', 'December'];


                                        let data = {
                                            month: months[today.getMonth()],
                                            day: days[today.getDay()],
                                            date: today.getDate(),
                                            hour: today.getHours(),
                                            minute: today.getMinutes(),
                                            timeEnd: "",
                                            city: result[0].city,
                                            state: result[0].state,
                                            fullstate: abbToState(result[0].state),
                                            zipcode: displayPref[0].zip,
                                        };
                                        //Time


                                        //Date Processing
                                        if (data.minute <= 9) {
                                            data.minute = "0" + data.minute;
                                        }
                                        if (data.hour < 12) {
                                            data.timeEnd = "am";

                                        } else {
                                            data.timeEnd = "pm";
                                            data.hour = data.hour - 12;
                                        }

                                        console.log("snapcrack");

                                        response.render("dashboard.ejs", {preferences: preferences, data: data});

                                    });
                                }
                            });
                        });
                    });
                });
            });
        }
        else {
            preferences = 0;
        }

    });

});


/**
 * Crack List
 * Access is: /crack-details
 * NOTE: This only server the page with a get request, does not process anything with the page
 */
app.get('/crack-details', checkIfAuthed, (request, response) =>
{
    response.render("crackList");
});

/**
 * After the user hits edit tags button
 * Access is: /edit-tags with a post request
 */
app.post('/edit-tags', checkIfAuthed, (request, response) =>
{
    let sql = "INSERT INTO tags (any) VALUES ('" + request.body.description + "')";

    connectionSQL.query(sql, function (err, result) {
       if(err) throw err;
    });
    response.redirect("/dashboard");
});

/**
 * After the user hits the remove crack submit button
 * Access is: /remove-crack with a post request
 */
app.post('/remove-crack', checkIfAuthed, (request, response) =>
{
    let sql = "DELETE FROM cracks WHERE id= '" + request.body.new +"'";
    connectionSQL.query(sql, function (err, result) {
        if(err) throw err;
    });
    console.log(request.body.new);
    response.redirect("/dashboard");
});

/**
 * resets the dashboard for updates
 */
app.post('/reset-display', checkIfAuthed, (request, response) =>
{
    response.redirect("/dashboard");
});

/**
 * When the user clicks logout
 */
app.get('/logout', (request, response) => {
   request.logOut();
   response.redirect('/');
});

/**
 * Is the user authed??
 */
function checkIfAuthed(request, response, nextThing) {
    if(request.isAuthenticated())
    {
        return(nextThing());
    }
    else
    {
        response.redirect('/login');
    }
};

/**
 * Is the user not authed??
 */
function checkIfNotAuth(request, response, nextThing) {
    if(request.isAuthenticated()) {
        return(response.redirect("/dashboard"));
    }
    else {
        return(nextThing());
    }
};



