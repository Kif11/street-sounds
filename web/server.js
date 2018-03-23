const express = require('express');
const bodyParser = require('body-parser');
const multer = require('multer');
const uuidv4 = require('uuid/v4');
const path = require('path');
// const fs = require('fs');
const utils = require('./utils.js');
const md5File = require('md5-file/promise');
const fs = require("fs.promised/promisify")(require("bluebird"));
const app = express();

const port = process.env.PORT || 8082;
const debug = process.env.DEBUG || false;

const tmpDir = `tmp`;
const storageDir = 'storage';

// TODO: return promice and use it with async!
async function clearDir (dir) {
  try {
    let files = await fs.readdir(dir);
    for (const file of files) {
      await fs.unlink(path.join(dir, file));
    }
  } catch (err) {
    console.log(err);
  }
}

// configure storage
const storage = multer.diskStorage({
  destination: (req, file, cb) => {
    /*
      Files will be saved in the 'uploads' directory. Make
      sure this directory already exists!
    */
    cb(null, tmpDir);
  },
  filename: (req, file, cb) => {
    /*
      uuidv4() will generate a random ID that we'll use for the
      new filename. We use path.extname() to get
      the extension from the original file name and add that to the new
      generated ID. These combined will create the file name used
      to save the file on the server and will be available as
      req.file.pathname in the router handler.
    */
    const newFilename = uuidv4();
    cb(null, newFilename);
  },
});

// create the multer instance that will be used to upload/save the file
const upload = multer({ storage });

app.use(express.static('public'));
app.use(express.static('public/html'));

app.use(bodyParser.json());
app.use(bodyParser.urlencoded({ extended: true }));

app.post('/upload', upload.single('selectedFile'), async (req, res) => {
  /*
    We now have a new req.file object here. At this point the file has been saved
    and the req.file.filename value will be the name returned by the
    filename() function defined in the diskStorage configuration. Other form fields
    are available here in req.body.
  */
  try {

    let uploadedFile = path.join(tmpDir, req.file.filename);
    let convertedMp3 = `./converted/${req.file.filename}.mp3`;
    let finalQrsDir = `./public/qrs`
    let finalImgExt = 'png';
    let finalQr = `${finalQrsDir}/${req.file.filename}.${finalImgExt}`

    // compute md5hash of uploaded file
    let md5hash = await md5File(uploadedFile);
    
    // check if file with this hash already exist
    let storedFiles = await fs.readdir(storageDir);
    if (storedFiles.includes(md5hash)) {
      let msg = `File ${md5hash} already exist!`;
      console.log(msg);
      res.status(500).send({ error: msg });
      return
    }

    let storedFile = path.join(storageDir, md5hash);
    await fs.copyFile(uploadedFile, storedFile, err => {
      if (err) throw err;
    });

    // clear tmp from uploaded files
    await clearDir(tmpDir);

    // compress and convert mp3 into qr sheet
    let out1 = await utils.compressMp3(storedFile, convertedMp3);
    let out2 = await utils.fileToQrs(convertedMp3, tmpDir);
    let out3 = await utils.combineQrs(tmpDir, finalQr);

    if (debug) {
      console.log(out1, out2, out3);
    }

    console.log(`New QR image created ${finalQr}`);

    await clearDir(tmpDir);

    res.send(`${req.file.filename}.${finalImgExt}`);
  
  } catch (e) {
    console.log(e);
    res.status(500).send({ error: e });
  }

});

app.listen(port, () => {
  console.log(`Listening on port ${port}`);
});