const exec = require('node-exec-promise').exec;

function compressMp3(inputFile, outputFile) {
  let cmd = `ffmpeg -i ${inputFile} -codec:a libmp3lame -b:a 8k -ar 8000 ${outputFile}`
  return exec(cmd);
}

function fileToQrs(inputFile, outputDir) {
  let cmd = `multiqr encode ${inputFile} ${outputDir}`;
  return exec(cmd);  
}

function combineQrs(qrDir, outputFile) {
  let cmd = `tile ${qrDir} ${outputFile}`;
  return exec(cmd);
}

module.exports = {compressMp3, fileToQrs, combineQrs};
