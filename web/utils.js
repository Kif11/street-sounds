const exec = require('node-exec-promise').exec;

const debug = process.env.DEBUG || false;

function compressMp3(inputFile, outputFile) {
  let cmd = `ffmpeg -i ${inputFile} -codec:a libmp3lame -b:a 8k -ar 8000 ${outputFile}`
  if (debug) console.log('Running command: ', cmd);
  return exec(cmd);
}

function fileToQrs(inputFile, outputDir) {
  let cmd = `multiqr encode ${inputFile} ${outputDir}`;
  if (debug) console.log('Running command: ', cmd);  
  return exec(cmd);  
}

function combineQrs(qrDir, outputDir, footerFile) {
  footerFile = footerFile || "";
  let cmd = `tile ${qrDir} ${outputDir} ${footerFile}`;
  if (debug) console.log('Running command: ', cmd);  
  return exec(cmd);
}

module.exports = {compressMp3, fileToQrs, combineQrs};
