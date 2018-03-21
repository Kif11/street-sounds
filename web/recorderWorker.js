/*License (MIT)

Copyright © 2013 Matt Diamond

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated 
documentation files (the "Software"), to deal in the Software without restriction, including without limitation 
the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and 
to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of 
the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO 
THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE 
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF 
CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER 
DEALINGS IN THE SOFTWARE.
*/

self.importScripts('lame.all.js');

var recLength = 0,
  recBuffersL = [],
  recBuffersR = [],
  sampleRate;

this.onmessage = function(e){
  switch(e.data.command){
    case 'init':
      init(e.data.config);
      break;
    case 'record':
      record(e.data.buffer);
      break;
    case 'exportWAV':
      exportWAV(e.data.type);
      break;
    case 'exportMp3':
      exportMp3(e.data.type);
      break;
    case 'exportMonoWAV':
      exportMonoWAV(e.data.type);
      break;
    case 'getBuffers':
      getBuffers();
      break;
    case 'clear':
      clear();
      break;
  }
};

function init(config){
  sampleRate = config.sampleRate;  
}

function record(inputBuffer){
  recBuffersL.push(inputBuffer[0]);
  recBuffersR.push(inputBuffer[1]);
  recLength += inputBuffer[0].length;
}

function exportWAV(type){  
  var bufferL = mergeBuffers(recBuffersL, recLength);
  var bufferR = mergeBuffers(recBuffersR, recLength);
  var interleaved = interleave(bufferL, bufferR);
  var dataview = encodeWAV(interleaved);
  var audioBlob = new Blob([dataview], { type: type });

  this.postMessage(audioBlob);
}

function exportMonoWAV(type){
  var bufferL = mergeBuffers(recBuffersL, recLength);
  var dataview = encodeWAV(bufferL, true);
  var audioBlob = new Blob([dataview], { type: type });

  this.postMessage(audioBlob);
}

function exportMp3(type){
  var bufferL = mergeBuffers(recBuffersL, recLength);
  
  var mp3data = encodeMp3(bufferL);
  var blob = new Blob(mp3data, {type: 'audio/mp3'});
  // var audioBlob = new Blob([dataview], { type: type });

  this.postMessage(blob);
}

function getBuffers() {
  var buffers = [];
  buffers.push( mergeBuffers(recBuffersL, recLength) );
  buffers.push( mergeBuffers(recBuffersR, recLength) );
  this.postMessage(buffers);
}

function clear(){
  recLength = 0;
  recBuffersL = [];
  recBuffersR = [];
}

function mergeBuffers(recBuffers, recLength){
  var result = new Float32Array(recLength);
  var offset = 0;
  for (var i = 0; i < recBuffers.length; i++){
    result.set(recBuffers[i], offset);
    offset += recBuffers[i].length;
  }
  return result;
}

function interleave(inputL, inputR){
  var length = inputL.length + inputR.length;
  var result = new Float32Array(length);

  var index = 0,
    inputIndex = 0;

  while (index < length){
    result[index++] = inputL[inputIndex];
    result[index++] = inputR[inputIndex];
    inputIndex++;
  }
  return result;
}

// function floatTo16BitPCM(output, offset, input){
//   for (var i = 0; i < input.length; i++, offset+=2){
//     var s = Math.max(-1, Math.min(1, input[i]));
//     output.setInt16(offset, s < 0 ? s * 0x8000 : s * 0x7FFF, true);
//   }
// }

function floatTo16BitPCM(input, output) {
  //var offset = 0;
  for (var i = 0; i < input.length; i++) {
    var s = Math.max(-1, Math.min(1, input[i]));
    output[i] = (s < 0 ? s * 0x8000 : s * 0x7FFF);
  }
};

var convertBuffer = function(arrayBuffer){
  var data = new Float32Array(arrayBuffer);
  var out = new Int16Array(arrayBuffer.length);
  floatTo16BitPCM(data, out)
  return out;
};

function writeString(view, offset, string){
  for (var i = 0; i < string.length; i++){
    view.setUint8(offset + i, string.charCodeAt(i));
  }
}

function encodeMp3(samples, mono) {
  // var mp3Data = [];

  // let samplesMono = convertBuffer(samples);

  // var mp3encoder = new lamejs.Mp3Encoder(1, 44100, 128); //mono 44.1khz encode to 128kbps
  // // var samples = new Int16Array(44100); //one second of silence replace that with your own samples
  // var mp3Tmp = mp3encoder.encodeBuffer(samplesMono); //encode mp3

  // //Push encode buffer to mp3Data variable
  // mp3Data.push(mp3Tmp);

  // // Get end part of mp3
  // mp3Tmp = mp3encoder.flush();

  // // Write last data to the output data, too
  // // mp3Data contains now the complete mp3Data
  // mp3Data.push(mp3Tmp);

  // var c = new Int16Array(mp3Data[0].length + mp3Data[1].length);
  // c.set(mp3Data[0]);
  // c.set(mp3Data[1], mp3Data[0].length);

  // var view = new DataView(c.buffer); 

  // return view;

  channels = 1; //1 for mono or 2 for stereo
  sampleRate = 44100; //44.1khz (normal mp3 samplerate)
  kbps = 128; //encode 128kbps mp3
  mp3encoder = new lamejs.Mp3Encoder(channels, sampleRate, kbps);

  // samples = new Int16Array(44100); //one second of silence (get your data from the source you have)
  sampleBlockSize = 1152; //can be anything but make it a multiple of 576 to make encoders life easier

  let samplesMono = convertBuffer(samples);

  var mp3Data = [];
  for (var i = 0; i < samplesMono.length; i += sampleBlockSize) {
    sampleChunk = samplesMono.subarray(i, i + sampleBlockSize);
    var mp3buf = mp3encoder.encodeBuffer(sampleChunk);
    if (mp3buf.length > 0) {
        mp3Data.push(mp3buf);
    }
  }
  var mp3buf = mp3encoder.flush();   //finish writing mp3

  if (mp3buf.length > 0) {
      mp3Data.push(new Int8Array(mp3buf));
  }

  return mp3Data;
}

function encodeWAV(samples, mono){
  var buffer = new ArrayBuffer(44 + samples.length * 2);
  var view = new DataView(buffer);

  /* RIFF identifier */
  writeString(view, 0, 'RIFF');
  /* file length */
  view.setUint32(4, 32 + samples.length * 2, true);
  /* RIFF type */
  writeString(view, 8, 'WAVE');
  /* format chunk identifier */
  writeString(view, 12, 'fmt ');
  /* format chunk length */
  view.setUint32(16, 16, true);
  /* sample format (raw) */
  view.setUint16(20, 1, true);
  /* channel count */
  view.setUint16(22, mono?1:2, true);
  /* sample rate */
  view.setUint32(24, sampleRate, true);
  /* byte rate (sample rate * block align) */
  view.setUint32(28, sampleRate * 4, true);
  /* block align (channel count * bytes per sample) */
  view.setUint16(32, 4, true);
  /* bits per sample */
  view.setUint16(34, 16, true);
  /* data chunk identifier */
  writeString(view, 36, 'data');
  /* data chunk length */
  view.setUint32(40, samples.length * 2, true);

  floatTo16BitPCM(view, 44, samples);

  return view;
}
