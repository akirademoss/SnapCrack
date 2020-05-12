const express = require('express');
const multer = require('multer');
const ejs = require('ejs');
const path = require('path');

// Set Storage Engine
const storage = multer.diskStorage({
  destination: './public/uploads/',
  filename: function(req, file, cb){
  cb(null,file.fieldname + '-' + Date.now() +
  path.extname(file.originalname));
  }
});

// Init Upload 
const upload = multer({
  storage: storage
}).single('myImage');

// Init app
const app = express();

// EJS 
app.set('view engine', 'ejs');

// Public Folder
app.use(express.static('./public'));

app.get('/', (req, res) => res.render('index'));

app.post('/upload', (req, res) => {
  //res.send('Test submit: OK!');
  upload(req, res, (err) => {
  if(err){
    res.render('index', {
      msg: err
    }); 
  } else{
   // console.log(req.file);
   // res.send('Image has been uploaded to server!!!');
   if(req.file == undefined){
     res.render('index', {
       msg: 'Error: No File Selected!'
     });
   }else{
     res.render('index',{
       msg: 'File Uploaded!', 
       file: `uploads/${req.file.filename}`
     });
    }  
  }
 });
});

const port = 3000;

app.listen(port, () => console.log(`Server started on port ${port}`));

 
