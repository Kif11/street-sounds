window.AudioContext = window.AudioContext || window.webkitAudioContext;
let audioContext = new AudioContext();
let audioRecorder = null;
let analyserContext = null;
let player = document.getElementById('player');
let isRecording = false;
let recIndex = 0;
let recordBtnText = 'RECORD';
let recordDuration = 8; // seconds
let curRecordingData = null;

$('#recordBtn').click(event => {

  if (isRecording)
    return;

  // start recording
  $('#recordBtn').attr('recording', 'true');
  isRecording = true;
  audioRecorder.clear();
  audioRecorder.record();

  let counter = recordDuration;
  $('#recordBtn p').text("");
  
  let intervalId = setInterval(() => {
    counter--;      
    $('#recordBtn p').text("");
  }, 1000);

  setTimeout(intervalId => {
    if (!audioRecorder)
      return;
    // stop recording
    audioRecorder.stop();
    
    isRecording = false;
    $('#recordBtn').attr('recording', 'false');
    
    audioRecorder.exportMp3( doneEncoding );
    
    clearInterval(intervalId);
    $('#recordBtn p').text(recordBtnText);
  }, recordDuration * 1000, intervalId);
})

$('#saveQrBtn').click(event => {
  
  let formData = new FormData();

  // formData.append('description', description);
  formData.append('selectedFile', curRecordingData);

  axios.post('/upload', formData).then((result) => {
    result.data.forEach(imgPath => {      
      let img = $('<img id="qrImg">');
      img.attr('src', imgPath);
      img.appendTo('#finalQrs');
    });
  }).catch(err => {
    console.log(err);
  });

});

function updateAnalysers(time) {
  if (!analyserContext) {
      var canvas = document.getElementById("analyser");
      canvasWidth = canvas.width;
      canvasHeight = canvas.height;
      analyserContext = canvas.getContext('2d');
  }

  // analyzer draw code here
  {
      var SPACING = 1;
      var BAR_WIDTH = 1;
      var numBars = Math.round(canvasWidth / SPACING);
      var freqByteData = new Uint8Array(analyserNode.frequencyBinCount);

      analyserNode.getByteFrequencyData(freqByteData); 

      analyserContext.clearRect(0, 0, canvasWidth, canvasHeight);
      analyserContext.fillStyle = 'black';
      // analyserContext.lineCap = 'round';
      var multiplier = analyserNode.frequencyBinCount / numBars;

      // Draw rectangle for each frequency bin.
      for (var i = 0; i < numBars; ++i) {
          var magnitude = 0;
          var offset = Math.floor( i * multiplier );
          // gotta sum/average the block, or we miss narrow-bandwidth spikes
          for (var j = 0; j< multiplier; j++)
              magnitude += freqByteData[offset + j];
          magnitude = magnitude / multiplier;
          var magnitude2 = freqByteData[i * multiplier];
          // analyserContext.fillStyle = "hsl( " + Math.round((i*360)/numBars) + ", 100%, 50%)";
          analyserContext.fillRect(i * SPACING, canvasHeight, BAR_WIDTH, -magnitude);
      }
  }
  
  rafID = window.requestAnimationFrame( updateAnalysers );
}

navigator.mediaDevices.getUserMedia({audio: true, video: false}).then(stream => {
  inputPoint = audioContext.createGain();

  // Create an AudioNode from the stream.
  realAudioInput = audioContext.createMediaStreamSource(stream);
  audioInput = realAudioInput;
  audioInput.connect(inputPoint);

  analyserNode = audioContext.createAnalyser();
  analyserNode.fftSize = 2048;
  inputPoint.connect( analyserNode );

  audioRecorder = new Recorder( inputPoint );

  zeroGain = audioContext.createGain();
  zeroGain.gain.value = 0.0;
  inputPoint.connect( zeroGain );
  zeroGain.connect( audioContext.destination );
  updateAnalysers();
});

function doneEncoding(blob) {
  player.src = window.URL.createObjectURL(blob);

  curRecordingData = blob;
  $('#saveQrBtn').prop('disabled', false);
  
  Recorder.setupDownload( blob, "myRecording" + ((recIndex<10)?"0":"") + recIndex + ".wav" );
  recIndex++;
}
